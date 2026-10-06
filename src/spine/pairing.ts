import type { FileMap } from './runtimes/files';
import { atlasPageNames, findFile } from './runtimes/files';

/**
 * §3.3 文件配对：解包目录里骨架与图集常不同名，按旧 Java 版 Loader 的推导
 * （去数据后缀 → 去 -pma/-pro/-ess → 补 .atlas[.txt|.bytes]）先精确配对，
 * 配不上再退回目录里的任意图集（skel 与 pma 图集共目录是常态）。
 */

const SKELETON_EXT = /\.(skel|json|txt|bytes)$/i;
const ATLAS_EXT = /\.atlas($|\.(txt|bytes)$)/i;
const DATA_SUFFIX = /\.(skel|json|txt|bytes)$/i;
const PACK_SUFFIX = /-(pma|pro|ess)$/i;

export interface AssetPair {
  skeletonFile: string;
  atlasFile: string;
  /** 0 = 名称推导精确命中，1 = 图集与骨架同前缀（spineboy.skel ↔ spineboy-pma.atlas），2 = 目录里的任意图集 */
  match: number;
}

export interface AssetInventory {
  /** 有序的可加载组合：推导命中优先，其次 .skel 优先，最后按文件名 */
  pairs: AssetPair[];
  hasSkeleton: boolean;
  hasAtlas: boolean;
}

function skeletonRank(name: string): number {
  if (/\.skel$/i.test(name)) return 0;
  if (/\.json$/i.test(name)) return 1;
  return 2;
}

export function pairAssets(files: FileMap): AssetInventory {
  const names = Object.keys(files);
  const atlases = names.filter((name) => ATLAS_EXT.test(name)).sort();
  // .atlas.txt / .atlas.bytes 也是图集，不能同时当骨架
  const skeletons = names.filter((name) => SKELETON_EXT.test(name) && !ATLAS_EXT.test(name));
  const pairs: AssetPair[] = [];

  for (const skeletonFile of skeletons) {
    const stripped = skeletonFile.replace(DATA_SUFFIX, '');
    const bases = [stripped, stripped.replace(PACK_SUFFIX, '')];
    let atlasFile: string | null = null;
    for (const base of bases) {
      for (const suffix of ['.atlas', '.atlas.txt', '.atlas.bytes']) {
        const hit = findFile(files, base + suffix);
        if (hit) {
          atlasFile = hit;
          break;
        }
      }
      if (atlasFile) break;
    }
    let match = 0;
    if (!atlasFile) {
      // 图集常带 -pma 等后缀而骨架不带：按「图集名以骨架基名开头」再配一轮
      const prefixes = bases.map((base) => base.toLowerCase() + '-');
      atlasFile = atlases.find((name) => prefixes.some((prefix) => name.toLowerCase().startsWith(prefix))) ?? null;
      if (atlasFile) match = 1;
    }
    if (!atlasFile && atlases.length > 0) {
      atlasFile = atlases[0];
      match = 2;
    }
    if (atlasFile) pairs.push({ skeletonFile, atlasFile, match });
  }

  pairs.sort((a, b) => {
    if (a.match !== b.match) return a.match - b.match;
    if (skeletonRank(a.skeletonFile) !== skeletonRank(b.skeletonFile)) {
      return skeletonRank(a.skeletonFile) - skeletonRank(b.skeletonFile);
    }
    return a.skeletonFile.localeCompare(b.skeletonFile);
  });

  return {
    pairs,
    hasSkeleton: skeletons.length > 0,
    hasAtlas: atlases.length > 0,
  };
}

/** 图集 page 按 createFileReader 的同一套匹配规则预检，缺失时在 UI 前列出文件名 */
export function missingAtlasPages(files: FileMap, atlasFile: string): string[] {
  const text = new TextDecoder('utf-8', { fatal: false }).decode(files[atlasFile]);
  return atlasPageNames(text).filter((page) => findFile(files, page) === null);
}
