import { RenderSession, RenderWorkerError } from '../src/spine/renderSession';
import { detectSpineVersion } from '../src/spine/versionLoader';

// exts 缺省为 ['json','skel']；goblins31 没有 .skel 语料，只跑 .json
const CASES = [
  { dir: 'spineboy30', atlas: 'spineboy.atlas', files: ['spineboy'] },
  { dir: 'spineboy31', atlas: 'spineboy.atlas', files: ['spineboy'] },
  { dir: 'spineboy32', atlas: 'spineboy.atlas', files: ['spineboy'] },
  // 官方 3.1.07 tag 的 goblins-mesh：legacy 渲染器 mesh / skinnedmesh 路径的唯一覆盖
  { dir: 'goblins31', atlas: 'goblins-mesh.atlas', files: ['goblins-mesh'], exts: ['json'] },
  { dir: 'spineboy34', atlas: 'spineboy-pma.atlas', files: ['spineboy', 'spineboy-mesh'] },
  { dir: 'spineboy35', atlas: 'spineboy-pma.atlas', files: ['spineboy', 'spineboy-hover'] },
  { dir: 'spineboy36', atlas: 'spineboy-pma.atlas', files: ['spineboy-pro', 'spineboy-ess'] },
  { dir: 'spineboy37', atlas: 'spineboy-pma.atlas', files: ['spineboy-pro', 'spineboy-ess'] },
  { dir: 'spineboy38', atlas: 'spineboy-pma.atlas', files: ['spineboy-pro', 'spineboy-ess'] },
  { dir: 'spineboy40', atlas: 'spineboy-pma.atlas', files: ['spineboy-pro', 'spineboy-ess'] },
  { dir: 'spineboy41', atlas: 'spineboy-pma.atlas', files: ['spineboy-pro', 'spineboy-ess'] },
  { dir: 'spineboy42', atlas: 'spineboy-pro.atlas', files: ['spineboy-pro'] },
  { dir: 'spineboy43', atlas: 'spineboy-pro.atlas', files: ['spineboy-pro'] },
];

// alpha 语义回归：同一骨架，把贴图页换成另一种 alpha 存储（预乘 ↔ 直通）后渲染，
// 合成像素必须一致。只要有任何一处把预乘贴图当直通 alpha 用（接缝暗边），两种存储
// 的结果就会差出暗边。未声明 pma 的图集（≤3.8）只换贴图字节，考验解码期判定；
// 已声明的（4.0+）同时翻转声明，考验声明优先。
const ALPHA_SWAP_CASES = [
  { dir: 'spineboy30', atlas: 'spineboy.atlas', file: 'spineboy.json', page: 'spineboy.png', toPma: false },
  { dir: 'goblins31', atlas: 'goblins-mesh.atlas', file: 'goblins-mesh.json', page: 'goblins-mesh.png', toPma: true },
  { dir: 'spineboy34', atlas: 'spineboy-pma.atlas', file: 'spineboy.json', page: 'spineboy-pma.png', toPma: false },
  { dir: 'spineboy36', atlas: 'spineboy-pma.atlas', file: 'spineboy-pro.json', page: 'spineboy-pma.png', toPma: false },
  { dir: 'spineboy38', atlas: 'spineboy-pma.atlas', file: 'spineboy-pro.json', page: 'spineboy-pma.png', toPma: false },
  { dir: 'spineboy40', atlas: 'spineboy-pma.atlas', file: 'spineboy-pro.json', page: 'spineboy-pma.png', toPma: false, flip: true },
  { dir: 'spineboy41', atlas: 'spineboy-pma.atlas', file: 'spineboy-pro.json', page: 'spineboy-pma.png', toPma: false, flip: true },
  { dir: 'spineboy43', atlas: 'spineboy-pro.atlas', file: 'spineboy-pro.json', page: 'spineboy-pro.png', toPma: false, flip: true },
];

const ERROR_CASES = [
  { dir: 'spineboy21', atlas: 'spineboy.atlas', file: 'spineboy.json', expect: 'runtimeUnavailable:2d' },
];

