// 解析级验证：版本嗅探 -> 候选运行时链 -> pack 解析 -> §12.6 结果校验
// 全程复用应用代码（runtimeMap / runtimes registry / validate），经 vite SSR 转译后在 Node 里跑。
// 渲染（canvaskit surface / WebGL readPixels）需要浏览器，见 REFACTORING_PLAN §5 M2a 的人工验证清单。
// 用法: node scripts/verify-runtimes.mjs [版本 ...]

import fs from 'node:fs';
import path from 'node:path';
import { createServer } from 'vite';

const ROOT = path.resolve(import.meta.dirname, '..');
const ASSETS = path.join(ROOT, 'spine-testfiles');

// expect 为 [骨骼数, 动画数] 表示必须解析成功；
// null 表示候选链必须为空（运行时未接入），code 为期望原因码；
// 'info' 表示运行时未接入但存在相邻候选，解析结果只作信息输出，不计通过/失败；
// 'no-binary' 表示候选链可用但没有任何 .skel 读取路径（3.0–3.2，等 M2e 的自研读取器）。
const CASES = {
  '3.8': { dir: 'spineboy38', atlas: 'spineboy-pma.atlas', files: { 'spineboy-pro': [64, 11], 'spineboy-ess': [18, 7] } },
  '4.0': { dir: 'spineboy40', atlas: 'spineboy-pma.atlas', files: { 'spineboy-pro': [67, 11], 'spineboy-ess': [18, 8] } },
  '4.1': { dir: 'spineboy41', atlas: 'spineboy-pma.atlas', files: { 'spineboy-pro': [67, 11], 'spineboy-ess': [18, 8] } },
  '4.2': { dir: 'spineboy42', atlas: 'spineboy-pro.atlas', files: { 'spineboy-pro': [67, 11] } },
  '4.3': { dir: 'spineboy43', atlas: 'spineboy-pro.atlas', files: { 'spineboy-pro': [67, 11] } },

  // M2d：3.4–3.7 的官方 core 没有 SkeletonBinary，.skel 走自研读取器；
  // 同目录同名 .json 由官方 core 解析，两者再做逐字段结构比对（oracle，见文件末尾）
  '3.7': { dir: 'spineboy37', atlas: 'spineboy-pma.atlas', files: { 'spineboy-pro': [64, 11], 'spineboy-ess': [18, 7] } },
  '3.6': { dir: 'spineboy36', atlas: 'spineboy-pma.atlas', files: { 'spineboy-pro': [65, 11], 'spineboy-ess': [19, 7] } },
  '3.5': { dir: 'spineboy35', atlas: 'spineboy-pma.atlas', files: { spineboy: [17, 8], 'spineboy-hover': [37, 1], 'spineboy-mesh': [28, 1] } },
  '3.4': { dir: 'spineboy34', atlas: 'spineboy-pma.atlas', files: { spineboy: [17, 8], 'spineboy-hover': [37, 1], 'spineboy-mesh': [28, 1] } },
  // 3.3 没有独立运行时，按 §1.1 由 3.4 承接
  '3.3': { dir: 'spineboy33', atlas: 'spineboy.atlas', files: { spineboy: [17, 8] } },
  // M2c：3.0–3.2 由自 vendor 的 3.1 pack 承接，.skel 等 M2e 的 3.0–3.2 读取器
  '3.2': { dir: 'spineboy32', atlas: 'spineboy.atlas', files: { spineboy: [17, 8] } },
  '3.1': { dir: 'spineboy31', atlas: 'spineboy.atlas', files: { spineboy: [17, 8] } },
  '3.0': { dir: 'spineboy30', atlas: 'spineboy.atlas', files: { spineboy: [17, 8] } },
  // 官方 3.1.07 tag 的 goblins-mesh：无 skeleton 段的 3.1 导出，且带 mesh / skinnedmesh，
  // 是 3.1 pack 里 mesh 路径的唯一覆盖（spineboy30–32 全是 region）
  '3.1-mesh': { dir: 'goblins31', atlas: 'goblins-mesh.atlas', files: { 'goblins-mesh': { json: [21, 1] } } },
  // 2.x 二进制没有版本字段，.skel 只能嗅探失败；json 按结构判定为 2.1
  '2.1': {
    dir: 'spineboy21',
    atlas: 'spineboy.atlas',
    code: '2d',
    files: { spineboy: { json: null, skel: 'no-version' }, 'spineboy-old': { json: null } },
  },
};

