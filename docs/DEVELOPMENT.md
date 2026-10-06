# SuperSpineViewer 技术手册

面向贡献者与二次开发者。面向使用者的说明见 [README.md](../README.md)。

- 运行时要求：Node ≥ 20.19、pnpm（仓库锁定的 `packageManager` 为准）
- 技术栈：pnpm + Vite + TypeScript(strict) + Fluent UI + Tailwind CSS + vite-plugin-pwa
- 许可：AGPL-3.0；`src/spine/runtimes/generated/` 为 vendored 的 Spine 官方运行时，各自附官方许可证

## 1. 命令一览

| 命令 | 作用 |
| --- | --- |
| `pnpm install` | 安装依赖 |
| `pnpm dev` | 本地开发（默认 `http://localhost:5173`） |
| `pnpm build` | 类型检查 + 构建，产物在 `dist/` |
| `pnpm preview` | 预览构建产物 |
| `pnpm typecheck` | 只做类型检查 |
| `pnpm fetch:runtimes` | 重新拉取并生成 vendored 旧版运行时包（仅在需要更新时跑） |
| `pnpm verify:runtimes` | 解析层验收：版本嗅探 + 候选链 + 骨架结构比对（Node 内跑，复用应用代码） |
| `pnpm check:render` | 渲染层验收：无头浏览器逐像素回归（3.0–4.3，`.json` + `.skel`） |
| `pnpm check:app` | UI 端到端：真实拖拽、错误文案、首屏不加载运行时包 |
| `pnpm check:control` | 控制面板端到端：播放/进度/动画/皮肤/循环/预乘 alpha/偏移缩放 |
| `pnpm check:export` | 导出端到端：取消回滚、下载校验、参数切换 |
| `pnpm check:offline` | 离线验收：build + preview 后断网，仅凭缓存加载并导出 |

### 测试脚本的通用约定

- 所有 `check:*` 都驱动**本机无头 Chromium 系浏览器**（Edge / Chrome / Chromium 任一）。
  查找顺序：命令行 `--browser=<路径>` → 环境变量 `SSV_BROWSER`（旧名 `SSV_EDGE` 仍兼容）→
  各平台常见安装路径 → `PATH` 上的 `msedge`/`chrome`/`chromium`/`chromium-browser`。
  公共实现集中在 `scripts/lib/browser.mjs`（跨平台进程管理 + 最小 CDP 客户端）。
- 先起服务再跑脚本：`pnpm dev`（或 `pnpm build` 后 `pnpm preview`），
  脚本第一个参数是页面地址，缺省 `http://localhost:5173/`。
  `check:offline` 自带 preview 起停，直接 `pnpm build && pnpm check:offline`。
- 环境变量：`SSV_PORT`（CDP 端口，多个脚本同时跑时要错开）、`SSV_TIMEOUT`、`SSV_WAIT`、
  `SSV_LANG=zh|en`（`check:app` 用它验证中英文案）、`SSV_CDP`（`check:offline` 的 CDP 端口）。
- 测试素材从 Node 磁盘读 `spine-testfiles/`，dev/preview **不发布**该目录，
  与用户本地文件等价。每次跑前删掉临时 profile，避免复用到上一次构建的旧 Service Worker。
- 用例参数：`app-check` 与 `control-check` 接
  `<baseUrl> <目录> <骨架文件|*> <图集|*> <playing|error> [预期文案]`，
  骨架文件传 `*` 表示整目录拖入（自动配对）。

## 2. 架构

```
UI 线程（主线程，React + FluentUI + Tailwind）
  │  拖文件 / 版本选择 / 参数面板 / 进度 / 下载
  ▼
渲染 Worker（OffscreenCanvas 固定尺寸 + 按版本动态加载 runtime pack）
  │  clear(0,0,0,0) → 渲染骨架 → readPixels → straight-alpha RGBA
  │  postMessage + transfer(ImageBitmap) → 预览实时帧 / 编码输入
  ▼
编码 Worker（WebCodecs）
  │  色彩流：RGBA 原样 VideoFrame → VideoEncoder(vp9)
  │  alpha 流：A 平面作 I420 灰度 Y → 第二个 VideoEncoder(vp9)
  │  → 自研 EBML muxer（AlphaMode=1 + BlockAdditions）→ .webm（透明）
  │  APNG 路径：UPNG 逐帧 PNG → store 打包 zip
  ▼
UI：Blob URL → 下载 / 棋盘格预览
```

预览默认走 GPU 预乘 alpha 直传（满帧）；切到直通 alpha 后改走 CPU 读回，
像素与导出完全一致，帧率受读回开销限制。

## 3. 目录结构

