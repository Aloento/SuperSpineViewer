import type { SpineVersionInfo } from './types';

export type { SpineVersionInfo };

/** 运行时 pack 未接入的原因码：UI 文案按 code 分支，避免匹配硬编码字符串 */
export type UnavailableRuntimeCode = 'legacy' | '2d' | 'future';

/** 已接入运行时的版本区间（M2a：3.8–4.3；3.0–3.7 与 2.x 在 M2b/M2c/M2d 接入） */
export const RUNTIME_RANGE = { min: { major: 3, minor: 8 }, max: { major: 4, minor: 3 } };

export interface RuntimeVersionSpec {
  raw: string;
  /** 加载运行时 pack 用的 id */
  packId: string;
}

/** UI 提示可引用的版本清单 */
export const SUPPORTED_SPINE_VERSIONS = [
  '2.1.27',
  '3.0.0',
  '3.2.0',
  '3.4.0',
  '3.5.0',
  '3.6.0',
  '3.7.0',
  '3.8.0',
  '4.0.0',
  '4.1.0',
  '4.2.0',
  '4.3.0',
] as const;

export const RUNTIME_VERSIONS: readonly RuntimeVersionSpec[] = [
  { raw: '4.3.0', packId: '4.3' },
  { raw: '4.2.0', packId: '4.2' },
  { raw: '4.1.0', packId: '4.1' },
  { raw: '4.0.0', packId: '4.0' },
  { raw: '3.8.0', packId: '3.8' },
];

export function parseSpineVersion(raw: string): SpineVersionInfo | null {
  const matched = /^(\d+)\.(\d+)(?:\.(\d+))?/.exec(raw.trim());
  if (!matched) return null;
  return {
    raw: raw.trim(),
    major: Number(matched[1]),
    minor: Number(matched[2]),
    patch: Number(matched[3] ?? 0),
  };
}

function compare(left: SpineVersionInfo, right: SpineVersionInfo): number {
  return left.major !== right.major ? left.major - right.major : left.minor - right.minor;
}

/** 该版本自身是否有已接入的 pack；null 表示有 */
export function unavailableRuntimeCode(version: SpineVersionInfo): UnavailableRuntimeCode | null {
  if (version.major < RUNTIME_RANGE.min.major) return '2d';
  if (
    version.major === RUNTIME_RANGE.min.major &&
    version.minor < RUNTIME_RANGE.min.minor
  ) {
    return 'legacy';
  }
  if (
    version.major > RUNTIME_RANGE.max.major ||
    (version.major === RUNTIME_RANGE.max.major && version.minor > RUNTIME_RANGE.max.minor)
  ) {
    return 'future';
  }
  return null;
}

/** 单个候选运行时 pack */
export interface RuntimeCandidate {
  version: string;
  packId: string;
}

export interface RuntimeResolution {
  /** 版本读不出来 */
  unknown: boolean;
  /** 可实际尝试的候选，按优先级降序；声明版本自身排第一 */
  candidates: RuntimeCandidate[];
  /** 声明版本自身未接入的原因码；null 表示自身有 pack */
  declaredUnavailable: UnavailableRuntimeCode | null;
  /** 没有任何候选时的失败原因 */
  reason: UnavailableRuntimeCode;
  /** 最接近的已接入版本，供文案提示 */
  nearest: string | null;
}

/**
 * 候选 pack 链（§12.4）：先同号版本，再同 minor ±1 的相邻运行时（从高到低），
 * 最后回退到不高于声明版本的最近运行时。纯函数，Worker 与主线程共用同一份判断。
 */
export function resolveRuntimeCandidates(version: SpineVersionInfo | null): RuntimeResolution {
  if (!version) {
    return { unknown: true, candidates: [], declaredUnavailable: 'legacy', reason: 'legacy', nearest: null };
  }

  const specs = RUNTIME_VERSIONS.map((spec) => ({ spec, parsed: parseSpineVersion(spec.raw)! }));
  const exact = specs.filter((entry) => compare(entry.parsed, version) === 0);
  const neighbours = specs
    .filter(
      (entry) =>
        compare(entry.parsed, version) !== 0 &&
        entry.parsed.major === version.major &&
        Math.abs(entry.parsed.minor - version.minor) <= 1,
    )
    .sort((left, right) => compare(right.parsed, left.parsed));
  const older = specs
    .filter((entry) => compare(entry.parsed, version) < 0)
    .sort((left, right) => compare(right.parsed, left.parsed));

  const candidates: RuntimeCandidate[] = [];
  for (const entry of [...exact, ...neighbours, ...older]) {
    if (candidates.some((candidate) => candidate.packId === entry.spec.packId)) continue;
    candidates.push({ version: entry.spec.raw, packId: entry.spec.packId });
  }
  const declaredUnavailable = unavailableRuntimeCode(version);

  return {
    unknown: false,
    candidates,
    declaredUnavailable,
    reason: declaredUnavailable ?? 'legacy',
    nearest: candidates[0]?.version ?? null,
  };
}

export function isSupportedRuntimeVersion(version: SpineVersionInfo | null): boolean {
  return !!(version && !unavailableRuntimeCode(version));
}
