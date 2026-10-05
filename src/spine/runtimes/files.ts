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

/** atlas 里的 page 行就是图片文件名；4.2/4.3 的 atlas 没有缩进，只能按扩展名识别。 */
export function atlasPageNames(atlasText: string): string[] {
  const names: string[] = [];
  for (const line of atlasText.split('\n')) {
    const trimmed = line.trim();
    if (IMAGE_EXT.test(trimmed)) names.push(trimmed);
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
