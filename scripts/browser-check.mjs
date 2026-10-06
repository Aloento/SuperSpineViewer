// 通过 CDP 驱动无头浏览器（Edge / Chrome / Chromium）跑 scripts/browser-check.html，取回渲染级验证结果。
// 用法: node scripts/browser-check.mjs [页面地址]；浏览器路径可用 --browser=<路径> 或环境变量 SSV_BROWSER 覆盖。

import { openBrowser, sleep } from './lib/browser.mjs';

const targetUrl = process.argv[2] ?? 'http://localhost:5173/scripts/browser-check.html';
const timeoutMs = Number(process.env.SSV_TIMEOUT ?? 300000);

const problems = [];
const { send, evaluate, close } = await openBrowser({
  port: 9333,
  onMessage: (message) => {
    if (message.method === 'Runtime.exceptionThrown') {
      problems.push(message.params?.exceptionDetails?.exception?.description ?? 'exception');
    }
    if (message.method === 'Runtime.consoleAPICalled' && message.params?.type === 'error') {
      problems.push((message.params.args ?? []).map((a) => a.value ?? a.description ?? '').join(' '));
    }
  },
});

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

close();
process.exit(exitCode);
