import type { CanvasKit, Image as CKImage, MallocObj, Surface } from 'canvaskit-wasm';
import { RuntimeError } from '../types';
import type { FileMap, FrameSize, FrameSource, FrameSourceContext, SkeletonSummary, SpineRuntimePack } from '../types';
import { findFile } from '../runtimes/files';
import { readLegacy31SkeletonData } from '../binary/legacyBinary31';
import { validateSkeletonData } from '../runtimes/validate';
import { loadCanvasKit } from './canvaskit';
import { track0, writeTrackTime } from './track';
import { nonDefaultSkinNames } from './skins';

const REGION_TRIANGLES = [0, 1, 2, 2, 3, 0];

// spine-runtimes 3.1.07 的 spine.BlendMode
const SPINE_BLEND = { normal: 0, additive: 1, multiply: 2, screen: 3 } as const;

interface PageTexture {
  image: CKImage;
  width: number;
  height: number;
  /** spine.BlendMode → 绑定 image shader 的 paint */
  paints: Map<number, any>;
  shaders: any[];
}

/**
 * 3.0–3.2 的自研 CanvasKit 渲染器（§12.3：官方对 3.1/3.2 只有 demo 级 Canvas2D）。
 * 数据与动画走 vendor 的 spine-js 3.1.07，绘制走 drawVertices + Modulate：
 * 纹理 shader 与顶点色相乘得到 skeleton × slot × attachment 的最终 tint，
 * 与官方 spine-canvaskit 4.2 的 quad 路径同构。
 */
export class LegacyFrameSource implements FrameSource {
  readonly backend = 'legacy' as const;

  private readonly spine: any;
  private readonly ck: CanvasKit;
  private readonly surface: Surface;
  private readonly canvas: OffscreenCanvas;
  private readonly glSurface: boolean;
  private skeleton: any = null;
  private state: any = null;
  private summaryInfo: SkeletonSummary = { declaredVersion: '', bones: 0, animationCount: 0, width: 0, height: 0, duration: 0 };
  private animationNames: string[] = [];
  private skinNames: string[] = [];
  private durations: Record<string, number> = {};
  private readonly pageTextures = new Map<unknown, PageTexture>();
  private readonly regionVertices = new Float32Array(8);
  private quadPositions = new Float32Array(8);
  private quadUvs = new Float32Array(8);
  private quadColors = new Uint32Array(4);
  private readonly imageInfo: any;
  private readonly pixels: MallocObj;
  private readonly pixelView: Uint8ClampedArray<ArrayBuffer>;
  private readonly size: FrameSize;
  // 3.1 的 Skeleton 没有 scaleX/scaleY，缩放平移在这里自持并施加到世界坐标上
  private scale = 1;
  private translateX = 0;
  private translateY = 0;
  // 自动取景基准，用户缩放/偏移在基准上叠加（围绕画布中心）
  private baseScale = 1;
  private baseTX = 0;
  private baseTY = 0;
  private lastMs = -1;
  private disposed = false;

  private constructor(
    spine: any,
    ck: CanvasKit,
    surface: Surface,
    size: FrameSize,
    canvas: OffscreenCanvas,
    glSurface: boolean,
  ) {
    this.spine = spine;
    this.ck = ck;
    this.surface = surface;
    this.canvas = canvas;
    this.glSurface = glSurface;
    this.size = size;
    this.imageInfo = {
      width: size.width,
      height: size.height,
      colorType: ck.ColorType.RGBA_8888,
      alphaType: ck.AlphaType.Unpremul,
      colorSpace: ck.ColorSpace.SRGB,
    };
    this.pixels = ck.Malloc(Uint8Array, size.width * size.height * 4);
    this.pixelView = new Uint8ClampedArray(size.width * size.height * 4);
  }

