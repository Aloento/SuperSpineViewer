// 播放控制端到端验证：加载骨架后逐项操作右侧控制（播放/暂停、进度条、动画、皮肤、循环、偏移/缩放），
// 断言时间文本推进/静止、画布像素变化、无错误条。
// 用法: node scripts/control-check.mjs <baseUrl> <dir> <skeletonFile> <atlasFile> [--expect-skin <name>]
// 浏览器路径可用 --browser=<路径> 或环境变量 SSV_BROWSER 覆盖。

import fs from 'node:fs';
import path from 'node:path';
import { openBrowser, sleep } from './lib/browser.mjs';

const base = process.argv[2] ?? 'http://localhost:5173/';
const dir = process.argv[3] ?? 'spineboy38';
const skeletonFile = process.argv[4] ?? 'spineboy-pro.skel';
const atlasFile = process.argv[5] ?? 'spineboy-pma.atlas';
const expectSkinIdx = process.argv.indexOf('--expect-skin');
const expectSkin = expectSkinIdx >= 0 ? process.argv[expectSkinIdx + 1] : '';
const wantIdx = process.argv.indexOf('--want');
const wantAnim = wantIdx >= 0 ? process.argv[wantIdx + 1] : 'death';

const problems = [];
const { send, evaluate, close } = await openBrowser({
  port: Number(process.env.SSV_PORT ?? 9335),
  profile: 'control-' + (process.env.SSV_PORT ?? 9335),
  onMessage: (message) => {
    if (message.method === 'Runtime.exceptionThrown') {
      problems.push(message.params?.exceptionDetails?.exception?.description ?? 'exception');
    }
  },
});

await send('Page.navigate', { url: base });

// 页面工具函数：注入一次，后续表达式复用
const HELPERS = [
  'window.__H = {',
  '  time: () => {',
  "    const el = document.querySelector('[data-ssv=\\'time\\']');",
  "    if (!el) return null;",
  "    const [shown, total] = el.textContent.split('/').map((s) => s.trim());",
  "    const parse = (v) => (v.includes(':') ? (() => { const [m, s] = v.split(':'); return (+m * 60 + parseFloat(s)) * 1000; })() : parseFloat(v) * 1000);",
  '    return { shown: parse(shown), total: parse(total), raw: el.textContent };',
  '  },',
  '  opaque: () => {',
  "    const canvas = document.querySelector('canvas');",
  "    if (!canvas) return -1;",
  "    const context = canvas.getContext('2d', { willReadFrequently: true });",
  '    const data = context.getImageData(0, 0, canvas.width, canvas.height).data;',
  '    let count = 0;',
  '    for (let i = 3; i < data.length; i += 4) if (data[i] > 8) count++;',
  '    return count;',
  '  },',
  '  setRange: (sel, value) => {',
  '    const host = document.querySelector(sel);',
  "    const input = host && (host.matches('input[type=range]') ? host : host.querySelector('input[type=range]'));",
  '    if (!input) return false;',
  "    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;",
  '    setter.call(input, String(value));',
  "    input.dispatchEvent(new Event('input', { bubbles: true }));",
  '    return true;',
  '  },',
  '  click: (sel) => { const el = document.querySelector(sel); if (!el) return false; el.click(); return true; },',
  '  text: () => document.body.innerText.replace(/\\s+/g, \' \'),',
  '};',
].join('\n');

const names = [skeletonFile, atlasFile].concat(
  fs.readFileSync(path.join('spine-testfiles', dir, atlasFile), 'utf8').split('\n').map((l) => l.trim()).filter((l) => /\.(png|jpe?g|webp)$/i.test(l)),
);
const files = names.map((name) => ({ name, b64: fs.readFileSync(path.join('spine-testfiles', dir, name)).toString('base64') }));

const results = [];
const check = (name, ok, detail) => { results.push({ name, ok: !!ok, detail }); };
for (let i = 0; i < 120; i++) {
  const ready = await evaluate("document.querySelector('[role=button]') && /Spine/.test(document.body.innerText) ? 'ready' : ''");
  if (ready === 'ready') break;
  await sleep(400);
}
await evaluate(HELPERS);

