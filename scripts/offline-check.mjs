// 离线验收：pnpm build 产物 + vite preview，联网预热（SW 安装 + 运行时包 CacheFirst），
// 然后直接杀掉 preview 进程模拟断网 → 刷新应仅凭缓存完成加载、拖入骨架并导出 VP9。
// 用法: pnpm build && node scripts/offline-check.mjs
// 浏览器路径可用 --browser=<路径> 或环境变量 SSV_BROWSER 覆盖。
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawn } from 'node:child_process';
import { openBrowser, killTree, sleep } from './lib/browser.mjs';

const port = Number(process.env.SSV_PORT ?? 4179);
const cdpPort = Number(process.env.SSV_CDP ?? 9348);
const base = `http://localhost:${port}/`;
const dlDir = path.join(os.tmpdir(), 'ssv-offline-dl-' + cdpPort);
fs.rmSync(dlDir, { recursive: true, force: true });
fs.mkdirSync(dlDir, { recursive: true });

const distIndex = path.join('dist', 'index.html');
if (!fs.existsSync(distIndex)) {
  console.log('ERROR: 缺少 dist/，请先 pnpm build');
  process.exit(2);
}

// 测试素材从 Node 磁盘读取（preview 不发布 spine-testfiles），与用户本地文件等价
const FILES = ['goblins-mesh.json', 'goblins-mesh.atlas', 'goblins-mesh.png'].map((name) => ({
  name,
  b64: fs.readFileSync(path.join('spine-testfiles', 'goblins31', name)).toString('base64'),
}));

const viteBin = path.join('node_modules', 'vite', 'bin', 'vite.js');
if (!fs.existsSync(viteBin)) {
  console.log('ERROR: 缺少 ' + viteBin + '，请先 pnpm install');
  process.exit(2);
}
// 直接跑本地 vite 产物，避免依赖 PATH 上的 pnpm/npx
const server = spawn(process.execPath, [viteBin, 'preview', '--port', String(port), '--strictPort'], {
  stdio: 'ignore',
});
const killServer = () => killTree(server);

let swResponses = 0;
const failedAssetRequests = [];
const problems = [];
const { send, evaluate, close } = await openBrowser({
  port: cdpPort,
  profile: 'offline-' + cdpPort,
  onMessage: (message) => {
    if (message.method === 'Network.responseReceived' && message.params.response?.fromServiceWorker) swResponses += 1;
    if (message.method === 'Network.loadingFailed') {
      const url = message.params.request?.url ?? '';
      if (/\/(assets|index\.html|sw\.js)/.test(url)) failedAssetRequests.push(url + ' :: ' + message.params.errorText);
    }
    if (message.method === 'Runtime.exceptionThrown') {
      problems.push(message.params?.exceptionDetails?.exception?.description ?? 'exception');
    }
  },
});

await send('Browser.setDownloadBehavior', { behavior: 'allow', downloadPath: dlDir });

// —— 联网阶段：加载页面、等 SW 接管、拖入骨架让运行时包进缓存 ——
await send('Page.navigate', { url: base });
const outcome = { steps: [] };
function fail(name, value) {
  outcome.steps.push({ name, value });
  outcome.status = 'FAIL:' + name;
  outcome.swResponses = swResponses;
  if (problems.length) outcome.problems = problems.slice(0, 5);
  console.log(JSON.stringify(outcome, null, 2));
  killServer();
  close();
  process.exit(1);
}
function ok(name, value, passed) {
  outcome.steps.push({ name, value });
  if (!passed) fail(name, value);
}

await send('Page.enable');
await send('Runtime.enable');
await send('Network.enable');
await send('Browser.setDownloadBehavior', { behavior: 'allow', downloadPath: dlDir });

// —— 联网阶段：加载页面、等 SW 接管、拖入骨架让运行时包进缓存 ——
await send('Page.navigate', { url: base });

async function waitPaint(label) {
  const deadline = Date.now() + 60000;
  while (Date.now() < deadline) {
    const painted = await evaluate("document.querySelector('[role=button]') ? 'ready' : ''");
    if (painted === 'ready') return;
    await sleep(400);
  }
  fail(label, 'timeout');
}
await waitPaint('online-paint');

