import type { EncodeConfigPayload, EncodeRequest, EncodeResponse } from '../workers/protocol';

export interface ExportClientEvents {
  onProgress: (encoded: number, total: number) => void;
}

/**
 * 与 encode.worker 的交互封装。帧消息只发不等，避免主线程被编码背压拖住，
 * 渲染与编码因此流水线并行；进度以 Worker 已编码帧数为准。
 * 取消只让 Worker 丢弃本轮状态（不 terminate），实例跨导出复用；
 * 仅 dispose 或出错后重建。
 */
export class ExportClient {
  private worker: Worker | null;
  private nextId = 1;
  private pending: { resolve: (blob: Blob) => void; reject: (error: Error) => void } | null = null;
  private events: ExportClientEvents = { onProgress: () => {} };
  /** 取消到下一轮 configure 之间为 true：残留消息一律丢弃 */
  private muted = false;
  /** 工作线程报过错，不可信，下次使用需重建 */
  private broken = false;

  constructor() {
    this.worker = this.spawn();
  }

  private spawn(): Worker {
    const worker = new Worker(new URL('../workers/encode.worker.ts', import.meta.url), { type: 'module' });
    worker.onmessage = (event: MessageEvent<EncodeResponse>) => this.receive(event.data);
    worker.onerror = (event) => {
      this.broken = true;
      this.reject(new Error(event.message || 'encode-worker-error'));
    };
    return worker;
  }

  private post(message: EncodeRequest, transfer?: Transferable[]): void {
    this.worker!.postMessage(message, { transfer });
  }

  private receive(response: EncodeResponse) {
    // 取消后旧一轮的残留消息（含 done 里的 Blob）一律丢弃
    if (this.muted) return;
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
    this.broken = true;
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
    if (this.broken) {
      // 出错过的 worker 状态未知，重建后再配置
      this.worker?.terminate();
      this.worker = this.spawn();
      this.broken = false;
    }
    this.muted = false;
    this.post({ id: this.nextId++, type: 'configure', payload });
  }

  /** 帧所有权转移给 Worker，调用方不得再使用 */
  sendFrame(index: number, frame: ImageBitmap): void {
    if (this.muted) {
      frame.close();
      return;
    }
    this.post({ id: this.nextId++, type: 'frame', payload: { index, frame } }, [frame]);
  }

  finalize(): Promise<Blob> {
    return new Promise((resolve, reject) => {
      if (this.pending) {
        reject(new Error('encode-busy'));
        return;
      }
      this.pending = { resolve, reject };
      this.post({ id: this.nextId++, type: 'finalize' });
    });
  }

  cancel(): void {
    this.muted = true;
    if (!this.broken) this.post({ id: this.nextId++, type: 'cancel' });
    this.reject(new Error('cancelled'));
  }

  dispose(): void {
    if (!this.worker) return;
    this.worker.terminate();
    this.worker = null;
    this.reject(new Error('cancelled'));
  }
}
