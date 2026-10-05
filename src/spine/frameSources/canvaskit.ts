import CanvasKitInit from 'canvaskit-wasm';
import wasmUrl from 'canvaskit-wasm/bin/canvaskit.wasm?url';
import type { CanvasKit, ImageInfo, MallocObj, Surface } from 'canvaskit-wasm';
import { RuntimeError } from '../types';
import type { FrameSize, FrameSource, FrameSourceContext, SkeletonSummary, SpineRuntimePack } from '../types';
import { createFileReader } from '../runtimes/files';
import { validateSkeletonData } from '../runtimes/validate';

let ckPromise: Promise<CanvasKit> | null = null;

export function loadCanvasKit(): Promise<CanvasKit> {
  ckPromise ??= CanvasKitInit({ locateFile: () => wasmUrl });
  return ckPromise;
}

/** 官方 spine-canvaskit 把 Skeleton.yDown 置为 true，world 的 +y 对应屏幕向上。 */
function fitSkeleton(skeleton: any, data: any, width: number, height: number, yDown: boolean) {
  if (!(data.width > 0) || !(data.height > 0)) {
    skeleton.x = width / 2;
    skeleton.y = height / 2;
    return;
  }
  const scale = Math.min(width / data.width, height / data.height) * 0.9;
  skeleton.scaleX = scale;
  skeleton.scaleY = scale;
  const offsetY = yDown ? 1 : -1;
  skeleton.x = width / 2 - (data.x + data.width / 2) * scale;
  skeleton.y = height / 2 + offsetY * (data.y + data.height / 2) * scale;
}

export class CanvaskitFrameSource implements FrameSource {
  readonly backend = 'canvaskit' as const;

  private readonly ck: CanvasKit;
  private readonly surface: Surface;
  private readonly renderer: { render(canvas: any, skeleton: any): void };
  private readonly drawable: any;
  private readonly imageInfo: ImageInfo;
  private readonly pixels: MallocObj;
  private readonly pixelView: Uint8ClampedArray<ArrayBuffer>;
  private readonly size: FrameSize;
  private readonly summaryInfo: SkeletonSummary;
  private readonly animationNames: string[];
  private lastMs = -1;
  private disposed = false;

  private constructor(args: {
    ck: CanvasKit;
    surface: Surface;
    renderer: { render(canvas: any, skeleton: any): void };
    drawable: any;
    size: FrameSize;
    summary: SkeletonSummary;
    animations: string[];
  }) {
    this.ck = args.ck;
    this.surface = args.surface;
    this.renderer = args.renderer;
    this.drawable = args.drawable;
    this.size = args.size;
    this.summaryInfo = args.summary;
    this.animationNames = args.animations;
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
    // worker 中拿不到 WebGL 时退回 CPU 光栅 surface，两条路径都用 canvas.readPixels 取像素
    surface ??= ck.MakeSurface(size.width, size.height);
    if (!surface) throw new RuntimeError('backendUnavailable');

    const readFile = createFileReader(context.files);
    let data: any;
    let summary: SkeletonSummary;
    try {
      const atlas = await helpers.loadTextureAtlas(ck, context.atlasFile, readFile);
      data = await helpers.loadSkeletonData(context.skeletonFile, atlas, readFile);
      summary = validateSkeletonData(data, context.version);
    } catch (error) {
      surface.delete();
      if (error instanceof RuntimeError) throw error;
      throw new RuntimeError('parseInvalid', error instanceof Error ? error.message : String(error));
    }

    const animations: string[] = (data.animations ?? []).map((item: any) => String(item.name));
    if (animations.length === 0) {
      surface.delete();
      throw new RuntimeError('noAnimation');
    }

    const drawable = new helpers.SkeletonDrawable(data);
    fitSkeleton(drawable.skeleton, data, size.width, size.height, pack.capabilities.yDown);
    drawable.animationState.setAnimation(0, animations[0], true);

    return new CanvaskitFrameSource({
      ck,
      surface,
      renderer: new helpers.SkeletonRenderer(ck),
      drawable,
      size,
      summary,
      animations,
    });
  }

  summary(): SkeletonSummary {
    return this.summaryInfo;
  }

  animations(): string[] {
    return this.animationNames;
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
    this.ck.Free(this.pixels);
    this.surface.delete();
  }
}

function pixelsView(pixels: MallocObj): Uint8ClampedArray<ArrayBuffer> {
  const view = pixels.toTypedArray() as unknown as Uint8Array<ArrayBuffer>;
  return new Uint8ClampedArray(view.buffer, view.byteOffset, view.length);
}
