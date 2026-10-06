import CanvasKitInit from 'canvaskit-wasm';
import wasmUrl from 'canvaskit-wasm/bin/canvaskit.wasm?url';
import type { CanvasKit, ImageInfo, MallocObj, Surface } from 'canvaskit-wasm';
import { RuntimeError } from '../types';
import type { FrameSize, FrameSource, FrameSourceContext, SkeletonSummary, SpineRuntimePack } from '../types';
import { createFileReader } from '../runtimes/files';
import { validateSkeletonData } from '../runtimes/validate';
import { nonDefaultSkinNames } from './skins';

let ckPromise: Promise<CanvasKit> | null = null;

export function loadCanvasKit(): Promise<CanvasKit> {
  ckPromise ??= CanvasKitInit({ locateFile: () => wasmUrl });
  return ckPromise;
}

/**
 * 自动取景基准参数。sy = yDown ? -1 : 1，用于在 setTransform 中推导世界坐标偏移。
 * baseX/baseY 是自动取景时 skeleton.x/y 的值（无用户缩放/偏移）。
 */
interface FitParams {
  hasBounds: boolean;
  dataCenterX: number;
  dataCenterY: number;
  scale: number;
  /** yDown ? -1 : 1 */
  sy: number;
  baseX: number;
  baseY: number;
}

function computeFit(data: any, width: number, height: number, yDown: boolean): FitParams {
  const sy = yDown ? -1 : 1;
  const hasBounds = data.width > 0 && data.height > 0;
  if (!hasBounds) {
    return { hasBounds: false, dataCenterX: 0, dataCenterY: 0, scale: 1, sy, baseX: width / 2, baseY: height / 2 };
  }
  const scale = Math.min(width / data.width, height / data.height) * 0.9;
  const dataCenterX = data.x + data.width / 2;
  const dataCenterY = data.y + data.height / 2;
  // 原始 fitSkeleton: skeleton.x = w/2 - dcX*scale; skeleton.y = h/2 + (yDown?1:-1)*dcY*scale
  const baseX = width / 2 - dataCenterX * scale;
  const baseY = height / 2 + (yDown ? 1 : -1) * dataCenterY * scale;
  return { hasBounds, dataCenterX, dataCenterY, scale, sy, baseX, baseY };
}

export class CanvaskitFrameSource implements FrameSource {
  readonly backend = 'canvaskit' as const;

  private readonly ck: CanvasKit;
  private readonly surface: Surface;
  private readonly renderer: { render(canvas: any, skeleton: any): void };
  private readonly drawable: any;
  private readonly atlas: any;
  private readonly imageInfo: ImageInfo;
  private readonly pixels: MallocObj;
  private readonly pixelView: Uint8ClampedArray<ArrayBuffer>;
  private readonly size: FrameSize;
  private readonly summaryInfo: SkeletonSummary;
  private readonly animationNames: string[];
  private readonly skinNames: string[];
  private readonly durations: Record<string, number>;
  private readonly fit: FitParams;
  private lastMs = -1;
  private disposed = false;

  private constructor(args: {
    ck: CanvasKit;
    surface: Surface;
    renderer: { render(canvas: any, skeleton: any): void };
    drawable: any;
    atlas: any;
    size: FrameSize;
    summary: SkeletonSummary;
    animations: string[];
    skins: string[];
    durations: Record<string, number>;
    fit: FitParams;
  }) {
    this.ck = args.ck;
    this.surface = args.surface;
    this.renderer = args.renderer;
    this.drawable = args.drawable;
    this.atlas = args.atlas;
    this.size = args.size;
    this.summaryInfo = args.summary;
    this.animationNames = args.animations;
    this.skinNames = args.skins;
    this.durations = args.durations;
    this.fit = args.fit;
    this.imageInfo = {
      width: args.size.width,
      height: args.size.height,
      colorType: args.ck.ColorType.RGBA_8888,
      alphaType: args.ck.AlphaType.Unpremul,
      colorSpace: args.ck.ColorSpace.SRGB,
    };
    this.pixels = args.ck.Malloc(Uint8Array, args.size.width * args.size.height * 4);
    this.pixelView = new Uint8ClampedArray(args.size.width * args.size.height * 4);
  }

