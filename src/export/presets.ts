export type ExportFormat = 'vp9' | 'apng';

export interface CanvasPreset {
  id: string;
  labelKey: string;
  width: number;
  height: number;
}

export const MAX_CANVAS_SIZE = 4096;

export const canvasPresets: CanvasPreset[] = [
  { id: 'sd', labelKey: 'export.canvas.sd', width: 640, height: 640 },
  { id: 'hd', labelKey: 'export.canvas.hd', width: 1024, height: 1024 },
  { id: 'uhd', labelKey: 'export.canvas.uhd', width: 2048, height: 2048 },
];

export interface ExportOptions {
  format: ExportFormat;
  fps: 30 | 60;
  bitrate: number;
  width: number;
  height: number;
}

export const defaultExportOptions: ExportOptions = {
  format: 'vp9',
  fps: 30,
  bitrate: 4_000_000,
  width: 1024,
  height: 1024,
};

/** VP9 的 I420 色度半采样要求偶数边长，自定义尺寸向下取整；上限 4096，最小 16 */
export function sanitizeSize(value: number): number {
  const clamped = Math.min(MAX_CANVAS_SIZE, Math.max(16, Math.floor(value) || 16));
  return clamped % 2 === 0 ? clamped : clamped - 1;
}
