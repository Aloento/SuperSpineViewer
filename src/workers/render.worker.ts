import CanvasKitInit from 'canvaskit-wasm';
import wasmUrl from 'canvaskit-wasm/bin/canvaskit.wasm?url';
import type { CanvasKit, ImageInfo, MallocObj, Surface } from 'canvaskit-wasm';
import type { RenderRequest, RenderResponse, SkeletonPayload } from './protocol';

type SpineModule = typeof import('@esotericsoftware/spine-canvaskit');

interface RenderSession {
  surface: Surface;
  renderer: InstanceType<SpineModule['SkeletonRenderer']>;
  drawable: InstanceType<SpineModule['SkeletonDrawable']>;
  imageInfo: ImageInfo;
  pixels: MallocObj;
  pixelView: Uint8ClampedArray<ArrayBuffer>;
}

let target: OffscreenCanvas | null = null;
let size = { width: 0, height: 0 };
let ck: CanvasKit | null = null;
let ckPromise: Promise<CanvasKit> | null = null;
let surface: Surface | null = null;
let session: RenderSession | null = null;
let lastTimeMs = -1;

function post(message: RenderResponse, transfer: Transferable[] = []) {
  self.postMessage(message, transfer);
}

function message(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

async function ensureCanvasKit(): Promise<CanvasKit> {
  if (ck) return ck;
  ckPromise ??= CanvasKitInit({ locateFile: () => wasmUrl });
  ck = await ckPromise;
  return ck;
}

function ensureSurface(canvasKit: CanvasKit, canvas: OffscreenCanvas): Surface {
  if (surface) return surface;
  // worker 里 WebGL 不可用（或创建失败）时退回 CPU 光栅 surface，两条路径都用 canvas.readPixels 取像素
  let created: Surface | null = null;
  try {
    created = canvasKit.MakeWebGLCanvasSurface(canvas as unknown as HTMLCanvasElement);
  } catch {
    created = null;
  }
  created ??= canvasKit.MakeSurface(size.width, size.height);
  if (!created) throw new Error('canvas-surface-unavailable');
  surface = created;
  return created;
}

function createReader(files: Record<string, ArrayBuffer>) {
  return async (requested: string): Promise<ArrayBuffer> => {
    const name = requested.split('/').pop() ?? requested;
    const data = files[requested] ?? files[name];
    if (!data) throw new Error(`missing-file:${name}`);
    return data;
  };
}

function fitSkeleton(
  skeleton: InstanceType<SpineModule['Skeleton']>,
  data: { x: number; y: number; width: number; height: number },
) {
  if (data.width <= 0 || data.height <= 0) {
    skeleton.x = size.width / 2;
    skeleton.y = size.height / 2;
    return;
  }
  const scale = Math.min(size.width / data.width, size.height / data.height) * 0.9;
  skeleton.scaleX = scale;
  skeleton.scaleY = scale;
  skeleton.x = size.width / 2 - (data.x + data.width / 2) * scale;
  // spine-canvaskit 把 Skeleton.yDown 置为 true，world 的 +y 对应屏幕向上
  skeleton.y = size.height / 2 + (data.y + data.height / 2) * scale;
}

async function loadSkeleton(payload: SkeletonPayload): Promise<string> {
  if (!target) throw new Error('canvas-not-initialized');
  const canvasKit = await ensureCanvasKit();
  const renderSurface = ensureSurface(canvasKit, target);
  const spine = await import('@esotericsoftware/spine-canvaskit');
  const readFile = createReader(payload.files);
  const atlas = await spine.loadTextureAtlas(canvasKit, payload.atlasFile, readFile);
  const data = await spine.loadSkeletonData(payload.skeletonFile, atlas, readFile);
  const animation = data.animations[0];
  if (!animation) throw new Error('no-animation');
  const drawable = new spine.SkeletonDrawable(data);
  fitSkeleton(drawable.skeleton, data);
  drawable.animationState.setAnimation(0, animation.name, true);

  const imageInfo: ImageInfo = {
    width: size.width,
    height: size.height,
    colorType: canvasKit.ColorType.RGBA_8888,
    alphaType: canvasKit.AlphaType.Unpremul,
    colorSpace: canvasKit.ColorSpace.SRGB,
  };
  const pixels = canvasKit.Malloc(Uint8Array, size.width * size.height * 4);
  const view = pixels.toTypedArray();
  session = {
    surface: renderSurface,
    renderer: new spine.SkeletonRenderer(canvasKit),
    drawable,
    imageInfo,
    pixels,
    pixelView: new Uint8ClampedArray(view.buffer as ArrayBuffer, view.byteOffset, view.byteLength),
  };
  lastTimeMs = -1;
  return animation.name;
}

async function renderFrame(timeMs: number): Promise<ImageBitmap> {
  if (!session || !ck) throw new Error('skeleton-not-loaded');
  const { surface: renderSurface, renderer, drawable, imageInfo, pixels, pixelView } = session;
  if (lastTimeMs < 0) lastTimeMs = timeMs;
  drawable.update(Math.max(0, (timeMs - lastTimeMs) / 1000));
  lastTimeMs = timeMs;

  const canvas = renderSurface.getCanvas();
  canvas.clear(ck.TRANSPARENT);
  renderer.render(canvas, drawable);
  renderSurface.flush();
  canvas.readPixels(0, 0, imageInfo, pixels);
  return createImageBitmap(new ImageData(pixelView, imageInfo.width, imageInfo.height));
}

async function handle(request: RenderRequest) {
  switch (request.type) {
    case 'init': {
      target = request.payload.canvas;
      size = { width: request.payload.width, height: request.payload.height };
      post({ id: request.id, type: 'ready' });
      break;
    }
    case 'load': {
      try {
        const animation = await loadSkeleton(request.payload);
        post({ id: request.id, type: 'loaded', payload: { animation } });
      } catch (error) {
        session = null;
        post({ id: request.id, type: 'error', payload: { message: message(error) } });
      }
      break;
    }
    case 'render': {
      try {
        const frame = await renderFrame(request.payload.timeMs);
        post({ id: request.id, type: 'frame', payload: { index: request.payload.index, frame } }, [frame]);
      } catch (error) {
        post({ id: request.id, type: 'error', payload: { message: message(error) } });
      }
      break;
    }
    case 'dispose': {
      session = null;
      surface?.delete();
      surface = null;
      target = null;
      break;
    }
  }
}

self.onmessage = (event: MessageEvent<RenderRequest>) => {
  void handle(event.data);
};
