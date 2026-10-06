// 导出 UI 端到端：加载 goblins → 点开始导出 → 中途取消（回到空闲、无下载）→ 再次导出到完成并校验下载文件。
// 用法: node scripts/export-check.mjs <baseUrl>
// 浏览器路径可用 --browser=<路径> 或环境变量 SSV_BROWSER 覆盖。
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { openBrowser, sleep } from './lib/browser.mjs';

const base = process.argv[2] ?? 'http://localhost:5173/';
const port = Number(process.env.SSV_PORT ?? 9337);
const dlDir = path.join(os.tmpdir(), 'ssv-export-dl-' + port);
fs.rmSync(dlDir, { recursive: true, force: true });
fs.mkdirSync(dlDir, { recursive: true });

const problems = [];
const { send, evaluate, close } = await openBrowser({
  port,
  profile: 'export-' + port,
  onMessage: (message) => {
    if (message.method === 'Runtime.exceptionThrown') {
      problems.push(message.params?.exceptionDetails?.exception?.description ?? 'exception');
    }
  },
});

await send('Browser.setDownloadBehavior', { behavior: 'allow', downloadPath: dlDir });
await send('Page.navigate', { url: base });

// 等首屏渲染出 dropzone，避免在 React 挂载前查询
const paintDeadline = Date.now() + 60000;
let painted = '';
while (Date.now() < paintDeadline) {
  painted = await evaluate("document.querySelector('[role=button]') ? 'ready' : ''");
  if (painted === 'ready') break;
  await sleep(400);
}
if (painted !== 'ready') {
  console.log(JSON.stringify({ status: 'FAIL:first-paint' }));
  close();
  process.exit(1);
}

const LOAD_SCRIPT = [
  '(async () => {',
  "  const names = ['goblins-mesh.json', 'goblins-mesh.atlas', 'goblins-mesh.png'];",
  '  const files = [];',
  '  for (const name of names) {',
  "    const r = await fetch('/spine-testfiles/goblins31/' + name);",
  "    if (!r.ok) throw new Error('missing ' + name);",
  '    files.push(new File([await r.arrayBuffer()], name));',
  '  }',
  "  const dropzone = document.querySelector('[role=button]');",
  '  const dt = new DataTransfer();',
  '  for (const f of files) dt.items.add(f);',
  "  const ev = new DragEvent('drop', { bubbles: true, cancelable: true });",
  "  Object.defineProperty(ev, 'dataTransfer', { value: dt });",
  '  dropzone.dispatchEvent(ev);',
  '  const deadline = Date.now() + 60000;',
  '  while (Date.now() < deadline) {',
  '    await new Promise((r) => setTimeout(r, 250));',
  "    if (document.querySelector('[data-ssv=export-start]:not([disabled])')) return 'ready';",
  '  }',
  "  return 'timeout:' + document.body.innerText.slice(0, 200);",
  '})()',
].join('\n');

const outcome = { steps: [] };

async function step(name, value, ok) {
  outcome.steps.push({ name, value });
  if (!ok) {
    outcome.status = 'FAIL:' + name;
    console.log(JSON.stringify(outcome, null, 2));
    close();
    process.exit(1);
  }
}

const status = await evaluate(LOAD_SCRIPT);
await step('load', status, status === 'ready');

// 第一轮：开始导出后立刻取消，进度条消失且无下载文件
const phaseText = "document.querySelector('[data-ssv=export-phase]')?.textContent ?? ''";
const cancelRun = await evaluate([
  '(async () => {',
  "  document.querySelector('[data-ssv=export-start]').click();",
  '  const deadline = Date.now() + 30000;',
  '  while (Date.now() < deadline) {',
  '    await new Promise((r) => setTimeout(r, 100));',
  "    const phase = document.querySelector('[data-ssv=export-phase]')?.textContent ?? '';",
  "    if (/渲染|编码|Encoding/i.test(phase)) {",
  "      const btn = document.querySelector('[data-ssv=export-cancel]');",
  '      if (btn) { btn.click(); return phase; }',
  '    }',
  '  }',
  "  return 'no-progress:' + " + JSON.stringify(phaseText) + ';',
  '})()',
].join('\n'));
await step('cancel-observed-phase', cancelRun, typeof cancelRun === 'string' && !cancelRun.startsWith('no-progress'));
await sleep(1000);
const phaseAfterCancel = await evaluate(phaseText);
await step('cancel-returns-idle', phaseAfterCancel, phaseAfterCancel === '');
let downloads = fs.readdirSync(dlDir);
await step('cancel-no-download', downloads, downloads.length === 0);