// 注入文件并触发拖拽
await send('Runtime.evaluate', { expression: 'window.__FILES = ' + JSON.stringify(files) });
await evaluate([
  '(() => {',
  '  const files = window.__FILES.map((f) => new File([Uint8Array.from(atob(f.b64), (c) => c.charCodeAt(0))], f.name));',
  "  const dropzone = document.querySelector('[role=button]');",
  '  const dt = new DataTransfer();',
  '  for (const file of files) dt.items.add(file);',
  "  const event = new DragEvent('drop', { bubbles: true, cancelable: true });",
  "  Object.defineProperty(event, 'dataTransfer', { value: dt });",
  '  dropzone.dispatchEvent(event);',
  '})()',
].join('\n'));

let playing = false;
for (let i = 0; i < 120; i++) {
  await sleep(300);
  const ok = await evaluate("/播放中|Playing/.test(window.__H.text()) && window.__H.opaque() > 500");
  if (ok) { playing = true; break; }
}
check('load-plays', playing, '进入播放态且画布有像素');
if (!playing) {
  console.log(JSON.stringify({ case: dir + '/' + skeletonFile, results, problems, text: await evaluate('window.__H.text()') }, null, 2));
  close(); process.exit(1);
}

// 先切到有时长的动画（首个动画 aim 是 0 时长姿势动画，不适合测时间轴）
const pickAnim = () => [
  '(async () => {',
  "  const host = document.querySelector('[data-ssv=animation-select]');",
  "  const dd = host ? (host.matches('button[role=combobox]') ? host : host.querySelector('button[role=combobox]')) : document.querySelectorAll('button[role=combobox]')[1];",
  '  if (!dd) return { error: "no-dropdown" };',
  '  dd.click();',
  '  await new Promise((r) => setTimeout(r, 350));',
  "  const opts = Array.from(document.querySelectorAll('[role=listbox] [role=option]'));",
  '  const names = opts.map((o) => o.textContent.trim());',
  '  const target = opts.find((o) => o.textContent.trim() === window.__WANT);',
  '  if (target) target.click(); else if (opts[0]) opts[0].click();',
  '  await new Promise((r) => setTimeout(r, 700));',
  '  return { names, picked: target ? target.textContent.trim() : null };',
  '})()',
];
// 上面用第一个 combobox（动画）；皮肤是第二个。want 名字先放进 window
await send('Runtime.evaluate', { expression: 'window.__WANT = ' + JSON.stringify(wantAnim) });
const animPick = await evaluate(pickAnim().join('\n'));
check('animation-switched', animPick && animPick.picked === wantAnim, JSON.stringify(animPick));
const animText = await evaluate('window.__H.text()');
check('animation-in-status', typeof animText === 'string' && animText.includes(wantAnim), 'status 行包含 ' + wantAnim + '（实际 ' + JSON.stringify(animText) + '）');

// 1. 时长显示 > 0.8s
const t0 = await evaluate('window.__H.time()');
check('duration-shown', t0 && t0.total > 800, 'time=' + (t0 && t0.raw));

// 2. 播放中时间推进
await sleep(600);
const t1 = await evaluate('window.__H.time()');
check('clock-advances', t1 && t0 && t1.shown !== t0.shown, (t0 && t0.raw) + ' -> ' + (t1 && t1.raw));

// 3. 点暂停：时间静止，按钮文案切换
await evaluate("window.__H.click('[data-ssv=play-toggle]')");
await sleep(450);
const pA = await evaluate('window.__H.time()');
await sleep(600);
const pB = await evaluate('window.__H.time()');
check('pause-freezes', pA && pB && pA.shown === pB.shown, pA.raw + ' == ' + pB.raw);
const pauseBtn = await evaluate("document.querySelector('[data-ssv=play-toggle]').textContent.trim()");
check('button-toggles-label', /播放|Play/.test(pauseBtn), 'button=' + pauseBtn);

