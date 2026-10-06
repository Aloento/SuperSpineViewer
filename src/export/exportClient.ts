import type { EncodeConfigPayload, EncodeRequest, EncodeResponse } from '../workers/protocol';

export interface ExportClientEvents {
  onProgress: (encoded: number, total: number) => void;
}

/**
 * 与 encode.worker 的交互封装。帧消息只发不等，避免主线程被编码背压拖住，
 * 渲染与编码因此流水线并行；进度以 Worker 已编码帧数为准。
 */
export class ExportClient {
  private readonly worker: Worker;
  private nextId = 1;
  private pending: { resolve: (blob: Blob) => void; reject: (error: Error) => void } | null = null;
  private events: ExportClientEvents = { onProgress: () => {} };
  private closed = false;

  constructor() {
    this.worker = new Worker(new URL('../workers/encode.worker.ts', import.meta.url), { type: 'module' });
    this.worker.onmessage = (event: MessageEvent<EncodeResponse>) => this.receive(event.data);
    this.worker.onerror = (event) => this.reject(new Error(event.message || 'encode-worker-error'));
  }

  private receive(response: EncodeResponse) {
    // 取消后旧 Worker 的残留消息（含 done 里的 Blob）一律丢弃
    if (this.closed) return;
    if (response.type === 'progress') {
      this.events.onProgress(response.payload.encoded, response.payload.total);
      return;
    }
    if (response.type === 'done') {
      const entry = this.pending;
      this.pending = null;
      entry?.resolve(response.payload.blob);
      return;
    }
    this.reject(new Error(response.payload.message));
  }

  private reject(error: Error) {
    const entry = this.pending;
    this.pending = null;
    entry?.reject(error);
  }

  onEvents(events: ExportClientEvents): void {
    this.events = events;
  }

  async configure(payload: EncodeConfigPayload): Promise<void> {
    this.worker.postMessage({ id: this.nextId++, type: 'configure', payload } satisfies EncodeRequest);
  }

  /** 帧所有权转移给 Worker，调用方不得再使用 */
  sendFrame(index: number, frame: ImageBitmap): void {
    if (this.closed) {
      frame.close();
      return;
    }
    this.worker.postMessage({ id: this.nextId++, type: 'frame', payload: { index, frame } } satisfies EncodeRequest, [
      frame,
    ]);
  }

  finalize(): Promise<Blob> {
    return new Promise((resolve, reject) => {
      if (this.pending) {
        reject(new Error('encode-busy'));
        return;
      }
      this.pending = { resolve, reject };
      this.worker.postMessage({ id: this.nextId++, type: 'finalize' } satisfies EncodeRequest);
    });
  }

  cancel(): void {
    this.worker.postMessage({ id: this.nextId++, type: 'cancel' } satisfies EncodeRequest);
    this.release();
  }

  private release() {
    this.closed = true;
    this.worker.terminate();
    this.reject(new Error('cancelled'));
  }

  dispose(): void {
    if (this.closed) return;
    this.release();
  }
}
