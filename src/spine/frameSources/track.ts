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
