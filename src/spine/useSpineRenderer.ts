import { useCallback, useEffect, useRef, useState } from 'react';
import { detectSpineVersion } from './versionLoader';
import { parseSpineVersion, resolveRuntimeCandidates } from './runtimeMap';
import type { SpineVersionInfo, UnavailableRuntimeCode } from './runtimeMap';
import { RenderSession, RenderWorkerError } from './renderSession';
import { missingAtlasPages, pairAssets } from './pairing';
import type { LoadResponsePayload, TransformPayload } from '../workers/protocol';

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
  /** 全部动画名，选择框数据源 */
  animations: string[];
  /** 当前动画时长（秒），导出总帧数据此计算 */
  duration: number;
  /** 用户侧播放中（status=playing 期间导出暂停或手动暂停时为 false） */
  playing: boolean;
  /** 播放头位置（毫秒；非循环到终点时停在动画时长处） */
  elapsedMs: number;
  loop: boolean;
  /** 当前皮肤（'' = 默认皮肤） */
  skin: string;
  skins: string[];
  /** 各动画时长（秒），key 为动画名 */
  animationDurations: Record<string, number>;
  /** 骨架基础偏移（画布像素，+x 右、+y 上） */
  offsetX: number;
  offsetY: number;
  /** 相对自动取景的额外缩放 */
  scale: number;
}

const DEFAULT_TRANSFORM: TransformPayload = { offsetX: 0, offsetY: 0, scale: 1 };

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
  animations: [],
  duration: 0,
  playing: false,
  elapsedMs: 0,
  loop: true,
  skin: '',
  skins: [],
  animationDurations: {},
  offsetX: 0,
  offsetY: 0,
  scale: 1,
};

/** 导出会话重建渲染流水线所需的全部信息 */
export interface LoadInfo {
  files: Record<string, ArrayBuffer>;
  skeletonFile: string;
  atlasFile: string;
  version: SpineVersionInfo;
  /** 锁定当前生效的 pack，导出与预览必须用同一运行时 */
  packId: string;
  /** 当前选中的动画，导出同样按它渲染 */
  animation: string;
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
  // 播放控制的“真值”放 ref，回调读最新值；state 只驱动 UI
  const playingRef = useRef(false);
  const loopRef = useRef(true);
  const animationRef = useRef('');
  const durationRef = useRef(0);
  const durationsRef = useRef<Record<string, number>>({});
  const transformRef = useRef<TransformPayload>({ ...DEFAULT_TRANSFORM });
  const transformSeqRef = useRef(0);
  /** 拖动进度条的合并状态：在途 seek 只保留最新目标 */
  const seekBusyRef = useRef(false);
  const seekPendingRef = useRef(0);

