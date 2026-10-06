import type { EncodeConfigPayload, EncodeRequest, EncodeResponse } from './protocol';
import { WebmWriter } from '../export/webm';
import { encodePng, zipEntries, type ZipEntry } from '../export/apng';

interface Chunk {
  key: boolean;
  data: Uint8Array;
}

let config: EncodeConfigPayload | null = null;
let replyId = 0;
let cancelled = false;
// 代际号：configure/cancel 各自递增，旧一轮的编码器回调按代际丢弃，防止跨导出串帧
let generation = 0;

let colorEncoder: VideoEncoder | null = null;
let alphaEncoder: VideoEncoder | null = null;
let writer: WebmWriter | null = null;
let colorQueue: Chunk[] = [];
let alphaQueue: Chunk[] = [];
let muxed = 0;
let keyframeEvery = 60;

let canvas: OffscreenCanvas | null = null;
let ctx: OffscreenCanvasRenderingContext2D | null = null;
const zipStore: ZipEntry[] = [];

function post(message: EncodeResponse) {
  if (!cancelled) self.postMessage(message);
}

function fail(error: unknown) {
  const message = error instanceof Error ? error.message : String(error);
  reset();
  post({ id: replyId, type: 'error', payload: { message } });
}

function reset() {
  generation++;
  for (const encoder of [colorEncoder, alphaEncoder]) {
    try {
      if (encoder && encoder.state !== 'closed') encoder.close();
    } catch {
      // 队列非空或状态异常的编码器无法关闭，回调已由代际号隔离，忽略
    }
  }
  colorEncoder = null;
  alphaEncoder = null;
  writer = null;
  colorQueue = [];
  alphaQueue = [];
  zipStore.length = 0;
  muxed = 0;
  config = null;
}

function onChunk(gen: number, queue: Chunk[], chunk: EncodedVideoChunk) {
  // 取消后旧编码器的迟到回调不得写进新一轮导出的队列
  if (gen !== generation) return;
  queue.push({ key: chunk.type === 'key', data: copy(chunk) });
  pumpPairs();
}

function onEncodeError(gen: number, error: unknown) {
  if (gen !== generation) return;
  fail(error);
}

function copy(chunk: EncodedVideoChunk): Uint8Array {
  const data = new Uint8Array(chunk.byteLength);
  chunk.copyTo(data);
  return data;
}

// 两路编码器输出各自保序，按到达顺序配对即可还原帧序
function pumpPairs() {
  if (!writer) return;
  while (colorQueue.length > 0 && alphaQueue.length > 0) {
    const color = colorQueue.shift()!;
    const alpha = alphaQueue.shift()!;
    writer.addFrame(muxed, color.data, alpha.data, color.key);
    muxed += 1;
    post({ id: replyId, type: 'progress', payload: { encoded: muxed, total: config?.frameCount ?? muxed } });
  }
}

function encoderConfig(payload: EncodeConfigPayload, bitrate: number): VideoEncoderConfig {
  return { codec: 'vp09.00.10.08', width: payload.width, height: payload.height, bitrate, framerate: payload.fps, latencyMode: 'quality' };
}

function configure(payload: EncodeConfigPayload) {
  reset();
  config = payload;
  cancelled = false;
  keyframeEvery = Math.max(1, Math.round(payload.fps * 2));

  if (payload.format !== 'vp9') return;
  // reset 已推进代际，编码器回调绑定本轮代际：取消关闭旧编码器后，其迟到回调即被丢弃
  const gen = generation;
  colorEncoder = new VideoEncoder({
    output: (chunk) => onChunk(gen, colorQueue, chunk),
    error: (error) => onEncodeError(gen, error),
  });
  alphaEncoder = new VideoEncoder({
    output: (chunk) => onChunk(gen, alphaQueue, chunk),
    error: (error) => onEncodeError(gen, error),
  });
  // alpha 是单通道灰度平面，信息量远低于色彩流，一半码率足够
  colorEncoder.configure(encoderConfig(payload, payload.bitrate));
  alphaEncoder.configure(encoderConfig(payload, Math.round(payload.bitrate / 2)));
  writer = new WebmWriter(payload.width, payload.height, payload.fps);
}

function pixelsOf(frame: ImageBitmap): Uint8ClampedArray {
  const { width, height } = config!;
  if (!canvas || canvas.width !== width || canvas.height !== height) {
    canvas = new OffscreenCanvas(width, height);
    ctx = canvas.getContext('2d', { willReadFrequently: true });
  }
  if (!ctx) throw new Error('encode: 无法创建 2D 上下文');
  ctx.clearRect(0, 0, width, height);
  ctx.drawImage(frame, 0, 0, width, height);
  return ctx.getImageData(0, 0, width, height).data;
}

