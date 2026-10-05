// 3.4–3.7 的官方 spine-webgl 直接引用只有主线程才有的 DOM 全局：
// HTMLCanvasElement / HTMLImageElement 的 instanceof，以及 WebGLRenderingContext 的混合常量。
// Worker 里 instanceof 的语义本来就是 false，用空类兜住 ReferenceError；
// Node（解析级验证）连 WebGLRenderingContext 都没有，补上 3.4 用到的常量。
const domGlobals: Record<string, unknown> = {
  HTMLCanvasElement: class {},
  HTMLImageElement: class {},
  HTMLVideoElement: class {},
  WebGLRenderingContext: { POINTS: 0, LINES: 1, TRIANGLES: 4, SRC_ALPHA: 770, ONE_MINUS_SRC_ALPHA: 771 },
};

for (const [name, value] of Object.entries(domGlobals)) {
  if (!(name in globalThis)) (globalThis as Record<string, unknown>)[name] = value;
}