function readFresh(p) {
  const stat = fs.statSync(p);
  const buf = Buffer.allocUnsafeSlow(stat.size);
  const fd = fs.openSync(p, 'r');
  fs.readSync(fd, buf, 0, stat.size, 0);
  fs.closeSync(fd);
  return new Uint8Array(buf.buffer, buf.byteOffset, buf.byteLength);
}

function toArrayBuffer(bytes) {
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
}

// 解析级验证不需要真实纹理，按 atlas 里的量级给假纹理对象
function fakeTexture() {
  const image = { width: 2048, height: 2048 };
  return {
    ...image,
    setFilters() {},
    setWraps() {},
    getImage() {
      return image;
    },
  };
}


// §12.9 oracle：同名 .json 由官方 SkeletonJson 解析作为期望值，.skel 走当前读取路径，
// 逐字段深度比对，报告第一个不匹配的字段路径。浮点用 1e-4 绝对容差（float32 vs 十进制文本）。
const ORACLE_SKIP = new Set(['hash', 'version', 'name']);
// bone color（编辑器装饰，官方 JSON 读取器不解析）与 4.x Skin 附件条目的内部 id
// （binary/json 两条路径分配规则不同，官方实现自身就不一致）；bones[].visible 是
// 4.3 官方 binary 写 true 而 json 路径留构造默认的内部标志。均为渲染无关的实现差异。
// 官方 3.1 JS 实现自身两路径不一致的内部伪影，比对时跳过：
// bones[].color 编辑器装饰（JSON 路径不解析）；drawOrders 的 JSON 路径误用 Uint32Array，
// -1 哨兵回绕成 4294967295 且回填失效（binary 路径的 int[] 语义才是正确值）
const ORACLE_SKIP_PATH = /^bones\[\d+\]\.(color|visible|icon)$|^fps$|\.id$|\.edges$|\.offset$|\.width$|\.height$|\.propertyIds$|^events\[\d+\]\.stringValue$|\.drawOrders$/;
// .json 导出按十进制截断（老版本 2 位小数，截尾），float32 落回真值；
// offset 一类派生几何值还会叠加截断的放大效应，用相对项覆盖；粗错仍会超出
const FLOAT_ABS = 0.012;
const FLOAT_REL = 2e-4;

function isIdentityOrder(v) {
  if (!Array.isArray(v) || v.length === 0) return false;
  for (let i = 0; i < v.length; i++) if (v[i] !== i) return false;
  return true;
}

function isArrayLike(v) {
  return (
    (Array.isArray(v) || (ArrayBuffer.isView(v) && !(v instanceof DataView))) &&
    typeof v !== 'string'
  );
}