  const teardown = useCallback(() => {
    runRef.current += 1;
    stopRef.current?.();
    stopRef.current = null;
    playingRef.current = false;
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
            animation: loaded.animation,
            duration: loaded.animationDuration,
          };
          playingRef.current = true;
          loopRef.current = true;
          animationRef.current = loaded.animation;
          durationRef.current = loaded.animationDuration;
          durationsRef.current = loaded.animationDurations;
          elapsedRef.current = 0;
          transformRef.current = { ...DEFAULT_TRANSFORM };
          setState({
            ...initialState,
            status: 'playing',
            playing: true,
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
            animations: loaded.animations,
            duration: loaded.animationDuration,
            skin: loaded.skin,
            skins: loaded.skins,
            animationDurations: loaded.animationDurations,
          });
          stopRef.current = startPlayback(session, stale, teardown, setState, elapsedRef, 0, transformRef, {
            isLoop: () => loopRef.current,
            durationMs: () => durationRef.current * 1000,
            onEnded: () => {
              playingRef.current = false;
            },
          });
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

  /** 时钟起点为 startOffsetMs 的播放循环；transformRef 每帧被读取，播放中拖滑杆即时生效 */
  const beginPlayback = useCallback(
    (session: RenderSession, startOffsetMs = 0) => {
      const run = runRef.current;
      stopRef.current?.();
      stopRef.current = startPlayback(session, () => run !== runRef.current, teardown, setState, elapsedRef, startOffsetMs, transformRef, {
        isLoop: () => loopRef.current,
        durationMs: () => durationRef.current * 1000,
        onEnded: () => {
          playingRef.current = false;
        },
      });
    },
    [teardown],
  );

  const stopPlayback = useCallback(() => {
    stopRef.current?.();
    stopRef.current = null;
  }, []);

  /** 对齐 worker 侧动画状态到 target 后开始播放（暂停恢复、进度条回放共用） */
  const playFrom = useCallback(
    async (target: number) => {
      const session = sessionRef.current;
      if (!session) return;
      playingRef.current = true;
      setState((current) => ({ ...current, playing: true }));
      const run = runRef.current;
      const frame = await session.seek(target);
      if (run !== runRef.current) {
        frame.close();
        return;
      }
      frame.close();
      elapsedRef.current = target;
      beginPlayback(session, target);
    },
    [beginPlayback],
  );

  const togglePlay = useCallback(async () => {
    if (!sessionRef.current) return;
    if (playingRef.current) {
      playingRef.current = false;
      stopPlayback();
      setState((current) => ({ ...current, playing: false }));
      return;
    }
    const durMs = durationRef.current * 1000;
    let target = elapsedRef.current;
    if (durMs > 0) {
      // 循环播放取模对齐 worker 状态；非循环播完后再按播放从头开始
      target = target >= durMs ? (loopRef.current ? target % durMs : 0) : target;
    }
    await playFrom(target);
  }, [playFrom, stopPlayback]);

  /** 拖动进度条：总是先暂停再跳转，暂停态也立刻出画。
   *  在途 seek 期间只记住最新目标，完成后直接跳过去，中间的滑杆值丢弃——
   *  worker 单线程顺序渲染，逐值排队会让拖动明显滞后 */
  const seekTo = useCallback(
    async (timeMs: number) => {
      const session = sessionRef.current;
      if (!session) return;
      playingRef.current = false;
      stopPlayback();
      setState((current) => ({ ...current, playing: false }));
      const run = runRef.current;
      seekPendingRef.current = timeMs;
      if (seekBusyRef.current) return;
      seekBusyRef.current = true;
      try {
        let target = timeMs;
        for (;;) {
          const frame = await session.seek(target);
          if (run !== runRef.current) {
            frame.close();
            break;
          }
          elapsedRef.current = target;
          setState((current) => {
            current.frame?.close();
            return { ...current, frame, elapsedMs: target };
          });
          if (seekPendingRef.current === target) break;
          target = seekPendingRef.current;
        }
      } finally {
        seekBusyRef.current = false;
      }
    },
    [stopPlayback],
  );

  /** 换动画/切循环共用：重建轨道条目并重置到 0，播放中则时钟同步重开 */
  const restartAnimation = useCallback(async () => {
    const session = sessionRef.current;
    if (!session) return;
    stopPlayback();
    const run = runRef.current;
    const frame = await session.setAnimation(animationRef.current, loopRef.current);
    if (run !== runRef.current) {
      frame.close();
      return;
    }
    elapsedRef.current = 0;
    if (playingRef.current) {
      frame.close();
      beginPlayback(session, 0);
    } else {
      setState((current) => {
        current.frame?.close();
        return { ...current, frame, elapsedMs: 0 };
      });
    }
  }, [beginPlayback, stopPlayback]);

  const changeAnimation = useCallback(
    async (name: string) => {
      if (!sessionRef.current || name === animationRef.current) return;
      animationRef.current = name;
      const dur = durationsRef.current[name] ?? 0;
      durationRef.current = dur;
      elapsedRef.current = 0;
      if (loadInfoRef.current) {
        loadInfoRef.current = { ...loadInfoRef.current, animation: name, duration: dur };
      }
      setState((current) => ({ ...current, animation: name, duration: dur, elapsedMs: 0 }));
      await restartAnimation();
    },
    [restartAnimation],
  );

  const changeLoop = useCallback(
    async (value: boolean) => {
      if (value === loopRef.current) return;
      loopRef.current = value;
      setState((current) => ({ ...current, loop: value }));
      // 3.x 条目上的 loop 字段不保证被 update 消费，统一重开动画，行为跨版本一致
      await restartAnimation();
    },
    [restartAnimation],
  );

  const changeSkin = useCallback(async (name: string) => {
    const session = sessionRef.current;
    if (!session) return;
    setState((current) => ({ ...current, skin: name }));
    const run = runRef.current;
    const frame = await session.setSkin(name);
    if (run !== runRef.current) {
      frame.close();
      return;
    }
    if (playingRef.current) {
      // 播放循环马上会推新帧，这次回帧只用于确认皮肤已生效
      frame.close();
    } else {
      setState((current) => {
        current.frame?.close();
        return { ...current, frame };
      });
    }
  }, []);

  /** 更新偏移/缩放：播放中由播放循环下一帧带走；暂停态原地重绘当前帧（seq 防连拖乱序） */
  const changeTransform = useCallback(async (patch: Partial<TransformPayload>) => {
    const session = sessionRef.current;
    if (!session) return;
    const next = { ...transformRef.current, ...patch };
    transformRef.current = next;
    setState((current) => ({ ...current, offsetX: next.offsetX, offsetY: next.offsetY, scale: next.scale }));
    if (playingRef.current) return;
    const seq = ++transformSeqRef.current;
    const run = runRef.current;
    const frame = await session.setTransform(next.offsetX, next.offsetY, next.scale);
    if (run !== runRef.current || seq !== transformSeqRef.current) {
      frame.close();
      return;
    }
    setState((current) => {
      current.frame?.close();
      return { ...current, frame };
    });
  }, []);

  const reset = useCallback(() => {
    teardown();
    lastFilesRef.current = null;
    durationsRef.current = {};
    animationRef.current = '';
    durationRef.current = 0;
    elapsedRef.current = 0;
    transformRef.current = { ...DEFAULT_TRANSFORM };
    setState(initialState);
  }, [teardown]);

  // 导出期间预览交给导出帧驱动：暂停正常播放、随时恢复、允许直接塞入导出流水线的帧
  const pausePlayback = useCallback(() => {
    stopPlayback();
  }, [stopPlayback]);

  const resumePlayback = useCallback(() => {
    const session = sessionRef.current;
    // 用户在导出前就按了暂停：导出结束后不要擅自恢复播放
    if (!session || stopRef.current || !playingRef.current) return;
    beginPlayback(session, elapsedRef.current);
  }, [beginPlayback]);

  const showFrame = useCallback((frame: ImageBitmap) => {
    setState((current) => {
      current.frame?.close();
      return { ...current, frame };
    });
  }, []);

  const getLoadInfo = useCallback(() => loadInfoRef.current, []);

  return {
    ...state,
    loadFiles,
    reset,
    pausePlayback,
    resumePlayback,
    showFrame,
    getLoadInfo,
    togglePlay,
    seekTo,
    changeAnimation,
    changeLoop,
    changeSkin,
    changeTransform,
  };
}

interface PlaybackGuards {
  isLoop: () => boolean;
  durationMs: () => number;
  /** 非循环播到终点：时钟已停，通知调用方把 playing 置 false */
  onEnded: () => void;
}

function startPlayback(
  session: RenderSession,
  stale: () => boolean,
  teardown: () => void,
  setState: (update: (current: SpineRendererState) => SpineRendererState) => void,
  elapsedRef: { current: number },
  startOffsetMs: number,
  transformRef: { current: TransformPayload },
  guards: PlaybackGuards,
): () => void {
  let controller: (() => void) | null = null;
  let ended = false;
  const stop = () => controller?.();

  controller = session.play(
    (frame) => {
      setState((current) => {
        if (current.status !== 'playing') {
          frame.close();
          return current;
        }
        current.frame?.close();
        return { ...current, frame, elapsedMs: elapsedRef.current };
      });
    },
    () => {
      if (stale()) return;
      teardown();
      setState((current) => ({
        ...current,
        status: 'error',
        frame: null,
        playing: false,
        error: { key: 'errors.renderFailed' },
      }));
    },
    {
      startOffsetMs,
      onElapsed: (elapsed) => {
        elapsedRef.current = elapsed;
        const durMs = guards.durationMs();
        if (!ended && !guards.isLoop() && durMs > 0 && elapsed >= durMs) {
          ended = true;
          stop();
          guards.onEnded();
          const clamped = Math.floor(durMs);
          elapsedRef.current = clamped;
          setState((current) => ({ ...current, playing: false, elapsedMs: clamped }));
        }
      },
      getTransform: () => transformRef.current,
    },
  );

  return () => {
    controller?.();
    controller = null;
  };
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