```
src/
├── components/            # FluentUI 组件：拖放区、预览、控制面板、导出面板、导航、页脚
├── i18n/                  # zh.json / en.json + 语言检测（跟随系统，仅显式切换才持久化）
├── spine/
│   ├── versionLoader.ts   # 版本嗅探（4.x / 3.1–3.8 头部 + 扫描兜底 + 2.x 结构判据）
│   ├── runtimeMap.ts      # 版本解析、支持区间、候选链与未接入原因码
│   ├── pairing.ts         # 文件 / 图集自动配对（名称推导 → 同前缀 → 目录兜底）
│   ├── runtimes/          # pack registry、结果校验、DOM 垫片、文件名不敏感查找
│   │   └── generated/     # scripts/fetch-runtimes.mjs 产物 + 官方 LICENSE
│   ├── binary/            # 自研 .skel 读取器：legacyBinary.ts（3.4–3.7）、legacyBinary31.ts（3.0–3.2）
│   ├── frameSources/      # 取像素后端：canvaskit / webgl / legacy / track（跨版本时间字段）/ skins
│   ├── renderSession.ts   # 单 Worker 渲染会话（预览与导出各持一个）
│   ├── useSpineRenderer.ts# 主线程编排：加载 / 播放 / 时长 / 控制面板状态 / 可恢复偏移
│   └── types.ts
├── workers/               # render.worker / encode.worker / protocol
└── export/                # webm muxer / apng + zip / presets / 导出编排与客户端
scripts/                   # 验收与构建脚本（见 §1）
docs/images/               # 使用文档配图
spine-testfiles/           # 测试语料（不进 dev/preview 发布）
```

## 4. 版本支持矩阵

| 文件版本 | 运行时 | `.json` | `.skel` | 渲染后端 |
| --- | --- | --- | --- | --- |
| 3.0 / 3.1 / 3.2 | 3.1 系（官方 JS 无 3.0/3.1 渲染器） | ✅ | 自研读取器 | 自研 CanvasKit |
| 3.3 / 3.4 | 3.4 | ✅ | 自研读取器 | 官方 spine-webgl |
| 3.5 / 3.6 / 3.7 | 各版本 | ✅ | 自研读取器 | 官方 spine-webgl |
| 3.8 | 3.8 | ✅ | 官方 SkeletonBinary | 官方 spine-webgl |
| 4.0 / 4.1 | 各版本 | ✅ | 官方 | 官方 spine-webgl |
| 4.2 / 4.3 | 各版本 | ✅ | 官方 | 官方 spine-canvaskit |
| 2.x | 不做，给「不支持」提示 | — | — | — |

要点：

- **官方 JS 只有 3.8 起才有 `SkeletonBinary`**（3.4–3.7 的 core 里 0 处引用），
  所以 3.1–3.7 的 `.skel` 必须自研读取器。
- 旧版官方构建是全局脚本（`var spine;` + IIFE），构建期由 `fetch-runtimes.mjs` 包成 ESM，
  产物提交进仓库；`@esotericsoftware/spine-core@4.0.x` 无 `type:module` 且相对导入无扩展名，
  改用同一包的 `dist/iife/`。
- 3.0 / 3.1 / 3.2 的 `.skel` 三个 minor **互不兼容**（3.0 parent 为 1-based 且每条都写、
  boundingbox 计数写浮点总数、drawOrder 时间在帧尾；3.2 rotation/shear 提前、时间线编号重排并新增
  transform 时间线），读取器按 minor 分支。
- nonessential 段演进：3.4 只有 `imagesPath`；3.5 起前面多一个 `fps`；3.7 起尾部多一个 `audioPath`。

## 5. 版本嗅探与候选链

1. `.skel`：① 4.x 结构（偏移 8 读 version，hash 为 2×int32）② 3.1–3.8（偏移 0 读 hash + version
   两个字符串）③ 兜底扫前 64 字节的 `\d+\.\d+\.\d+`。2.1 `.skel` 没有 version 字符串，三种都失败
   → 返回 null。JSON 嗅探容忍前导空白（取前 16 字节判 `/^\s*\{/`）。
2. `.json`：读 `skeleton.spine`；老格式无该字段时走结构标记兜底（ffd/deform/drawOrder、
   skinnedmesh/weightedmesh/linkedmesh），避免把无 skeleton 段的 3.x 导出误判成 2.1。
3. 无精确 runtime 时按候选顺序逐个试（同大版本优先），不引导用户转格式；
   解析后**必须做结果校验**（bones>0、animations>0、版本一致）—— 4.x 运行时完全不校验版本号，
   格式不匹配时会静默产出 bones=0 的垃圾而不是抛异常。
4. 用户可在 `VersionSelector` 手动指定运行时（`packOverride` 直达候选链，不回退）。

## 6. 取像素与编码

统一接口 `FrameSource.render(t) → straight-alpha RGBA`，两条实现：
CanvasKit `canvas.readPixels({alphaType:Unpremul})`、WebGL `gl.readPixels`
（`premultipliedAlpha:false` + `drawSkeleton(skeleton, false)`）。

已修掉的真实坑（改动这块前务必先读）：

- 4.0/4.1 之前的 `blendFunc` 把 SRC_ALPHA 同时用于 RGB 和 A，透明背景首帧 alpha 退化为 α²，
  导出整幅偏淡 → 用 `fixupAlphaBlending` 补齐官方 4.0 的 `blendFuncSeparate` 规则。