/** alpha 平面（I420：Y=alpha，U/V 置 128 中性灰）；缓冲按尺寸复用。
 *  RGBA 侧直接用 getImageData 的输出（VideoFrame 构造时自行拷贝），不再多复制一份 */
let alphaPlane: Uint8Array | null = null;

function alphaPlaneOf(data: Uint8ClampedArray): Uint8Array {
  const { width, height } = config!;
  const count = width * height;
  const alphaLen = count + (width >> 1) * (height >> 1) * 2;
  if (!alphaPlane || alphaPlane.length !== alphaLen) alphaPlane = new Uint8Array(alphaLen);
  const alpha = alphaPlane;
  for (let i = 0; i < count; i++) alpha[i] = data[i * 4 + 3];
  alpha.fill(128, count);
  return alpha;
}

function backpressure(): Promise<void> {
  const color = colorEncoder;
  const alpha = alphaEncoder;
  if (!color || !alpha) return Promise.resolve();
  const settled = () => cancelled || color.state === 'closed' || (color.encodeQueueSize <= 4 && alpha.encodeQueueSize <= 4);
  if (settled()) return Promise.resolve();
  return new Promise((resolve) => {
    const tick = () => (settled() ? resolve() : void setTimeout(tick, 4));
    setTimeout(tick, 4);
  });
}

async function encodeFrame(index: number, frame: ImageBitmap) {
  const payload = config;
  if (!payload) throw new Error('encode: 未配置');
  const timestamp = Math.round((index * 1_000_000) / payload.fps);
  const duration = Math.round(1_000_000 / payload.fps);
  const keyFrame = index % keyframeEvery === 0;
  const pixels = pixelsOf(frame);
  frame.close();
  const alpha = alphaPlaneOf(pixels);

  if (payload.format === 'vp9') {
    // getImageData 已是直通 alpha，色彩流按直通编码；alpha 通道本身走独立的灰度流
    const colorFrame = new VideoFrame(pixels, {
      format: 'RGBA',
      codedWidth: payload.width,
      codedHeight: payload.height,
      timestamp,
      duration,
    });
    const alphaFrame = new VideoFrame(alpha, {
      format: 'I420',
      codedWidth: payload.width,
      codedHeight: payload.height,
      timestamp,
      duration,
      // alpha 当亮度平面处理，满量程避免被限制范围再压一档动态
      colorSpace: { primaries: 'smpte170m', transfer: 'smpte170m', matrix: 'smpte170m', fullRange: true },
    });
    colorEncoder!.encode(colorFrame, { keyFrame });
    alphaEncoder!.encode(alphaFrame, { keyFrame });
    colorFrame.close();
    alphaFrame.close();
    await backpressure();
    return;
  }

  zipStore.push({
    name: `frame_${String(index + 1).padStart(5, '0')}.png`,
    data: new Uint8Array(encodePng(pixels.buffer as ArrayBuffer, payload.width, payload.height)),
  });
  post({ id: replyId, type: 'progress', payload: { encoded: index + 1, total: payload.frameCount } });
}

async function finalize() {
  const payload = config;
  if (!payload) throw new Error('encode: 未配置');

  if (payload.format === 'vp9') {
    await Promise.all([colorEncoder!.flush(), alphaEncoder!.flush()]);
    pumpPairs();
    if (muxed !== payload.frameCount) throw new Error(`encode: 帧数不匹配 ${muxed}/${payload.frameCount}`);
    const blob = writer!.finalize();
    reset();
    post({ id: replyId, type: 'done', payload: { blob } });
    return;
  }

  if (zipStore.length !== payload.frameCount) {
    throw new Error(`encode: 帧数不匹配 ${zipStore.length}/${payload.frameCount}`);
  }
  const blob = zipEntries(zipStore);
  reset();
  post({ id: replyId, type: 'done', payload: { blob } });
}

async function handle(request: EncodeRequest) {
  replyId = request.id;
  switch (request.type) {
    case 'configure':
      configure(request.payload);
      break;
    case 'frame':
      if (cancelled) {
        request.payload.frame.close();
        return;
      }
      await encodeFrame(request.payload.index, request.payload.frame);
      break;
    case 'finalize':
      await finalize();
      break;
    case 'cancel':
      cancelled = true;
      reset();
      break;
  }
}

// configure/frame/finalize 必须串行：frame 在背压里等待时 finalize 可能已到达
let chain: Promise<void> = Promise.resolve();

self.onmessage = (event: MessageEvent<EncodeRequest>) => {
  chain = chain.then(() => handle(event.data)).catch(fail);
};
