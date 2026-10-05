import type { EncodeRequest, EncodeResponse } from './protocol';

export type {} from './protocol';

function post(message: EncodeResponse) {
  self.postMessage(message);
}

self.onmessage = (event: MessageEvent<EncodeRequest>) => {
  const request = event.data;

  switch (request.type) {
    case 'configure':
      // M3：VP9-alpha 时用 vp09.00.10.08 + alpha:'keep'，输入 I420A
      break;
    case 'frame':
      // M3：RGBA → I420A → VideoFrame → VideoEncoder.encode()
      break;
    case 'finalize':
      // M3：flush 后交给 muxer 产出 Blob，再 post({ type: 'done', payload: { blob } })
      break;
    case 'cancel':
      break;
  }
};

void post;
