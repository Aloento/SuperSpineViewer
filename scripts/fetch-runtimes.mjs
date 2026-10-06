#!/usr/bin/env node
// 拉取旧版 Spine 运行时并包成 ESM pack，产物提交进 src/spine/runtimes/generated/
import { mkdir, readFile, writeFile, access } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const outDir = join(root, 'src/spine/runtimes/generated');
const cacheDir = join(root, 'node_modules/.cache/fetch-runtimes');

const raw = (ref, path) => `https://raw.githubusercontent.com/EsotericSoftware/spine-runtimes/${ref}/${path}`;

// 3.1 的 spine.js 是 sloppy-mode 脚本：对定长 typed array 赋值 .length 在 ESM 严格模式下直接抛错，
// 这些行原本就是 no-op；唯一有实际语义的 FFD attachmentVertices 改成普通数组保住 resize。
function legacyStrictFixes(text) {
  let count = 0;
  const drop = (pattern) => {
    text = text.replace(pattern, () => {
      count += 1;
      return '';
    });
  };
  drop(/\n\t*this\.curves\.length = count;/g);
  drop(/\n\t*this\.frames\.length = frameCount[^;]*;/g);
  drop(/\n\t*this\.offset\.length = 8;/g);
  drop(/\n\t*this\.uvs\.length = 8;/g);
  drop(/\n\t*vertices\.length = vertexCount;/g);
  drop(/\n\t*drawOrder\.length = slotCount;/g);
  drop(/\n\t*unchanged\.length = slotCount - offsets\.length;/g);
  drop(/\n\t*polygon\.length = boundingBox\.vertices\.length;/g);
  if (count !== 18) throw new Error(`3.1 strict-mode 补丁命中 ${count} 处，预期 18 处（上游内容有出入）`);
  const swapped = text.replace('this.attachmentVertices = new spine.Float32Array();', 'this.attachmentVertices = [];');
  if (swapped === text) throw new Error('3.1 attachmentVertices 补丁未命中');
  return swapped;
}

const targets = [
  // 3.0–3.2 没有独立分支/tag，统一由 3.1.07 的 spine-js 承接（见 REFACTORING_PLAN §1.1）；
  // 该产物只有核心层，没有 spine.webgl，渲染走自研 CanvasKit frameSource
  { id: '3.1', enabled: true, kind: 'global-script', ref: '3.1.07', entry: 'spine-js/spine.js', noRenderer: true,
    // 上游 FfdTimeline.apply 引用了不存在的 sourceAttachment，联动网格 FFD 一播放就 ReferenceError
    patch: (text) => legacyStrictFixes(text.replace('slotAttachment.parentMesh != sourceAttachment', 'slotAttachment.parentMesh != this.attachment')) },
  // 官方没有 3.4 分支，只有 tag 3.4.02
  { id: '3.4', enabled: true, kind: 'global-script', ref: '3.4.02' },
  { id: '3.5', enabled: true, kind: 'global-script', ref: '3.5' },
  { id: '3.6', enabled: true, kind: 'global-script', ref: '3.6' },
  { id: '3.7', enabled: true, kind: 'global-script', ref: '3.7' },
  { id: '3.8', enabled: true, kind: 'global-script', ref: '3.8' },
  {
    // 4.0 的 npm 包没有 "type": "module"，只能用 dist/iife 产物
    id: '4.0',
    enabled: true,
    kind: 'esbuild-iife',
    entry: 'node_modules/@esotericsoftware/spine-webgl-40/dist/iife/spine-webgl.js',
    license: 'node_modules/@esotericsoftware/spine-webgl-40/LICENSE',
  },
];

const declaration = `export declare const spine: any;
export declare const webgl: any;
declare const pack: { spine: any; webgl: any };
export default pack;
`;

async function exists(path) {
  try {
    await access(path);
    return true;
  } catch {
    return false;
  }
}

async function fetchText(url, cacheName) {
  const cached = join(cacheDir, cacheName);
  // 命中缓存也报原始 URL，生成物的「来源」头与是否离线无关
  if (await exists(cached)) return { text: await readFile(cached, 'utf8'), from: url };
  const response = await fetch(url);
  if (!response.ok) throw new Error(`${response.status} ${response.statusText} ${url}`);
  const text = await response.text();
  await mkdir(cacheDir, { recursive: true });
  await writeFile(cached, text);
  return { text, from: url };
}

async function readLocal(path) {
  const abs = join(root, path);
  if (!(await exists(abs))) throw new Error(`缺少本地文件 ${path}，请先执行 pnpm install`);
  return readFile(abs, 'utf8');
}

function stripSourceMap(text) {
  return text.replace(/\/\/# sourceMappingURL=[^\n\r]*/g, '');
}

function asBlockComment(text) {
  const body = text
    .trim()
    .replace(/\*\//g, '* /')
    .split('\n')
    .map((line) => ` * ${line}`.trimEnd())
    .join('\n');
  return `/**\n${body}\n */`;
}

function header(sourceUrl, license) {
  return ['/* 由 scripts/fetch-runtimes.mjs 生成，请勿手工修改。 */', `/* 来源：${sourceUrl} */`, asBlockComment(license)].join('\n');
}

// 3.x 的 build 产物是 global script：顶层反复 var spine / var webgl，
// 且顶层有 (this && this.__extends)，所以在 this 为非严格对象的函数里执行后取回命名空间
function wrapGlobalScript(source, banner) {
  return `${banner}
const pack = (function () {
${stripSourceMap(source)}
return { spine: spine, webgl: spine.webgl };
}).call({});

export const spine = pack.spine;
export const webgl = pack.webgl;
export default pack;
`;
}

// 4.0 的 dist/iife 是 esbuild IIFE：var spine = (() => { ... return src_exports; })();
function wrapIife(source, banner) {
  return `${banner}
const pack = (function () {
${stripSourceMap(source)}
return spine;
})();

export const spine = pack;
export const webgl = null;
export default { spine: pack, webgl: null };
`;
}

async function generate(target, enableLegacy) {
  if (!target.enabled && !enableLegacy) {
    console.log(`skip  ${target.id}（未启用）`);
    return;
  }

  let source;
  let license;
  let sourceUrl;
  if (target.kind === 'esbuild-iife') {
    source = await readLocal(target.entry);
    license = await readLocal(target.license);
    sourceUrl = target.entry;
  } else {
    const entry = target.entry ?? 'spine-ts/build/spine-webgl.js';
    const script = await fetchText(raw(target.ref, entry), `spine-webgl-${target.id}.js`);
    const licenseFile = await fetchText(raw(target.ref, 'LICENSE'), `LICENSE-${target.id}.txt`);
    source = script.text;
    license = licenseFile.text;
    sourceUrl = script.from;
    if (target.patch) source = target.patch(source);
  }

  const banner = header(sourceUrl, license);
  const code = target.kind === 'esbuild-iife' ? wrapIife(source, banner) : wrapGlobalScript(source, banner);
  const base = `spine-${target.id}`;

  await mkdir(outDir, { recursive: true });
  await writeFile(join(outDir, `${base}.js`), code);
  await writeFile(join(outDir, `${base}.d.ts`), declaration);
  await writeFile(join(outDir, `LICENSE-${target.id}.txt`), license);
  console.log(`write ${base}.js  ${(code.length / 1024).toFixed(0)} KB`);
}

if (process.argv.includes('--list')) {
  for (const target of targets) console.log(`${target.id}\t${target.enabled ? 'on' : 'off'}\t${target.kind}`);
} else {
  const enableLegacy = process.argv.includes('--enable-legacy');
  for (const target of targets) await generate(target, enableLegacy);
}