function compareSkeleton(a, b, path, seen) {
  if (a === b) return null;
  if (typeof a === 'number' || typeof b === 'number') {
    if (typeof a !== 'number' || typeof b !== 'number') return path + ' 类型 ' + typeof a + ' vs ' + typeof b;
    if (!Number.isFinite(a) || !Number.isFinite(b)) return (a === b || (Number.isNaN(a) && Number.isNaN(b))) ? null : path + ' 数值 ' + a + ' vs ' + b;
    if (Math.abs(a - b) <= FLOAT_ABS + FLOAT_REL * Math.abs(b)) return null;
    return path + ' 数值 ' + a + ' vs ' + b;
  }
  if (a == null || b == null) {
    // '' 与 null 等价：binary 写空串、json 留空（stringValue/audioPath 等），官方两路径本就不一致
    if ((a == null || a === '') && (b == null || b === '')) return null;
    // json 读取器把无变化的 drawOrder 帧折叠为 null，binary 物化为恒等排列，语义等价（apply 时 null 即跳过）
    if ((a == null && isIdentityOrder(b)) || (b == null && isIdentityOrder(a))) return null;
    return a == null && b == null ? null : path + ' 存在性 ' + String(a) + ' vs ' + String(b);
  }
  if (typeof a === 'string' || typeof a === 'boolean') {
    return a === b ? null : path + ' 值 ' + JSON.stringify(a) + ' vs ' + JSON.stringify(b);
  }
  if (typeof a !== 'object') {
    return typeof b === 'object' ? path + ' 类型 ' + typeof a + ' vs object' : null;
  }
  if (a instanceof Map || b instanceof Map) {
    if (!(a instanceof Map) || !(b instanceof Map)) return path + ' Map vs 非 Map';
    if (a.size !== b.size) return path + ' Map 大小 ' + a.size + ' vs ' + b.size;
    for (const [key, va] of a) {
      if (!b.has(key)) return path + '.' + String(key) + ' 缺失于 skel';
      const bad = compareSkeleton(va, b.get(key), path + '.' + String(key), seen);
      if (bad) return bad;
    }
    return null;
  }
  if (isArrayLike(a) || isArrayLike(b)) {
    if (!isArrayLike(a) || !isArrayLike(b)) return path + ' 数组 vs 非数组';
    if (a.length !== b.length) return path + ' 长度 ' + a.length + ' vs ' + b.length;
    for (let i = 0; i < a.length; i++) {
      const bad = compareSkeleton(a[i], b[i], path + '[' + i + ']', seen);
      if (bad) return bad;
    }
    return null;
  }
  if (a instanceof Set || b instanceof Set) {
    if (!(a instanceof Set) || !(b instanceof Set) || a.size !== b.size) return path + ' Set 不一致';
    return null;
  }
  const ca = a.constructor?.name ?? '?';
  const cb = b.constructor?.name ?? '?';
  if (ca !== cb) return path + ' 类型 ' + ca + ' vs ' + cb;
  let pairs = seen.get(a);
  if (pairs && pairs.has(b)) return null;
  if (pairs) pairs.add(b);
  else seen.set(a, new Set([b]));
  const editorDecor = ca === 'PointAttachment' || ca === 'ClippingAttachment' || ca === 'BoundingBoxAttachment';
  const keys = new Set([...Object.keys(a), ...Object.keys(b)]);
  for (const key of keys) {
    const childPath = path ? path + '.' + key : key;
    if (ORACLE_SKIP.has(key) && path === '') continue;
    if (ORACLE_SKIP_PATH.test(childPath)) continue;
    if (editorDecor && key === 'color') continue;
    let va = a[key];
    let vb = b[key];
    // binary 与 json 两条路径收集 timelines 的顺序不同（如 attachment 先于 color），按签名对齐
    if (key === 'timelines' && isArrayLike(va) && isArrayLike(vb)) {
      const sig = (t) =>
        (t?.constructor?.name ?? '?') + '#' +
        (t?.slotIndex ?? t?.boneIndex ?? t?.ikConstraintIndex ?? t?.transformConstraintIndex ?? t?.pathConstraintIndex ?? -1) + '#' +
        (t?.attachment?.name ?? t?.event?.data?.name ?? '') + '#' +
        (t?.frames?.length ?? -1);
      va = [...va].sort((x, y) => (sig(x) < sig(y) ? -1 : sig(x) > sig(y) ? 1 : 0));
      vb = [...vb].sort((x, y) => (sig(x) < sig(y) ? -1 : sig(x) > sig(y) ? 1 : 0));
    }
    if (typeof va === 'function' || typeof vb === 'function') continue;
    const bad = compareSkeleton(va, vb, path ? path + '.' + key : key, seen);
    if (bad) return bad;
  }
  return null;
}

