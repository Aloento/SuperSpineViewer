// 端到端验证：加载首页，断言首屏不请求任何 spine 运行时 chunk，
// 再用真实 File 触发拖拽，按用例断言 UI 进入播放态（并确认画布真的画出了像素）或给出预期错误文案。
// 用法: node scripts/app-check.mjs <baseUrl> <dir> <skeletonFile> <atlasFile> <playing|error> [预期文案]
// 例: node scripts/app-check.mjs http://localhost:5173/ spineboy38 spineboy-pro.skel spineboy-pma.atlas playing

const { spawn } = await import('node:child_process');
const path = await import('node:path');
const os = await import('node:os');

const edgePath =
  process.env.SSV_EDGE ?? 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const base = process.argv[2] ?? 'http://localhost:5173/';
const args = {
  dir: process.argv[3] ?? 'spineboy38',
  skeletonFile: process.argv[4] ?? 'spineboy-pro.skel',
  atlasFile: process.argv[5] ?? 'spineboy-pma.atlas',
  expect: process.argv[6] ?? 'playing',
  contains: process.argv[7] ?? '',
  waitMs: Number(process.env.SSV_WAIT ?? 45000),
};
// SSV_LANG=en 时切到英文文案，验证 UI 没有硬编码字符串
const language = process.env.SSV_LANG ?? '';
const port = Number(process.env.SSV_PORT ?? 9334);
const timeoutMs = Number(process.env.SSV_TIMEOUT ?? 180000);
// 生产构建下才有独立 pack chunk，dev 下按需加载体现为动态模块请求
const PACK_PATTERN = /spine-3\.[4-8]|spine-4\.0|spine-webgl-41|spine-canvaskit|dist-[A-Za-z0-9_-]+\.js|canvaskit|\.wasm/i;

const DROP_SCRIPT = [
  '(async () => {',
  '  const { dir, skeletonFile, atlasFile, expect, contains, waitMs } = window.__SSV_ARGS;',
  '  const names = [skeletonFile, atlasFile];',
  "  const atlasText = await (await fetch('/spine-testfiles/' + dir + '/' + atlasFile)).text();",
  '  for (const line of atlasText.split(String.fromCharCode(10))) {',
  '    const name = line.trim();',
  '    if (/\\.(png|jpe?g|webp)$/i.test(name)) names.push(name);',
  '  }',
  '  const files = [];',
  '  for (const name of names) {',
  "    const response = await fetch('/spine-testfiles/' + dir + '/' + name);",
  "    if (!response.ok) throw new Error('missing ' + name);",
  '    files.push(new File([await response.arrayBuffer()], name));',
  '  }',
  "  const dropzone = document.querySelector('[role=button]');",
  '  const dataTransfer = new DataTransfer();',
  '  for (const file of files) dataTransfer.items.add(file);',
  "  const event = new DragEvent('drop', { bubbles: true, cancelable: true });",
  "  Object.defineProperty(event, 'dataTransfer', { value: dataTransfer });",
  '  dropzone.dispatchEvent(event);',
  '',
  '  const opaqueOnCanvas = () => {',
  "    const canvas = document.querySelector('canvas');",
  "    if (!canvas) return -1;",
  "    const context = canvas.getContext('2d', { willReadFrequently: true });",
  '    const data = context.getImageData(0, 0, canvas.width, canvas.height).data;',
  '    let count = 0;',
  '    for (let i = 3; i < data.length; i += 4) if (data[i] > 8) count++;',
  '    return count;',
  '  };',
  '',
  '  const deadline = Date.now() + waitMs;',
  '  let warning = null;',
  '  while (Date.now() < deadline) {',
  '    await new Promise((resolve) => setTimeout(resolve, 250));',
  "    const text = document.body.innerText.replace(/\\s+/g, ' ');",
  "    const bar = document.querySelector('[data-ssv]');",
  "    const intent = bar ? bar.getAttribute('data-ssv') : null;",
  "    if (bar && intent === 'error') {",
  "      const message = bar.textContent.replace(/\\s+/g, ' ').trim();",
  '      return {',
  "        status: expect === 'error' ? (message.includes(contains) ? 'expected-error' : 'wrong-error') : 'unexpected-error',",
  '        message,',
  '      };',
  '    }',
  "    if (bar && intent === 'warning') warning = bar.textContent.replace(/\\s+/g, ' ').trim();",
  '    if (/播放中/.test(text) || /Playing/.test(text)) {',
  '      const opaque = opaqueOnCanvas();',
  "      if (expect === 'playing') {",
  "        if (opaque > 500) return { status: 'playing', opaque, warning, text: text.slice(0, 340) };",
  '      } else {',
  "        return { status: 'unexpected-playing', opaque, warning, text: text.slice(0, 340) };",
  '      }',
  '    }',
  '  }',
  "  return { status: 'timeout', warning, text: document.body.innerText.replace(/\\s+/g, ' ').slice(0, 340) };",
  '})()',
].join('\n');

