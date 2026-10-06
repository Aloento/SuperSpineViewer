// 透明 WebM 封装：视频轨 V_VP9 + AlphaMode=1，每帧 BlockGroup 内以
// BlockAdditions/BlockMore/BlockAddID=1 携带 alpha 平面的独立 VP9 帧（I420，Y=alpha）。
// 与 Chromium vpx_video_decoder / ffmpeg libvpx-vp9 yuva420p 的容器约定一致。

function idBytes(id: number): number[] {
  // EBML ID 宽度由首字节前导零决定：0x80+=1B，0x4000+=2B，0x200000+=3B，0x10000000+=4B
  if (id >= 0x10000000) return [(id >>> 24) & 0xff, (id >>> 16) & 0xff, (id >>> 8) & 0xff, id & 0xff];
  if (id >= 0x200000) return [(id >>> 16) & 0xff, (id >>> 8) & 0xff, id & 0xff];
  if (id >= 0x4000) return [(id >>> 8) & 0xff, id & 0xff];
  return [id];
}

function sizeVint(value: number): number[] {
  let len = 1;
  while (value >= Math.pow(2, 7 * len) - 2) len++;
  const out = new Array<number>(len).fill(0);
  out[0] = 0x80 >> (len - 1);
  let v = value;
  for (let i = len - 1; i >= 0; i--) {
    out[i] |= v & 0xff;
    v = Math.floor(v / 0x100);
  }
  return out;
}

// BlobPart 要求 ArrayBuffer 后端的 typed array（TS7 lib 区分 ArrayBufferLike）
function concat(parts: Uint8Array[]): Uint8Array<ArrayBuffer> {
  let total = 0;
  for (const p of parts) total += p.length;
  const out = new Uint8Array(total);
  let off = 0;
  for (const p of parts) {
    out.set(p, off);
    off += p.length;
  }
  return out;
}

function fromNumbers(nums: number[]): Uint8Array<ArrayBuffer> {
  return Uint8Array.from(nums);
}

function elem(id: number, payload: Uint8Array): Uint8Array<ArrayBuffer> {
  const idB = fromNumbers(idBytes(id));
  const sz = fromNumbers(sizeVint(payload.length));
  return concat([idB, sz, payload]);
}

function uint(id: number, value: number): Uint8Array<ArrayBuffer> {
  let v = value;
  const bytes: number[] = [];
  do {
    bytes.unshift(v & 0xff);
    v = Math.floor(v / 0x100);
  } while (v > 0);
  return elem(id, fromNumbers(bytes));
}

function fixedUint(id: number, value: number, width: number): Uint8Array<ArrayBuffer> {
  const bytes = new Array<number>(width).fill(0);
  let v = value;
  for (let i = width - 1; i >= 0; i--) {
    bytes[i] = v & 0xff;
    v = Math.floor(v / 0x100);
  }
  return elem(id, fromNumbers(bytes));
}

function float(id: number, value: number): Uint8Array<ArrayBuffer> {
  const b = new Uint8Array(8);
  new DataView(b.buffer).setFloat64(0, value);
  return elem(id, b);
}

function text(id: number, value: string): Uint8Array<ArrayBuffer> {
  return elem(id, new TextEncoder().encode(value));
}

const ID = {
  segment: 0x18538067,
  seekHead: 0x114d9b74,
  seek: 0x4dbb,
  seekId: 0x53ab,
  seekPos: 0x53ac,
  info: 0x1549a966,
  timecodeScale: 0x2ad7b1,
  duration: 0x4489,
  tracks: 0x1654ae6b,
  trackEntry: 0xae,
  trackNumber: 0xd7,
  trackUid: 0x73c5,
  trackType: 0x83,
  codecId: 0x86,
  defaultDuration: 0x23e383,
  video: 0xe0,
  pixelWidth: 0xb0,
  pixelHeight: 0xba,
  alphaMode: 0x53c0,
  cluster: 0x1f43b675,
  timecode: 0xe7,
  blockGroup: 0xa0,
  block: 0xa1,
  blockAdditions: 0x75a1,
  blockMore: 0xa6,
  blockAddId: 0xee,
  blockAdditional: 0xa5,
  referenceBlock: 0xfb,
  cues: 0x1c53bb6b,
  cuePoint: 0xbb,
  cueTime: 0xb3,
  cuePositions: 0xb7,
  cueTrack: 0xf1,
  cuePos: 0xf0,
} as const;

function ebmlHeader(): Uint8Array<ArrayBuffer> {
  return elem(0x1a45dfa3, concat([
    uint(0x4286, 1),
    uint(0x42f7, 1),
    uint(0x42f2, 4),
    uint(0x42f3, 8),
    text(0x4282, 'webm'),
    uint(0x4287, 2),
    uint(0x4285, 2),
  ]));
}

function trackEntry(width: number, height: number, fps: number): Uint8Array<ArrayBuffer> {
  return elem(ID.trackEntry, concat([
    uint(ID.trackNumber, 1),
    uint(ID.trackUid, 1),
    uint(ID.trackType, 1),
    text(ID.codecId, 'V_VP9'),
    uint(ID.defaultDuration, Math.round(1_000_000 / fps)),
    elem(ID.video, concat([
      uint(ID.pixelWidth, width),
      uint(ID.pixelHeight, height),
      uint(ID.alphaMode, 1),
    ])),
  ]));
}

