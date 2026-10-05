#!/usr/bin/env node
// 拉取旧版 Spine 运行时并包成 ESM pack，产物提交进 src/spine/runtimes/generated/
import { mkdir, readFile, writeFile, access } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const outDir = join(root, 'src/spine/runtimes/generated');
const cacheDir = join(root, 'node_modules/.cache/fetch-runtimes');

const raw = (ref, path) => `https://raw.githubusercontent.com/EsotericSoftware/spine-runtimes/${ref}/${path}`;

const targets = [
  // 3.4–3.7 属于 M2b：先固定来源与生成方式，需要时加 --enable-legacy 再生成
  { id: '3.4', enabled: false, kind: 'global-script', ref: '3.4' },
  { id: '3.5', enabled: false, kind: 'global-script', ref: '3.5' },
  { id: '3.6', enabled: false, kind: 'global-script', ref: '3.6' },
  { id: '3.7', enabled: false, kind: 'global-script', ref: '3.7' },
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
  if (await exists(cached)) return { text: await readFile(cached, 'utf8'), from: `cache:${cacheName}` };
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