// 第二轮：完整导出，等待完成并出现下载文件；顺带断言导出期间预览实时跟随当前帧、结束后恢复播放
const secondRun = await evaluate([
  '(async () => {',
  "  const sample = () => {",
  "    const canvas = document.querySelector('canvas');",
  "    if (!canvas) return -1;",
  "    const ctx = canvas.getContext('2d', { willReadFrequently: true });",
  '    const d = ctx.getImageData(0, 0, canvas.width, canvas.height).data;',
  '    let sum = 0;',
  '    for (let i = 3; i < d.length; i += 40) sum += d[i];',
  '    return sum;',
  '  };',
  "  document.querySelector('[data-ssv=export-start]').click();",
  '  const deadline = Date.now() + 240000;',
  '  let sawEncoding = false; let sawPackaging = false;',
  '  const during = [];',
  '  while (Date.now() < deadline) {',
  '    await new Promise((r) => setTimeout(r, 200));',
  "    const phase = document.querySelector('[data-ssv=export-phase]')?.textContent ?? '';",
  '    if (/渲染|Rendering/.test(phase)) {',
  '      sawEncoding = true;',
  '      during.push(sample());',
  '    }',
  "    if (/打包|Packaging/.test(phase)) sawPackaging = true;",
  "    if (/完成|Done/.test(phase)) return JSON.stringify({ sawEncoding, sawPackaging, phase, during });",
  '  }',
  "  return 'timeout:' + " + JSON.stringify(phaseText) + ';',
  '})()',
].join('\n'));
const second = (() => { try { return JSON.parse(secondRun); } catch { return null; } })();
await step('export-completes', secondRun, second?.phase && second.sawEncoding);

// §9：导出期间预览实时显示正在渲染的帧（采样值随导出帧推进而变化）
const distinct = [...new Set(second.during ?? [])].filter((v) => v > 0);
await step('preview-live-sync', { samples: (second.during ?? []).length, distinct: distinct.length }, distinct.length >= 3);

// 导出结束后恢复正常播放：预览仍在变化
const resumedSamples = [];
for (let i = 0; i < 6; i++) {
  resumedSamples.push(
    await evaluate(
      "(() => { const c = document.querySelector('canvas'); const x = c.getContext('2d', { willReadFrequently: true }); const d = x.getImageData(0, 0, c.width, c.height).data; let s = 0; for (let i = 3; i < d.length; i += 40) s += d[i]; return s; })()",
    ),
  );
  await sleep(150);
}
const resumedDistinct = [...new Set(resumedSamples)].filter((v) => v > 0);
await step('playback-resumed', resumedDistinct.length, resumedDistinct.length >= 2);

for (let i = 0; i < 60 && downloads.length === 0; i++) {
  await sleep(500);
  downloads = fs.readdirSync(dlDir);
}
await step('download-file', downloads, downloads.length === 1);

const file = path.join(dlDir, downloads[0]);
const size = fs.statSync(file).size;
await step('download-size', size, size > 10_000 && downloads[0] === 'goblins-mesh.webm');
outcome.downloaded = { name: downloads[0], size };

// 第三轮：UI 切参数（格式→APNG、画布→标清640）再导出，参数必须生效
const pick = (selector, text) =>
  [
    '(async () => {',
    `  document.querySelector('${selector}').click();`,
    '  const deadline = Date.now() + 5000;',
    '  while (Date.now() < deadline) {',
    '    await new Promise((r) => setTimeout(r, 100));',
    "    const opt = [...document.querySelectorAll('[role=option]')].find((o) => o.textContent.includes(" + JSON.stringify(text) + '));',
    '    if (opt) { opt.click(); return "picked:" + opt.textContent.trim(); }',
    '  }',
    '  return "option-missing";',
    '})()',
  ].join('\n');
const pickedFormat = await evaluate(pick('[data-ssv=export-format]', 'APNG'));
await step('switch-format', pickedFormat, pickedFormat.startsWith('picked:'));
const pickedCanvas = await evaluate(pick('[data-ssv=export-canvas]', '标清'));
await step('switch-canvas', pickedCanvas, pickedCanvas.startsWith('picked:'));

const thirdRun = await evaluate([
  '(async () => {',
  "  document.querySelector('[data-ssv=export-start]').click();",
  '  const deadline = Date.now() + 240000;',
  "  if (document.querySelector('[data-ssv=export-bitrate]')) return 'bitrate-still-visible';",
  '  while (Date.now() < deadline) {',
  '    await new Promise((r) => setTimeout(r, 200));',
  "    const phase = document.querySelector('[data-ssv=export-phase]')?.textContent ?? '';",
  "    if (/完成|Done/.test(phase)) return 'done';",
  '  }',
  "  return 'timeout:' + " + JSON.stringify(phaseText) + ';',
  '})()',
].join('\n'));
await step('apng-export-completes', thirdRun, thirdRun === 'done');

let zipDownloads = [];
for (let i = 0; i < 60 && zipDownloads.length === 0; i++) {
  await sleep(500);
  zipDownloads = fs.readdirSync(dlDir).filter((n) => n.endsWith('.zip'));
}
await step('zip-download', zipDownloads, zipDownloads.length === 1 && zipDownloads[0] === 'goblins-mesh-frames.zip');
outcome.zip = { name: zipDownloads[0], size: fs.statSync(path.join(dlDir, zipDownloads[0])).size };

outcome.status = 'PASS';
if (problems.length) outcome.problems = problems.slice(0, 5);
console.log(JSON.stringify(outcome, null, 2));
close();
process.exit(0);