function describe(error) {
  if (!error) return String(error);
  if (error.code) return error.detail ? error.code + ':' + error.detail : String(error.code);
  return error.message ? error.message : String(error);
}

// DOM 全局兜底在 src/spine/runtimes/index.ts 入口，这里不再重复
const server = await createServer({ root: ROOT, configFile: false, logLevel: 'error', server: { middlewareMode: true } });

let failed = 0;
let passed = 0;

const wanted = process.argv.slice(2).filter((arg) => CASES[arg]);

try {
  const versionLoader = await server.ssrLoadModule('/src/spine/versionLoader.ts');
  const runtimeMap = await server.ssrLoadModule('/src/spine/runtimeMap.ts');
  const registry = await server.ssrLoadModule('/src/spine/runtimes/index.ts');
  const { validateSkeletonData } = await server.ssrLoadModule('/src/spine/runtimes/validate.ts');
  const { readLegacySkeletonData } = await server.ssrLoadModule('/src/spine/binary/legacyBinary.ts');
  const { readLegacy31SkeletonData } = await server.ssrLoadModule('/src/spine/binary/legacyBinary31.ts');
  const readLegacyBinary = (spine, bytes, loader) => readLegacySkeletonData(spine, bytes, loader);

  for (const [version, spec] of Object.entries(CASES)) {
    if (wanted.length && !wanted.includes(version)) continue;

    const dir = path.join(ASSETS, spec.dir);
    for (const name of Object.keys(spec.files)) {
      for (const ext of ['.json', '.skel']) {
        const declared = spec.files[name];
        const expected = declared && !Array.isArray(declared) && typeof declared === 'object' ? declared[ext.slice(1)] : declared;
        if (expected === undefined) continue;
        const file = name + ext;
        const label = version + ' ' + file;

        try {
          const bytes = readFresh(path.join(dir, file));
          const sniffed = versionLoader.detectSpineVersion(toArrayBuffer(bytes));
          const resolution = runtimeMap.resolveRuntimeCandidates(sniffed);
          const chain = resolution.candidates.map((c) => c.packId).join('/') || '-';

          if (expected === 'no-version') {
            if (sniffed !== null || resolution.candidates.length > 0) {
              throw new Error('2.x 二进制应嗅探失败，实际 ' + (sniffed ? sniffed.raw : chain));
            }
            console.log('INFO ' + label.padEnd(26) + ' 二进制无版本字段，待 M2d 自研读取器');
            continue;
          }

          if (expected === null) {
            if (sniffed === null) throw new Error('版本嗅探失败');
            if (resolution.candidates.length > 0) throw new Error('候选链应为空，实际 ' + chain);
            if (resolution.reason !== spec.code) throw new Error('原因码 ' + resolution.reason + ' != ' + spec.code);
            passed++;
            console.log('PASS ' + label.padEnd(26) + ' 未接入 嗅探=' + sniffed.raw + ' 原因=' + resolution.reason);
            continue;
          }

          if (expected === 'no-binary') {
            if (sniffed === null) throw new Error('版本嗅探失败');
            if (resolution.candidates.length === 0) throw new Error('候选链不应为空');
            const binaryPack = await registry.loadRuntimePack(resolution.candidates[0].packId);
            if (typeof binaryPack.core.SkeletonBinary === 'function') throw new Error('该版本已带 SkeletonBinary，期望值应改为解析结果');
            passed++;
            console.log('PASS ' + label.padEnd(26) + ' 官方无 SkeletonBinary，待 M2d 自研读取器 pack=' + binaryPack.id);
            continue;
          }

          if (sniffed === null) throw new Error('版本嗅探失败');
          if (spec.declared && resolution.declaredUnavailable !== spec.declared) {
            throw new Error('声明未接入码 ' + String(resolution.declaredUnavailable) + ' != ' + spec.declared);
          }
          if (resolution.candidates.length === 0) throw new Error('候选链为空');

          const pack = await registry.loadRuntimePack(resolution.candidates[0].packId);
          const spine = pack.core;
          const atlasText = fs.readFileSync(path.join(dir, spec.atlas), 'utf8').replace(/\r\n/g, '\n');
          let atlas;
          let attachmentLoader;
          let data;
          if (pack.backend === 'legacy') {
            // 3.1：Atlas 构造期同步回调 load(page, path)，贴图挂到 page.rendererObject
            atlas = new spine.Atlas(atlasText, { load: (page) => { page.rendererObject = fakeTexture(); }, unload: () => {} });
            attachmentLoader = new spine.AtlasAttachmentLoader(atlas);
            data =
              ext === '.skel'
                ? readLegacy31SkeletonData(spine, bytes, attachmentLoader)
                : new spine.SkeletonJson(attachmentLoader).readSkeletonData(JSON.parse(fs.readFileSync(path.join(dir, file), 'utf8')));
          } else {
            atlas = pack.capabilities.synchronousAtlasLoader
              ? new spine.TextureAtlas(atlasText, () => fakeTexture())
              : new spine.TextureAtlas(atlasText);
            if (!pack.capabilities.synchronousAtlasLoader) {
              for (const page of atlas.pages) page.setTexture(fakeTexture());
            }
            attachmentLoader = new spine[pack.capabilities.attachmentLoader](atlas);
            data =
              ext === '.skel'
                ? typeof spine.SkeletonBinary === 'function'
                  ? new spine.SkeletonBinary(attachmentLoader).readSkeletonData(bytes)
                  : readLegacyBinary(spine, bytes, attachmentLoader)
                : new spine.SkeletonJson(attachmentLoader).readSkeletonData(fs.readFileSync(path.join(dir, file), 'utf8'));
          }

          const summary = validateSkeletonData(data, sniffed);

          if (expected === 'info') {
            console.log(
              'INFO ' + label.padEnd(26) + ' ' + pack.backend.padEnd(9) + ' 回退→' + chain +
                ' 声明=' + summary.declaredVersion + ' 骨骼=' + summary.bones + ' 动画=' + summary.animationCount,
            );
            continue;
          }

          if (summary.bones !== expected[0]) throw new Error('骨骼 ' + summary.bones + ' != ' + expected[0]);
          if (summary.animationCount !== expected[1]) throw new Error('动画 ' + summary.animationCount + ' != ' + expected[1]);

          if (ext === '.skel') {
            const jsonPath = path.join(dir, name + '.json');
            if (fs.existsSync(jsonPath)) {
              const expectedData =
                pack.backend === 'legacy'
                  ? new spine.SkeletonJson(attachmentLoader).readSkeletonData(JSON.parse(fs.readFileSync(jsonPath, 'utf8')))
                  : new spine.SkeletonJson(attachmentLoader).readSkeletonData(fs.readFileSync(jsonPath, 'utf8'));
              if (data.hash && expectedData.hash && data.hash !== expectedData.hash) {
                console.log('INFO ' + label.padEnd(26) + ' .skel/.json 非同一次导出（hash 不同），跳过结构比对');
              } else {
                const mismatch = compareSkeleton(data, expectedData, '', new Map());
                if (mismatch) throw new Error('结构比对失败: ' + mismatch);
              }
            }
          }
          passed++;
          console.log(
            'PASS ' + label.padEnd(26) + ' ' + pack.backend.padEnd(9) + ' pack=' + pack.id +
              ' 声明=' + summary.declaredVersion + ' 骨骼=' + summary.bones + ' 动画=' + summary.animationCount,
          );
        } catch (error) {
          if (expected === 'info') {
            console.log('INFO ' + label.padEnd(26) + ' 回退解析被拒（预期） ' + describe(error).slice(0, 48));
            continue;
          }
          failed++;
          console.log('FAIL ' + label.padEnd(26) + ' ' + describe(error).slice(0, 90));
        }
      }
    }
  }
} finally {
  await server.close();
}

console.log('\n通过 ' + passed + '，失败 ' + failed);
process.exit(failed === 0 ? 0 : 1);
