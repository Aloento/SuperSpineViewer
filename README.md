# SuperSpineViewer

[**For English User**](https://github.com/Aloento/SuperSpineViewer/blob/master/README-Eng.md)

一个用来加载与导出 Spine 动画的浏览器 PWA

- 纯浏览器运行，无需 JVM / JavaFX / FFmpeg
- 透明视频导出：WebM（VP9-alpha，带 alpha 通道）或 APNG 帧序列（ZIP）；格式 / 帧率 / 码率 / 画布尺寸（640 / 1024 / 2048 / 自定义）可调
- 固定画布尺寸，与窗口/屏幕大小无关
- 多 Spine 版本加载：.skel 与 .json 均覆盖 3.0–4.3（2.1 不支持）
- 播放控制面板：暂停/进度拖拽/切动画/切皮肤/循环开关/偏移缩放
- 离线可用：核心资源预缓存，各版本运行时包按需拉取后 CacheFirst 缓存

技术栈：pnpm + Vite + TypeScript + Fluent UI + Tailwind CSS

编码依赖 WebCodecs，透明导出目前仅 Chromium 系浏览器可用（Firefox / Safari 回归进行中）。

## 开发与构建

```bash
pnpm install        # 安装依赖
pnpm dev            # 本地开发
pnpm build          # 构建，产物在 dist/
pnpm preview        # 预览构建产物
pnpm typecheck      # 类型检查
```

旧版运行时包（3.1–4.0）已 vendor 提交进 `src/spine/runtimes/generated/`，仅在需要更新时重跑：

```bash
pnpm fetch:runtimes
```

## 验证脚本

自动化验收均驱动本机无头 Edge（路径可用环境变量 `SSV_EDGE` 覆盖）。先启动 `pnpm dev`（或 `pnpm build` 后 `pnpm preview`）再跑对应命令；脚本接受 `<baseUrl> <目录> <骨架文件> <atlas 文件> …` 参数，缺省用各自默认用例：

```bash
pnpm verify:runtimes   # 解析层：版本嗅探 + 候选运行时链 + 骨架结构校验（Node 内跑，全版本语料）
pnpm check:render      # 渲染层：无头浏览器逐像素回归（3.0–4.3，.json + .skel）
pnpm check:app         # UI 端到端：真实拖拽、错误文案、首屏不加载运行时包
pnpm check:control     # 控制面板端到端：播放/进度/动画/皮肤/循环/偏移缩放
pnpm check:export      # 导出端到端：取消回滚、下载校验、参数切换
pnpm check:offline     # 离线验收：build + preview 后断网，仅凭缓存加载并导出
```

## 部署

纯静态站点：`pnpm build` 产物直接部署 Cloudflare Pages（framework preset: Vite，SPA 回退由 `public/_redirects` 提供）。

## 许可

AGPL-3.0，见 [LICENSE](LICENSE)。`src/spine/runtimes/generated/` 内含 vendored 的官方 Spine 运行时代码及对应 LICENSE。
