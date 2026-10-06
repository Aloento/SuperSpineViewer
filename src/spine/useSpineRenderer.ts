import { useCallback, useEffect, useRef, useState } from 'react';
import { detectSpineVersion } from './versionLoader';
import { parseSpineVersion, resolveRuntimeCandidates } from './runtimeMap';
import type { SpineVersionInfo, UnavailableRuntimeCode } from './runtimeMap';
import { RenderSession, RenderWorkerError } from './renderSession';
import { missingAtlasPages, pairAssets } from './pairing';
import type { LoadResponsePayload } from '../workers/protocol';

export interface RendererMessage {
  key: string;
  values?: Record<string, string>;
}

export type RendererStatus = 'idle' | 'loading' | 'playing' | 'error';

export interface SpineRendererState {
  status: RendererStatus;
  error: RendererMessage | null;
  warning: RendererMessage | null;
  frame: ImageBitmap | null;
  /** 骨架文件内声明/嗅探到的版本 */
  version: string | null;
  /** 实际生效的运行时版本、pack 与后端 */
  runtime: string | null;
  packId: string | null;
  backend: string | null;
  bones: number | null;
  animationCount: number | null;
  animation: string | null;
}

const initialState: SpineRendererState = {
  status: 'idle',
  error: null,
  warning: null,
  frame: null,
  version: null,
  runtime: null,
  packId: null,
  backend: null,
  bones: null,
  animationCount: null,
  animation: null,
};



export function useSpineRenderer(width: number, height: number, manualPackId: string | null) {
  const [state, setState] = useState<SpineRendererState>(initialState);
  const sessionRef = useRef<RenderSession | null>(null);
  const stopRef = useRef<(() => void) | null>(null);
  const runRef = useRef(0);
  const lastFilesRef = useRef<File[] | null>(null);
  const loadRef = useRef<(files: File[]) => Promise<void>>(async () => {});

  const teardown = useCallback(() => {
    runRef.current += 1;
    stopRef.current?.();
    stopRef.current = null;
    sessionRef.current?.dispose();
    sessionRef.current = null;
  }, []);

  useEffect(() => teardown, [teardown]);

  const loadFiles = useCallback(
    async (files: File[]) => {
      teardown();
      lastFilesRef.current = files;
      const run = runRef.current;
      const stale = () => run !== runRef.current;

      const buffers: Record<string, ArrayBuffer> = {};
      for (const file of files) buffers[file.name] = await file.arrayBuffer();
      if (stale()) return;

      const inventory = pairAssets(buffers);
      if (!inventory.hasSkeleton) {
        setState({ ...initialState, status: 'error', error: { key: 'errors.missingSkeleton' } });
        return;
      }
      if (!inventory.hasAtlas) {
        setState({ ...initialState, status: 'error', error: { key: 'errors.missingAtlas' } });
        return;
      }

      setState({ ...initialState, status: 'loading' });

      const session = new RenderSession();
      sessionRef.current = session;
      try {
        await session.init(width, height);
      } catch (error) {
        if (stale()) return;
        teardown();
        setState({
          ...initialState,
          status: 'error',
          error: { key: 'errors.loadFailed', values: { detail: detail(error) } },
        });
        return;
      }

      // 两层回退：外层候选骨架文件，内层由 Worker 展开候选运行时链
      let unknownVersion = false;
      let unavailable: UnavailableRuntimeCode | null = null;
      let nearest: string | null = null;
      let fallbackUsed = false;
      let failure: RendererMessage | null = null;
      let missingTextures: RendererMessage | null = null;
      let previous: string | null = null;

      for (const { skeletonFile, atlasFile } of inventory.pairs) {
        // 贴图没拖全时先把名字亮出来，比 worker 里的 allCandidatesFailed 详情可读
        const missing = missingAtlasPages(buffers, atlasFile);
        if (missing.length > 0) {
          missingTextures = { key: 'errors.missingTextures', values: { names: missing.join(', ') } };
          previous = skeletonFile;
          continue;
        }
        const sniffed = detectSpineVersion(buffers[skeletonFile]);
        // 手动指定 pack：候选链交给 Worker 的 packOverride，版本读不出来时用 pack 版本兜底
        const version = manualPackId ? sniffed ?? parseSpineVersion(manualPackId + '.0') : sniffed;
        if (!version) {
          unknownVersion = true;
          previous = skeletonFile;
          continue;
        }
        if (!manualPackId) {
          const resolution = resolveRuntimeCandidates(version);
          if (resolution.unknown) {
            unknownVersion = true;
            previous = skeletonFile;
            continue;
          }
          if (resolution.candidates.length === 0) {
            unavailable = resolution.reason;
            nearest = resolution.nearest ?? nearest;
            previous = skeletonFile;
            continue;
          }
          if (resolution.declaredUnavailable) {
            unavailable = resolution.declaredUnavailable;
            nearest = resolution.nearest ?? nearest;
            fallbackUsed = true;
          }
        }

        try {
          const loaded = await session.load({ files: buffers, skeletonFile, atlasFile, version, packOverride: manualPackId ?? undefined });
          if (stale()) return;
          setState({
            ...initialState,
            status: 'playing',
            warning: manualPackId
              ? { key: 'warnings.manualRuntime', values: { pack: loaded.packId } }
              : fallbackWarning(previous, skeletonFile, version, loaded, fallbackUsed),
            version: version.raw,
            runtime: loaded.runtimeVersion,
            packId: loaded.packId,
            backend: loaded.backend,
            bones: loaded.bones,
            animationCount: loaded.animationCount,
            animation: loaded.animation,
          });
          stopRef.current = startPlayback(session, stale, teardown, setState);
          return;
        } catch (error) {
          previous = skeletonFile;
          failure = loadFailure(error);
        }
      }

      if (stale()) return;
      teardown();
      if (unavailable === '2d') {
        setState({ ...initialState, status: 'error', error: { key: 'errors.runtime2d' } });
      } else if (unavailable === 'future') {
        setState({
          ...initialState,
          status: 'error',
          error: nearest ? { key: 'errors.runtimeFuture', values: { version: nearest } } : { key: 'errors.runtimeFuture' },
        });
      } else if (failure) {
        setState({ ...initialState, status: 'error', error: failure });
      } else if (missingTextures) {
        setState({ ...initialState, status: 'error', error: missingTextures });
      } else if (unknownVersion) {
        setState({ ...initialState, status: 'error', error: { key: 'errors.unknownVersion' } });
      } else {
        setState({ ...initialState, status: 'error', error: { key: 'errors.loadFailed', values: { detail: '' } } });
      }
    },
    [teardown, width, height, manualPackId],
  );

  loadRef.current = loadFiles;

  useEffect(() => {
    // 换 pack 后用同一批文件重载，避免用户重新拖拽
    const files = lastFilesRef.current;
    if (files) void loadRef.current(files);
  }, [manualPackId]);

  const reset = useCallback(() => {
    teardown();
    lastFilesRef.current = null;
    setState(initialState);
  }, [teardown]);

  return { ...state, loadFiles, reset };
}

