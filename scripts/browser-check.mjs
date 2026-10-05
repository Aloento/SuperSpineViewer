// 通过 CDP 驱动无头 Edge 跑 scripts/browser-check.html，取回 M2a 渲染级验证结果
// 用法: node scripts/browser-check.mjs [url]；Edge 路径可用 SSV_EDGE 覆盖

const { spawn } = await import('node:child_process');
const path = await import('node:path');
const os = await import('node:os');

const edgePath =
  process.env.SSV_EDGE ?? 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const targetUrl = process.argv[2] ?? 'http://localhost:5173/scripts/browser-check.html';
const port = 9333;
const timeoutMs = Number(process.env.SSV_TIMEOUT ?? 300000);

const child = spawn(
  edgePath,
  [
    '--headless=new',
    '--disable-gpu',
    '--no-sandbox',
    '--disable-extensions',
    '--disable-background-timer-throttling',
    '--disable-renderer-backgrounding',
    '--remote-debugging-port=' + port,
    '--user-data-dir=' + path.join(os.tmpdir(), 'ssv-edge-profile'),
    'about:blank',
  ],
  { stdio: 'ignore' },
);

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

let targets = [];
for (let attempt = 0; attempt < 90; attempt++) {
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
  console.log('ERROR: 找不到无头 Edge 的 page target');
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
  if (message.method === 'Runtime.exceptionThrown') {
    problems.push(message.params?.exceptionDetails?.exception?.description ?? 'exception');
  }
  if (message.method === 'Runtime.consoleAPICalled' && message.params?.type === 'error') {
    problems.push((message.params.args ?? []).map((a) => a.value ?? a.description ?? '').join(' '));
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
  const result = await send('Runtime.evaluate', { expression, returnByValue: true });
  return result?.result?.value;
}

await send('Page.enable');
await send('Runtime.enable');
await send('Page.navigate', { url: targetUrl });

const started = Date.now();
let text = '';
while (Date.now() - started < timeoutMs) {
  text = (await evaluate("document.getElementById('out')?.textContent ?? ''")) ?? '';
  if (text.startsWith('M2A_RESULT ')) break;
  await sleep(1000);
}

let exitCode = 1;
if (!text.startsWith('M2A_RESULT ')) {
  console.log('TIMEOUT: 页面在 ' + timeoutMs + 'ms 内没有输出结果');
} else {
  const parsed = JSON.parse(text.slice('M2A_RESULT '.length));
  for (const item of parsed.results ?? []) console.log(JSON.stringify(item));
  console.log('FAILED = ' + parsed.failed);
  if (parsed.fatal) console.log('FATAL = ' + parsed.fatal);
  exitCode = parsed.failed === 0 && !parsed.fatal ? 0 : 1;
}

if (problems.length) console.log('页面异常:\n' + problems.slice(0, 8).join('\n'));

ws.close();
child.kill();
process.exit(exitCode);