// 手动指定运行时 pack（packOverride）：跳过候选链，只用所选 pack；错配时必须失败而不是静默渲染
const OVERRIDE_CASES: { dir: string; atlas: string; file: string; pack: string; expectOk: boolean }[] = [
  { dir: 'goblins31', atlas: 'goblins-mesh.atlas', file: 'goblins-mesh.json', pack: '3.1', expectOk: true },
  // 3.4 的 .skel 指定到 3.1 pack：legacyBinary 版本区间不含 3.4，必须报 binaryUnsupported
  { dir: 'spineboy34', atlas: 'spineboy-pma.atlas', file: 'spineboy.skel', pack: '3.1', expectOk: false },
  // 指定到正确的 pack：.skel 经自研读取器渲染成功
  { dir: 'spineboy34', atlas: 'spineboy-pma.atlas', file: 'spineboy.skel', pack: '3.4', expectOk: true },
];

// 3.2 与 3.3 的 spineboy：贴图逐字节相同、可绘制数据一致（3.3 只多了 boundingbox 的 vertexCount），
// 分别走 3.1 pack 的自研 CanvasKit 渲染器和 3.4 pack 的官方 spine-webgl，同一时刻逐帧像素比对。
// 两侧都是 straight-alpha 输出（webgl context premultipliedAlpha:false，canvaskit Unpremul）。
const COMPARE_CASES = [
  { leftDir: 'spineboy32', rightDir: 'spineboy33', atlas: 'spineboy.atlas', file: 'spineboy.json', times: [0, 1000] },
];

