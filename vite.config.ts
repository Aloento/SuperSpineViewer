import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  // 渲染 Worker 里按版本动态 import spine 运行时，iife 不支持代码分割
  worker: { format: 'es' },
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg', 'icons/icon.svg', 'icons/maskable.svg'],
      manifest: {
        id: '/',
        name: 'Super Spine Viewer',
        short_name: 'SpineViewer',
        description: '在浏览器中加载并导出 Spine 动画的 PWA',
        lang: 'zh-CN',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        orientation: 'any',
        background_color: '#0f0f0f',
        theme_color: '#0f6cbd',
        categories: ['graphics', 'developer', 'utilities'],
        icons: [
          { src: 'icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          {
            src: 'icons/maskable-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,json,svg,png,ico,woff2,wasm}'],
        cleanupOutdatedCaches: true,
        clientsClaim: true,
        maximumFileSizeToCacheInBytes: 12 * 1024 * 1024,
        navigateFallback: '/index.html',
      },
      devOptions: {
        enabled: true,
        type: 'module',
      },
    }),
  ],
});