  static async create(pack: SpineRuntimePack, context: FrameSourceContext): Promise<LegacyFrameSource> {
    const spine = pack.core;
    const ck = await loadCanvasKit();
    const size = { width: context.width, height: context.height };

    const canvas = new OffscreenCanvas(size.width, size.height);
    let surface: Surface | null = null;
    try {
      surface = ck.MakeWebGLCanvasSurface(canvas as unknown as HTMLCanvasElement) ?? null;
    } catch {
      surface = null;
    }
    // worker 拿不到 WebGL 时退回 CPU 光栅 surface，与 canvaskit 后端同策略
    const glSurface = surface !== null;
    surface ??= ck.MakeSurface(size.width, size.height);
    if (!surface) throw new RuntimeError('backendUnavailable');

    const source = new LegacyFrameSource(spine, ck, surface, size, canvas, glSurface);
    // 3.1 的骨骼世界变换在 flipY != yDown 时翻转 y；导出数据的 y 朝上，canvas 语义取朝下
    spine.Bone.yDown = true;

    try {
      const atlasText = decodeFile(context.files, context.atlasFile);
      const atlas = new spine.Atlas(atlasText, source.syncTextureLoader(context.files));
      for (const page of atlas.pages) {
        if (!page.rendererObject) throw new RuntimeError('missingFile', String(page.name));
      }

      const loader = new spine.AtlasAttachmentLoader(atlas);
      const skeletonKey = findFile(context.files, context.skeletonFile);
      if (!skeletonKey) throw new RuntimeError('missingFile', context.skeletonFile);
      const bytes = context.files[skeletonKey];
      // 3.1 的官方 core 没有 SkeletonBinary，.skel 走自研读取器（§12.7）
      const data = context.skeletonFile.toLowerCase().endsWith('.json')
        ? new spine.SkeletonJson(loader).readSkeletonData(JSON.parse(decodeFile(context.files, context.skeletonFile)))
        : readLegacy31SkeletonData(spine, new Uint8Array(bytes), loader);
      const summary = validateSkeletonData(data, context.version);

      const animations: string[] = data.animations.map((item: any) => String(item.name));
      if (animations.length === 0) throw new RuntimeError('noAnimation');
      const durations: Record<string, number> = {};
      for (const item of data.animations) durations[String(item.name)] = Number(item.duration) || 0;
      const skins = nonDefaultSkinNames(data);

      const skeleton = new spine.Skeleton(data);
      // 3.1 的 AnimationState 不会自动落到 defaultSkin，必须显式 setSkin
      skeleton.setSkin(data.defaultSkin ?? null);
      skeleton.setToSetupPose();
      const state = new spine.AnimationState(new spine.AnimationStateData(data));
      // 3.1 的 setAnimation 收 Animation 对象，按名字切入要用 setAnimationByName
      state.setAnimationByName(0, animations[0], true);
      state.apply(skeleton);
      skeleton.updateWorldTransform();

      source.skeleton = skeleton;
      source.state = state;
      source.fit(data);
      // fit 只写自动取景基准；用户偏移/缩放通过 setTransform 叠加
      source.baseScale = source.scale;
      source.baseTX = source.translateX;
      source.baseTY = source.translateY;
      source.summaryInfo = summary;
      source.animationNames = animations;
      source.skinNames = skins;
      source.durations = durations;
      return source;
    } catch (error) {
      source.disposeResources();
      if (error instanceof RuntimeError) throw error;
      throw new RuntimeError('parseInvalid', error instanceof Error ? error.message : String(error));
    }
  }

  /**
   * spine.Atlas 构造期同步回调 load(page, path)。贴图都在内存里，
   * 用同步的 MakeImageFromEncoded 直接挂到 page.rendererObject，避免渲染时纹理未就绪。
   */
  private syncTextureLoader(files: FileMap) {
    return {
      load: (page: any, path: string) => {
        const key = findFile(files, path) ?? findFile(files, String(path).split('/').pop() ?? path);
        if (!key) return;
        const image = this.ck.MakeImageFromEncoded(new Uint8Array(files[key]));
        if (!image) return;
        // 旧 TexturePacker 导出的 atlas 没有 size 行，3.1 的 Atlas 只在有 size 行时写 page 尺寸；
        // region UV 在构造期就按 page.width/height 计算，这里必须用贴图实际尺寸补齐
        page.width = image.width();
        page.height = image.height();
        page.rendererObject = image;
      },
      unload: () => {},
    };
  }

  private textureFor(page: any): PageTexture {
    const cached = this.pageTextures.get(page);
    if (cached) return cached;
    const image: CKImage | null = page?.rendererObject ?? null;
    if (!image) throw new RuntimeError('missingFile', String(page?.name ?? 'atlas page'));
    const paints = new Map<number, any>();
    const shaders: any[] = [];
    for (const mode of [SPINE_BLEND.normal, SPINE_BLEND.additive, SPINE_BLEND.multiply, SPINE_BLEND.screen]) {
      const shader = image.makeShaderOptions(this.ck.TileMode.Clamp, this.ck.TileMode.Clamp, this.ck.FilterMode.Linear, this.ck.MipmapMode.None);
      const paint = new this.ck.Paint();
      paint.setShader(shader);
      paint.setBlendMode(this.toCkBlendMode(mode));
      paints.set(mode, paint);
      shaders.push(shader);
    }
    const texture: PageTexture = { image, width: image.width(), height: image.height(), paints, shaders };
    this.pageTextures.set(page, texture);
    return texture;
  }

  // 与官方 spine-canvaskit 一致：multiply 在顶点色路径下退化回 SrcOver
  private toCkBlendMode(mode: number) {
    if (mode === SPINE_BLEND.additive) return this.ck.BlendMode.Plus;
    if (mode === SPINE_BLEND.screen) return this.ck.BlendMode.Screen;
    return this.ck.BlendMode.SrcOver;
  }

