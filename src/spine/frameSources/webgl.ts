import { RuntimeError } from '../types';
import type { FrameSize, FrameSource, FrameSourceContext, SkeletonSummary, SpineRuntimePack } from '../types';
import { readLegacySkeletonData } from '../binary/legacyBinary';
import { atlasPageNames, findFile } from '../runtimes/files';
import { validateSkeletonData } from '../runtimes/validate';

async function decodeImage(files: Record<string, ArrayBuffer>, name: string) {
  const key = findFile(files, name);
  if (!key) throw new RuntimeError('missingFile', name);
  const bitmap = await createImageBitmap(new Blob([files[key]], { type: 'image/png' }));
  // Chrome 把 ImageBitmap 上传到纹理时无条件预乘 alpha（UNPACK_PREMULTIPLY_ALPHA_WEBGL 无效），
  // 而渲染器按直通 alpha 走 SRC_ALPHA 混合，等于乘两次导致半透明区域偏暗；
  // 画布源配合 UNPACK_PREMULTIPLY_ALPHA_WEBGL=false 才能保持直通 alpha
  const decoded = new OffscreenCanvas(bitmap.width, bitmap.height);
  const context = decoded.getContext('2d', { alpha: true })!;
  context.drawImage(bitmap, 0, 0);
  bitmap.close();
  return decoded;
}

/**
 * 3.4–3.8 的 PolygonBatcher 用单一 blendFunc，alpha 通道也跟着乘 SRC_ALPHA，
 * 透明背景上第一帧的 alpha 会退化成 α²，透明导出整幅偏淡。
 * 官方 4.0 改为 blendFuncSeparate 并按混合模式给 alpha 源函数，这里给旧版本补齐同一规则。
 */
function fixupAlphaBlending(renderer: any, gl: WebGLRenderingContext) {
  const batcher = renderer.batcher;
  if (!batcher || typeof batcher.begin !== 'function' || typeof batcher.srcBlend !== 'number') return;
  const begin = batcher.begin.bind(batcher);
  const setBlendMode = batcher.setBlendMode.bind(batcher);
  const apply = () => {
    const srcAlpha =
      batcher.srcBlend === gl.DST_COLOR ? gl.ONE_MINUS_SRC_ALPHA
      : batcher.srcBlend === gl.ONE ? gl.ONE_MINUS_SRC_COLOR
      : gl.ONE;
    gl.blendFuncSeparate(batcher.srcBlend, batcher.dstBlend, srcAlpha, batcher.dstBlend);
  };
  batcher.begin = (shader: any) => {
    begin(shader);
    apply();
  };
  batcher.setBlendMode = (src: number, dst: number) => {
    const drawing = batcher.isDrawing;
    setBlendMode(src, dst);
    if (drawing) apply();
  };
}

/** 3.8 的 TextureAtlas 在构造期同步回调 textureLoader，所以要先解码好所有 page 图片。 */
function syncTextureLoader(webgl: any, gl: WebGLRenderingContext, images: Map<string, OffscreenCanvas>) {
  return (pageName: string) => {
    const image = images.get(pageName.toLowerCase());
    if (!image) throw new RuntimeError('missingFile', pageName);
    return new webgl.GLTexture(gl, image);
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
        const images = new Map<string, OffscreenCanvas>();
        for (const name of atlasPageNames(atlasText)) {
          images.set(name.toLowerCase(), await decodeImage(context.files, name));
        }
        atlas = new spine.TextureAtlas(atlasText, syncTextureLoader(webgl, gl, images));
      } else {
        atlas = new spine.TextureAtlas(atlasText);
        for (const page of atlas.pages) {
          page.setTexture(new webgl.GLTexture(gl, await decodeImage(context.files, page.name)));
        }
      }

      const loader = new spine[pack.capabilities.attachmentLoader](atlas);
      const bytes = context.files[context.skeletonFile];
      const isBinary = !context.skeletonFile.toLowerCase().endsWith('.json');
      if (isBinary) {
        const view = new Uint8Array(bytes);
        // 官方 JS 的 SkeletonBinary 从 3.8 才有，3.3–3.7 走自研读取器（§12.7）
        data =
          typeof spine.SkeletonBinary === 'function'
            ? new spine.SkeletonBinary(loader).readSkeletonData(view)
            : readLegacySkeletonData(spine, view, loader);
      } else {
        data = new spine.SkeletonJson(loader).readSkeletonData(new TextDecoder().decode(bytes));
      }
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

    const state = new spine.AnimationState(new spine.AnimationStateData(data));
    state.setAnimation(0, animations[0], true);
    state.apply(skeleton);
    updateWorld();

    const renderer = new webgl.SceneRenderer(canvas, gl);
    fixupAlphaBlending(renderer, gl);
    // 3.4–3.6 的 Bone 根变换不消费 skeleton.scaleX/scaleY（3.7 才并入根骨骼），
    // 缩放平移统一走正交相机，各版本才能得到一致的取景结果
    if (data.width > 0 && data.height > 0) {
      const scale = Math.min(size.width / data.width, size.height / data.height) * 0.9;
      renderer.camera.position.x = (data.x ?? 0) + data.width / 2;
      renderer.camera.position.y = (data.y ?? 0) + data.height / 2;
      renderer.camera.zoom = 1 / scale;
    }

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

    // readPixels 自底向上，ImageData 自顶向下；且 GL 帧缓冲始终按预乘存储，
    // 而 ImageData 约定直通 alpha，需要反预乘，否则半透明区域会比实际暗
    const row = width * 4;
    for (let y = 0; y < height; y++) {
      const src = (height - 1 - y) * row;
      const dst = y * row;
      for (let x = 0; x < row; x += 4) {
        const alpha = this.pixels[src + x + 3];
        this.flipped[dst + x + 3] = alpha;
        if (alpha >= 255) {
          this.flipped[dst + x] = this.pixels[src + x];
          this.flipped[dst + x + 1] = this.pixels[src + x + 1];
          this.flipped[dst + x + 2] = this.pixels[src + x + 2];
        } else if (alpha === 0) {
          this.flipped[dst + x] = 0;
          this.flipped[dst + x + 1] = 0;
          this.flipped[dst + x + 2] = 0;
        } else {
          const inv = 255 / alpha;
          this.flipped[dst + x] = this.pixels[src + x] * inv;
          this.flipped[dst + x + 1] = this.pixels[src + x + 1] * inv;
          this.flipped[dst + x + 2] = this.pixels[src + x + 2] * inv;
        }
      }
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
