import { useCallback, useEffect, useRef, useState } from 'react';
import { RenderSession } from '../spine/renderSession';
import type { LoadInfo } from '../spine/useSpineRenderer';
import { ExportClient } from './exportClient';
import { defaultExportOptions, sanitizeSize, type ExportOptions } from './presets';

export type ExportPhase = 'idle' | 'preparing' | 'encoding' | 'packaging' | 'done' | 'failed';

export interface ExportState {
  phase: ExportPhase;
  encoded: number;
  total: number;
  errorDetail: string;
}

export interface ExportControls {
  getLoadInfo: () => LoadInfo | null;
  pausePlayback: () => void;
  resumePlayback: () => void;
  showFrame: (frame: ImageBitmap) => void;
}

const idleState: ExportState = { phase: 'idle', encoded: 0, total: 0, errorDetail: '' };

/** 预览画布尺寸与导出尺寸无关，导出用独立渲染会话按导出分辨率逐帧渲染。
 *  编码 worker 跨导出复用（M3 遗留项）；渲染会话每轮新建——帧源动画状态是有累积的，
 *  跨轮复用会带上上一轮的时序/物理速度，破坏确定性 */
export function useExport(controls: ExportControls) {
  const [options, setOptions] = useState<ExportOptions>(() => ({ ...DEFAULTS }));
  const [state, setState] = useState<ExportState>(idleState);
  const cancelRef = useRef(false);
  const clientRef = useRef<ExportClient | null>(null);

  const running = isRunning(state.phase);

  useEffect(
    () => () => {
      clientRef.current?.dispose();
      clientRef.current = null;
    },
    [],
  );

  const start = useCallback(async () => {
    const info = controls.getLoadInfo();
    if (!info || isRunning(state.phase)) return;
    cancelRef.current = false;

    const width = sanitizeSize(options.width);
    const height = sanitizeSize(options.height);
    // 时长 × fps 向上取整，最后一帧不越过动画末尾太久
    const total = Math.max(1, Math.ceil(info.duration * options.fps));
    let encoded = 0;
    const onProgress = (done: number) => {
      encoded = done;
      setState((current) => (current.phase === 'encoding' ? { ...current, encoded: done } : current));
    };

    setState({ phase: 'preparing', encoded: 0, total, errorDetail: '' });
    controls.pausePlayback();

    // 导出会话与预览会话隔离：独立画布尺寸 + 时间轴从 0 确定性推进，锁定同一 pack
    const client = (clientRef.current ??= new ExportClient());
    const session = new RenderSession();
    let resumed = false;
    const resumePreview = () => {
      if (!resumed) {
        resumed = true;
        controls.resumePlayback();
      }
    };
    try {
      client.onEvents({ onProgress });
      await session.init(width, height);
      if (cancelRef.current) throw CANCELLED;
      const loaded = await session.load({
        files: info.files,
        skeletonFile: info.skeletonFile,
        atlasFile: info.atlasFile,
        version: info.version,
        packOverride: info.packId,
      });
      if (cancelRef.current) throw CANCELLED;
      // 导出会话默认播第一个动画；预览里换了动画的话按选中的来（重置到 0，仍确定性）
      if (info.animation && info.animation !== loaded.animation) {
        (await session.setAnimation(info.animation, true)).close();
        if (cancelRef.current) throw CANCELLED;
      }
      await client.configure({
        format: options.format,
        width,
        height,
        fps: options.fps,
        bitrate: options.bitrate,
        frameCount: total,
      });
      if (cancelRef.current) throw CANCELLED;

      setState((current) => ({ ...current, phase: 'encoding' }));
      // 渲染与编码流水线并行：帧所有权交给编码 worker，预览用一份副本实时跟进
      for (let i = 0; i < total; i++) {
        if (cancelRef.current) throw CANCELLED;
        await waitInflight(() => cancelRef.current, () => encoded, i);
        const frame = await session.frame((i * 1000) / options.fps);
        if (cancelRef.current) {
          frame.close();
          throw CANCELLED;
        }
        controls.showFrame(await createImageBitmap(frame));
        client.sendFrame(i, frame);
      }

      setState((current) => ({ ...current, phase: 'packaging', encoded: total }));
      const blob = await client.finalize();
      if (cancelRef.current) throw CANCELLED;

      download(blob, fileName(info, options.format));
      // 预览停留在最后一帧，正常播放随后从暂停处继续
      resumePreview();
      setState({ phase: 'done', encoded: total, total, errorDetail: '' });
    } catch (error) {
      const cancelled = error === CANCELLED;
      client.cancel();
      resumePreview();
      setState(cancelled ? idleState : { ...idleState, phase: 'failed', errorDetail: detail(error) });
    } finally {
      session.dispose();
    }
  }, [controls, options, state.phase]);

  const cancel = useCallback(() => {
    if (!isRunning(state.phase)) return;
    cancelRef.current = true;
  }, [state.phase]);

  const dismiss = useCallback(() => {
    setState((current) => (current.phase === 'done' || current.phase === 'failed' ? idleState : current));
  }, []);

  return { options, setOptions, state, start, cancel, dismiss, running };
}

const DEFAULTS: ExportOptions = defaultExportOptions;

export function isRunning(phase: ExportPhase): boolean {
  return phase === 'preparing' || phase === 'encoding' || phase === 'packaging';
}

const CANCELLED = new Error('export-cancelled');

// 编码 worker 消费速度以进度回调为准，在途帧过多时压住渲染循环，避免 ImageBitmap 堆积
async function waitInflight(cancelled: () => boolean, encoded: () => number, sent: number): Promise<void> {
  while (!cancelled() && sent - encoded() > 16) {
    await new Promise((resolve) => setTimeout(resolve, 8));
  }
  if (cancelled()) throw CANCELLED;
}

function fileName(info: LoadInfo, format: ExportOptions['format']): string {
  const base = info.skeletonFile.replace(/\.(skel|json)$/i, '') || 'spine';
  return format === 'vp9' ? `${base}.webm` : `${base}-frames.zip`;
}

function download(blob: Blob, name: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  // 下载已入队，留一秒给浏览器取走 blob 再释放
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function detail(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
