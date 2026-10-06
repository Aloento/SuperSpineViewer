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
// 'no-binary' 表示候选链可用但官方 core 没有 SkeletonBinary（3.4–3.7，等 M2d 的自研读取器）。
const CASES = {
  '3.8': { dir: 'spineboy38', atlas: 'spineboy-pma.atlas', files: { 'spineboy-pro': [64, 11], 'spineboy-ess': [18, 7] } },
  '4.0': { dir: 'spineboy40', atlas: 'spineboy-pma.atlas', files: { 'spineboy-pro': [67, 11], 'spineboy-ess': [18, 8] } },
  '4.1': { dir: 'spineboy41', atlas: 'spineboy-pma.atlas', files: { 'spineboy-pro': [67, 11], 'spineboy-ess': [18, 8] } },
  '4.2': { dir: 'spineboy42', atlas: 'spineboy-pro.atlas', files: { 'spineboy-pro': [67, 11] } },
  '4.3': { dir: 'spineboy43', atlas: 'spineboy-pro.atlas', files: { 'spineboy-pro': [67, 11] } },

  // M2b：3.4–3.7 只有官方 core（无 SkeletonBinary），.json 必须解析成功，.skel 断言「无二进制读取器」
  '3.7': { dir: 'spineboy37', atlas: 'spineboy-pma.atlas', files: { 'spineboy-pro': { json: [64, 11], skel: 'no-binary' }, 'spineboy-ess': { json: [18, 7], skel: 'no-binary' } } },
  '3.6': { dir: 'spineboy36', atlas: 'spineboy-pma.atlas', files: { 'spineboy-pro': { json: [65, 11], skel: 'no-binary' }, 'spineboy-ess': { json: [19, 7], skel: 'no-binary' } } },
  '3.5': { dir: 'spineboy35', atlas: 'spineboy-pma.atlas', files: { spineboy: { json: [17, 8], skel: 'no-binary' }, 'spineboy-hover': { json: [37, 1], skel: 'no-binary' }, 'spineboy-mesh': { json: [28, 1], skel: 'no-binary' } } },
  '3.4': { dir: 'spineboy34', atlas: 'spineboy-pma.atlas', files: { spineboy: { json: [17, 8], skel: 'no-binary' }, 'spineboy-hover': { json: [37, 1], skel: 'no-binary' }, 'spineboy-mesh': { json: [28, 1], skel: 'no-binary' } } },
  // 3.3 没有独立运行时，按 §1.1 由 3.4 承接
  '3.3': { dir: 'spineboy33', atlas: 'spineboy.atlas', files: { spineboy: { json: [17, 8], skel: 'no-binary' } } },
  // M2c：3.0–3.2 由自 vendor 的 3.1 pack 承接，官方 3.1 core 没有 SkeletonBinary
  '3.2': { dir: 'spineboy32', atlas: 'spineboy.atlas', files: { spineboy: { json: [17, 8], skel: 'no-binary' } } },
  '3.1': { dir: 'spineboy31', atlas: 'spineboy.atlas', files: { spineboy: { json: [17, 8], skel: 'no-binary' } } },
  '3.0': { dir: 'spineboy30', atlas: 'spineboy.atlas', files: { spineboy: { json: [17, 8], skel: 'no-binary' } } },
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
            const json = JSON.parse(fs.readFileSync(path.join(dir, file), 'utf8'));
            data = new spine.SkeletonJson(attachmentLoader).readSkeletonData(json);
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
                ? new spine.SkeletonBinary(attachmentLoader).readSkeletonData(bytes)
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