  /** 3.1 时代常能见到没有 skeleton 段的导出（官方 goblins-mesh 就是），这时只能实测当前帧的顶点包围盒 */
  private measureBounds(): { centerX: number; centerY: number; width: number; height: number } | null {
    const region = this.regionVertices;
    let minX = Infinity;
    let minY = Infinity;
    let maxX = -Infinity;
    let maxY = -Infinity;
    const consume = (positions: Float32Array, count: number) => {
      for (let i = 0; i < count; i += 2) {
        if (positions[i] < minX) minX = positions[i];
        if (positions[i] > maxX) maxX = positions[i];
        if (positions[i + 1] < minY) minY = positions[i + 1];
        if (positions[i + 1] > maxY) maxY = positions[i + 1];
      }
    };
    for (const slot of this.skeleton.drawOrder) {
      const attachment = slot.attachment;
      if (!attachment) continue;
      if (attachment.type === this.spine.AttachmentType.region) {
        attachment.computeVertices(0, 0, slot.bone, region);
        consume(region, 8);
      } else if (attachment.type === this.spine.AttachmentType.mesh || attachment.type === this.spine.AttachmentType.weightedmesh) {
        const world = new Float32Array(attachment.uvs.length);
        attachment.computeWorldVertices(0, 0, slot, world);
        consume(world, world.length);
      }
    }
    if (!Number.isFinite(minX) || maxX <= minX || maxY <= minY) return null;
    return { centerX: (minX + maxX) / 2, centerY: (minY + maxY) / 2, width: maxX - minX, height: maxY - minY };
  }

  private fit(data: any) {
    const size = this.size;
    const scale = (width: number, height: number) => Math.min(size.width / width, size.height / height) * 0.9;
    // yDown 世界下内容占据 y ∈ [-height, 0]（数据 y 朝上），世界中心在 (width/2, -height/2)
    if (data.width > 0 && data.height > 0) {
      this.scale = scale(data.width, data.height);
      this.translateX = size.width / 2 - (data.width / 2) * this.scale;
      this.translateY = size.height / 2 + (data.height / 2) * this.scale;
      return;
    }
    const measured = this.measureBounds();
    if (!measured) {
      this.scale = 1;
      this.translateX = size.width / 2;
      this.translateY = size.height / 2;
      return;
    }
    this.scale = scale(measured.width, measured.height);
    this.translateX = size.width / 2 - measured.centerX * this.scale;
    this.translateY = size.height / 2 - measured.centerY * this.scale;
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
    // 3.1 的 setSkinByName('') 会抛异常，默认皮肤直接 setSkin(defaultSkin)
    if (!name) {
      this.skeleton.setSkin(this.skeleton.data.defaultSkin ?? null);
      return;
    }
    this.skeleton.setSkinByName(name);
  }

  setAnimation(name: string, loop: boolean): void {
    // 3.1 的 setAnimation 收 Animation 对象，按名字要用 setAnimationByName
    this.state.setAnimationByName(0, name, loop);
    writeTrackTime(track0(this.state), 0);
    this.lastMs = -1;
  }

  seek(timeMs: number): void {
    writeTrackTime(track0(this.state), timeMs / 1000);
    this.lastMs = timeMs;
  }

  setTransform(offsetX: number, offsetY: number, scale: number): void {
    // 围绕画布中心把自动取景结果放大 scale 倍：p' = C + (p − C)·k，
    // 而 p = world·s + t ⟹ s′ = s·k, t′ = C + (t − C)·k；再叠加屏幕偏移（+y 上，画布 y 朝下取负）
    const { width, height } = this.size;
    this.scale = this.baseScale * scale;
    this.translateX = width / 2 + (this.baseTX - width / 2) * scale + offsetX;
    this.translateY = height / 2 + (this.baseTY - height / 2) * scale - offsetY;
  }

  private advanceAndDraw(timeMs: number) {
    const delta = this.lastMs < 0 ? 0 : Math.max(0, (timeMs - this.lastMs) / 1000);
    this.lastMs = timeMs;

    this.state.update(delta);
    this.state.apply(this.skeleton);
    this.skeleton.updateWorldTransform();

    const canvas = this.surface.getCanvas();
    canvas.clear(this.ck.TRANSPARENT);
    this.drawSkeleton(canvas);
    this.surface.flush();
  }

  async render(timeMs: number): Promise<ImageBitmap> {
    if (this.disposed) throw new RuntimeError('notLoaded');
    this.advanceAndDraw(timeMs);
    this.surface.getCanvas().readPixels(0, 0, this.imageInfo, this.pixels);

    this.pixelView.set(pixelsView(this.pixels));
    return createImageBitmap(new ImageData(this.pixelView, this.imageInfo.width, this.imageInfo.height));
  }

