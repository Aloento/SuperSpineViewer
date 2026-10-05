import type { ExportOptions } from './presets';

export interface ApngExportHandle {
  cancel: () => void;
}

// M3 实现：UPNG 逐帧编码 PNG 序列，打包为 zip
export async function exportApngZip(
  _frames: ImageBitmap[],
  _options: ExportOptions,
): Promise<Blob> {
  throw new Error('exportApngZip 尚未实现（M3）');
}
