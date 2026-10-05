import type { RenderRequest, RenderResponse } from './protocol';

export type {} from './protocol';

let canvas: OffscreenCanvas | null = null;
void canvas;

function post(message: RenderResponse, transfer: Transferable[] = []) {
  self.postMessage(message, transfer);
}

self.onmessage = (event: MessageEvent<RenderRequest>) => {
  const request = event.data;

  switch (request.type) {
    case 'init': {
      canvas = request.payload.canvas;
      post({ id: request.id, type: 'ready' });
      break;
    }
    case 'load':
      // M1：动态 import() spine-core + spine-canvaskit，用 CanvasKit 建骨架
      break;
    case 'render':
      // M1：canvas.clear(TRANSPARENT) → renderer.render(canvas, drawable) → readPixels
      break;
    case 'dispose':
      canvas = null;
      break;
  }
};