const child = spawn(
  edgePath,
  [
    '--headless=new',
    '--disable-gpu',
    '--no-sandbox',
    '--disable-extensions',
    '--remote-debugging-port=' + port,
    '--user-data-dir=' + path.join(os.tmpdir(), 'ssv-edge-profile-' + port),
    'about:blank',
  ],
  { stdio: 'ignore' },
);

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

let targets = [];
for (let i = 0; i < 90; i++) {
  try {
    const response = await fetch('http://127.0.0.1:' + port + '/json/list');
    if (response.ok) {
      targets = await response.json();
      if (targets.some((t) => t.type === 'page')) break;
    }
  } catch {}
  await sleep(500);
}

const page = targets.find((t) => t.type === 'page');
if (!page) {
  console.log('ERROR: 找不到 page target');
  child.kill();
  process.exit(2);
}

const ws = new WebSocket(page.webSocketDebuggerUrl);
await new Promise((resolve, reject) => {
  ws.onopen = resolve;
  ws.onerror = () => reject(new Error('CDP 连接失败'));
});

let nextId = 1;
const waiting = new Map();
const requests = [];
const problems = [];

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  if (message.id && waiting.has(message.id)) {
    const entry = waiting.get(message.id);
    waiting.delete(message.id);
    if (message.error) entry.reject(new Error(JSON.stringify(message.error)));
    else entry.resolve(message.result);
    return;
  }
  if (message.method === 'Network.requestWillBeSent') requests.push(message.params.request.url);
  if (message.method === 'Runtime.exceptionThrown') {
    problems.push(message.params?.exceptionDetails?.exception?.description ?? 'exception');
  }
};

function send(method, params = {}) {
  const id = nextId++;
  return new Promise((resolve, reject) => {
    waiting.set(id, { resolve, reject });
    ws.send(JSON.stringify({ id, method, params }));
  });
}

async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
  if (result?.exceptionDetails) return { error: result.exceptionDetails.exception?.description ?? result.exceptionDetails.text };
  return result?.result?.value;
}

if (language) {
  await send('Page.enable');
  await send('Page.addScriptToEvaluateOnNewDocument', {
    source: 'localStorage.setItem("ssv.language", ' + JSON.stringify(language) + ');',
  });
}
await send('Page.enable');
await send('Runtime.enable');
await send('Network.enable');
await send('Page.navigate', { url: base });

const started = Date.now();
while (Date.now() - started < timeoutMs) {
  const ready = await evaluate(
    "document.querySelector('[role=button]') && /Spine/.test(document.body.innerText) ? 'ready' : ''",
  );
  if (ready === 'ready') break;
  await sleep(400);
}

const firstPaint = [...new Set(requests)];
const firstPaintPacks = firstPaint.filter((url) => PACK_PATTERN.test(url));

await send('Runtime.evaluate', { expression: 'window.__SSV_ARGS = ' + JSON.stringify(args) });
const outcome = await evaluate(DROP_SCRIPT);

const packsAfterLoad = [...new Set(requests.filter((url) => PACK_PATTERN.test(url)))].map((url) =>
  url.replace(base, '').split('?')[0],
);

const verdict =
  args.expect === 'playing'
    ? outcome?.status === 'playing' && outcome.opaque > 500
    : outcome?.status === 'expected-error';
const lazyOk = firstPaintPacks.length === 0;

console.log(
  JSON.stringify(
    {
      case: args.dir + '/' + args.skeletonFile,
      expect: args.expect,
      language: language || 'auto',
      firstPaintRequests: firstPaint.length,
      firstPaintPacks: firstPaintPacks.map((url) => url.split('/').pop()),
      lazyOk,
      outcome,
      runtimePacksRequestedAfterLoad: packsAfterLoad,
    },
    null,
    2,
  ),
);
if (problems.length) console.log('页面异常:\n' + problems.slice(0, 5).join('\n'));

ws.close();
child.kill();
process.exit(verdict && lazyOk ? 0 : 1);
