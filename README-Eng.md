# SuperSpineViewer

[中文说明](https://github.com/Aloento/SuperSpineViewer/blob/master/README.md)

A browser PWA to load and export Spine animations

- Runs purely in the browser, no JVM / JavaFX / FFmpeg required
- Transparent video export: WebM (VP9-alpha with alpha channel) or APNG frame sequence (ZIP); format / frame rate / bitrate / canvas size (640 / 1024 / 2048 / custom) configurable
- Fixed canvas size, independent of window/screen size
- Multi Spine version loading: both .skel and .json across 3.0-4.3 (2.1 not supported)
- Playback controls: pause/seek/switch animation/switch skin/loop toggle/offset & scale
- Works offline: core assets are precached, runtime packs are cached CacheFirst on first use

Tech stack: pnpm + Vite + TypeScript + Fluent UI + Tailwind CSS

Encoding relies on WebCodecs, so transparent export currently works in Chromium-based browsers only (Firefox / Safari regression in progress).

## Development & build

```bash
pnpm install        # install dependencies
pnpm dev            # local dev server
pnpm build          # build into dist/
pnpm preview        # preview the build output
pnpm typecheck      # type check
```

Legacy runtime packs (3.1-4.0) are vendored into `src/spine/runtimes/generated/` and committed; rerun only when updating them:

```bash
pnpm fetch:runtimes
```

## Verification scripts

All automated checks drive a local headless Edge (override the path with the `SSV_EDGE` env var). Start `pnpm dev` (or `pnpm build` then `pnpm preview`) first; the scripts accept `<baseUrl> <dir> <skeleton> <atlas> ...` arguments and fall back to their own default cases:

```bash
pnpm verify:runtimes   # parsing layer: version sniffing + candidate runtime chain + skeleton checks (Node, full corpus)
pnpm check:render      # render layer: headless browser pixel regression (3.0-4.3, .json + .skel)
pnpm check:app         # UI end-to-end: real drag & drop, error messages, no runtime packs on first paint
pnpm check:control     # control panel end-to-end: play/seek/animation/skin/loop/transform
pnpm check:export      # export end-to-end: cancel, download verification, parameter switching
pnpm check:offline     # offline acceptance: after build + preview, cut the network and load & export from cache only
```

## Deployment

Fully static: deploy the `pnpm build` output to Cloudflare Pages (framework preset: Vite; SPA fallback via `public/_redirects`).

## License

AGPL-3.0, see [LICENSE](LICENSE). `src/spine/runtimes/generated/` contains vendored official Spine runtime code with its own LICENSE files.
