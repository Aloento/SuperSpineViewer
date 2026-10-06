import './domShims';
import { RuntimeError } from '../types';
import { legacyBinarySupports } from '../binary/legacyBinary';
import type { BinaryVersionRange } from '../binary/legacyBinary';
import type {
  FrameSource,
  FrameSourceContext,
  SpineRuntimeCapabilities,
  SpineRuntimePack,
  SpineVersionInfo,
} from '../types';
import { CanvaskitFrameSource } from '../frameSources/canvaskit';
import { WebglFrameSource } from '../frameSources/webgl';
import { LegacyFrameSource } from '../frameSources/legacy';
import type { RuntimeCandidate } from '../runtimeMap';

type Loader = () => Promise<Record<string, any>>;

interface PackDefinition {
  id: string;
  backend: 'canvaskit' | 'webgl' | 'legacy';
  /** 官方 core 是否自带 SkeletonBinary；false 表示只能走自研读取器或 .json */
  binary?: boolean;
  /** 官方 core 无 SkeletonBinary 时，自研读取器可接管的 .skel 版本区间（§12.7） */
  legacyBinary?: BinaryVersionRange;
  capabilities: SpineRuntimeCapabilities;
  load: Loader;
  /** 从模块导出中取出解析层命名空间 */
  core: (mod: Record<string, any>) => any;
  /** 从模块导出中取出渲染层命名空间，缺省表示与 core 同一个 */
  renderer?: (mod: Record<string, any>) => any;
}

const WEBGL_CAPABILITY_BASE: SpineRuntimeCapabilities = {
  setupPoseMethod: 'setToSetupPose',
  yDown: false,
  synchronousAtlasLoader: false,
  attachmentLoader: 'AtlasAttachmentLoader',
};

const CANVASKIT_CAPABILITY_BASE: SpineRuntimeCapabilities = {
  setupPoseMethod: 'setToSetupPose',
  yDown: true,
  synchronousAtlasLoader: false,
  attachmentLoader: 'AtlasAttachmentLoader',
};

const LEGACY_CAPABILITY_BASE: SpineRuntimeCapabilities = {
  setupPoseMethod: 'setToSetupPose',
  yDown: true,
  synchronousAtlasLoader: true,
  attachmentLoader: 'AtlasAttachmentLoader',
};

