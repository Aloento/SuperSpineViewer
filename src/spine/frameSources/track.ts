/**
 * TrackEntry 字段名跨版本兼容：3.1–3.6 用 time/lastTime，3.7+ 用 trackTime/animationLast。
 * 统一写入这两个字段，seek / setAnimation 各后端共用。
 */
/** 动画时间（秒）写入 entry；lastTime 置 -1 让下一帧从头采样，避免跨 seek 的插值拖影 */
export function writeTrackTime(entry: any, seconds: number): void {
  if (!entry) return;
  if ('trackTime' in entry) {
    entry.trackTime = seconds;
    entry.animationLast = -1;
  } else {
    entry.time = seconds;
    entry.lastTime = -1;
  }
}

/** 4.2/4.3 canvaskit 的 AnimationState 删掉了 getCurrent，只剩 tracks 数组；其余版本走方法 */
export function track0(state: any): any {
  return state.getCurrent ? state.getCurrent(0) : state.tracks?.[0] ?? null;
}

/** 默认动画优先 idle；否则首个非零时长动画。
 *  spineboy 的 [0] 是零时长的 aim，直接取会让进度条失去量程、播放一步到点 */
export function pickDefaultAnimation(animations: string[], durations: Record<string, number>): string {
  const idle = animations.find((name) => name.toLowerCase() === 'idle');
  if (idle !== undefined) return idle;
  return animations.find((name) => (durations[name] ?? 0) > 0) ?? animations[0] ?? '';
}
