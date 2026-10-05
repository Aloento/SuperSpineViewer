import { RenderSession, RenderWorkerError } from '../src/spine/renderSession';
import { detectSpineVersion } from '../src/spine/versionLoader';

// exts 缺省为 ['json','skel']；3.4–3.7 的官方 core 没有 SkeletonBinary，只跑 .json
const CASES = [
  { dir: 'spineboy34', atlas: 'spineboy-pma.atlas', files: ['spineboy', 'spineboy-mesh'], exts: ['json'] },
  { dir: 'spineboy35', atlas: 'spineboy-pma.atlas', files: ['spineboy', 'spineboy-hover'], exts: ['json'] },
  { dir: 'spineboy36', atlas: 'spineboy-pma.atlas', files: ['spineboy-pro', 'spineboy-ess'], exts: ['json'] },
  { dir: 'spineboy37', atlas: 'spineboy-pma.atlas', files: ['spineboy-pro', 'spineboy-ess'], exts: ['json'] },
  { dir: 'spineboy38', atlas: 'spineboy-pma.atlas', files: ['spineboy-pro', 'spineboy-ess'] },
  { dir: 'spineboy40', atlas: 'spineboy-pma.atlas', files: ['spineboy-pro', 'spineboy-ess'] },
  { dir: 'spineboy41', atlas: 'spineboy-pma.atlas', files: ['spineboy-pro', 'spineboy-ess'] },
  { dir: 'spineboy42', atlas: 'spineboy-pro.atlas', files: ['spineboy-pro'] },
  { dir: 'spineboy43', atlas: 'spineboy-pro.atlas', files: ['spineboy-pro'] },
];

const ERROR_CASES = [
  // 3.4–3.7 的 .skel：自身 pack 已接入但官方 core 无 SkeletonBinary，回退到 3.8 会被 §12.6 校验挡住
  { dir: 'spineboy37', atlas: 'spineboy-pma.atlas', file: 'spineboy-pro.skel', expect: 'binaryUnsupported' },
  { dir: 'spineboy34', atlas: 'spineboy-pma.atlas', file: 'spineboy.skel', expect: 'binaryUnsupported' },
  { dir: 'spineboy21', atlas: 'spineboy.atlas', file: 'spineboy.json', expect: 'runtimeUnavailable:2d' },
  { dir: 'spineboy32', atlas: 'spineboy.atlas', file: 'spineboy.json', expect: 'runtimeUnavailable:legacy' },
];

const out = document.getElementById('out')!;

function report(payload: unknown) {
  out!.textContent = 'M2A_RESULT ' + JSON.stringify(payload);
}

async function fetchBuffer(url: string): Promise<ArrayBuffer | null> {
  const response = await fetch(url);
  return response.ok ? response.arrayBuffer() : null;
}

async function gather(dir: string, atlas: string, names: string[]) {
  const files: Record<string, ArrayBuffer> = {};
  const atlasBuffer = await fetchBuffer(`/spine-testfiles/${dir}/${atlas}`);
  if (atlasBuffer) files[atlas] = atlasBuffer;
  const text = new TextDecoder().decode(files[atlas] ?? new ArrayBuffer(0));
  const textures = text.split('\n').map((line) => line.trim()).filter((line) => /\.(png|jpe?g|webp)$/i.test(line));
  for (const texture of textures) {
    const buffer = await fetchBuffer(`/spine-testfiles/${dir}/${texture}`);
    if (buffer) files[texture] = buffer;
  }
  for (const name of names) {
    for (const ext of ['.json', '.skel']) {
      const buffer = await fetchBuffer(`/spine-testfiles/${dir}/${name}${ext}`);
      if (buffer) files[`${name}${ext}`] = buffer;
    }
  }
  return files;
}

function opaquePixels(bitmap: ImageBitmap): number {
  const canvas = new OffscreenCanvas(bitmap.width, bitmap.height);
  const context = canvas.getContext('2d', { willReadFrequently: true })!;
  context.drawImage(bitmap, 0, 0);
  const data = context.getImageData(0, 0, bitmap.width, bitmap.height).data;
  let count = 0;
  for (let i = 3; i < data.length; i += 4) if (data[i] > 8) count++;
  return count;
}

async function play(session: RenderSession): Promise<{ frames: number; opaque: number; error: string | null }> {
  let frames = 0;
  let opaque = 0;
  let error: string | null = null;
  const stop = session.play(
    (frame) => {
      frames += 1;
      opaque = Math.max(opaque, opaquePixels(frame));
      frame.close();
    },
    (err) => {
      error = String(err instanceof Error ? err.message : err);
    },
  );
  await new Promise((resolve) => setTimeout(resolve, 500));
  stop();
  return { frames, opaque, error };
}

async function main() {
  const results: Record<string, unknown>[] = [];

  for (const spec of CASES) {
    const files = await gather(spec.dir, spec.atlas, spec.files);
    for (const name of spec.files) {
      for (const ext of (spec.exts ?? ['json', 'skel']).map((item) => `.${item}`)) {
        const skeletonFile = `${name}${ext}`;
        const label = `${spec.dir}/${skeletonFile}`;
        const session = new RenderSession();
        try {
          await session.init(320, 320);
          const loaded = await session.load({
            files,
            skeletonFile,
            atlasFile: spec.atlas,
            version: detectSpineVersion(files[skeletonFile])!,
          });
          const { frames, opaque, error } = await play(session);
          results.push({
            status: opaque > 500 && frames > 3 ? 'PASS' : 'FAIL',
            case: label,
            pack: loaded.packId,
            backend: loaded.backend,
            runtime: loaded.runtimeVersion,
            declared: loaded.declaredVersion,
            bones: loaded.bones,
            anims: loaded.animationCount,
            animation: loaded.animation,
            frames,
            opaque,
            ...(error ? { renderError: error } : {}),
          });
        } catch (error) {
          results.push({
            status: 'FAIL',
            case: label,
            error: error instanceof RenderWorkerError ? `${error.code} ${error.message}` : String(error),
          });
        } finally {
          session.dispose();
        }
      }
    }
  }

  for (const spec of ERROR_CASES) {
    const files = await gather(spec.dir, spec.atlas, [spec.file.replace(/\.(json|skel)$/, '')]);
    const session = new RenderSession();
    try {
      await session.init(320, 320);
      const loaded = await session.load({
        files,
        skeletonFile: spec.file,
        atlasFile: spec.atlas,
        version: detectSpineVersion(files[spec.file]!)!,
      });
      results.push({ status: 'FAIL', case: `${spec.dir}/${spec.file}`, error: 'unexpected success pack=' + loaded.packId });
    } catch (error) {
      const code = error instanceof RenderWorkerError ? `${error.code}:${error.message}` : String(error);
      results.push({
        status: code.startsWith(spec.expect) ? 'PASS' : 'FAIL',
        case: `${spec.dir}/${spec.file}`,
        expected: spec.expect,
        actual: code.slice(0, 160),
      });
    } finally {
      session.dispose();
    }
  }

  report({ failed: results.filter((item) => item.status === 'FAIL').length, results });
}

window.addEventListener('error', (event) => report({ failed: -1, fatal: String(event.message) }));
main().catch((error) => report({ failed: -1, fatal: String(error instanceof Error ? error.stack : error) }));