function startPlayback(
  session: RenderSession,
  stale: () => boolean,
  teardown: () => void,
  setState: (update: (current: SpineRendererState) => SpineRendererState) => void,
): () => void {
  return session.play(
    (frame) => {
      setState((current) => {
        if (current.status !== 'playing') {
          frame.close();
          return current;
        }
        current.frame?.close();
        return { ...current, frame };
      });
    },
    () => {
      if (stale()) return;
      teardown();
      setState((current) => ({ ...current, status: 'error', frame: null, error: { key: 'errors.renderFailed' } }));
    },
  );
}

/** 换过骨架文件或跨 minor 回退过运行时都要提示，方便判断资源实际由哪个 pack 渲染 */
function fallbackWarning(
  previous: string | null,
  skeletonFile: string,
  sniffed: SpineVersionInfo,
  loaded: LoadResponsePayload,
  declaredUnavailable: boolean,
): RendererMessage | null {
  if (declaredUnavailable) {
    return {
      key: 'warnings.runtimeFallback',
      values: { from: sniffed.raw, to: loaded.runtimeVersion, pack: loaded.packId },
    };
  }
  if (loaded.attempts.length > 0) {
    const detail = loaded.attempts.map((attempt) => `${attempt.packId} ${attempt.message}`).join(' | ');
    return { key: 'warnings.runtimeRetry', values: { pack: loaded.packId, detail } };
  }
  return previous ? { key: 'warnings.skeletonFallback', values: { from: previous, to: skeletonFile } } : null;
}

/** 区分「版本不支持」与「候选运行时全部解析失败」，后者带上每个候选的失败原因 */
function loadFailure(error: unknown): RendererMessage {
  if (error instanceof RenderWorkerError) {
    if (error.code === 'unknownVersion') return { key: 'errors.unknownVersion' };
    if (error.code === 'binaryUnsupported') {
      return { key: 'errors.binaryUnsupported', values: { version: error.message } };
    }
    if (error.code === 'runtimeUnavailable') return unavailableMessage(error.message);
    if (error.code === 'allCandidatesFailed') {
      return { key: 'errors.allCandidatesFailed', values: { detail: error.message } };
    }
  }
  return { key: 'errors.loadFailed', values: { detail: detail(error) } };
}

function unavailableMessage(code: string): RendererMessage {
  if (code === 'future') return { key: 'errors.runtimeFuture', values: {} };
  if (code === '2d') return { key: 'errors.runtime2d' };
  // 3.0–4.3 都有候选运行时，走到这里说明原因码不认识，保留原始码便于排查
  return { key: 'errors.loadFailed', values: { detail: code } };
}

function detail(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
