import type { FrameSize, FrameSource, LoadAttempt } from '../spine/types';
import { RuntimeError } from '../spine/types';
import { loadFrameFromCandidates } from '../spine/runtimes';
import { resolveRuntimeCandidates } from '../spine/runtimeMap';
import type { LoadPayload, LoadResponsePayload, RenderRequest, RenderResponse } from './protocol';

let size: FrameSize = { width: 0, height: 0 };
let frameSource: FrameSource | null = null;

function post(message: RenderResponse, transfer: Transferable[] = []) {
  self.postMessage(message, transfer);
}

async function loadSkeleton(id: number, payload: LoadPayload) {
  const resolution = resolveRuntimeCandidates(payload.version);
  if (resolution.unknown) {
    post({ id, type: 'error', payload: { code: 'unknownVersion', message: 'sniff-failed', attempts: [] } });
    return;
  }
  if (resolution.candidates.length === 0) {
    post({ id, type: 'error', payload: { code: 'runtimeUnavailable', message: resolution.reason, attempts: [] } });
    return;
  }

  const outcome = await loadFrameFromCandidates(resolution.candidates, {
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
  const response: LoadResponsePayload = {
    packId: outcome.outcome.packId,
    backend: outcome.outcome.backend,
    runtimeVersion: outcome.outcome.version,
    declaredVersion: summary.declaredVersion,
    bones: summary.bones,
    animationCount: summary.animationCount,
    animation: frameSource.animations()[0] ?? '',
    animations: frameSource.animations(),
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

async function renderFrame(id: number, timeMs: number) {
  if (!frameSource) throw new RuntimeError('notLoaded');
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
        await renderFrame(request.id, request.payload.timeMs);
      } catch (error) {
        post({ id: request.id, type: 'error', payload: { code: 'renderFailed', message: text(error), attempts: [] } });
      }
      break;
    case 'dispose':
      frameSource?.dispose();
      frameSource = null;
      break;
  }
}

function text(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

self.onmessage = (event: MessageEvent<RenderRequest>) => {
  void handle(event.data);
};
