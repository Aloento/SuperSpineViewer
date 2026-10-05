import { RuntimeError } from '../types';

export type FileMap = Record<string, ArrayBuffer>;

function normalize(name: string) {
  return name.replace(/\\/g, '/').replace(/^\.\//, '').toLowerCase();
}

export function findFile(files: FileMap, name: string): string | null {
  const wanted = normalize(name);
  if (files[name]) return name;
  for (const key of Object.keys(files)) {
    const normalized = normalize(key);
    if (normalized === wanted || normalized.endsWith('/' + wanted)) return key;
  }
  return null;
}

const IMAGE_EXT = /\.(png|jpe?g|webp)$/i;

// page 属性（size/format/filter/repeat/pma/scale）与 region 属性（rotate/xy/offset/index/bounds）不重叠，
// 4.0 起 page 属性带缩进，2.x 的 page 第二段是 format 而不是 size
const PAGE_PROPERTY = /^[\t ]*(size|format|filter|repeat|pma|scale)\s*:/i;

/**
 * 图集 page 名 = 顶格的图片文件名行，且下一行是 page 属性。
 * 不能按「任何以 .png 结尾的行」判定：region 名也可以 .png 结尾，会被误认成 page。
 */
export function atlasPageNames(atlasText: string): string[] {
  const lines = atlasText.split('\n');
  const names: string[] = [];
  for (let i = 0; i + 1 < lines.length; i++) {
    const line = lines[i].trimEnd();
    if (/^\s/.test(line) || !IMAGE_EXT.test(line)) continue;
    if (PAGE_PROPERTY.test(lines[i + 1])) names.push(line.trim());
  }
  return names;
}

/** 运行时按 atlas 里写的相对路径回读贴图，这里做大小写/路径不敏感的匹配。 */
export function createFileReader(files: FileMap) {
  return async (path: string): Promise<ArrayBuffer> => {
    const key = findFile(files, path);
    if (!key) throw new RuntimeError('missingFile', path);
    return files[key].slice(0);
  };
}