// 4. 进度条 seek：跳到 40%，暂停态也要出画
const seekOk = await evaluate("window.__H.setRange('[data-ssv=progress]', window.__H.time().total * 0.4)");
await sleep(600);
const seeked = await evaluate('window.__H.time()');
const target40 = await evaluate('window.__H.time().total * 0.4');
check('seek-lands', seekOk && seeked && Math.abs(seeked.shown - target40) <= Math.max(150, target40 * 0.12), 'seek到 ' + (seeked && seeked.raw) + '，目标 ' + Math.round(target40) + 'ms');
const seekPixels = await evaluate('window.__H.opaque()');
check('seek-frames-when-paused', seekPixels > 500, 'opaque=' + seekPixels);

// 5. 恢复播放
await evaluate("window.__H.click('[data-ssv=play-toggle]')");
await sleep(600);
const rA = await evaluate('window.__H.time()');
await sleep(500);
const rB = await evaluate('window.__H.time()');
check('resume-advances', rA && rB && rB.shown !== rA.shown, rA.raw + ' -> ' + rB.raw);

// 6. 循环开关：关闭后 seek 到 99% 再播，到点应停住不回卷
const switchSel = "[data-ssv=loop-toggle] input, [data-ssv=loop-toggle][role=switch]";
const before = await evaluate("String(document.querySelector('" + switchSel + "')?.checked)");
await evaluate("window.__H.click('[data-ssv=loop-toggle]')");
await sleep(700);
const after = await evaluate("String(document.querySelector('" + switchSel + "')?.checked)");
check('loop-toggles', before === 'true' && after === 'false', 'checked ' + before + ' -> ' + after);
await evaluate("window.__H.setRange('[data-ssv=progress]', window.__H.time().total * 0.99)");
await sleep(400);
await evaluate("window.__H.click('[data-ssv=play-toggle]')");
await sleep(1500);
const endA = await evaluate('window.__H.time()');
await sleep(700);
const endB = await evaluate('window.__H.time()');
check('nonloop-stops-at-end', endA && endB && endA.shown >= endA.total - 250 && Math.abs(endB.shown - endA.shown) < 120, JSON.stringify([endA && endA.raw, endB && endB.raw]));
// 重新打开循环，恢复播放态
await evaluate("window.__H.click('[data-ssv=loop-toggle]')");
await sleep(600);
const relBtn = await evaluate("document.querySelector('[data-ssv=play-toggle]').textContent.trim()");
if (/播放|Play/.test(relBtn)) { await evaluate("window.__H.click('[data-ssv=play-toggle]')"); await sleep(500); }
const backPlaying = await evaluate('window.__H.time()');
await sleep(500);
const backPlaying2 = await evaluate('window.__H.time()');
check('loop-reopen-resumes', backPlaying && backPlaying2 && backPlaying2.shown !== backPlaying.shown, (backPlaying && backPlaying.raw) + ' -> ' + (backPlaying2 && backPlaying2.raw));

// 7. 皮肤下拉：spineboy 应有 default 皮肤；切皮肤不应报错
await send('Runtime.evaluate', { expression: 'window.__EXPECT_SKIN = ' + JSON.stringify(expectSkin) });
const skinOptions = await evaluate([
  '(async () => {',
  "  const host = document.querySelector('[data-ssv=skin-select]');",
  "  const dd = host ? (host.matches('button[role=combobox]') ? host : host.querySelector('button[role=combobox]')) : document.querySelectorAll('button[role=combobox]')[2];",
  '  if (!dd) return { error: "no-skin-dropdown" };',
  '  dd.click();',
  '  await new Promise((r) => setTimeout(r, 350));',
  "  const opts = Array.from(document.querySelectorAll('[role=listbox] [role=option]'));",
  '  const names = opts.map((o) => o.textContent.trim());',
  '  let picked = null;',
  '  const want = window.__EXPECT_SKIN;',
  '  const match = want ? opts.find((o) => o.textContent.trim() === want) : (names.length > 1 ? opts[1] : opts[0]);',
  '  if (match) { match.click(); picked = names.indexOf(match.textContent.trim()) >= 0 ? match.textContent.trim() : null; }',
  '  await new Promise((r) => setTimeout(r, 600));',
  '  return { names, picked };',
  '})()',
].join('\n'));
if (expectSkin) {
  check('skin-listed', skinOptions && skinOptions.names.includes(expectSkin), JSON.stringify(skinOptions && skinOptions.names));
  check('skin-switched', skinOptions && skinOptions.picked === expectSkin, 'picked=' + (skinOptions && skinOptions.picked));
} else {
  check('skin-dropdown-works', skinOptions && skinOptions.names && skinOptions.names.length >= 1, JSON.stringify(skinOptions && skinOptions.names));
}

