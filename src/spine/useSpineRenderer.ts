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
  /** 当前动画时长（秒），导出总帧数据此计算 */
  duration: number;
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
  duration: 0,
};

/** 导出会话重建渲染流水线所需的全部信息 */
export interface LoadInfo {
  files: Record<string, ArrayBuffer>;
  skeletonFile: string;
  atlasFile: string;
  version: SpineVersionInfo;
  /** 锁定当前生效的 pack，导出与预览必须用同一运行时 */
  packId: string;
  /** 当前动画时长（秒） */
  duration: number;
}



export function useSpineRenderer(width: number, height: number, manualPackId: string | null) {
  const [state, setState] = useState<SpineRendererState>(initialState);
  const sessionRef = useRef<RenderSession | null>(null);
  const stopRef = useRef<(() => void) | null>(null);
  const elapsedRef = useRef(0);
  const runRef = useRef(0);
  const lastFilesRef = useRef<File[] | null>(null);
  const loadRef = useRef<(files: File[]) => Promise<void>>(async () => {});
  const loadInfoRef = useRef<LoadInfo | null>(null);

  const teardown = useCallback(() => {
    runRef.current += 1;
    stopRef.current?.();
    stopRef.current = null;
    loadInfoRef.current = null;
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
          loadInfoRef.current = {
            files: buffers,
            skeletonFile,
            atlasFile,
            version,
            // 导出会话必须复用同一个 pack，否则像素与预览对不上
            packId: loaded.packId,
            duration: loaded.animationDuration,
          };
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
            duration: loaded.animationDuration,
          });
          stopRef.current = startPlayback(session, stale, teardown, setState, elapsedRef, 0);
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

  const beginPlayback = useCallback(
    (session: RenderSession, startOffsetMs = 0) => {
      const run = runRef.current;
      stopRef.current = startPlayback(session, () => run !== runRef.current, teardown, setState, elapsedRef, startOffsetMs);
    },
    [teardown],
  );

  const reset = useCallback(() => {
    teardown();
    lastFilesRef.current = null;
    setState(initialState);
  }, [teardown]);

  // 导出期间预览交给导出帧驱动：暂停正常播放、随时恢复、允许直接塞入导出流水线的帧
  const pausePlayback = useCallback(() => {
    stopRef.current?.();
    stopRef.current = null;
  }, []);

  const resumePlayback = useCallback(() => {
    const session = sessionRef.current;
    if (!session || stopRef.current) return;
    beginPlayback(session, elapsedRef.current);
  }, [beginPlayback]);

  const showFrame = useCallback((frame: ImageBitmap) => {
    setState((current) => {
      current.frame?.close();
      return { ...current, frame };
    });
  }, []);

  const getLoadInfo = useCallback(() => loadInfoRef.current, []);

  return { ...state, loadFiles, reset, pausePlayback, resumePlayback, showFrame, getLoadInfo };
}

function startPlayback(
  session: RenderSession,
  stale: () => boolean,
  teardown: () => void,
  setState: (update: (current: SpineRendererState) => SpineRendererState) => void,
  elapsedRef: { current: number },
  startOffsetMs: number,
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
    {
      startOffsetMs,
      onElapsed: (elapsed) => {
        elapsedRef.current = elapsed;
      },
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
