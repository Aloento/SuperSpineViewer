import type {
  ErrorPayload,
  LoadPayload,
  LoadResponsePayload,
  RenderRequest,
  RenderResponse,
  TransformPayload,
} from '../workers/protocol';

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

  /** 渲染指定时刻的单帧；导出的逐帧编码与像素回归比对都走这条确定性路径。
   *  transform 只在预览播放中传入，导出会话不传，保证导出像素不受预览偏移/缩放影响 */
  async frame(timeMs: number, transform?: TransformPayload | null): Promise<ImageBitmap> {
    const response = await this.send({
      id: this.nextId++,
      type: 'render',
      payload: transform ? { timeMs, transform } : { timeMs },
    });
    if (response.type !== 'frame') throw new Error('unexpected-response');
    return response.payload.frame;
  }

  /** 跳到 timeMs 并渲染该时刻帧；预览暂停态拖动进度条走这里 */
  async seek(timeMs: number): Promise<ImageBitmap> {
    const response = await this.send({ id: this.nextId++, type: 'seek', payload: { timeMs } });
    if (response.type !== 'frame') throw new Error('unexpected-response');
    return response.payload.frame;
  }

  /** 切换动画（重置到 0）并渲染首帧 */
  async setAnimation(animation: string, loop: boolean): Promise<ImageBitmap> {
    const response = await this.send({ id: this.nextId++, type: 'setAnimation', payload: { animation, loop } });
    if (response.type !== 'frame') throw new Error('unexpected-response');
    return response.payload.frame;
  }

  /** 切换皮肤并在当前时刻原地重绘 */
  async setSkin(skin: string): Promise<ImageBitmap> {
    const response = await this.send({ id: this.nextId++, type: 'setSkin', payload: { skin } });
    if (response.type !== 'frame') throw new Error('unexpected-response');
    return response.payload.frame;
  }

  /** 更新基础偏移/缩放并在当前时刻原地重绘 */
  async setTransform(offsetX: number, offsetY: number, scale: number): Promise<ImageBitmap> {
    const response = await this.send({ id: this.nextId++, type: 'setTransform', payload: { offsetX, offsetY, scale } });
    if (response.type !== 'frame') throw new Error('unexpected-response');
    return response.payload.frame;
  }

  /** 从 0 时刻起按 rAF 推进时间轴；返回停止函数。导出期间可暂停后从原时刻恢复 */
  play(
    onFrame: (frame: ImageBitmap) => void,
    onError: (error: Error) => void,
    options?: {
      startOffsetMs?: number;
      onElapsed?: (elapsedMs: number) => void;
      /** 每帧取最新的用户偏移/缩放，播放中拖动滑杆立即生效，无需单独发消息 */
      getTransform?: () => TransformPayload | null;
    },
  ): () => void {
    let stopped = false;
    const startedAt = performance.now();
    const offset = options?.startOffsetMs ?? 0;

    const loop = async () => {
      while (!stopped) {
        const elapsed = performance.now() - startedAt + offset;
        options?.onElapsed?.(elapsed);
        const frame = await this.frame(elapsed, options?.getTransform?.() ?? null);
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