- Chrome 上传 ImageBitmap 到 GL 纹理时无条件预乘 alpha（`UNPACK_PREMULTILY_ALPHA_WEBGL`
  对 ImageBitmap 源无效）→ 纹理源改用 canvas，读回后反预乘还原成直通 alpha。
- 3.4–3.6 忽略 `skeleton.scaleX/scaleY` → 缩放平移统一走 `renderer.camera`。
- 3.4 的 attachment loader 类名是 `TextureAtlasAttachmentLoader`（3.5+ 才叫 `AtlasAttachmentLoader`），
  且 vendored 产物直接引用主线程全局，需要 `runtimes/domShims.ts` 兜底。
- 上游 `spine-js@3.1.07` 的 skinnedmesh 权重用 `arr[arr.length]=v` 往定长数组 push，越界被静默忽略
  → `fetch-runtimes.mjs` 打补丁（换普通数组 + 命中数断言防漂移）；同期还有 18 处严格模式修补。
- 旧 TexturePacker 图集可能没有 `size:` 行 → 用解码后的图片实际尺寸补写 page.width/height。
- 编码只用 WebCodecs：`alpha:'keep'` 在 Chromium 一律抛 `NotSupportedError`，
  因此做**双流 VP9 单 WebM**（色彩流 RGBA + alpha 流 I420 灰度，自研 EBML muxer 以
  `Video/AlphaMode=1` + BlockAdditions 合并）。`@esotericsoftware/spine-core@4.0.x` 用 `dist/iife/`。
- 禁止 MediaRecorder、禁止 ffmpeg.wasm（ffmpeg 只用于离线验证产物，不进运行时）。

## 7. 首屏体积与离线

- PWA 预缓存只放壳（十项级、数百 KiB），`globIgnores` 排除 `assets/spine-*`、`assets/dist-*`、
  `render.worker-*`、`**/*.wasm`；这些资源走 `CacheFirst`（cacheName `spine-runtimes`）。
  曾把 `js/wasm` 全量预缓存导致「按需加载」被架空，改动 workbox 配置要重新确认这一点。
- `worker.format = 'es'`：渲染 Worker 里要按版本动态 `import()`，iife 不支持代码分割。
- `public/_redirects` 提供 Cloudflare Pages 的 SPA 回退；部署为纯静态 `dist/`。

## 8. 已知边界

- Spine 2.x 不支持（含 2.1：`.skel` 无版本字段，官方 JS 也读不了）。
- 3.1 官方运行时无 `SkeletonClipping`，legacy 后端不实现 clipping（现有测试语料不含 clipping）。
- 透明视频导出依赖 WebCodecs，目前只在 Chromium 系浏览器验证通过（Safari / Firefox 回归待做）。
- 取消导出会立即 terminate worker，下次导出重建；未做 worker 复用。
- 导出时长取 `animations[0].duration`：首动画是 0 时长姿势动画时只出 1 帧属预期。

## 9. 约定

- 主要工作语言中文；文档 / commit / issue 用中文，commit message 只描述代码变更本身。
- UI 必须走 i18n（zh + en，默认跟随系统语言），禁止硬编码用户可见文案。
- 注释尽量少，只在逻辑不直观或有坑处写「为什么」。
- `spine-testfiles/` 语料版本（逐个核对 `.json` 的 `skeleton.spine` 与 `.skel` 头部 version）：

| 目录 | 版本 | bones / bone0 | 备注 |
| --- | --- | --- | --- |
| spineboy21 | 2.1 | 17 / hip | 无 version 字段，不支持，用于断言错误路径 |
| spineboy30 / 31 / 32 / 33 | 3.0.16 / 3.1.08 / 3.2.01 / 3.3.07 | 17 / hip | 3.3 由 3.4 pack 承接 |
| spineboy34 / 35 | 3.4.02 / 3.5.03-beta | 17（另有 hover/mesh 骨架） | |
| spineboy36 / 37 / 38 | 3.6.32 / 3.7.90 / 3.8.55 | 64–65 / root | JSON 从 3.8 起有 `skeleton.x/y` |
| spineboy40 / 41 | 4.0.31 / 4.1.23-beta | 67 / root | 4.1 另有 run 图集 |
| spineboy42 / 43 | 4.2.43 / 4.3.26 | 67 / root | 官方稳定版资产 |
| goblins31 | 3.1（无 skeleton 段） | 21 / — | region + mesh + skinnedmesh 混合，3.1 mesh 路径唯一覆盖 |

- 结构比对（`verify:runtimes` 的 oracle）以同名 `.json` 经官方解析器的产出为期望值，
  比对 bones / slots / skins / animations 逐字段，并报第一个不一致的字段路径；
  官方实现自身的记账差异（`bones[].color`、4.x 的 `.id`/`.edges`、drawOrder 的 Uint32 哨兵等）
  列在 `ORACLE_SKIP_PATH` 里跳过。