const DEFINITIONS: PackDefinition[] = [
  {
    // 3.0–3.2 共用 3.1.07 的 spine-js（无渲染层），绘制走自研 CanvasKit frameSource
    id: '3.1',
    backend: 'legacy',
    binary: false,
    // 3.0–3.2 的 .skel 走自研读取器 binary/legacyBinary31（§12.7）
    legacyBinary: { min: { major: 3, minor: 0 }, max: { major: 3, minor: 2 } },
    capabilities: LEGACY_CAPABILITY_BASE,
    load: () => import('./generated/spine-3.1.js'),
    core: (mod) => mod.default.spine,
  },
  {
    // 3.4–3.8 由 scripts/fetch-runtimes.mjs 从官方分支 vendor 成 ESM，core 与 webgl 分成两个命名空间
    id: '3.8',
    backend: 'webgl',
    capabilities: { ...WEBGL_CAPABILITY_BASE, synchronousAtlasLoader: true },
    load: () => import('./generated/spine-3.8.js'),
    core: (mod) => mod.default.spine,
    renderer: (mod) => mod.default.webgl ?? mod.default.spine,
  },
  {
    id: '3.7',
    backend: 'webgl',
    binary: false,
    legacyBinary: { min: { major: 3, minor: 7 }, max: { major: 3, minor: 7 } },
    capabilities: { ...WEBGL_CAPABILITY_BASE, synchronousAtlasLoader: true },
    load: () => import('./generated/spine-3.7.js'),
    core: (mod) => mod.default.spine,
    renderer: (mod) => mod.default.webgl ?? mod.default.spine,
  },
  {
    id: '3.6',
    backend: 'webgl',
    binary: false,
    legacyBinary: { min: { major: 3, minor: 6 }, max: { major: 3, minor: 6 } },
    capabilities: { ...WEBGL_CAPABILITY_BASE, synchronousAtlasLoader: true },
    load: () => import('./generated/spine-3.6.js'),
    core: (mod) => mod.default.spine,
    renderer: (mod) => mod.default.webgl ?? mod.default.spine,
  },
  {
    id: '3.5',
    backend: 'webgl',
    binary: false,
    legacyBinary: { min: { major: 3, minor: 5 }, max: { major: 3, minor: 5 } },
    capabilities: { ...WEBGL_CAPABILITY_BASE, synchronousAtlasLoader: true },
    load: () => import('./generated/spine-3.5.js'),
    core: (mod) => mod.default.spine,
    renderer: (mod) => mod.default.webgl ?? mod.default.spine,
  },
  {
    // 3.4 的图集 attachment 加载器类名与 3.5+ 不同
    id: '3.4',
    backend: 'webgl',
    binary: false,
    // 3.3 没有独立运行时，自研读取器的 3.4 分支同时覆盖 3.3（§1.1）
    legacyBinary: { min: { major: 3, minor: 3 }, max: { major: 3, minor: 4 } },
    capabilities: {
      ...WEBGL_CAPABILITY_BASE,
      synchronousAtlasLoader: true,
      attachmentLoader: 'TextureAtlasAttachmentLoader',
    },
    load: () => import('./generated/spine-3.4.js'),
    core: (mod) => mod.default.spine,
    renderer: (mod) => mod.default.webgl ?? mod.default.spine,
  },
  {
    // 4.0 vendor 的是 dist/iife/spine-webgl.js，全部符号在 spine 命名空间
    id: '4.0',
    backend: 'webgl',
    capabilities: WEBGL_CAPABILITY_BASE,
    load: () => import('./generated/spine-4.0.js'),
    core: (mod) => mod.default.spine,
  },
  {
    id: '4.1',
    backend: 'webgl',
    capabilities: WEBGL_CAPABILITY_BASE,
    load: () => import('@esotericsoftware/spine-webgl-41'),
    core: (mod) => mod,
  },
  {
    id: '4.2',
    backend: 'canvaskit',
    capabilities: CANVASKIT_CAPABILITY_BASE,
    load: () => import('@esotericsoftware/spine-canvaskit'),
    core: (mod) => mod,
  },
  {
    id: '4.3',
    backend: 'canvaskit',
    capabilities: { ...CANVASKIT_CAPABILITY_BASE, setupPoseMethod: 'setupPose' },
    load: () => import('@esotericsoftware/spine-canvaskit-43'),
    core: (mod) => mod,
  },
];

// 3.4 的 spine-webgl 还没有 ManagedWebGLRenderingContext，裸 gl 直接可用
const WEBGL_API = ['SceneRenderer', 'GLTexture', 'OrthoCamera'];

const cache = new Map<string, SpineRuntimePack>();

function buildPack(definition: PackDefinition, mod: Record<string, any>): SpineRuntimePack {
  const core = definition.core(mod);
  const webgl = definition.renderer ? definition.renderer(mod) : undefined;
  if (typeof core?.SkeletonJson !== 'function') throw new RuntimeError('packLoadFailed', definition.id);
  if (definition.binary !== false && typeof core?.SkeletonBinary !== 'function') {
    throw new RuntimeError('packLoadFailed', `${definition.id}:SkeletonBinary`);
  }
  if (typeof core?.[definition.capabilities.attachmentLoader] !== 'function') {
    throw new RuntimeError('packLoadFailed', `${definition.id}:${definition.capabilities.attachmentLoader}`);
  }
  if (definition.backend === 'webgl') {
    const target = webgl ?? core;
    for (const name of WEBGL_API) {
      if (typeof target[name] !== 'function') throw new RuntimeError('packLoadFailed', `${definition.id}:${name}`);
    }
  }

  return {
    id: definition.id,
    backend: definition.backend,
    capabilities: definition.capabilities,
    core,
    webgl,
    async createFrameSource(context: FrameSourceContext): Promise<FrameSource> {
      if (definition.backend === 'canvaskit') return CanvaskitFrameSource.create(this, context);
      if (definition.backend === 'legacy') return LegacyFrameSource.create(this, context);
      return WebglFrameSource.create(this, context);
    },
  };
}