// 首轮访问 SW 刚安装，可能未接管本页；reload 一次直到 controller 就位
async function controllerState() {
  const deadline = Date.now() + 20000;
  while (Date.now() < deadline) {
    const state = await evaluate('navigator.serviceWorker.controller ? "controlled" : ""');
    if (state === 'controlled') return state;
    await sleep(200);
  }
  return 'uncontrolled';
}
let controlled = await controllerState();
if (controlled !== 'controlled') {
  await send('Page.navigate', { url: base });
  await waitPaint('online-paint-2');
  controlled = await controllerState();
}
ok('sw-controlled', controlled, controlled === 'controlled');
// 等 precache 落库完成
await sleep(1500);

const dropScript = [
  '(async () => {',
  '  const files = window.__SSV_FILES.map((f) => {',
  '    const bytes = Uint8Array.from(atob(f.b64), (c) => c.charCodeAt(0));',
  '    return new File([bytes], f.name);',
  '  });',
  "  const dropzone = document.querySelector('[role=button]');",
  '  const dt = new DataTransfer();',
  '  for (const f of files) dt.items.add(f);',
  "  const ev = new DragEvent('drop', { bubbles: true, cancelable: true });",
  "  Object.defineProperty(ev, 'dataTransfer', { value: dt });",
  '  dropzone.dispatchEvent(ev);',
  '  const deadline = Date.now() + 120000;',
  '  while (Date.now() < deadline) {',
  '    await new Promise((r) => setTimeout(r, 250));',
  "    if (document.querySelector('[data-ssv=export-start]:not([disabled])')) return 'ready';",
  "    const bar = document.querySelector('[data-ssv=error]');",
  "    if (bar) return 'error:' + bar.textContent.trim().slice(0, 160);",
  '  }',
  "  return 'timeout:' + document.body.innerText.slice(0, 200);",
  '})()',
].join('\n');

await send('Runtime.evaluate', { expression: 'window.__SSV_FILES = ' + JSON.stringify(FILES) });
const onlineLoad = await evaluate(dropScript);
ok('online-load-ready', onlineLoad, onlineLoad === 'ready');
// 让 CacheFirst 的响应完成写缓存
await sleep(2000);

// —— 断网阶段：杀掉 preview 进程，任何未命中缓存的请求都会连接失败 ——
killServer();
const failedBefore = failedAssetRequests.length;
await send('Page.navigate', { url: base });
await waitPaint('offline-paint');

await send('Runtime.evaluate', { expression: 'window.__SSV_FILES = ' + JSON.stringify(FILES) });
const offlineLoad = await evaluate(dropScript);
ok('offline-load-ready', offlineLoad, offlineLoad === 'ready');

const offlineExport = await evaluate([
  '(async () => {',
  "  document.querySelector('[data-ssv=export-start]').click();",
  '  const deadline = Date.now() + 240000;',
  '  while (Date.now() < deadline) {',
  '    await new Promise((r) => setTimeout(r, 250));',
  "    const phase = document.querySelector('[data-ssv=export-phase]')?.textContent ?? '';",
  "    if (/完成|Done/.test(phase)) return 'done';",
  '  }',
  "  return 'timeout:' + (document.querySelector('[data-ssv=export-phase]')?.textContent ?? '');",
  '})()',
].join('\n'));
ok('offline-export-done', offlineExport, offlineExport === 'done');

let downloads = [];
for (let i = 0; i < 60 && downloads.length === 0; i++) {
  await sleep(500);
  downloads = fs.readdirSync(dlDir);
}
ok('offline-download', downloads, downloads.length === 1 && downloads[0] === 'goblins-mesh.webm');
const size = fs.statSync(path.join(dlDir, downloads[0])).size;
ok('offline-download-size', size, size > 10_000);
outcome.downloaded = { name: downloads[0], size };

outcome.swResponsesAfterKill = swResponses;
outcome.failedAssetRequests = failedAssetRequests.slice(0, 5);
ok('no-asset-network-failure', failedAssetRequests.length - failedBefore, failedAssetRequests.length === failedBefore);

outcome.status = 'PASS';
if (problems.length) outcome.problems = problems.slice(0, 5);
console.log(JSON.stringify(outcome, null, 2));
close();
process.exit(0);
