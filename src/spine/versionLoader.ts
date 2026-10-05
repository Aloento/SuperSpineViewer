import type { SpineVersionInfo } from './types';
import { parseSpineVersion } from './runtimeMap';

export { parseSpineVersion };

const versionScanPattern = /(\d{1,3})\.(\d{1,3})\.(\d{1,3})/;
const scanWindowBytes = 64;
const modernHashBytes = 8;

// 2.x 导出的 json 没有 skeleton.spine 字段，也读不到二进制版本，只能按结构判定为 2.1
const spine2xVersion: SpineVersionInfo = { raw: '2.1', major: 2, minor: 1, patch: 0 };

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
    return skeleton === undefined && Array.isArray(bones) ? spine2xVersion : null;
  } catch {
    return null;
  }
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
  const head = new Uint8Array(data, 0, 3);
  return head[0] === 0x7b || (head[0] === 0xef && head[1] === 0xbb && head[2] === 0xbf)
    ? detectFromJson(data)
    : detectFromBinary(new DataView(data));
}
