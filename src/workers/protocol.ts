import type { FileMap, LoadAttempt, SpineVersionInfo } from '../spine/types';

export interface InitPayload {
  width: number;
  height: number;
}

export interface LoadPayload {
  files: FileMap;
  skeletonFile: string;
  atlasFile: string;
  /** 文件头嗅探出的版本，Worker 据此展开候选运行时链 */
  version: SpineVersionInfo;
}

export interface FramePayload {
  timeMs: number;
}

/** 加载成功后的实际结果：用了哪个 pack、骨骼与动画规模，供 UI 展示与排错 */
export interface LoadResponsePayload {
  packId: string;
  backend: 'canvaskit' | 'webgl' | 'legacy';
  /** 生效的候选运行时版本 */
  runtimeVersion: string;
  /** 骨架文件内声明的版本 */
  declaredVersion: string;
  bones: number;
  animationCount: number;
  /** 当前播放的动画 */
  animation: string;
  animations: string[];
  /** 回退到非首个候选时，前面候选的失败原因 */
  attempts: LoadAttempt[];
}

export type LoadErrorCode =
  | 'unknownVersion'
  | 'runtimeUnavailable'
  | 'allCandidatesFailed'
  /** 声明版本的官方 core 没有 SkeletonBinary（3.4–3.7），.skel 要等自研读取器 */
  | 'binaryUnsupported'
  | 'loadCrashed';

export interface ErrorPayload {
  code: LoadErrorCode | 'renderFailed';
  /** runtimeUnavailable 时是原因码，其余是可直接展示的拼接详情 */
  message: string;
  attempts: LoadAttempt[];
}

export interface EncodeConfigPayload {
  format: 'vp9' | 'apng';
  width: number;
  height: number;
  fps: 30 | 60;
  bitrate: number;
  frameCount: number;
}

export type RenderRequest =
  | { id: number; type: 'init'; payload: InitPayload }
  | { id: number; type: 'load'; payload: LoadPayload }
  | { id: number; type: 'render'; payload: FramePayload }
  | { id: number; type: 'dispose' };

export type EncodeRequest =
  | { id: number; type: 'configure'; payload: EncodeConfigPayload }
  | { id: number; type: 'frame'; payload: { index: number; frame: VideoFrame | ImageBitmap } }
  | { id: number; type: 'finalize' }
  | { id: number; type: 'cancel' };

export type WorkerRequest = RenderRequest | EncodeRequest;

export type RenderResponse =
  | { id: number; type: 'ready' }
  | { id: number; type: 'loaded'; payload: LoadResponsePayload }
  | { id: number; type: 'frame'; payload: { timeMs: number; frame: ImageBitmap } }
  | { id: number; type: 'error'; payload: ErrorPayload };

export type EncodeResponse =
  | { id: number; type: 'progress'; payload: { encoded: number; total: number } }
  | { id: number; type: 'done'; payload: { blob: Blob } }
  | { id: number; type: 'error'; payload: { message: string } };

