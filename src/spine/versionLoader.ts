import type { SpineVersionInfo } from './types';
import { parseSpineVersion } from './runtimeMap';

export { parseSpineVersion };

const versionScanPattern = /(\d{1,3})\.(\d{1,3})\.(\d{1,3})/;
const scanWindowBytes = 64;
const modernHashBytes = 8;

// 2.x 导出的 json 没有 skeleton.spine 字段，也读不到二进制版本，只能按结构判定为 2.1
const spine2xVersion: SpineVersionInfo = { raw: '2.1', major: 2, minor: 1, patch: 0 };
// 3.0–3.3 的部分导出同样没有 skeleton 段（官方 goblins-mesh 是 3.1 导出且无此段），
// 只能靠 3.x 独有结构判定；3.1 pack 覆盖 3.0–3.3，故按 3.1 报
const spine3xLegacyVersion: SpineVersionInfo = { raw: '3.1', major: 3, minor: 1, patch: 0 };

// 3.x 独有的动画时间轴名：mesh 顶点动画（3.1 叫 ffd，3.4+ 叫 deform）与大写 O 的 drawOrder；
// 2.x 的动画只有 slots/bones/events/draworder（实测 spineboy21）
const spine3xMarkers = ['ffd', 'deform', 'drawOrder'];

interface Cursor {
  offset: number;
}

function readVarint(view: DataView, cursor: Cursor): number {
  let value = 0;
  let shift = 1;
  for (let i = 0; i < 5; i++) {
    const byte = view.getInt8(cursor.offset++);
    value += (byte & 0x7f) * shift;
    if ((byte & 0x80) === 0) break;
    shift *= 128;
  }
  return value;
}

function readBinaryString(view: DataView, cursor: Cursor): string | null {
  const byteCount = readVarint(view, cursor);
  if (byteCount === 0) return null;
  if (byteCount === 1) return '';
  const length = byteCount - 1;
  const bytes = new Uint8Array(view.buffer, view.byteOffset + cursor.offset, length);
  cursor.offset += length;
  return new TextDecoder().decode(bytes);
}

function detectFromJson(data: ArrayBuffer): SpineVersionInfo | null {
  try {
    const text = new TextDecoder().decode(data);
    const parsed: unknown = JSON.parse(text);
    const skeleton = (parsed as { skeleton?: { spine?: unknown } }).skeleton;
    if (typeof skeleton?.spine === 'string') return parseSpineVersion(skeleton.spine);
    const bones = (parsed as { bones?: unknown }).bones;
    if (skeleton !== undefined || !Array.isArray(bones)) return null;
    return hasSpine3xMarkers(parsed) ? spine3xLegacyVersion : spine2xVersion;
  } catch {
    return null;
  }
}

/** 无 skeleton 段时只能看内容：mesh 顶点动画与约束体系是 3.x 才有的东西 */
function hasSpine3xMarkers(parsed: unknown): boolean {
  const root = parsed as { animations?: unknown; skins?: unknown };
  const animations = root.animations;
  if (animations && typeof animations === 'object') {
    for (const animation of Object.values(animations as Record<string, unknown>)) {
      if (!animation || typeof animation !== 'object') continue;
      if (Object.keys(animation as Record<string, unknown>).some((key) => spine3xMarkers.includes(key))) return true;
    }
  }
  const skins = root.skins;
  if (skins && typeof skins === 'object') {
    const entries: unknown[] = Array.isArray(skins) ? skins : Object.values(skins as Record<string, unknown>);
    for (const skin of entries) {
      const slots = (skin as { attachments?: unknown })?.attachments;
      if (!slots || typeof slots !== 'object') continue;
      for (const slot of Object.values(slots as Record<string, unknown>)) {
        if (!slot || typeof slot !== 'object') continue;
        for (const attachment of Object.values(slot as Record<string, unknown>)) {
          const type = (attachment as { type?: unknown })?.type;
          // 2.x 也有 region/mesh/boundingbox；skinnedmesh 与 linkedmesh 系列是 3.0 起才有的
          if (typeof type === 'string' && (type === 'skinnedmesh' || type === 'weightedmesh' || type.endsWith('linkedmesh'))) return true;
        }
      }
    }
  }
  return false;
}

function detectFromBinary(view: DataView): SpineVersionInfo | null {
  // 4.x：hash 是两个 int32（低/高），version 紧随其后
  const modern = versionAtOffset(view, modernHashBytes);
  if (modern) return modern;
  // 3.1–3.8：hash 与 version 都是字符串
  const legacy = legacyVersion(view);
  if (legacy) return legacy;
  // 兜底：hash 的原始字节里可能混入可打印字符，直接在前 64 字节里扫版本号
  return scannedVersion(view);
}

function versionAtOffset(view: DataView, offset: number): SpineVersionInfo | null {
  try {
    const value = readBinaryString(view, { offset });
    return value ? parseSpineVersion(value) : null;
  } catch {
    return null;
  }
}

function legacyVersion(view: DataView): SpineVersionInfo | null {
  try {
    const cursor: Cursor = { offset: 0 };
    readBinaryString(view, cursor);
    const value = readBinaryString(view, cursor);
    return value ? parseSpineVersion(value) : null;
  } catch {
    return null;
  }
}

function scannedVersion(view: DataView): SpineVersionInfo | null {
  const length = Math.min(scanWindowBytes, view.byteLength);
  const text = String.fromCharCode(...new Uint8Array(view.buffer, view.byteOffset, length));
  const matched = versionScanPattern.exec(text);
  return matched ? parseSpineVersion(matched[0]) : null;
}

export function detectSpineVersion(data: ArrayBuffer): SpineVersionInfo | null {
  if (data.byteLength < 9) return null;
  // 解包资源常见 BOM / 前导换行，只看首个非空白字符是不是 JSON 起始符
  const prefix = new TextDecoder().decode(new Uint8Array(data, 0, Math.min(16, data.byteLength)));
  return /^\s*\{/.test(prefix)
    ? detectFromJson(data)
    : detectFromBinary(new DataView(data));
}
