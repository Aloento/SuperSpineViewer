# SuperSpineViewer

A browser PWA to load and export Spine animations (rewriting in progress)

- Runs purely in the browser, no JVM / JavaFX / FFmpeg required
- Transparent video export (WebM VP9-alpha with alpha channel)
- Fixed canvas size, independent of window/screen size
- Multi Spine version loading (.json 3.0-4.3 and .skel 3.8-4.3 today, targeting full .skel + .json coverage for 3.0-4.3)

Tech stack: pnpm + Vite + TypeScript + Fluent UI + Tailwind CSS
