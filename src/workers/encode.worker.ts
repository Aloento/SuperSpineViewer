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
  for (const encoder of [colorEncoder, alphaEncoder]) {
    try {
      if (encoder && encoder.state !== 'closed') encoder.close();
    } catch {
      // 状态异常的编码器已无法关闭，忽略
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

function onColorChunk(chunk: EncodedVideoChunk) {
  colorQueue.push({ key: chunk.type === 'key', data: copy(chunk) });
  pumpPairs();
}

function onAlphaChunk(chunk: EncodedVideoChunk) {
  alphaQueue.push({ key: chunk.type === 'key', data: copy(chunk) });
  pumpPairs();
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
  config = payload;
  cancelled = false;
  keyframeEvery = Math.max(1, Math.round(payload.fps * 2));

  if (payload.format !== 'vp9') return;
  colorEncoder = new VideoEncoder({ output: onColorChunk, error: fail });
  alphaEncoder = new VideoEncoder({ output: onAlphaChunk, error: fail });
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

/** 直通 RGBA 字节 + alpha 平面（I420：Y=alpha，U/V 置 128 中性灰） */
function planesOf(data: Uint8ClampedArray): { rgba: Uint8Array; alpha: Uint8Array } {
  const { width, height } = config!;
  const rgba = new Uint8Array(data.length);
  rgba.set(data);
  const count = width * height;
  const alpha = new Uint8Array(count + (width >> 1) * (height >> 1) * 2);
  for (let i = 0; i < count; i++) alpha[i] = data[i * 4 + 3];
  alpha.fill(128, count);
  return { rgba, alpha };
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
  const { rgba, alpha } = planesOf(pixels);

  if (payload.format === 'vp9') {
    // getImageData 已是直通 alpha，色彩流按直通编码；alpha 通道本身走独立的灰度流
    const colorFrame = new VideoFrame(rgba, {
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
    data: new Uint8Array(encodePng(rgba.buffer as ArrayBuffer, payload.width, payload.height)),
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
      cancelled = false;
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