// 同一目录的 .json（官方 SkeletonJson）与 .skel（自研读取器）逐帧像素回归：
// 同一 pack、同一渲染路径，差异只可能来自解析层；JSON 十进制文本与 float32 的
// 微小数值差（截断 2 位）允许 AA 级抖动，阈值与后端对比一致
const COMPARE_FORMAT_CASES = [
  { dir: 'spineboy30', atlas: 'spineboy.atlas', name: 'spineboy', times: [0, 1000] },
  { dir: 'spineboy32', atlas: 'spineboy.atlas', name: 'spineboy', times: [0, 1000] },
  { dir: 'spineboy34', atlas: 'spineboy-pma.atlas', name: 'spineboy', times: [0, 1000] },
  { dir: 'spineboy35', atlas: 'spineboy-pma.atlas', name: 'spineboy', times: [0, 1000] },
  { dir: 'spineboy36', atlas: 'spineboy-pma.atlas', name: 'spineboy-pro', times: [0, 1000] },
  { dir: 'spineboy37', atlas: 'spineboy-pma.atlas', name: 'spineboy-pro', times: [0, 1000] },
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

/** 把图集里某一页贴图的 alpha 存储翻转（预乘 ↔ 直通）；声明了 pma 的图集同时翻转声明 */
async function swapPageAlpha(
  files: Record<string, ArrayBuffer>,
  spec: { atlas: string; page: string; toPma: boolean; flip?: boolean },
): Promise<Record<string, ArrayBuffer>> {
  const key = Object.keys(files).find((name) => name.toLowerCase().endsWith(spec.page.toLowerCase()));
  if (!key) throw new Error('page not loaded: ' + spec.page);
  const bitmap = await createImageBitmap(new Blob([files[key]], { type: 'image/png' }));
  const canvas = new OffscreenCanvas(bitmap.width, bitmap.height);
  const context = canvas.getContext('2d', { alpha: true })!;
  context.drawImage(bitmap, 0, 0);
  bitmap.close();
  const image = context.getImageData(0, 0, canvas.width, canvas.height);
  const data = image.data;
  for (let i = 0; i < data.length; i += 4) {
    const alpha = data[i + 3];
    if (alpha === 0 || alpha >= 255) continue;
    if (spec.toPma) {
      data[i] = Math.round((data[i] * alpha) / 255);
      data[i + 1] = Math.round((data[i + 1] * alpha) / 255);
      data[i + 2] = Math.round((data[i + 2] * alpha) / 255);
    } else {
      data[i] = Math.min(255, Math.round((data[i] * 255) / alpha));
      data[i + 1] = Math.min(255, Math.round((data[i + 1] * 255) / alpha));
      data[i + 2] = Math.min(255, Math.round((data[i + 2] * 255) / alpha));
    }
  }
  context.putImageData(image, 0, 0);
  const encoded = await canvas.convertToBlob({ type: 'image/png' });
  const next: Record<string, ArrayBuffer> = { ...files, [key]: await encoded.arrayBuffer() };
  if (spec.flip) {
    const text = new TextDecoder().decode(files[spec.atlas]);
    next[spec.atlas] = new TextEncoder().encode(
      /pma:\s*true/i.test(text) ? text.replace(/pma:\s*true/gi, 'pma:false') : text.replace(/pma:\s*false/gi, 'pma:true'),
    ).buffer;
  }
  return next;
}

/** 用给定文件集加载骨架，取若干时刻的直通 alpha 帧像素 */
async function renderTimes(
  files: Record<string, ArrayBuffer>,
  atlasFile: string,
  skeletonFile: string,
  times: number[],
): Promise<Map<number, Uint8ClampedArray>> {
  const frames = new Map<number, Uint8ClampedArray>();
  const session = new RenderSession();
  try {
    await session.init(320, 320);
    await session.load({
      files,
      skeletonFile,
      atlasFile,
      version: detectSpineVersion(files[skeletonFile])!,
    });
    for (const timeMs of times) {
      const bitmap = await session.frame(timeMs);
      const canvas = new OffscreenCanvas(bitmap.width, bitmap.height);
      const context = canvas.getContext('2d', { willReadFrequently: true })!;
      context.drawImage(bitmap, 0, 0);
      bitmap.close();
      frames.set(timeMs, context.getImageData(0, 0, canvas.width, canvas.height).data);
    }
  } finally {
    session.dispose();
  }
  return frames;
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

  for (const spec of ALPHA_SWAP_CASES) {
    const label = `${spec.dir}/alpha-swap ${spec.page}`;
    try {
      const base = await gather(spec.dir, spec.atlas, [spec.file.replace(/\.(json|skel)$/, '')]);
      const original = await renderTimes(base, spec.atlas, spec.file, [0, 1000]);
      const swapped = await renderTimes(await swapPageAlpha(base, spec), spec.atlas, spec.file, [0, 1000]);
      let worst = 0;
      let changed = 0;
      let total = 0;
      for (const timeMs of [0, 1000]) {
        const a = original.get(timeMs)!;
        const b = swapped.get(timeMs)!;
        for (let i = 0; i < a.length; i += 4) {
          // 只看合成结果；反预乘的取整误差合成后最多 3，暗边差在 20 以上
          const d = Math.max(
            Math.abs(a[i] * a[i + 3] - b[i] * b[i + 3]),
            Math.abs(a[i + 1] * a[i + 3] - b[i + 1] * b[i + 3]),
            Math.abs(a[i + 2] * a[i + 3] - b[i + 2] * b[i + 3]),
          ) / 255;
          total += 1;
          if (d > 4) changed += 1;
          if (d > worst) worst = d;
        }
      }
      const ratio = total > 0 ? changed / total : 0;
      results.push({
        status: ratio <= 0.005 ? 'PASS' : 'FAIL',
        case: label,
        ratio: Number(ratio.toFixed(5)),
        worst: Number(worst.toFixed(1)),
      });
    } catch (error) {
      results.push({ status: 'FAIL', case: label, error: error instanceof Error ? error.message : String(error) });
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

  for (const spec of OVERRIDE_CASES) {
    const files = await gather(spec.dir, spec.atlas, [spec.file.replace(/\.(json|skel)$/, '')]);
    const session = new RenderSession();
    try {
      await session.init(320, 320);
      const loaded = await session.load({
        files,
        skeletonFile: spec.file,
        atlasFile: spec.atlas,
        version: detectSpineVersion(files[spec.file]) ?? { raw: '3.1', major: 3, minor: 1, patch: 0 },
        packOverride: spec.pack,
      });
      const { frames, opaque, error } = await play(session);
      const ok = loaded.packId === spec.pack && opaque > 500 && frames > 3 && !error;
      results.push({
        status: spec.expectOk === ok ? 'PASS' : 'FAIL',
        case: `${spec.dir}/${spec.file} packOverride=${spec.pack}`,
        pack: loaded.packId,
        opaque,
        frames,
      });
    } catch (error) {
      results.push({
        status: spec.expectOk ? 'FAIL' : 'PASS',
        case: `${spec.dir}/${spec.file} packOverride=${spec.pack}`,
        actual: error instanceof RenderWorkerError ? `${error.code}:${error.message}` : String(error),
      });
    } finally {
      session.dispose();
    }
  }

  for (const spec of COMPARE_CASES) {
    type Shot = { opaque: number; data: Uint8ClampedArray; width: number; height: number };
    const label = `${spec.leftDir}(legacy) vs ${spec.rightDir}(webgl) ${spec.file}`;
    const shots = new Map<string, Map<number, Shot>>();
    let compareError: string | null = null;
    try {
      for (const dir of [spec.leftDir, spec.rightDir]) {
        const files = await gather(dir, spec.atlas, [spec.file.replace(/\.(json|skel)$/, '')]);
        const session = new RenderSession();
        try {
          await session.init(320, 320);
          const loaded = await session.load({
            files,
            skeletonFile: spec.file,
            atlasFile: spec.atlas,
            version: detectSpineVersion(files[spec.file])!,
          });
          const want = dir === spec.leftDir ? 'legacy' : 'webgl';
          if (loaded.backend !== want) compareError = `${dir} backend=${loaded.backend}，应为 ${want}`;
          const frames = new Map<number, Shot>();
          for (const timeMs of spec.times) {
            const bitmap = await session.frame(timeMs);
            const canvas = new OffscreenCanvas(bitmap.width, bitmap.height);
            const context = canvas.getContext('2d', { willReadFrequently: true })!;
            context.drawImage(bitmap, 0, 0);
            bitmap.close();
            const image = context.getImageData(0, 0, canvas.width, canvas.height);
            let opaque = 0;
            for (let i = 3; i < image.data.length; i += 4) if (image.data[i] > 8) opaque++;
            frames.set(timeMs, { opaque, data: image.data, width: canvas.width, height: canvas.height });
          }
          shots.set(dir, frames);
        } finally {
          session.dispose();
        }
      }
    } catch (error) {
      compareError = error instanceof Error ? error.message : String(error);
    }

    if (!compareError) {
      const left = shots.get(spec.leftDir)!;
      const right = shots.get(spec.rightDir)!;
      for (const timeMs of spec.times) {
        const a = left.get(timeMs)!;
        const b = right.get(timeMs)!;
        if (a.width !== b.width || a.height !== b.height) {
          compareError = `${timeMs}ms 尺寸 ${a.width}x${a.height} != ${b.width}x${b.height}`;
          break;
        }
        // 两条后端的 AA 边缘取整路径不同：直通 alpha 下近透明像素的颜色是噪声，
        // 合成结果只看预乘值，因此统一预乘后再比，另对不透明内部做严格校验
        let diff = 0;
        let interior = 0;
        let interiorDiff = 0;
        let minX = a.width;
        let minY = a.height;
        let maxX = -1;
        let maxY = -1;
        for (let y = 0; y < a.height; y++) {
          for (let x = 0; x < a.width; x++) {
            const i = (y * a.width + x) * 4;
            const alphaA = a.data[i + 3];
            const alphaB = b.data[i + 3];
            let differs = false;
            for (let k = 0; k < 3; k++) {
              if (Math.abs(((a.data[i + k] * alphaA) >> 8) - ((b.data[i + k] * alphaB) >> 8)) > 16) {
                differs = true;
                break;
              }
            }
            if (alphaA > 200 && alphaB > 200) {
              interior++;
              if (
                Math.abs(a.data[i] - b.data[i]) > 16 ||
                Math.abs(a.data[i + 1] - b.data[i + 1]) > 16 ||
                Math.abs(a.data[i + 2] - b.data[i + 2]) > 16
              ) {
                interiorDiff++;
              }
            }
            if (differs) {
              diff++;
              if (x < minX) minX = x;
              if (x > maxX) maxX = x;
              if (y < minY) minY = y;
              if (y > maxY) maxY = y;
            }
          }
        }
        const ratio = diff / (a.width * a.height);
        const interiorRatio = interior > 0 ? interiorDiff / interior : 0;
        // 内部容差 1%：Skia 纹理解码后恒为预乘，线性过滤按预乘插值；
        // webgl 按 spine 约定用直通 alpha 纹理过滤，几何硬边上的 AA 混色两条路径本就不同
        if (ratio > 0.005 || interiorRatio > 0.01) {
          compareError =
            `${timeMs}ms 预乘差异像素 ${(ratio * 100).toFixed(2)}%，不透明区差异 ${(interiorRatio * 100).toFixed(2)}%` +
            `，差异范围 ${minX},${minY}..${maxX},${maxY}`;
          break;
        }
      }
    }

    results.push({
      status: compareError ? 'FAIL' : 'PASS',
      case: label,
      ...(compareError ? { error: compareError } : {}),
      opaque: [...shots.values()].map((frames) => [...frames.values()].map((f) => f.opaque).join(',')).join(' vs '),
    });
  }

  for (const spec of COMPARE_FORMAT_CASES) {
    type Shot2 = { opaque: number; data: Uint8ClampedArray; width: number; height: number };
    const label = `${spec.dir} .json vs .skel ${spec.name}`;
    const shots = new Map<string, Map<number, Shot2>>();
    let formatError: string | null = null;
    try {
      for (const suffix of ['.json', '.skel']) {
        const skeletonFile = `${spec.name}${suffix}`;
        const files = await gather(spec.dir, spec.atlas, [spec.name]);
        const session = new RenderSession();
        try {
          await session.init(320, 320);
          await session.load({
            files,
            skeletonFile,
            atlasFile: spec.atlas,
            version: detectSpineVersion(files[skeletonFile])!,
          });
          const frames = new Map<number, Shot2>();
          for (const timeMs of spec.times) {
            const bitmap = await session.frame(timeMs);
            const canvas = new OffscreenCanvas(bitmap.width, bitmap.height);
            const context = canvas.getContext('2d', { willReadFrequently: true })!;
            context.drawImage(bitmap, 0, 0);
            bitmap.close();
            const image = context.getImageData(0, 0, canvas.width, canvas.height);
            let opaque = 0;
            for (let i = 3; i < image.data.length; i += 4) if (image.data[i] > 8) opaque++;
            frames.set(timeMs, { opaque, data: image.data, width: canvas.width, height: canvas.height });
          }
          shots.set(suffix, frames);
        } finally {
          session.dispose();
        }
      }
      const a = shots.get('.json')!;
      const b = shots.get('.skel')!;
      for (const timeMs of spec.times) {
        const shotA = a.get(timeMs)!;
        const shotB = b.get(timeMs)!;
        if (shotA.width !== shotB.width || shotA.height !== shotB.height) {
          formatError = `${timeMs}ms 尺寸 ${shotA.width}x${shotA.height} != ${shotB.width}x${shotB.height}`;
          break;
        }
        let diff = 0;
        for (let y = 0; y < shotA.height; y++) {
          for (let x = 0; x < shotA.width; x++) {
            const i = (y * shotA.width + x) * 4;
            const alphaA = shotA.data[i + 3];
            const alphaB = shotB.data[i + 3];
            for (let k = 0; k < 3; k++) {
              if (Math.abs(((shotA.data[i + k] * alphaA) >> 8) - ((shotB.data[i + k] * alphaB) >> 8)) > 16) {
                diff++;
                break;
              }
            }
          }
        }
        const ratio = diff / (shotA.width * shotA.height);
        if (ratio > 0.005) {
          formatError = `${timeMs}ms 预乘差异像素 ${(ratio * 100).toFixed(2)}%`;
          break;
        }
      }
    } catch (error) {
      formatError = error instanceof Error ? error.message : String(error);
    }
    results.push({
      status: formatError ? 'FAIL' : 'PASS',
      case: label,
      ...(formatError ? { error: formatError } : {}),
      opaque: [...shots.values()].map((frames) => [...frames.values()].map((f) => f.opaque).join(',')).join(' vs '),
    });
  }

  report({ failed: results.filter((item) => item.status === 'FAIL').length, results });
}

window.addEventListener('error', (event) => report({ failed: -1, fatal: String(event.message) }));
main().catch((error) => report({ failed: -1, fatal: String(error instanceof Error ? error.stack : error) }));