/** 按需动态 import，只有真正用到的版本才会产生独立 chunk */
export async function loadRuntimePack(id: string): Promise<SpineRuntimePack> {
  const cached = cache.get(id);
  if (cached) return cached;

  const definition = DEFINITIONS.find((item) => item.id === id);
  if (!definition) throw new RuntimeError('packUnsupported', id);

  let mod: Record<string, any>;
  try {
    mod = await definition.load();
  } catch (error) {
    throw new RuntimeError('packLoadFailed', error instanceof Error ? error.message : String(error));
  }

  const pack = buildPack(definition, mod);
  cache.set(id, pack);
  return pack;
}

/** 该 pack 能否解析指定版本的 .skel：官方 SkeletonBinary，或自研读取器覆盖的版本区间（§12.7） */
export function packHasBinaryReader(id: string, version: SpineVersionInfo): boolean {
  const definition = DEFINITIONS.find((item) => item.id === id);
  if (!definition) return false;
  if (definition.binary !== false) return true;
  return !!definition.legacyBinary && legacyBinarySupports(definition.legacyBinary, version);
}

export interface LoadOutcome {
  frameSource: FrameSource;
  /** 实际生效的候选版本与 pack id */
  version: string;
  packId: string;
  backend: 'canvaskit' | 'webgl' | 'legacy';
  /** 生效前失败的候选，用于 UI 说明回退过程 */
  attempts: { candidate: RuntimeCandidate; error: RuntimeError }[];
}

/**
 * 候选链执行器：按顺序 loadRuntimePack → createFrameSource，解析或校验失败就换下一个候选。
 * 单个候选的 packLoadFailed / backendUnavailable 说明是环境问题而不是版本问题，直接终止回退。
 */
export async function loadFrameFromCandidates(
  candidates: RuntimeCandidate[],
  context: FrameSourceContext,
): Promise<{ ok: true; outcome: LoadOutcome } | { ok: false; attempts: { candidate: RuntimeCandidate; error: RuntimeError }[] }> {
  const attempts: { candidate: RuntimeCandidate; error: RuntimeError }[] = [];
  const wantsBinary = !context.skeletonFile.toLowerCase().endsWith('.json');

  for (const candidate of candidates) {
    // 二进制骨架 + 无读取器的 pack 直接跳过，省掉必然失败的 chunk 下载
    if (wantsBinary && !packHasBinaryReader(candidate.packId, context.version)) {
      attempts.push({ candidate, error: new RuntimeError('binaryUnsupported', candidate.packId) });
      continue;
    }

    let pack: SpineRuntimePack;
    try {
      pack = await loadRuntimePack(candidate.packId);
    } catch (error) {
      const failure = asRuntimeError(error, candidate.packId);
      if (failure.code === 'packUnsupported' || failure.code === 'packLoadFailed') {
        attempts.push({ candidate, error: failure });
        break;
      }
      attempts.push({ candidate, error: failure });
      continue;
    }

    try {
      const frameSource = await pack.createFrameSource(context);
      return {
        ok: true,
        outcome: { frameSource, version: candidate.version, packId: pack.id, backend: pack.backend, attempts },
      };
    } catch (error) {
      const failure = asRuntimeError(error, candidate.packId);
      // 后端建不起来与版本无关，换 pack 也是同样结果
      if (failure.code === 'backendUnavailable') {
        attempts.push({ candidate, error: failure });
        break;
      }
      attempts.push({ candidate, error: failure });
    }
  }

  return { ok: false, attempts };
}

function asRuntimeError(error: unknown, packId: string): RuntimeError {
  if (error instanceof RuntimeError) return error;
  return new RuntimeError('parseInvalid', `${packId}: ${error instanceof Error ? error.message : String(error)}`);
}