// 8. 缩放滑杆（暂停态验证像素变化）
await evaluate("window.__H.click('[data-ssv=play-toggle]')"); // pause
await sleep(450);
const opaqueFull = await evaluate('window.__H.opaque()');
await evaluate("window.__H.setRange('[data-ssv=scale-slider]', 0.5)");
await sleep(800);
const opaqueHalf = await evaluate('window.__H.opaque()');
check('scale-shrinks-pixels', opaqueHalf > 0 && opaqueHalf < opaqueFull * 0.85, 'opaque ' + opaqueFull + ' -> ' + opaqueHalf);
await evaluate("window.__H._hashBefore = (() => { const c = document.querySelector('canvas'); const d = c.getContext('2d', { willReadFrequently: true }).getImageData(0, 0, c.width, c.height).data; let h = 0; for (let i = 0; i < d.length; i += 997) h = (h * 31 + d[i]) | 0; return h; })()");

// 9. 偏移滑杆：平移后画布内容必须变化（计数可能不变，比对内容哈希）
await evaluate("window.__H.setRange('[data-ssv=offset-x]', 480)");
await sleep(800);
const opaqueShift = await evaluate('window.__H.opaque()');
const hashFrame = "(() => { const c = document.querySelector('canvas'); const d = c.getContext('2d', { willReadFrequently: true }).getImageData(0, 0, c.width, c.height).data; let h = 0; for (let i = 0; i < d.length; i += 997) h = (h * 31 + d[i]) | 0; return h; })()";
const hashBefore = await evaluate("window.__H._hashBefore");
const hashAfter = await evaluate(hashFrame);
check('offset-shifts-pixels', opaqueShift > 0 && hashAfter !== hashBefore, 'hash ' + hashBefore + ' -> ' + hashAfter + ' (opaque ' + opaqueShift + ')');

// 10. 复位变换：像素恢复
await evaluate("window.__H.click('[data-ssv=reset-transform]')");
await sleep(800);
const opaqueReset = await evaluate('window.__H.opaque()');
check('reset-restores-pixels', Math.abs(opaqueReset - opaqueFull) <= opaqueFull * 0.15, 'opaque reset=' + opaqueReset + ' (full=' + opaqueFull + ')');

// 11. 预乘alpha开关：切换应生效，暂停态两种模式都要出画
const premultSel = "[data-ssv=premultiplied-toggle] input, [data-ssv=premultiplied-toggle][role=switch]";
const premultBefore = await evaluate("String(document.querySelector('" + premultSel + "')?.checked)");
await evaluate("window.__H.click('[data-ssv=premultiplied-toggle]')");
await sleep(800);
const premultAfter = await evaluate("String(document.querySelector('" + premultSel + "')?.checked)");
check('premultiplied-toggles', premultBefore === 'true' && premultAfter === 'false', 'checked ' + premultBefore + ' -> ' + premultAfter);
const opaqueStraight = await evaluate('window.__H.opaque()');
check('straight-alpha-frames', opaqueStraight > 500, 'opaque=' + opaqueStraight);
// 切回预乘：GPU 直传路径同样出画
await evaluate("window.__H.click('[data-ssv=premultiplied-toggle]')");
await sleep(800);
const opaquePremult = await evaluate('window.__H.opaque()');
check('premultiplied-frames', opaquePremult > 500, 'opaque=' + opaquePremult);

// 12. 全程无错误条、无未捕获异常
const errBar = await evaluate("!!document.querySelector('[data-ssv=error]')");
check('no-error-bar', !errBar, 'errorBar=' + errBar);

const failures = results.filter((r) => !r.ok);
console.log(JSON.stringify({ case: dir + '/' + skeletonFile, ok: failures.length === 0, results, problems: problems.slice(0, 3) }, null, 2));
close();
process.exit(failures.length === 0 ? 0 : 1);
