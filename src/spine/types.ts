export interface SpineVersionInfo {
  raw: string;
  major: number;
  minor: number;
  patch: number;
}

/** 一次加载可用的全部资源，key 为文件名 */
export type FileMap = Record<string, ArrayBuffer>;

export interface FrameSize {
  width: number;
  height: number;
}

/** 解析骨架与图集所需的全部输入，由 pack.createFrameSource 消费 */
export interface FrameSourceContext {
  width: number;
  height: number;
  files: FileMap;
  skeletonFile: string;
  atlasFile: string;
  /** 文件头嗅探出的版本，用于 §12.6 的解析结果校验 */
  version: SpineVersionInfo;
}

/** 解析摘要：UI 展示与排错用，版本取骨架文件内声明值 */
export interface SkeletonSummary {
  declaredVersion: string;
  bones: number;
  animationCount: number;
  width: number;
  height: number;
  /** 默认动画（animations[0]）时长，秒；导出总帧数据此计算 */
  duration: number;
}

/**
 * 统一取帧后端：解析成功的骨架 + 渲染管线，调用方按时间轴取 RGBA 帧。
 * canvaskit / webgl / legacy（3.1 自研）三条实现，接口一致；解析在 create 阶段完成，失败抛 RuntimeError。
 */
export interface FrameSource {
  readonly backend: 'canvaskit' | 'webgl' | 'legacy';
  /** 解析摘要 */
  summary(): SkeletonSummary;
  /** 动画名，顺序与运行时一致；首项为默认播放动画 */
  animations(): string[];
  /** 皮肤名，顺序与运行时一致；首项为默认皮肤（无皮肤时为 ['']） */
  skins(): string[];
  /** 各动画时长（秒），key 为动画名；进度条切换动画时据此更新最大值 */
  animationDurations(): Record<string, number>;
  /** 切换皮肤（'' 表示默认皮肤）；立即生效，下一帧渲染体现 */
  setSkin(name: string): void;
  /** 切换动画并重置播放时间到 0；loop 控制是否循环 */
  setAnimation(name: string, loop: boolean): void;
  /** 跳转到 timeMs（动画时间，毫秒）；重置动画状态到该时刻 */
  seek(timeMs: number): void;
  /** 基础变换：画布像素偏移（+x 右、+y 上）与相对自动取景的额外缩放 */
  setTransform(offsetX: number, offsetY: number, scale: number): void;
  /** 渲染 timeMs 时刻并读回 RGBA 像素（straight alpha）；帧所有权交给调用方，用完须 close。
   *  导出与像素回归比对走这条确定性路径 */
  render(timeMs: number): Promise<ImageBitmap>;
  /** 预览取帧：能走 GPU 就不过 CPU 读回，返回的位图可能是预乘 alpha；仅用于上屏显示 */
  renderPreview(timeMs: number): Promise<ImageBitmap>;
  getSize(): FrameSize;
  /** 释放骨架、纹理、surface、GL context 等内部资源 */
  dispose(): void;
}

/** 运行时错误码：UI 按 code 决定文案 */
export type RuntimeErrorCode =
  | 'parseInvalid'
  /** 该版本的官方 JS 运行时没有 SkeletonBinary，.skel 要等自研读取器（§12.7） */
  | 'binaryUnsupported'
  | 'missingFile'
  | 'backendUnavailable'
  | 'packLoadFailed'
  | 'packUnsupported'
  | 'noAnimation'
  | 'notLoaded';

export class RuntimeError extends Error {
  code: RuntimeErrorCode;
  detail: string;

  constructor(code: RuntimeErrorCode, detail = '') {
    super(detail ? `${code}:${detail}` : code);
    this.name = 'RuntimeError';
    this.code = code;
    this.detail = detail;
  }
}

/** 运行时能力位：替代散落的 if/switch，frameSource 按能力选路径 */
export interface SpineRuntimeCapabilities {
  setupPoseMethod: 'setToSetupPose' | 'setupPose';
  /** canvaskit 后端 +y 朝屏幕上方 */
  yDown: boolean;
  /** 3.4–3.8 的 TextureAtlas 在构造期同步回调 textureLoader */
  synchronousAtlasLoader: boolean;
  /** 3.4 的图集 attachment 加载器叫 TextureAtlasAttachmentLoader */
  attachmentLoader: 'AtlasAttachmentLoader' | 'TextureAtlasAttachmentLoader';
}

/** 一个版本的运行时入口：core 为解析层命名空间，webgl 为渲染层（两者可能同名） */
export interface SpineRuntimePack {
  id: string;
  backend: 'canvaskit' | 'webgl' | 'legacy';
  capabilities: SpineRuntimeCapabilities;
  core: any;
  webgl?: any;
  /** 解析骨架与图集并建好渲染管线；失败抛 RuntimeError，由候选链试下一个 pack */
  createFrameSource(context: FrameSourceContext): Promise<FrameSource>;
}

/** 单个候选的失败记录，跨 Worker 边界回传给 UI 排错 */
export interface LoadAttempt {
  version: string;
  packId: string;
  message: string;
}
