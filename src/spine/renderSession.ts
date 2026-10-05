import type { ErrorPayload, LoadPayload, LoadResponsePayload, RenderRequest, RenderResponse } from '../workers/protocol';

export class RenderWorkerError extends Error {
  code: string;
  attempts: LoadResponsePayload['attempts'];

  constructor(payload: ErrorPayload) {
    super(payload.message);
    this.name = 'RenderWorkerError';
    this.code = payload.code;
    this.attempts = payload.attempts;
  }
}

interface Pending {
  resolve: (response: RenderResponse) => void;
  reject: (error: Error) => void;
}

export class RenderSession {
  private readonly worker: Worker;
  private readonly pending = new Map<number, Pending>();
  private nextId = 1;

  constructor() {
    this.worker = new Worker(new URL('../workers/render.worker.ts', import.meta.url), { type: 'module' });
    this.worker.onmessage = (event: MessageEvent<RenderResponse>) => this.receive(event.data);
    this.worker.onerror = (event) => this.rejectAll(new Error(event.message || 'render-worker-error'));
  }

  private receive(response: RenderResponse) {
    const entry = this.pending.get(response.id);
    if (!entry) {
      if (response.type === 'frame') response.payload.frame.close();
      return;
    }
    this.pending.delete(response.id);
    if (response.type === 'error') entry.reject(new RenderWorkerError(response.payload));
    else entry.resolve(response);
  }

  private rejectAll(error: Error) {
    for (const entry of this.pending.values()) entry.reject(error);
    this.pending.clear();
  }

  private send(request: RenderRequest): Promise<RenderResponse> {
    return new Promise((resolve, reject) => {
      this.pending.set(request.id, { resolve, reject });
      this.worker.postMessage(request);
    });
  }

  async init(width: number, height: number): Promise<void> {
    await this.send({ id: this.nextId++, type: 'init', payload: { width, height } });
  }

  async load(payload: LoadPayload): Promise<LoadResponsePayload> {
    const response = await this.send({ id: this.nextId++, type: 'load', payload });
    if (response.type !== 'loaded') throw new Error('unexpected-response');
    return response.payload;
  }

  private async render(timeMs: number): Promise<ImageBitmap> {
    const response = await this.send({ id: this.nextId++, type: 'render', payload: { timeMs } });
    if (response.type !== 'frame') throw new Error('unexpected-response');
    return response.payload.frame;
  }

  /** 从 0 时刻起按 rAF 推进时间轴；返回停止函数 */
  play(onFrame: (frame: ImageBitmap) => void, onError: (error: Error) => void): () => void {
    let stopped = false;
    const startedAt = performance.now();

    const loop = async () => {
      while (!stopped) {
        const frame = await this.render(performance.now() - startedAt);
        if (stopped) {
          frame.close();
          return;
        }
        onFrame(frame);
        // 等一次 vsync，避免预览帧率超过显示刷新率
        await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
      }
    };

    loop().catch((error: unknown) => {
      if (!stopped) onError(error instanceof Error ? error : new Error(String(error)));
    });

    return () => {
      stopped = true;
    };
  }

  dispose(): void {
    this.worker.terminate();
    this.rejectAll(new Error('render-worker-disposed'));
  }
}
