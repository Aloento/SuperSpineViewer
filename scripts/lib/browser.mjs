// 测试脚本共用：定位本机 Chromium 系浏览器、经 CDP 驱动无头实例。
// 浏览器路径优先级：--browser=<path> > SSV_BROWSER > SSV_EDGE > 常见安装路径 > PATH 上的可执行名。

import { spawn } from 'node:child_process';
import os from 'node:os';
import fs from 'node:fs';
import path from 'node:path';

const NAMES = ['msedge', 'chrome', 'chromium', 'chromium-browser'];

function candidates() {
  if (process.platform === 'win32') {
    const roots = [process.env['PROGRAMFILES'], process.env['PROGRAMFILES(X86)'], process.env['LOCALAPPDATA']]
      .filter(Boolean);
    const dirs = ['Microsoft/Edge/Application/msedge.exe', 'Google/Chrome/Application/chrome.exe'];
    return roots.flatMap((root) => dirs.map((rel) => path.join(root, rel)));
  }
  if (process.platform === 'darwin') {
    return [
      '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
      '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
      '/Applications/Chromium.app/Contents/MacOS/Chromium',
    ];
  }
  return [
    '/opt/microsoft/msedge/msedge',
    '/usr/bin/microsoft-edge',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
    '/usr/bin/chromium-browser',
  ];
}

function fromPath() {
  const dirs = (process.env.PATH ?? '').split(path.delimiter).filter(Boolean);
  const exts = process.platform === 'win32' ? ['.exe', '.cmd', ''] : [''];
  for (const name of NAMES) {
    for (const dir of dirs) {
      for (const ext of exts) {
        const file = path.join(dir, name + ext);
        if (fs.existsSync(file)) return file;
      }
    }
  }
  return null;
}

export function findBrowser() {
  const flag = process.argv.find((arg) => arg.startsWith('--browser='));
  const explicit = flag?.slice('--browser='.length) || process.env.SSV_BROWSER || process.env.SSV_EDGE;
  if (explicit) {
    if (fs.existsSync(explicit)) return explicit;
    const fromEnv = fromPath();
    if (fromEnv) return fromEnv;
    throw new Error(
      '找不到浏览器：' + explicit + '\n' +
        '请用 --browser=<路径> 或环境变量 SSV_BROWSER 指向本机 Edge / Chrome / Chromium 可执行文件。',
    );
  }
  return candidates().find((file) => fs.existsSync(file)) ?? fromPath();
}

export const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export function killTree(child) {
  if (!child || child.killed) return;
  if (process.platform === 'win32') {
    spawn('taskkill', ['/pid', String(child.pid), '/T', '/F'], { stdio: 'ignore' });
  } else {
    child.kill('SIGKILL');
  }
}

// 每次用全新 profile：复用会带进上一次构建的旧 Service Worker
function freshProfile(tag) {
  const dir = path.join(os.tmpdir(), 'ssv-cdp-' + tag);
  fs.rmSync(dir, { recursive: true, force: true });
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

export async function openBrowser(options = {}) {
  const { port = 9333, width = 1420, height = 900, scale = 1, profile = String(port), onMessage } = options;
  const browser = findBrowser();
  if (!browser) {
    throw new Error('找不到本机浏览器：请安装 Edge / Chrome / Chromium，或用 --browser=<路径> / SSV_BROWSER 指定。');
  }
  const child = spawn(
    browser,
    [
      '--headless=new',
      '--disable-gpu',
      '--no-sandbox',
      '--disable-extensions',
      '--disable-background-timer-throttling',
      '--disable-renderer-backgrounding',
      '--remote-debugging-port=' + port,
      '--user-data-dir=' + freshProfile(profile),
      'about:blank',
    ],
    { stdio: 'ignore' },
  );

  let targets = [];
  for (let i = 0; i < 90; i++) {
    try {
      const response = await fetch('http://127.0.0.1:' + port + '/json/list');
      if (response.ok) {
        targets = await response.json();
        if (targets.some((t) => t.type === 'page')) break;
      }
    } catch {}
    await sleep(400);
  }
  const page = targets.find((t) => t.type === 'page');
  if (!page) {
    killTree(child);
    throw new Error('找不到无头浏览器的 page target（' + browser + '）');
  }

  const ws = new WebSocket(page.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    ws.onopen = resolve;
    ws.onerror = () => reject(new Error('CDP 连接失败'));
  });

  let nextId = 1;
  const waiting = new Map();
  ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    if (message.id && waiting.has(message.id)) {
      const entry = waiting.get(message.id);
      waiting.delete(message.id);
      if (message.error) entry.reject(new Error(JSON.stringify(message.error)));
      else entry.resolve(message.result);
      return;
    }
    onMessage?.(message);
  };

  const send = (method, params = {}) =>
    new Promise((resolve, reject) => {
      const id = nextId++;
      waiting.set(id, { resolve, reject });
      ws.send(JSON.stringify({ id, method, params }));
    });
  const evaluate = async (expression) => {
    const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
    if (result?.exceptionDetails) {
      throw new Error(
        result.exceptionDetails.exception?.description ?? JSON.stringify(result.exceptionDetails).slice(0, 400),
      );
    }
    return result?.result?.value;
  };
  const screenshot = async (file) => {
    const shot = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false });
    fs.writeFileSync(file, Buffer.from(shot.data, 'base64'));
    return file;
  };
  const viewport = (w, h, deviceScaleFactor = scale) =>
    send('Emulation.setDeviceMetricsOverride', { width: w, height: h, deviceScaleFactor, mobile: false });

  await send('Page.enable');
  await send('Runtime.enable');
  await viewport(width, height);

  return {
    browser,
    child,
    send,
    evaluate,
    screenshot,
    viewport,
    close: () => {
      try { ws.close(); } catch {}
      killTree(child);
    },
  };
}
