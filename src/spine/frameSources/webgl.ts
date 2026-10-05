import { RuntimeError } from '../types';
import type { FrameSize, FrameSource, FrameSourceContext, SkeletonSummary, SpineRuntimePack } from '../types';
import { atlasPageNames, findFile } from '../runtimes/files';
import { validateSkeletonData } from '../runtimes/validate';

async function decodeImage(files: Record<string, ArrayBuffer>, name: string) {
  const key = findFile(files, name);
  if (!key) throw new RuntimeError('missingFile', name);
  return createImageBitmap(new Blob([files[key]], { type: 'image/png' }));
}

/** 3.8 的 TextureAtlas 在构造期同步回调 textureLoader，所以要先解码好所有 page 图片。 */
function syncTextureLoader(webgl: any, gl: WebGLRenderingContext, bitmaps: Map<string, ImageBitmap>) {
  return (pageName: string) => {
    const bitmap = bitmaps.get(pageName.toLowerCase());
    if (!bitmap) throw new RuntimeError('missingFile', pageName);
    return new webgl.GLTexture(gl, bitmap);
  };
}

export class WebglFrameSource implements FrameSource {
  readonly backend = 'webgl' as const;

  private readonly gl: WebGLRenderingContext;
  private readonly renderer: any;
  private readonly skeleton: any;
  private readonly state: any;
  private readonly updateWorld: () => void;
  private readonly atlas: any;
  private readonly pixels: Uint8Array<ArrayBuffer>;
  private readonly flipped: Uint8ClampedArray<ArrayBuffer>;
  private readonly size: FrameSize;
  private readonly summaryInfo: SkeletonSummary;
  private readonly animationNames: string[];
  private lastMs = -1;
  private disposed = false;

  private constructor(args: {
    gl: WebGLRenderingContext;
    renderer: any;
    skeleton: any;
    state: any;
    updateWorld: () => void;
    atlas: any;
    size: FrameSize;
    summary: SkeletonSummary;
    animations: string[];
  }) {
    this.gl = args.gl;
    this.renderer = args.renderer;
    this.skeleton = args.skeleton;
    this.state = args.state;
    this.updateWorld = args.updateWorld;
    this.atlas = args.atlas;
    this.size = args.size;
    this.summaryInfo = args.summary;
    this.animationNames = args.animations;
    this.pixels = new Uint8Array(args.size.width * args.size.height * 4);
    this.flipped = new Uint8ClampedArray(args.size.width * args.size.height * 4);
  }

