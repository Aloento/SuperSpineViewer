import { useCallback, useEffect, useRef, useState } from 'react';
import { detectSpineVersion, isSupportedVersion } from './versionLoader';
import { RenderSession } from './renderSession';

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
  version: string | null;
  animation: string | null;
}

const initialState: SpineRendererState = {
  status: 'idle',
  error: null,
  warning: null,
  frame: null,
  version: null,
  animation: null,
};

const skeletonPattern = /\.(skel|json)$/i;
const atlasPattern = /\.atlas$/i;

// .skel 优先：体积小、字段完整；无法解析时再退回 .json
function skeletonRank(name: string): number {
  return name.toLowerCase().endsWith('.skel') ? 0 : 1;
}

export function useSpineRenderer(width: number, height: number) {
  const [state, setState] = useState<SpineRendererState>(initialState);
  const sessionRef = useRef<RenderSession | null>(null);
  const stopRef = useRef<(() => void) | null>(null);
  const runRef = useRef(0);

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
      const run = runRef.current;

      const buffers: Record<string, ArrayBuffer> = {};
      for (const file of files) buffers[file.name] = await file.arrayBuffer();
      if (run !== runRef.current) return;

      const names = Object.keys(buffers);
      const skeletons = names.filter((name) => skeletonPattern.test(name)).sort((a, b) => skeletonRank(a) - skeletonRank(b));
      const atlasFile = names.find((name) => atlasPattern.test(name));

      if (skeletons.length === 0) {
        setState({ ...initialState, status: 'error', error: { key: 'errors.missingSkeleton' } });
        return;
      }
      if (!atlasFile) {
        setState({ ...initialState, status: 'error', error: { key: 'errors.missingAtlas' } });
        return;
      }

      setState({ ...initialState, status: 'loading' });

      const session = new RenderSession();
      sessionRef.current = session;
      try {
        await session.init(new OffscreenCanvas(width, height), width, height);
      } catch (error) {
        if (run !== runRef.current) return;
        teardown();
        setState({ ...initialState, status: 'error', error: { key: 'errors.loadFailed', values: { detail: detail(error) } } });
        return;
      }

      let unknownVersion = false;
      let unsupported: string | null = null;
      let failure: string | null = null;
      let previous: string | null = null;

      for (const skeletonFile of skeletons) {
        const version = detectSpineVersion(buffers[skeletonFile]);
        if (!version) {
          unknownVersion = true;
          previous = skeletonFile;
          continue;
        }
        if (!isSupportedVersion(version)) {
          unsupported = version.raw;
          previous = skeletonFile;
          continue;
        }

        try {
          const animation = await session.load({ files: buffers, skeletonFile, atlasFile, version: version.raw });
          if (run !== runRef.current) return;
          setState({
            status: 'playing',
            error: null,
            warning: previous ? { key: 'warnings.skeletonFallback', values: { from: previous, to: skeletonFile } } : null,
            frame: null,
            version: version.raw,
            animation,
          });
          stopRef.current = session.play(
            (frame) => {
              if (run !== runRef.current) {
                frame.close();
                return;
              }
              setState((current) => {
                current.frame?.close();
                return { ...current, frame };
              });
            },
            (error) => {
              if (run !== runRef.current) return;
              teardown();
              setState({ ...initialState, status: 'error', error: { key: 'errors.renderFailed', values: { detail: detail(error) } } });
            },
          );
          return;
        } catch (error) {
          failure = detail(error);
          previous = skeletonFile;
        }
      }

      if (run !== runRef.current) return;
      teardown();
      if (unsupported !== null) {
        setState({ ...initialState, status: 'error', error: { key: 'errors.unsupportedVersion', values: { version: unsupported } } });
      } else if (unknownVersion && failure === null) {
        setState({ ...initialState, status: 'error', error: { key: 'errors.unknownVersion' } });
      } else {
        setState({ ...initialState, status: 'error', error: { key: 'errors.loadFailed', values: { detail: failure ?? '' } } });
      }
    },
    [teardown, width, height],
  );

  const reset = useCallback(() => {
    teardown();
    setState(initialState);
  }, [teardown]);

  return { ...state, loadFiles, reset };
}

function detail(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
