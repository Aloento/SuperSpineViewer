import { atlasPages, findFile, type FileMap } from './runtimes/files';

/** 图集 page 的直通 alpha RGBA 像素 */
export interface StraightPage {
  width: number;
  height: number;
  data: Uint8Array;
}

/**
 * 直通 alpha 判据：预乘像素恒满足 max(R,G,B) ≤ A，出现 max(R,G,B) > A 的半透明像素
 * 即证明该页按直通 alpha 存储。只统计 A ∈ [64,250] 的像素——低 alpha 段两种存储都有
 * 编码噪声（实测预乘页在 A<64 时误判率可达 7%，A≥64 时为 0），直通页在该段仍有 11%。
 * 半透明像素太少时两种解释无实际差别，也按直通处理。
 */
function looksStraightAlpha(data: Uint8Array): boolean {
  let semi = 0;
  let violated = 0;
  for (let i = 0; i < data.length; i += 4) {
    const alpha = data[i + 3];
    if (alpha < 64 || alpha > 250) continue;
    semi += 1;
    const peak = Math.max(data[i], data[i + 1], data[i + 2]);
    // 容差 2：预乘取整上界是 A，留出编码噪声
    if (peak > alpha + 2) violated += 1;
  }
  return semi < 64 || violated / semi > 0.02;
}

/** 预乘 → 直通；a=0 处 rgb 归零，避免反预乘放大噪声 */
function unpremultiply(data: Uint8Array): void {
  for (let i = 0; i < data.length; i += 4) {
    const alpha = data[i + 3];
    if (alpha >= 255) continue;
    if (alpha === 0) {
      data[i] = 0;
      data[i + 1] = 0;
      data[i + 2] = 0;
      continue;
    }
    const inv = 255 / alpha;
    data[i] = Math.min(255, data[i] * inv);
    data[i + 1] = Math.min(255, data[i + 1] * inv);
    data[i + 2] = Math.min(255, data[i + 2] * inv);
  }
}

/**
 * 解码单个 page 并归一化成直通 alpha。premultiplyAlpha:'none' 保证拿到文件字面值，
 * 物理预乘的 page（贴图工具导出或 atlas 声明 pma:true）再手工反预乘。
 */
async function decodeStraightPage(bytes: ArrayBuffer, declaredPma: boolean | null): Promise<StraightPage> {
  const bitmap = await createImageBitmap(new Blob([bytes]), { premultiplyAlpha: 'none' });
  const canvas = new OffscreenCanvas(bitmap.width, bitmap.height);
  const context = canvas.getContext('2d', { alpha: true })!;
  context.drawImage(bitmap, 0, 0);
  bitmap.close();
  const imageData = context.getImageData(0, 0, canvas.width, canvas.height);
  const data = new Uint8Array(imageData.data.buffer);
  // 空图（全不透明或无半透明像素）无需反预乘，判据会给出直通
  if ((declaredPma ?? !looksStraightAlpha(data)) && data.length > 0) unpremultiply(data);
  return { width: canvas.width, height: canvas.height, data };
}

/**
 * 解码图集全部 page。同步图集加载器（3.1 legacy、3.4–3.8 webgl）在构造期就要拿到纹理，
 * 所以先一次性解好；键做 / 归一并转小写，与 atlas 里的书写路径大小写无关。
 */
export async function decodeStraightPages(files: FileMap, atlasText: string): Promise<Map<string, StraightPage>> {
  const pages = new Map<string, StraightPage>();
  for (const page of atlasPages(atlasText)) {
    const key = findFile(files, page.name) ?? findFile(files, page.name.split('/').pop() ?? page.name);
    if (!key) continue;
    pages.set(page.name.replace(/\\/g, '/').toLowerCase(), await decodeStraightPage(files[key], page.pma));
  }
  return pages;
}

export function findStraightPage(pages: Map<string, StraightPage>, name: string): StraightPage | null {
  return pages.get(String(name).replace(/\\/g, '/').toLowerCase()) ?? null;
}

/** 直通像素转画布源，供 WebGL 在 UNPACK_PREMULTIPLY_ALPHA_WEBGL=false 下上传 */
export function straightPageCanvas(page: StraightPage): OffscreenCanvas {
  const canvas = new OffscreenCanvas(page.width, page.height);
  const context = canvas.getContext('2d', { alpha: true })!;
  context.putImageData(new ImageData(new Uint8ClampedArray(page.data), page.width, page.height), 0, 0);
  return canvas;
}
