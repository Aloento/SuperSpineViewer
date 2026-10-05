export interface InitPayload {
  canvas: OffscreenCanvas;
  width: number;
  height: number;
}

export interface SkeletonPayload {
  files: Record<string, ArrayBuffer>;
  version: string;
}

export interface FramePayload {
  index: number;
  timeMs: number;
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
  | { id: number; type: 'load'; payload: SkeletonPayload }
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
  | { id: number; type: 'frame'; payload: { index: number; frame: ImageBitmap } }
  | { id: number; type: 'error'; payload: { message: string } };

export type EncodeResponse =
  | { id: number; type: 'progress'; payload: { encoded: number; total: number } }
  | { id: number; type: 'done'; payload: { blob: Blob } }
  | { id: number; type: 'error'; payload: { message: string } };

export function pickRenderRequest(request: WorkerRequest): RenderRequest | null {
  return request.type === 'init' || request.type === 'load' || request.type === 'render' || request.type === 'dispose'
    ? request
    : null;
}

export function pickEncodeRequest(request: WorkerRequest): EncodeRequest | null {
  return request.type === 'configure' || request.type === 'frame' || request.type === 'finalize' || request.type === 'cancel'
    ? request
    : null;
}
