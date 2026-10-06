import CanvasKitInit from 'canvaskit-wasm';
import wasmUrl from 'canvaskit-wasm/bin/canvaskit.wasm?url';
import type { CanvasKit, ImageInfo, MallocObj, Surface } from 'canvaskit-wasm';
import { RuntimeError } from '../types';
import type { FrameSize, FrameSource, FrameSourceContext, SkeletonSummary, SpineRuntimePack } from '../types';
import { createFileReader } from '../runtimes/files';
import { validateSkeletonData } from '../runtimes/validate';
import { decodeStraightPages, findStraightPage, type StraightPage } from '../alpha';
import { nonDefaultSkinNames } from './skins';
import { track0, writeTrackTime } from './track';

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
  private readonly canvas: OffscreenCanvas;
  private readonly surface: Surface;
  private readonly glSurface: boolean;
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
    canvas: OffscreenCanvas;
    surface: Surface;
    glSurface: boolean;
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
    this.canvas = args.canvas;
    this.surface = args.surface;
    this.glSurface = args.glSurface;
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
      surface = ck.MakeWebGLCanvasSurface(canvas as unknown as HTMLCanvasElement) ?? null;
    } catch {
      surface = null;
    }
    // 预览快路径只有 GPU surface 能 transferToImageBitmap；CPU 光栅 surface 仍走 readPixels
    const glSurface = surface !== null;
    surface ??= ck.MakeSurface(size.width, size.height);
    if (!surface) throw new RuntimeError('backendUnavailable');

    const readFile = createFileReader(context.files);
    let data: any;
    let summary: SkeletonSummary;
    let atlas: any;
    try {
      const atlasText = new TextDecoder().decode(context.files[context.atlasFile]).replace(/\r\n/g, '\n');
      const pages = await decodeStraightPages(context.files, atlasText);
      atlas = new helpers.TextureAtlas(atlasText);
      for (const page of atlas.pages) {
        const pageData = findStraightPage(pages, page.name);
        if (!pageData) throw new RuntimeError('missingFile', page.name);
        page.setTexture(makePageTexture(ck, helpers, pageData));
      }
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
      ck, canvas, surface, glSurface, renderer: new helpers.SkeletonRenderer(ck),
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
    writeTrackTime(track0(state), 0);
    this.lastMs = -1;
  }

  seek(timeMs: number): void {
    const state = this.drawable.animationState;
    writeTrackTime(track0(state), timeMs / 1000);
    this.lastMs = timeMs;
  }

  setTransform(offsetX: number, offsetY: number, scale: number): void {
    this.applyTransform(offsetX, offsetY, scale);
  }

  private advance(timeMs: number) {
    const delta = this.lastMs < 0 ? 0 : Math.max(0, (timeMs - this.lastMs) / 1000);
    this.lastMs = timeMs;
    this.drawable.update(delta);

    const canvas = this.surface.getCanvas();
    canvas.clear(this.ck.TRANSPARENT);
    this.renderer.render(canvas, this.drawable);
    this.surface.flush();
  }

  async render(timeMs: number): Promise<ImageBitmap> {
    if (this.disposed) throw new RuntimeError('notLoaded');
    this.advance(timeMs);
    this.surface.getCanvas().readPixels(0, 0, this.imageInfo, this.pixels);
    this.pixelView.set(pixelsView(this.pixels));
    return createImageBitmap(new ImageData(this.pixelView, this.imageInfo.width, this.imageInfo.height));
  }

  // 预览不过 CPU：transferToImageBitmap 留在 GPU 上，省掉 4MB readPixels + 两次全幅拷贝
  async renderPreview(timeMs: number): Promise<ImageBitmap> {
    if (this.disposed) throw new RuntimeError('notLoaded');
    if (!this.glSurface) return this.render(timeMs);
    this.advance(timeMs);
    return this.canvas.transferToImageBitmap();
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

/**
 * 官方 loadTextureAtlas 用 MakeImageFromEncoded，Skia 把 PNG 里的 RGB 当直通 alpha，
 * 物理预乘的贴图页会被再乘一次，接缝出现暗边；这里在解码期已归一化成直通像素，
 * 用 MakeImage(Unpremul) 重建图像，paint 组合与官方 CanvasKitTexture 保持一致。
 */
function makePageTexture(ck: CanvasKit, helpers: any, page: StraightPage): any {
  const image = ck.MakeImage(
    {
      width: page.width,
      height: page.height,
      colorType: ck.ColorType.RGBA_8888,
      alphaType: ck.AlphaType.Unpremul,
      colorSpace: ck.ColorSpace.SRGB,
    },
    page.data,
    page.width * 4,
  );
  if (!image) throw new RuntimeError('parseInvalid', 'atlas page decode failed');
  const paintPerBlendMode = new Map<number, any>();
  const shaders: any[] = [];
  // spine.BlendMode → Skia 混合模式，与官方 toCkBlendMode 相同（multiply 在顶点色路径下退化回 SrcOver）
  const blendOf = (mode: number) =>
    mode === 1 ? ck.BlendMode.Plus : mode === 3 ? ck.BlendMode.Screen : ck.BlendMode.SrcOver;
  for (const mode of [0, 1, 2, 3]) {
    const shader = image.makeShaderOptions(ck.TileMode.Clamp, ck.TileMode.Clamp, ck.FilterMode.Linear, ck.MipmapMode.Linear);
    const paint = new ck.Paint();
    paint.setShader(shader);
    paint.setBlendMode(blendOf(mode));
    paintPerBlendMode.set(mode, paint);
    shaders.push(shader);
  }
  const texture = new helpers.Texture({ shaders, paintPerBlendMode, image });
  texture.setFilters = () => {};
  texture.setWraps = () => {};
  texture.dispose = () => {
    for (const paint of paintPerBlendMode.values()) paint.delete();
    for (const shader of shaders) shader.delete();
    image.delete();
  };
  return texture;
}

function pixelsView(pixels: MallocObj): Uint8ClampedArray<ArrayBuffer> {
  const view = pixels.toTypedArray() as unknown as Uint8Array<ArrayBuffer>;
  return new Uint8ClampedArray(view.buffer, view.byteOffset, view.length);
}