  // 预览快路径同 canvaskit：CPU surface 时退回直通 alpha 的 render()
  async renderPreview(timeMs: number): Promise<ImageBitmap> {
    if (this.disposed) throw new RuntimeError('notLoaded');
    if (!this.glSurface) return this.render(timeMs);
    this.advanceAndDraw(timeMs);
    return this.canvas.transferToImageBitmap();
  }

  private drawSkeleton(canvas: any) {
    const skeleton = this.skeleton;
    const drawOrder = skeleton.drawOrder;
    const regionVertices = this.regionVertices;
    for (let i = 0, n = drawOrder.length; i < n; i++) {
      const slot = drawOrder[i];
      const attachment = slot.attachment;
      if (!attachment) continue;
      if (attachment.type === this.spine.AttachmentType.region) {
        // skeleton.x/y 保持 0，缩放平移统一在 drawVertices 里施加
        attachment.computeVertices(0, 0, slot.bone, regionVertices);
        this.drawVertices(canvas, slot, attachment, regionVertices, REGION_TRIANGLES);
      } else if (attachment.type === this.spine.AttachmentType.mesh || attachment.type === this.spine.AttachmentType.weightedmesh) {
        // uvs 与顶点一一对应（3.1 无 3.8 那种三元组的 mesh），长度即 2×顶点数
        const world = new Float32Array(attachment.uvs.length);
        attachment.computeWorldVertices(0, 0, slot, world);
        this.drawVertices(canvas, slot, attachment, world, attachment.triangles);
      }
    }
  }

  /** positions 为骨骼世界坐标（未含任何 skeleton 变换），这里乘 scale 加平移后写进顶点 */
  private drawVertices(canvas: any, slot: any, attachment: any, positions: Float32Array, triangles: number[]) {
    const texture = this.textureFor(attachment.rendererObject?.page);
    const skeleton = this.skeleton;
    const a = Math.round(clamp01(skeleton.a * slot.a * attachment.a) * 255);
    const r = Math.round(clamp01(skeleton.r * slot.r * attachment.r) * 255);
    const g = Math.round(clamp01(skeleton.g * slot.g * attachment.g) * 255);
    const b = Math.round(clamp01(skeleton.b * slot.b * attachment.b) * 255);
    const argb = ((a << 24) | (r << 16) | (g << 8) | b) >>> 0;

    const vertexCount = positions.length >> 1;
    if (this.quadPositions.length < positions.length) {
      this.quadPositions = new Float32Array(positions.length);
      this.quadUvs = new Float32Array(positions.length);
      this.quadColors = new Uint32Array(vertexCount);
    }
    const pos = this.quadPositions;
    const uvs = this.quadUvs;
    const colors = this.quadColors;
    const sourceUvs: Float32Array = attachment.uvs;
    for (let i = 0; i < positions.length; i += 2) {
      pos[i] = positions[i] * this.scale + this.translateX;
      pos[i + 1] = positions[i + 1] * this.scale + this.translateY;
      // 3.1 的 UV 归一化到图集，drawVertices 要像素纹理坐标
      uvs[i] = sourceUvs[i] * texture.width;
      uvs[i + 1] = sourceUvs[i + 1] * texture.height;
    }
    for (let i = 0; i < vertexCount; i++) colors[i] = argb;

    const paint = texture.paints.get(slot.data.blendMode ?? 0) ?? texture.paints.get(SPINE_BLEND.normal);
    const verts = this.ck.MakeVertices(
      this.ck.VertexMode.Triangles,
      pos.subarray(0, positions.length),
      uvs.subarray(0, positions.length),
      colors.subarray(0, vertexCount),
      triangles,
      true,
    );
    canvas.drawVertices(verts, this.ck.BlendMode.Modulate, paint);
    verts.delete();
  }

  getSize(): FrameSize {
    return this.size;
  }

  dispose(): void {
    if (this.disposed) return;
    this.disposed = true;
    this.disposeResources();
  }

  private disposeResources() {
    for (const texture of this.pageTextures.values()) {
      texture.shaders.forEach((shader) => shader?.delete());
      for (const paint of texture.paints.values()) paint?.delete();
      texture.image.delete();
    }
    this.pageTextures.clear();
    this.ck.Free(this.pixels);
    this.surface.delete();
  }
}

function decodeFile(files: FileMap, name: string): string {
  const key = findFile(files, name);
  if (!key) throw new RuntimeError('missingFile', name);
  return new TextDecoder().decode(files[key]).replace(/\r\n/g, '\n');
}

function pixelsView(pixels: MallocObj): Uint8ClampedArray<ArrayBuffer> {
  const view = pixels.toTypedArray() as unknown as Uint8Array<ArrayBuffer>;
  return new Uint8ClampedArray(view.buffer, view.byteOffset, view.length);
}

function clamp01(value: number): number {
  return Math.min(1, Math.max(0, value));
}
