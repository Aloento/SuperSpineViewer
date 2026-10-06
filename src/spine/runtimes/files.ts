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

export interface AtlasPage {
  name: string;
  /** 图集里 pma: 声明的字面值；3.9 以前的导出没这一行，为 null */
  pma: boolean | null;
}

/**
 * 图集 page = 顶格的图片文件名行，且上一行为空或下一行是 page 属性。
 * 不能按「任何以 .png 结尾的行」判定：region 名也可以 .png 结尾，会被误认成 page。
 * pma 行 4.0 起带缩进、4.2 起不带，两种都要认。
 */
export function atlasPages(atlasText: string): AtlasPage[] {
  const lines = atlasText.split('\n');
  const pages: AtlasPage[] = [];
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trimEnd();
    if (/^\s/.test(line) || !IMAGE_EXT.test(line)) continue;
    if (!(i === 0 || lines[i - 1].trim() === '' || PAGE_PROPERTY.test(lines[i + 1] ?? ''))) continue;
    let pma: boolean | null = null;
    for (let j = i + 1; j < lines.length; j++) {
      const declared = /^[\t ]*pma\s*:\s*(\w+)\s*$/i.exec(lines[j].trimEnd());
      if (declared) {
        pma = declared[1].toLowerCase() === 'true';
        break;
      }
      if (!PAGE_PROPERTY.test(lines[j])) break;
    }
    pages.push({ name: line.trim(), pma });
  }
  return pages;
}

/** 供只要文件名的调用方使用 */
export function atlasPageNames(atlasText: string): string[] {
  return atlasPages(atlasText).map((page) => page.name);
}

/** 运行时按 atlas 里写的相对路径回读贴图，这里做大小写/路径不敏感的匹配。 */
export function createFileReader(files: FileMap) {
  return async (path: string): Promise<ArrayBuffer> => {
    const key = findFile(files, path);
    if (!key) throw new RuntimeError('missingFile', path);
    return files[key].slice(0);
  };
}
