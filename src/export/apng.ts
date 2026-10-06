// UPNG 为 CommonJS：Vite 下 default 导出可能被包一层
import UPNGmod from '@pdf-lib/upng';

const UPNG: any = (UPNGmod as any).default || UPNGmod;

export function encodePng(rgba: ArrayBuffer, width: number, height: number): ArrayBuffer {
  // cnum=0：不做调色板量化，保留 8bit RGBA
  return UPNG.encode([rgba], width, height, 0);
}

export interface ZipEntry {
  name: string;
  data: Uint8Array<ArrayBuffer>;
}

const crcTable: Uint32Array = (() => {
  const table = new Uint32Array(256);
  for (let i = 0; i < 256; i++) {
    let c = i;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    table[i] = c >>> 0;
  }
  return table;
})();

function crc32(bytes: Uint8Array): number {
  let c = 0xffffffff;
  for (let i = 0; i < bytes.length; i++) c = crcTable[(c ^ bytes[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

// 固定时间戳保证同输入同输出；DOS 时间 1980-01-01 00:00:00
const DOS_TIME = 0;
const DOS_DATE = ((1980 - 1980) << 9) | (1 << 5) | 1;

function u16(value: number): number[] {
  return [value & 0xff, (value >>> 8) & 0xff];
}

function u32(value: number): number[] {
  return [value & 0xff, (value >>> 8) & 0xff, (value >>> 16) & 0xff, (value >>> 24) & 0xff];
}

/** 仅 store（PNG 已压缩过，deflate 收益极低），足够产出合规 zip */
export function zipEntries(entries: ZipEntry[]): Blob {
  const encoder = new TextEncoder();
  const parts: Uint8Array<ArrayBuffer>[] = [];
  const central: Uint8Array<ArrayBuffer>[] = [];
  let offset = 0;

  for (const entry of entries) {
    const name = encoder.encode(entry.name);
    const crc = crc32(entry.data);
    const local = Uint8Array.from([
      ...u32(0x04034b50), ...u16(20), ...u16(0), ...u16(0),
      ...u16(DOS_TIME), ...u16(DOS_DATE),
      ...u32(crc), ...u32(entry.data.length), ...u32(entry.data.length),
      ...u16(name.length), ...u16(0),
      ...name,
    ]);
    parts.push(local, entry.data);
    central.push(
      Uint8Array.from([
        ...u32(0x02014b50), ...u16(20), ...u16(20), ...u16(0), ...u16(0),
        ...u16(DOS_TIME), ...u16(DOS_DATE),
        ...u32(crc), ...u32(entry.data.length), ...u32(entry.data.length),
        ...u16(name.length), ...u16(0), ...u16(0), ...u16(0), ...u16(0),
        ...u32(0), ...u32(offset),
        ...name,
      ]),
    );
    offset += local.length + entry.data.length;
  }

  const centralStart = offset;
  let centralSize = 0;
  for (const c of central) {
    parts.push(c);
    centralSize += c.length;
  }
  parts.push(
    Uint8Array.from([
      ...u32(0x06054b50), ...u16(0), ...u16(0),
      ...u16(entries.length), ...u16(entries.length),
      ...u32(centralSize), ...u32(centralStart), ...u16(0),
    ]),
  );
  return new Blob(parts as BlobPart[], { type: 'application/zip' });
}