  static async create(pack: SpineRuntimePack, context: FrameSourceContext): Promise<WebglFrameSource> {
    const spine = pack.core;
    const webgl = pack.webgl ?? spine;
    const size = { width: context.width, height: context.height };
    const canvas = new OffscreenCanvas(size.width, size.height);
    const attributes: WebGLContextAttributes = {
      alpha: true,
      premultipliedAlpha: false,
      antialias: false,
      depth: false,
      stencil: false,
    };
    const gl =
      (canvas.getContext('webgl2', attributes) as WebGLRenderingContext | null) ??
      (canvas.getContext('webgl', attributes) as WebGLRenderingContext | null);
    if (!gl) throw new RuntimeError('backendUnavailable');

    const atlasText = new TextDecoder().decode(context.files[context.atlasFile]).replace(/\r\n/g, '\n');

    let atlas: any;
    let data: any;
    try {
      if (pack.capabilities.synchronousAtlasLoader) {
        const bitmaps = new Map<string, ImageBitmap>();
        for (const name of atlasPageNames(atlasText)) {
          bitmaps.set(name.toLowerCase(), await decodeImage(context.files, name));
        }
        atlas = new spine.TextureAtlas(atlasText, syncTextureLoader(webgl, gl, bitmaps));
      } else {
        atlas = new spine.TextureAtlas(atlasText);
        for (const page of atlas.pages) {
          page.setTexture(new webgl.GLTexture(gl, await decodeImage(context.files, page.name)));
        }
      }

      const loader = new spine[pack.capabilities.attachmentLoader](atlas);
      const bytes = context.files[context.skeletonFile];
      const isBinary = !context.skeletonFile.toLowerCase().endsWith('.json');
      if (isBinary && typeof spine.SkeletonBinary !== 'function') {
        // 官方 JS 的 SkeletonBinary 从 3.8 才有，3.4–3.7 要等 M2d 的自研读取器
        throw new RuntimeError('binaryUnsupported', pack.id);
      }
      data = isBinary
        ? new spine.SkeletonBinary(loader).readSkeletonData(new Uint8Array(bytes))
        : new spine.SkeletonJson(loader).readSkeletonData(new TextDecoder().decode(bytes));
    } catch (error) {
      if (error instanceof RuntimeError) throw error;
      throw new RuntimeError('parseInvalid', error instanceof Error ? error.message : String(error));
    }

    const summary = validateSkeletonData(data, context.version);
    const animations: string[] = (data.animations ?? []).map((item: any) => String(item.name));
    if (animations.length === 0) throw new RuntimeError('noAnimation');

    const skeleton = new spine.Skeleton(data);
    const physics = spine.Physics?.update;
    const updateWorld = () => skeleton.updateWorldTransform(physics);
    skeleton[pack.capabilities.setupPoseMethod]();
    // 正交相机以世界原点为中心、+y 向上，因此把包围盒中心平移到原点即可居中
    if (data.width > 0 && data.height > 0) {
      const scale = Math.min(size.width / data.width, size.height / data.height) * 0.9;
      skeleton.scaleX = scale;
      skeleton.scaleY = scale;
      // 3.8 之前的 SkeletonData 没有 x/y（3.1–3.7 头部就没有这两个字段）
      skeleton.x = -((data.x ?? 0) + data.width / 2) * scale;
      skeleton.y = -((data.y ?? 0) + data.height / 2) * scale;
    }

    const state = new spine.AnimationState(new spine.AnimationStateData(data));
    state.setAnimation(0, animations[0], true);
    state.apply(skeleton);
    updateWorld();

    const renderer = new webgl.SceneRenderer(canvas, gl);

    return new WebglFrameSource({ gl, renderer, skeleton, state, updateWorld, atlas, size, summary, animations });
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

    // 4.1 删掉了 Skeleton.update（只增 skeleton.time），4.0 与 4.2+ 才有
    this.skeleton.update?.(delta);
    this.state.update(delta);
    this.state.apply(this.skeleton);
    this.updateWorld();

    const { width, height } = this.size;
    this.gl.viewport(0, 0, width, height);
    this.gl.clearColor(0, 0, 0, 0);
    this.gl.clear(this.gl.COLOR_BUFFER_BIT);
    this.renderer.begin();
    this.renderer.drawSkeleton(this.skeleton, false);
    this.renderer.end();
    this.gl.readPixels(0, 0, width, height, this.gl.RGBA, this.gl.UNSIGNED_BYTE, this.pixels);

    // readPixels 自底向上，ImageData 自顶向下
    const row = width * 4;
    for (let y = 0; y < height; y++) {
      const src = (height - 1 - y) * row;
      this.flipped.set(this.pixels.subarray(src, src + row), y * row);
    }
    return createImageBitmap(new ImageData(this.flipped, width, height));
  }

  getSize(): FrameSize {
    return this.size;
  }

  dispose(): void {
    if (this.disposed) return;
    this.disposed = true;
    // 各运行时的 dispose 支持程度不一（3.x 的 TextureAtlas 有、AnimationState 没有），逐个可选调用
    for (const target of [this.state, this.skeleton, this.atlas, this.renderer]) {
      try {
        target?.dispose?.();
      } catch {
        // 忽略释放失败
      }
    }
    this.gl.getExtension('WEBGL_lose_context')?.loseContext();
  }
}