  static async create(pack: SpineRuntimePack, context: FrameSourceContext): Promise<CanvaskitFrameSource> {
    const helpers = pack.core;
    const ck = await loadCanvasKit();
    const size = { width: context.width, height: context.height };
    const canvas = new OffscreenCanvas(size.width, size.height);

    let surface: Surface | null = null;
    try {
      surface = ck.MakeWebGLCanvasSurface(canvas as unknown as HTMLCanvasElement);
    } catch {
      surface = null;
    }
    surface ??= ck.MakeSurface(size.width, size.height);
    if (!surface) throw new RuntimeError('backendUnavailable');

    const readFile = createFileReader(context.files);
    let data: any;
    let summary: SkeletonSummary;
    let atlas: any;
    try {
      atlas = await helpers.loadTextureAtlas(ck, context.atlasFile, readFile);
      data = await helpers.loadSkeletonData(context.skeletonFile, atlas, readFile);
      summary = validateSkeletonData(data, context.version);
    } catch (error) {
      atlas?.dispose?.();
      surface.delete();
      if (error instanceof RuntimeError) throw error;
      throw new RuntimeError('parseInvalid', error instanceof Error ? error.message : String(error));
    }

    const animations: string[] = (data.animations ?? []).map((item: any) => String(item.name));
    if (animations.length === 0) {
      atlas.dispose?.();
      surface.delete();
      throw new RuntimeError('noAnimation');
    }
    const skins = nonDefaultSkinNames(data);
    const durations: Record<string, number> = {};
    for (const item of data.animations ?? []) durations[String(item.name)] = Number(item.duration) || 0;

    const drawable = new helpers.SkeletonDrawable(data);
    const fit = computeFit(data, size.width, size.height, pack.capabilities.yDown);
    applyFit(drawable.skeleton, fit);
    drawable.animationState.setAnimation(0, animations[0], true);

    return new CanvaskitFrameSource({
      ck, surface, renderer: new helpers.SkeletonRenderer(ck),
      drawable, atlas, size, summary, animations, skins, durations, fit,
    });
  }

  private applyTransform(offsetX: number, offsetY: number, userScale: number) {
    const { fit } = this;
    const sk = this.drawable.skeleton;
    // 偏移按屏幕坐标（+x 右、+y 上）；画布 y 朝下，故画布偏移 = (offsetX, -offsetY)
    if (fit.hasBounds) {
      sk.scaleX = fit.scale * userScale;
      sk.scaleY = fit.scale * userScale;
      sk.x = fit.baseX + offsetX + fit.dataCenterX * fit.scale * (1 - userScale);
      sk.y = fit.baseY - offsetY + fit.sy * fit.dataCenterY * fit.scale * (1 - userScale);
    } else {
      sk.x = fit.baseX + offsetX;
      sk.y = fit.baseY - offsetY;
    }
  }

  summary(): SkeletonSummary {
    return this.summaryInfo;
  }

  animations(): string[] {
    return this.animationNames;
  }

  skins(): string[] {
    return this.skinNames.length > 0 ? this.skinNames : [''];
  }

  animationDurations(): Record<string, number> {
    return this.durations;
  }

  setSkin(name: string): void {
    const sk = this.drawable.skeleton;
    if (!name) {
      sk.setSkin(sk.data?.defaultSkin ?? null);
      return;
    }
    sk.setSkinByName(name);
  }

  setAnimation(name: string, loop: boolean): void {
    const state = this.drawable.animationState;
    state.setAnimation(0, name, loop);
    const entry = state.getCurrent(0);
    if (entry) {
      entry.trackTime = 0;
      entry.animationLast = -1;
    }
    this.lastMs = -1;
  }

  seek(timeMs: number): void {
    const state = this.drawable.animationState;
    const entry = state.getCurrent(0);
    if (entry) {
      entry.trackTime = timeMs / 1000;
      entry.animationLast = -1;
    }
    this.lastMs = timeMs;
  }

  setTransform(offsetX: number, offsetY: number, scale: number): void {
    this.applyTransform(offsetX, offsetY, scale);
  }

  async render(timeMs: number): Promise<ImageBitmap> {
    if (this.disposed) throw new RuntimeError('notLoaded');
    const delta = this.lastMs < 0 ? 0 : Math.max(0, (timeMs - this.lastMs) / 1000);
    this.lastMs = timeMs;
    this.drawable.update(delta);

    const canvas = this.surface.getCanvas();
    canvas.clear(this.ck.TRANSPARENT);
    this.renderer.render(canvas, this.drawable);
    this.surface.flush();
    canvas.readPixels(0, 0, this.imageInfo, this.pixels);

    this.pixelView.set(pixelsView(this.pixels));
    return createImageBitmap(new ImageData(this.pixelView, this.imageInfo.width, this.imageInfo.height));
  }

  getSize(): FrameSize {
    return this.size;
  }

  dispose(): void {
    if (this.disposed) return;
    this.disposed = true;
    try {
      this.drawable?.animationState?.dispose?.();
      this.atlas?.dispose?.();
    } catch {
      // 忽略释放失败
    }
    this.ck.Free(this.pixels);
    this.surface.delete();
  }
}

function applyFit(skeleton: any, fit: FitParams) {
  if (fit.hasBounds) {
    skeleton.scaleX = fit.scale;
    skeleton.scaleY = fit.scale;
    skeleton.x = fit.baseX;
    skeleton.y = fit.baseY;
  } else {
    skeleton.x = fit.baseX;
    skeleton.y = fit.baseY;
  }
}

function pixelsView(pixels: MallocObj): Uint8ClampedArray<ArrayBuffer> {
  const view = pixels.toTypedArray() as unknown as Uint8Array<ArrayBuffer>;
  return new Uint8ClampedArray(view.buffer, view.byteOffset, view.length);
}
