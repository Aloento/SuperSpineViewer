import type { ExportOptions } from './presets';

export interface WebmExportHandle {
  cancel: () => void;
}

// M3 实现：WebCodecs VideoEncoder（vp09.00.10.08 + alpha:'keep' + I420A）+ 原生 muxer
export async function exportWebm(
  _frames: ImageBitmap[],
  _options: ExportOptions,
): Promise<Blob> {
  throw new Error('exportWebm 尚未实现（M3）');
}