function blockGroup(relTimeMs: number, color: Uint8Array, alpha: Uint8Array, isKey: boolean): Uint8Array<ArrayBuffer> {
  const blockData = new Uint8Array(4 + color.length);
  blockData[0] = 0x81;
  blockData[1] = (relTimeMs >> 8) & 0xff;
  blockData[2] = relTimeMs & 0xff;
  blockData[3] = 0x00;
  blockData.set(color, 4);
  // demuxer 以 ReferenceBlock 的缺失判定关键帧：delta 帧必须携带，否则 seek 落到 delta 帧解码失败
  const parts = [elem(ID.block, blockData)];
  if (!isKey) parts.push(fromNumbers([ID.referenceBlock, 0x81, 0xff]));
  parts.push(elem(ID.blockAdditions, elem(ID.blockMore, concat([
    uint(ID.blockAddId, 1),
    elem(ID.blockAdditional, alpha),
  ]))));
  return elem(ID.blockGroup, concat(parts));
}

export class WebmWriter {
  // 闭簇即转 Blob 存档（'bytes' 类型在支持的实现里零拷贝移交缓冲），
  // finalize 只拼尾部结构，避免全片字节反复 concat 的双份内存峰值
  private readonly parts: BlobPart[] = [];
  private clusterParts: Uint8Array[] = [];
  private cueTimes: number[] = [];
  private clusterLens: number[] = [];
  private clustersTotal = 0;
  private clusterOpen = false;
  private clusterTimeMs = 0;
  private lastTimeMs = 0;
  private readonly width: number;
  private readonly height: number;
  private readonly fps: number;

  constructor(width: number, height: number, fps: number) {
    this.width = width;
    this.height = height;
    this.fps = fps;
  }

  addFrame(index: number, color: Uint8Array, alpha: Uint8Array, isKey: boolean): void {
    const tsMs = Math.round((index * 1000) / this.fps);
    if (this.clusterOpen && isKey && index > 0) this.closeCluster();
    if (!this.clusterOpen) {
      this.clusterTimeMs = tsMs;
      this.cueTimes.push(tsMs);
      this.clusterOpen = true;
    }
    this.clusterParts.push(blockGroup(tsMs - this.clusterTimeMs, color, alpha, isKey));
    this.lastTimeMs = tsMs;
  }

  private closeCluster(): void {
    if (!this.clusterOpen) return;
    const cluster = elem(ID.cluster, concat([
      fixedUint(ID.timecode, this.clusterTimeMs, 3),
      ...this.clusterParts,
    ]));
    this.parts.push(new Blob([cluster], { type: 'bytes' }));
    this.clusterLens.push(cluster.length);
    this.clustersTotal += cluster.length;
    this.clusterParts = [];
    this.clusterOpen = false;
  }

  /** SeekHead 的 Position 字段宽度固定，info/tracks 字节数与内容取值无关 → 头部布局一次算定 */
  private layout() {
    const ebml = ebmlHeader();
    const info = elem(ID.info, concat([
      uint(ID.timecodeScale, 1_000_000),
      float(ID.duration, this.lastTimeMs + 1000 / this.fps),
    ]));
    const tracks = this.tracksBlob();
    const buildSeekHead = (infoAt: number, tracksAt: number, cuesAt: number) =>
      elem(ID.seekHead, concat([
        ...([ID.info, ID.tracks, ID.cues] as const).map((target, i) =>
          elem(ID.seek, concat([
            fixedUint(ID.seekId, target, 4),
            fixedUint(ID.seekPos, [infoAt, tracksAt, cuesAt][i], 4),
          ])),
        ),
      ]));
    const seekHeadSize = buildSeekHead(0, 0, 0).length;
    // SeekHead/Cue 的 Position 均相对 Segment 数据起始（不含 Segment 头）
    const infoPos = seekHeadSize;
    const tracksPos = infoPos + info.length;
    const cuesPos = tracksPos + tracks.length + this.clustersTotal;
    const seekHead = buildSeekHead(infoPos, tracksPos, cuesPos);
    if (seekHead.length !== seekHeadSize) throw new Error('webm: SeekHead 尺寸不稳定');
    return { ebml, info, tracks, seekHead };
  }

  private tracksBlob(): Uint8Array<ArrayBuffer> {
    return elem(ID.tracks, trackEntry(this.width, this.height, this.fps));
  }

  finalize(): Blob {
    this.closeCluster();
    const { ebml, info, tracks, seekHead } = this.layout();

    // Cue 位置相对 Segment 数据起始：seekHead + info + tracks + 之前各簇长度
    let clustersAt = seekHead.length + info.length + tracks.length;
    const cueBody = this.cueTimes.map((t, i) => {
      const position = clustersAt;
      clustersAt += this.clusterLens[i];
      return elem(ID.cuePoint, concat([
        fixedUint(ID.cueTime, t, 3),
        elem(ID.cuePositions, concat([
          uint(ID.cueTrack, 1),
          fixedUint(ID.cuePos, position, 4),
        ])),
      ]));
    });
    const cues = elem(ID.cues, concat(cueBody));

    return new Blob(
      [
        ebml,
        fromNumbers(idBytes(ID.segment)),
        fromNumbers([0x01, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff]),
        seekHead,
        info,
        tracks,
        ...this.parts,
        cues,
      ],
      { type: 'video/webm' },
    );
  }
}
