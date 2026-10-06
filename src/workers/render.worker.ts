import type { FrameSize, FrameSource, LoadAttempt } from '../spine/types';
import { RuntimeError } from '../spine/types';
import { loadFrameFromCandidates } from '../spine/runtimes';
import { resolveRuntimeCandidates } from '../spine/runtimeMap';
import type { LoadPayload, LoadResponsePayload, RenderRequest, RenderResponse, TransformPayload } from './protocol';

let size: FrameSize = { width: 0, height: 0 };
let frameSource: FrameSource | null = null;
/** 最近一次渲染/跳转的时间（毫秒），皮肤与变换改动在暂停态据此原地重绘当前帧 */
let lastTimeMs = 0;

function post(message: RenderResponse, transfer: Transferable[] = []) {
  self.postMessage(message, transfer);
}

async function loadSkeleton(id: number, payload: LoadPayload) {
  // 手动指定 pack 时候选链就是它本身；否则按声明版本展开回退链
  const resolution = resolveRuntimeCandidates(payload.version);
  const candidates = payload.packOverride
    ? [{ version: `${payload.packOverride}.0`, packId: payload.packOverride }]
    : resolution.candidates;
  if (candidates.length === 0) {
    post({ id, type: 'error', payload: { code: 'runtimeUnavailable', message: resolution.reason, attempts: [] } });
    return;
  }

  const outcome = await loadFrameFromCandidates(candidates, {
    width: size.width,
    height: size.height,
    files: payload.files,
    skeletonFile: payload.skeletonFile,
    atlasFile: payload.atlasFile,
    version: payload.version,
  });

  if (!outcome.ok) {
    const attempts = toAttempts(outcome.attempts);
    // 声明版本自身的 pack 没有二进制读取器时，链式回退的细节只会淹没结论，直接报「该版本 .skel 未支持」
    if (attempts[0]?.message.startsWith('binaryUnsupported')) {
      post({ id, type: 'error', payload: { code: 'binaryUnsupported', message: payload.version.raw, attempts } });
      return;
    }
    post({ id, type: 'error', payload: { code: 'allCandidatesFailed', message: joinAttempts(attempts), attempts } });
    return;
  }

  frameSource?.dispose();
  frameSource = outcome.outcome.frameSource;
  const summary = frameSource.summary();
  const animation = frameSource.animations()[0] ?? '';
  const animationDurations = frameSource.animationDurations();
  // summary.duration 只有 legacy 会填，其它后端按当前动画查时长表
  const animationDuration = animationDurations[animation] ?? summary.duration ?? 0;
  const response: LoadResponsePayload = {
    packId: outcome.outcome.packId,
    backend: outcome.outcome.backend,
    runtimeVersion: outcome.outcome.version,
    declaredVersion: summary.declaredVersion,
    bones: summary.bones,
    animationCount: summary.animationCount,
    animation,
    animations: frameSource.animations(),
    skin: '',
    skins: frameSource.skins(),
    animationDurations,
    animationDuration,
    attempts: toAttempts(outcome.outcome.attempts),
  };
  post({ id, type: 'loaded', payload: response });
}

function toAttempts(
  attempts: { candidate: { version: string; packId: string }; error: RuntimeError }[],
): LoadAttempt[] {
  return attempts.map((attempt) => ({
    version: attempt.candidate.version,
    packId: attempt.candidate.packId,
    message: `${attempt.error.code}${attempt.error.detail ? `:${attempt.error.detail}` : ''}`,
  }));
}

function joinAttempts(attempts: LoadAttempt[]): string {
  return attempts.map((attempt) => `${attempt.version}/${attempt.packId} ${attempt.message}`).join(' | ');
}

async function renderFrame(id: number, timeMs: number, transform?: TransformPayload) {
  if (!frameSource) throw new RuntimeError('notLoaded');
  if (transform) frameSource.setTransform(transform.offsetX, transform.offsetY, transform.scale);
  lastTimeMs = timeMs;
  const frame = await frameSource.render(timeMs);
  post({ id, type: 'frame', payload: { timeMs, frame } }, [frame]);
}

/** 控件改动（跳转/换动画/换皮肤/变换）统一回一帧新渲染的 frame，暂停态也能立刻反映 */
async function controlFrame(id: number, apply: (source: FrameSource) => void, timeMs: number) {
  if (!frameSource) throw new RuntimeError('notLoaded');
  apply(frameSource);
  lastTimeMs = timeMs;
  const frame = await frameSource.render(timeMs);
  post({ id, type: 'frame', payload: { timeMs, frame } }, [frame]);
}

async function handle(request: RenderRequest) {
  switch (request.type) {
    case 'init':
      size = { width: request.payload.width, height: request.payload.height };
      post({ id: request.id, type: 'ready' });
      break;
    case 'load':
      try {
        await loadSkeleton(request.id, request.payload);
      } catch (error) {
        post({ id: request.id, type: 'error', payload: { code: 'loadCrashed', message: text(error), attempts: [] } });
      }
      break;
    case 'render':
      try {
        await renderFrame(request.id, request.payload.timeMs, request.payload.transform);
      } catch (error) {
        post({ id: request.id, type: 'error', payload: { code: 'renderFailed', message: text(error), attempts: [] } });
      }
      break;
    case 'seek':
      try {
        await controlFrame(request.id, (source) => source.seek(request.payload.timeMs), request.payload.timeMs);
      } catch (error) {
        post({ id: request.id, type: 'error', payload: { code: 'renderFailed', message: text(error), attempts: [] } });
      }
      break;
    case 'setAnimation':
      try {
        await controlFrame(request.id, (source) => source.setAnimation(request.payload.animation, request.payload.loop), 0);
      } catch (error) {
        post({ id: request.id, type: 'error', payload: { code: 'renderFailed', message: text(error), attempts: [] } });
      }
      break;
    case 'setSkin':
      try {
        await controlFrame(request.id, (source) => source.setSkin(request.payload.skin), lastTimeMs);
      } catch (error) {
        post({ id: request.id, type: 'error', payload: { code: 'renderFailed', message: text(error), attempts: [] } });
      }
      break;
    case 'setTransform':
      try {
        await controlFrame(
          request.id,
          (source) => source.setTransform(request.payload.offsetX, request.payload.offsetY, request.payload.scale),
          lastTimeMs,
        );
      } catch (error) {
        post({ id: request.id, type: 'error', payload: { code: 'renderFailed', message: text(error), attempts: [] } });
      }
      break;
    case 'dispose':
      frameSource?.dispose();
      frameSource = null;
      lastTimeMs = 0;
      break;
  }
}

function text(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

self.onmessage = (event: MessageEvent<RenderRequest>) => {
  void handle(event.data);
};
