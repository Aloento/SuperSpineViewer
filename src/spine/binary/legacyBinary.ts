import { RuntimeError } from '../types';
import type { SpineVersionInfo } from '../types';

/**
 * 自研 .skel 读取器（§12.7）：官方 JS 生态的 SkeletonBinary 从 3.8 才有，3.3–3.7 走这里。
 * 底稿是 vendored 3.8 的官方 SkeletonBinary，按 Java 参考实现的逐版本 diff 做分支
 * （参考官方同版本 SkeletonBinary.java，3.5 与 3.6 逐字节相同）。
 * 产出对象全部由各版本官方 core 的类构造，官方渲染器可直接消费。
 * §12.11：3.3↔3.4 同格式、3.5↔3.6 同格式，分支按文件内声明的 major.minor 决定。
 */

type FormatBranch = 34 | 35 | 37;

function formatBranch(major: number, minor: number): FormatBranch {
  if (major !== 3) throw new RuntimeError('parseInvalid', `legacy-binary-major=${major}`);
  if (minor === 3 || minor === 4) return 34;
  if (minor === 5 || minor === 6) return 35;
  if (minor === 7) return 37;
  throw new RuntimeError('parseInvalid', `legacy-binary-minor=${minor}`);
}

/** pack 可读取的 .skel 版本区间（major.minor 闭区间，超出即 binaryUnsupported） */
export interface BinaryVersionRange {
  min: { major: number; minor: number };
  max: { major: number; minor: number };
}

export function legacyBinarySupports(range: BinaryVersionRange, version: SpineVersionInfo): boolean {
  const value = version.major * 100 + version.minor;
  return value >= range.min.major * 100 + range.min.minor && value <= range.max.major * 100 + range.max.minor;
}

class BinaryInput {
  index = 0;
  private readonly buffer: DataView;

  constructor(data: Uint8Array) {
    this.buffer = new DataView(data.buffer, data.byteOffset, data.byteLength);
  }

  readByte(): number {
    return this.buffer.getInt8(this.index++);
  }

  readShort(): number {
    const value = this.buffer.getInt16(this.index);
    this.index += 2;
    return value;
  }

  readInt32(): number {
    const value = this.buffer.getInt32(this.index);
    this.index += 4;
    return value;
  }

  readInt(optimizePositive: boolean): number {
    let b = this.readByte();
    let result = b & 0x7f;
    if ((b & 0x80) !== 0) {
      b = this.readByte();
      result |= (b & 0x7f) << 7;
      if ((b & 0x80) !== 0) {
        b = this.readByte();
        result |= (b & 0x7f) << 14;
        if ((b & 0x80) !== 0) {
          b = this.readByte();
          result |= (b & 0x7f) << 21;
          if ((b & 0x80) !== 0) {
            b = this.readByte();
            result |= (b & 0x7f) << 28;
          }
        }
      }
    }
    return optimizePositive ? result : (result >>> 1) ^ -(result & 1);
  }

  readString(): string | null {
    const byteCount = this.readInt(true);
    if (byteCount === 0) return null;
    if (byteCount === 1) return '';
    const length = byteCount - 1;
    // spine 写的是 UTF-8；TextDecoder 对 ASCII 与官方逐字节解码一致，非 ASCII 更正确
    const bytes = new Uint8Array(this.buffer.buffer, this.buffer.byteOffset + this.index, length);
    this.index += length;
    return new TextDecoder('utf-8', { fatal: false }).decode(bytes);
  }

  readFloat(): number {
    const value = this.buffer.getFloat32(this.index);
    this.index += 4;
    return value;
  }

  readBoolean(): boolean {
    return this.readByte() !== 0;
  }
}

interface VerticesResult {
  bones: number[] | null;
  vertices: number[];
}

function rgba8888(target: any, value: number): void {
  target.r = ((value & 0xff000000) >>> 24) / 255;
  target.g = ((value & 0x00ff0000) >>> 16) / 255;
  target.b = ((value & 0x0000ff00) >>> 8) / 255;
  target.a = (value & 0x000000ff) / 255;
}

// Java rgb888ToColor 不动 alpha
function rgb888(target: any, value: number): void {
  target.r = ((value & 0x00ff0000) >>> 16) / 255;
  target.g = ((value & 0x0000ff00) >>> 8) / 255;
  target.b = (value & 0x000000ff) / 255;
}

const BONE_ROTATE = 0;
const BONE_TRANSLATE = 1;
const BONE_SCALE = 2;
const BONE_SHEAR = 3;
const SLOT_ATTACHMENT = 0;
const SLOT_COLOR = 1;
const SLOT_TWO_COLOR = 2;
const PATH_POSITION = 0;
const PATH_SPACING = 1;
const PATH_MIX = 2;
const CURVE_STEPPED = 1;
const CURVE_BEZIER = 2;

interface LinkedMesh {
  mesh: any;
  skin: string | null;
  slotIndex: number;
  parent: string;
}

export function readLegacySkeletonData(spine: any, bytes: Uint8Array, attachmentLoader: any): any {
  return new LegacyBinaryReader(spine, attachmentLoader).read(bytes);
}

class LegacyBinaryReader {
  private readonly scale = 1;
  private readonly linkedMeshes: LinkedMesh[] = [];
  private format: FormatBranch = 34;
  private readonly spine: any;
  private readonly attachmentLoader: any;

  constructor(spine: any, attachmentLoader: any) {
    this.spine = spine;
    this.attachmentLoader = attachmentLoader;
  }

  read(binary: Uint8Array): any {
    const spine = this.spine;
    const scale = this.scale;
    const skeletonData = new spine.SkeletonData();
    skeletonData.name = '';
    const input = new BinaryInput(binary);

    skeletonData.hash = input.readString();
    const version = input.readString();
    if (!version) throw new RuntimeError('parseInvalid', 'legacy-binary-no-version');
    skeletonData.version = version;
    const matched = /^(\d+)\.(\d+)/.exec(version);
    if (!matched) throw new RuntimeError('parseInvalid', `legacy-version=${version}`);
    this.format = formatBranch(Number(matched[1]), Number(matched[2]));
    const format = this.format;
    // 3.5 的 beta 导出不写 slot darkColor（正式版才加），测试语料 spineboy35 即 3.5.03-beta
    const slotDarkColor = format >= 35 && !(format === 35 && version.includes('-beta'));

    // 3.3–3.7 头部没有 x/y（§12.10），core 默认 0
    skeletonData.width = input.readFloat();
    skeletonData.height = input.readFloat();
    const nonessential = input.readBoolean();
    if (nonessential) {
      if (format >= 35) skeletonData.fps = input.readFloat();
      skeletonData.imagesPath = input.readString();
      if (format >= 37) skeletonData.audioPath = input.readString();
    }

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      const name = input.readString()!;
      const parent = i === 0 ? null : skeletonData.bones[input.readInt(true)];
      const data = new spine.BoneData(i, name, parent);
      data.rotation = input.readFloat();
      data.x = input.readFloat() * scale;
      data.y = input.readFloat() * scale;
      data.scaleX = input.readFloat();
      data.scaleY = input.readFloat();
      data.shearX = input.readFloat();
      data.shearY = input.readFloat();
      data.length = input.readFloat() * scale;
      if (format >= 35) {
        data.transformMode = input.readInt(true);
      } else {
        data.inheritRotation = input.readBoolean();
        data.inheritScale = input.readBoolean();
      }
      if (nonessential) {
        const color = input.readInt32();
        // 3.4–3.7 的 JS BoneData 没有 color（仅编辑器用，官方 JSON 读取器也不解析），字节仍需消费
        if (data.color) rgba8888(data.color, color);
      }
      skeletonData.bones.push(data);
    }

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      const slotName = input.readString()!;
      const boneData = skeletonData.bones[input.readInt(true)];
      const data = new spine.SlotData(i, slotName, boneData);
      rgba8888(data.color, input.readInt32());
      if (slotDarkColor) {
        const darkColor = input.readInt32();
        if (darkColor !== -1) rgb888((data.darkColor = new spine.Color(1, 1, 1, 1)), darkColor);
      }
      data.attachmentName = input.readString();
      data.blendMode = [spine.BlendMode.Normal, spine.BlendMode.Additive, spine.BlendMode.Multiply, spine.BlendMode.Screen][
        input.readInt(true)
      ];
      skeletonData.slots.push(data);
    }

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      const data = new spine.IkConstraintData(input.readString()!);
      if (format >= 35) data.order = input.readInt(true);
      for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
        data.bones.push(skeletonData.bones[input.readInt(true)]);
      }
      data.target = skeletonData.bones[input.readInt(true)];
      data.mix = input.readFloat();
      data.bendDirection = input.readByte();
      if (format >= 37) {
        data.compress = input.readBoolean();
        data.stretch = input.readBoolean();
        data.uniform = input.readBoolean();
      }
      skeletonData.ikConstraints.push(data);
    }

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      const data = new spine.TransformConstraintData(input.readString()!);
      if (format >= 35) data.order = input.readInt(true);
      for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
        data.bones.push(skeletonData.bones[input.readInt(true)]);
      }
      data.target = skeletonData.bones[input.readInt(true)];
      if (format >= 35) {
        data.local = input.readBoolean();
        data.relative = input.readBoolean();
      }
      data.offsetRotation = input.readFloat();
      data.offsetX = input.readFloat() * scale;
      data.offsetY = input.readFloat() * scale;
      data.offsetScaleX = input.readFloat();
      data.offsetScaleY = input.readFloat();
      data.offsetShearY = input.readFloat();
      data.rotateMix = input.readFloat();
      data.translateMix = input.readFloat();
      data.scaleMix = input.readFloat();
      data.shearMix = input.readFloat();
      skeletonData.transformConstraints.push(data);
    }

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      const data = new spine.PathConstraintData(input.readString()!);
      if (format >= 35) data.order = input.readInt(true);
      for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
        data.bones.push(skeletonData.bones[input.readInt(true)]);
      }
      data.target = skeletonData.slots[input.readInt(true)];
      data.positionMode = [spine.PositionMode.Fixed, spine.PositionMode.Percent][input.readInt(true)];
      data.spacingMode = [spine.SpacingMode.Length, spine.SpacingMode.Fixed, spine.SpacingMode.Percent][input.readInt(true)];
      data.rotateMode = [spine.RotateMode.Tangent, spine.RotateMode.Chain, spine.RotateMode.ChainScale][input.readInt(true)];
      data.offsetRotation = input.readFloat();
      data.position = input.readFloat();
      if (data.positionMode === spine.PositionMode.Fixed) data.position *= scale;
      data.spacing = input.readFloat();
      if (data.spacingMode === spine.SpacingMode.Length || data.spacingMode === spine.SpacingMode.Fixed) {
        data.spacing *= scale;
      }
      data.rotateMix = input.readFloat();
      data.translateMix = input.readFloat();
      skeletonData.pathConstraints.push(data);
    }

    const defaultSkin = this.readSkin(input, skeletonData, 'default', nonessential);
    if (defaultSkin != null) {
      skeletonData.defaultSkin = defaultSkin;
      skeletonData.skins.push(defaultSkin);
    }
    for (let i = 0, n = input.readInt(true); i < n; i++) {
      skeletonData.skins.push(this.readSkin(input, skeletonData, input.readString()!, nonessential));
    }

    for (const linkedMesh of this.linkedMeshes) {
      const skin = linkedMesh.skin == null ? skeletonData.defaultSkin : skeletonData.findSkin(linkedMesh.skin);
      if (skin == null) throw new Error(`Skin not found: ${linkedMesh.skin}`);
      const parent = skin.getAttachment(linkedMesh.slotIndex, linkedMesh.parent);
      if (parent == null) throw new Error(`Parent mesh not found: ${linkedMesh.parent}`);
      linkedMesh.mesh.setParentMesh(parent);
      linkedMesh.mesh.updateUVs();
    }
    this.linkedMeshes.length = 0;

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      const data = new spine.EventData(input.readString()!);
      data.intValue = input.readInt(false);
      data.floatValue = input.readFloat();
      data.stringValue = input.readString();
      if (format >= 37) {
        data.audioPath = input.readString();
        if (data.audioPath != null) {
          data.volume = input.readFloat();
          data.balance = input.readFloat();
        }
      }
      skeletonData.events.push(data);
    }

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      this.readAnimation(input, input.readString()!, skeletonData);
    }
    return skeletonData;
  }

  private readSkin(input: BinaryInput, skeletonData: any, skinName: string, nonessential: boolean): any {
    const slotCount = input.readInt(true);
    if (slotCount === 0) return null;
    const skin = new this.spine.Skin(skinName);
    for (let i = 0; i < slotCount; i++) {
      const slotIndex = input.readInt(true);
      for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
        const name = input.readString()!;
        const attachment = this.readAttachment(input, skeletonData, skin, slotIndex, name, nonessential);
        if (attachment != null) skin.addAttachment(slotIndex, name, attachment);
      }
    }
    return skin;
  }

  private readAttachment(
    input: BinaryInput,
    skeletonData: any,
    skin: any,
    slotIndex: number,
    attachmentName: string,
    nonessential: boolean,
  ): any {
    const scale = this.scale;
    const name = input.readString() ?? attachmentName;
    const typeIndex = input.readByte();

    switch (typeIndex) {
      case 0: {
        let path = input.readString();
        const rotation = input.readFloat();
        const x = input.readFloat();
        const y = input.readFloat();
        const scaleX = input.readFloat();
        const scaleY = input.readFloat();
        const width = input.readFloat();
        const height = input.readFloat();
        const color = input.readInt32();
        if (path == null) path = name;
        const region = this.attachmentLoader.newRegionAttachment(skin, name, path);
        if (region == null) return null;
        region.path = path;
        region.x = x * scale;
        region.y = y * scale;
        region.scaleX = scaleX;
        region.scaleY = scaleY;
        region.rotation = rotation;
        region.width = width * scale;
        region.height = height * scale;
        rgba8888(region.color, color);
        region.updateOffset();
        return region;
      }
      case 1: {
        const vertexCount = input.readInt(true);
        const vertices = this.readVertices(input, vertexCount);
        const color = nonessential ? input.readInt32() : 0;
        const box = this.attachmentLoader.newBoundingBoxAttachment(skin, name);
        if (box == null) return null;
        box.worldVerticesLength = vertexCount << 1;
        box.vertices = vertices.vertices;
        box.bones = vertices.bones;
        if (nonessential) rgba8888(box.color, color);
        return box;
      }
      case 2: {
        let path = input.readString();
        const color = input.readInt32();
        const vertexCount = input.readInt(true);
        const uvs = this.readFloatArray(input, vertexCount << 1, 1);
        const triangles = this.readShortArray(input);
        const vertices = this.readVertices(input, vertexCount);
        const hullLength = input.readInt(true);
        let edges: number[] | null = null;
        let width = 0;
        let height = 0;
        if (nonessential) {
          edges = this.readShortArray(input);
          width = input.readFloat();
          height = input.readFloat();
        }
        if (path == null) path = name;
        const mesh = this.attachmentLoader.newMeshAttachment(skin, name, path);
        if (mesh == null) return null;
        mesh.path = path;
        rgba8888(mesh.color, color);
        mesh.bones = vertices.bones;
        mesh.vertices = vertices.vertices;
        mesh.worldVerticesLength = vertexCount << 1;
        mesh.triangles = triangles;
        mesh.regionUVs = uvs;
        mesh.updateUVs();
        mesh.hullLength = hullLength << 1;
        if (nonessential) {
          mesh.edges = edges;
          mesh.width = width * scale;
          mesh.height = height * scale;
        }
        return mesh;
      }
      case 3: {
        let path = input.readString();
        const color = input.readInt32();
        const skinName = input.readString();
        const parent = input.readString()!;
        const inheritDeform = input.readBoolean();
        let width = 0;
        let height = 0;
        if (nonessential) {
          width = input.readFloat();
          height = input.readFloat();
        }
        if (path == null) path = name;
        const mesh = this.attachmentLoader.newMeshAttachment(skin, name, path);
        if (mesh == null) return null;
        mesh.path = path;
        rgba8888(mesh.color, color);
        mesh.inheritDeform = inheritDeform;
        if (nonessential) {
          mesh.width = width * scale;
          mesh.height = height * scale;
        }
        this.linkedMeshes.push({ mesh, skin: skinName, slotIndex, parent });
        return mesh;
      }
      case 4: {
        const closed = input.readBoolean();
        const constantSpeed = input.readBoolean();
        const vertexCount = input.readInt(true);
        const vertices = this.readVertices(input, vertexCount);
        // Java 整数除法：lengths 数量 = floor(vertexCount / 3)
        const lengthsCount = Math.floor(vertexCount / 3);
        const lengths = new Array<number>(lengthsCount);
        for (let i = 0; i < lengthsCount; i++) lengths[i] = input.readFloat() * scale;
        const color = nonessential ? input.readInt32() : 0;
        const path = this.attachmentLoader.newPathAttachment(skin, name);
        if (path == null) return null;
        path.closed = closed;
        path.constantSpeed = constantSpeed;
        path.worldVerticesLength = vertexCount << 1;
        path.vertices = vertices.vertices;
        path.bones = vertices.bones;
        path.lengths = lengths;
        if (nonessential) rgba8888(path.color, color);
        return path;
      }
      case 5: {
        const rotation = input.readFloat();
        const x = input.readFloat();
        const y = input.readFloat();
        const color = nonessential ? input.readInt32() : 0;
        if (typeof this.attachmentLoader.newPointAttachment !== 'function') {
          throw new RuntimeError('parseInvalid', 'point-attachment-not-supported');
        }
        const point = this.attachmentLoader.newPointAttachment(skin, name);
        if (point == null) return null;
        point.x = x * scale;
        point.y = y * scale;
        point.rotation = rotation;
        if (nonessential) rgba8888(point.color, color);
        return point;
      }
      case 6: {
        const endSlotIndex = input.readInt(true);
        const vertexCount = input.readInt(true);
        const vertices = this.readVertices(input, vertexCount);
        const color = nonessential ? input.readInt32() : 0;
        if (typeof this.attachmentLoader.newClippingAttachment !== 'function') {
          throw new RuntimeError('parseInvalid', 'clipping-attachment-not-supported');
        }
        const clip = this.attachmentLoader.newClippingAttachment(skin, name);
        if (clip == null) return null;
        clip.endSlot = skeletonData.slots[endSlotIndex];
        clip.worldVerticesLength = vertexCount << 1;
        clip.vertices = vertices.vertices;
        clip.bones = vertices.bones;
        if (nonessential) rgba8888(clip.color, color);
        return clip;
      }
      default:
        throw new RuntimeError('parseInvalid', `attachment-type=${typeIndex}`);
    }
  }

  private readVertices(input: BinaryInput, vertexCount: number): VerticesResult {
    const verticesLength = vertexCount << 1;
    const scale = this.scale;
    const vertices: VerticesResult = { bones: null, vertices: [] };
    if (!input.readBoolean()) {
      vertices.vertices = this.readFloatArray(input, verticesLength, scale);
      return vertices;
    }
    const weights: number[] = [];
    const bonesArray: number[] = [];
    for (let i = 0; i < vertexCount; i++) {
      const boneCount = input.readInt(true);
      bonesArray.push(boneCount);
      for (let ii = 0; ii < boneCount; ii++) {
        bonesArray.push(input.readInt(true));
        weights.push(input.readFloat() * scale);
        weights.push(input.readFloat() * scale);
        weights.push(input.readFloat());
      }
    }
    vertices.vertices = weights;
    vertices.bones = bonesArray;
    return vertices;
  }

  private readFloatArray(input: BinaryInput, n: number, scale: number): number[] {
    const array = new Array<number>(n);
    if (scale === 1) {
      for (let i = 0; i < n; i++) array[i] = input.readFloat();
    } else {
      for (let i = 0; i < n; i++) array[i] = input.readFloat() * scale;
    }
    return array;
  }

  private readShortArray(input: BinaryInput): number[] {
    const n = input.readInt(true);
    const array = new Array<number>(n);
    for (let i = 0; i < n; i++) array[i] = input.readShort();
    return array;
  }

  private readAnimation(input: BinaryInput, name: string, skeletonData: any): void {
    const spine = this.spine;
    const format = this.format;
    const scale = this.scale;
    const timelines: any[] = [];
    let duration = 0;

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      const slotIndex = input.readInt(true);
      for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
        const timelineType = input.readByte();
        const frameCount = input.readInt(true);
        switch (timelineType) {
          case SLOT_ATTACHMENT: {
            const timeline = new spine.AttachmentTimeline(frameCount);
            timeline.slotIndex = slotIndex;
            for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
              timeline.setFrame(frameIndex, input.readFloat(), input.readString());
            }
            timelines.push(timeline);
            duration = Math.max(duration, timeline.frames[frameCount - 1]);
            break;
          }
          case SLOT_COLOR: {
            const timeline = new spine.ColorTimeline(frameCount);
            timeline.slotIndex = slotIndex;
            for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
              const time = input.readFloat();
              const color = { r: 0, g: 0, b: 0, a: 0 };
              rgba8888(color, input.readInt32());
              timeline.setFrame(frameIndex, time, color.r, color.g, color.b, color.a);
              if (frameIndex < frameCount - 1) this.readCurve(input, frameIndex, timeline);
            }
            timelines.push(timeline);
            duration = Math.max(duration, timeline.frames[(frameCount - 1) * spine.ColorTimeline.ENTRIES]);
            break;
          }
          case SLOT_TWO_COLOR: {
            if (format < 35 || typeof spine.TwoColorTimeline !== 'function') {
              throw new RuntimeError('parseInvalid', 'two-color-not-supported');
            }
            const timeline = new spine.TwoColorTimeline(frameCount);
            timeline.slotIndex = slotIndex;
            for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
              const time = input.readFloat();
              const color1 = { r: 0, g: 0, b: 0, a: 0 };
              const color2 = { r: 0, g: 0, b: 0 };
              rgba8888(color1, input.readInt32());
              rgb888(color2, input.readInt32());
              timeline.setFrame(
                frameIndex,
                time,
                color1.r, color1.g, color1.b, color1.a,
                color2.r, color2.g, color2.b,
              );
              if (frameIndex < frameCount - 1) this.readCurve(input, frameIndex, timeline);
            }
            timelines.push(timeline);
            duration = Math.max(duration, timeline.frames[(frameCount - 1) * spine.TwoColorTimeline.ENTRIES]);
            break;
          }
        }
      }
    }

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      const boneIndex = input.readInt(true);
      for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
        const timelineType = input.readByte();
        const frameCount = input.readInt(true);
        switch (timelineType) {
          case BONE_ROTATE: {
            const timeline = new spine.RotateTimeline(frameCount);
            timeline.boneIndex = boneIndex;
            for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
              timeline.setFrame(frameIndex, input.readFloat(), input.readFloat());
              if (frameIndex < frameCount - 1) this.readCurve(input, frameIndex, timeline);
            }
            timelines.push(timeline);
            duration = Math.max(duration, timeline.frames[(frameCount - 1) * spine.RotateTimeline.ENTRIES]);
            break;
          }
          case BONE_TRANSLATE:
          case BONE_SCALE:
          case BONE_SHEAR: {
            let timeline: any;
            let timelineScale = 1;
            if (timelineType === BONE_SCALE) timeline = new spine.ScaleTimeline(frameCount);
            else if (timelineType === BONE_SHEAR) timeline = new spine.ShearTimeline(frameCount);
            else {
              timeline = new spine.TranslateTimeline(frameCount);
              timelineScale = scale;
            }
            timeline.boneIndex = boneIndex;
            for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
              timeline.setFrame(
                frameIndex,
                input.readFloat(),
                input.readFloat() * timelineScale,
                input.readFloat() * timelineScale,
              );
              if (frameIndex < frameCount - 1) this.readCurve(input, frameIndex, timeline);
            }
            timelines.push(timeline);
            duration = Math.max(duration, timeline.frames[(frameCount - 1) * spine.TranslateTimeline.ENTRIES]);
            break;
          }
        }
      }
    }

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      const index = input.readInt(true);
      const frameCount = input.readInt(true);
      const timeline = new spine.IkConstraintTimeline(frameCount);
      timeline.ikConstraintIndex = index;
      for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
        const time = input.readFloat();
        const mix = input.readFloat();
        const bendDirection = input.readByte();
        if (format >= 37) {
          timeline.setFrame(frameIndex, time, mix, bendDirection, input.readBoolean(), input.readBoolean());
        } else {
          timeline.setFrame(frameIndex, time, mix, bendDirection);
        }
        if (frameIndex < frameCount - 1) this.readCurve(input, frameIndex, timeline);
      }
      timelines.push(timeline);
      duration = Math.max(duration, timeline.frames[(frameCount - 1) * spine.IkConstraintTimeline.ENTRIES]);
    }

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      const index = input.readInt(true);
      const frameCount = input.readInt(true);
      const timeline = new spine.TransformConstraintTimeline(frameCount);
      timeline.transformConstraintIndex = index;
      for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
        timeline.setFrame(
          frameIndex,
          input.readFloat(),
          input.readFloat(),
          input.readFloat(),
          input.readFloat(),
          input.readFloat(),
        );
        if (frameIndex < frameCount - 1) this.readCurve(input, frameIndex, timeline);
      }
      timelines.push(timeline);
      duration = Math.max(duration, timeline.frames[(frameCount - 1) * spine.TransformConstraintTimeline.ENTRIES]);
    }

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      const index = input.readInt(true);
      const data = skeletonData.pathConstraints[index];
      for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
        const timelineType = input.readByte();
        const frameCount = input.readInt(true);
        switch (timelineType) {
          case PATH_POSITION:
          case PATH_SPACING: {
            let timeline: any;
            let timelineScale = 1;
            if (timelineType === PATH_SPACING) {
              timeline = new spine.PathConstraintSpacingTimeline(frameCount);
              if (data.spacingMode === spine.SpacingMode.Length || data.spacingMode === spine.SpacingMode.Fixed) {
                timelineScale = scale;
              }
            } else {
              timeline = new spine.PathConstraintPositionTimeline(frameCount);
              if (data.positionMode === spine.PositionMode.Fixed) timelineScale = scale;
            }
            timeline.pathConstraintIndex = index;
            for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
              timeline.setFrame(frameIndex, input.readFloat(), input.readFloat() * timelineScale);
              if (frameIndex < frameCount - 1) this.readCurve(input, frameIndex, timeline);
            }
            timelines.push(timeline);
            duration = Math.max(duration, timeline.frames[(frameCount - 1) * spine.PathConstraintPositionTimeline.ENTRIES]);
            break;
          }
          case PATH_MIX: {
            const timeline = new spine.PathConstraintMixTimeline(frameCount);
            timeline.pathConstraintIndex = index;
            for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
              timeline.setFrame(frameIndex, input.readFloat(), input.readFloat(), input.readFloat());
              if (frameIndex < frameCount - 1) this.readCurve(input, frameIndex, timeline);
            }
            timelines.push(timeline);
            duration = Math.max(duration, timeline.frames[(frameCount - 1) * spine.PathConstraintMixTimeline.ENTRIES]);
            break;
          }
        }
      }
    }

    for (let i = 0, n = input.readInt(true); i < n; i++) {
      const skin = skeletonData.skins[input.readInt(true)];
      for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
        const slotIndex = input.readInt(true);
        for (let iii = 0, nnn = input.readInt(true); iii < nnn; iii++) {
          const attachment = skin.getAttachment(slotIndex, input.readString()!);
          const weighted = attachment.bones != null;
          const vertices: number[] = Array.from(attachment.vertices as ArrayLike<number>);
          const deformLength = weighted ? (vertices.length / 3) * 2 : vertices.length;

          const frameCount = input.readInt(true);
          const timeline = new spine.DeformTimeline(frameCount);
          timeline.slotIndex = slotIndex;
          timeline.attachment = attachment;

          for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            const time = input.readFloat();
            let deform: number[];
            let end = input.readInt(true);
            if (end === 0) {
              deform = weighted ? spine.Utils.newFloatArray(deformLength) : vertices;
            } else {
              deform = spine.Utils.newFloatArray(deformLength);
              const start = input.readInt(true);
              end += start;
              if (scale === 1) {
                for (let v = start; v < end; v++) deform[v] = input.readFloat();
              } else {
                for (let v = start; v < end; v++) deform[v] = input.readFloat() * scale;
              }
              if (!weighted) {
                for (let v = 0, vn = deform.length; v < vn; v++) deform[v] += vertices[v];
              }
            }
            timeline.setFrame(frameIndex, time, deform);
            if (frameIndex < frameCount - 1) this.readCurve(input, frameIndex, timeline);
          }
          timelines.push(timeline);
          duration = Math.max(duration, timeline.frames[frameCount - 1]);
        }
      }
    }

    const drawOrderCount = input.readInt(true);
    if (drawOrderCount > 0) {
      const timeline = new spine.DrawOrderTimeline(drawOrderCount);
      const slotCount = skeletonData.slots.length;
      for (let i = 0; i < drawOrderCount; i++) {
        const time = input.readFloat();
        const offsetCount = input.readInt(true);
        const drawOrder = spine.Utils.newArray(slotCount, 0);
        for (let ii = slotCount - 1; ii >= 0; ii--) drawOrder[ii] = -1;
        const unchanged = spine.Utils.newArray(slotCount - offsetCount, 0);
        let originalIndex = 0;
        let unchangedIndex = 0;
        for (let ii = 0; ii < offsetCount; ii++) {
          const slotIndex = input.readInt(true);
          while (originalIndex !== slotIndex) unchanged[unchangedIndex++] = originalIndex++;
          drawOrder[originalIndex + input.readInt(true)] = originalIndex++;
        }
        while (originalIndex < slotCount) unchanged[unchangedIndex++] = originalIndex++;
        for (let ii = slotCount - 1; ii >= 0; ii--) {
          if (drawOrder[ii] === -1) drawOrder[ii] = unchanged[--unchangedIndex];
        }
        timeline.setFrame(i, time, drawOrder);
      }
      timelines.push(timeline);
      duration = Math.max(duration, timeline.frames[drawOrderCount - 1]);
    }

    const eventCount = input.readInt(true);
    if (eventCount > 0) {
      const timeline = new spine.EventTimeline(eventCount);
      for (let i = 0; i < eventCount; i++) {
        const time = input.readFloat();
        const eventData = skeletonData.events[input.readInt(true)];
        const event = new spine.Event(time, eventData);
        event.intValue = input.readInt(false);
        event.floatValue = input.readFloat();
        event.stringValue = input.readBoolean() ? input.readString() : eventData.stringValue;
        if (format >= 37 && eventData.audioPath != null) {
          event.volume = input.readFloat();
          event.balance = input.readFloat();
        }
        timeline.setFrame(i, event);
      }
      timelines.push(timeline);
      duration = Math.max(duration, timeline.frames[eventCount - 1]);
    }

    skeletonData.animations.push(new spine.Animation(name, timelines, duration));
  }

  private readCurve(input: BinaryInput, frameIndex: number, timeline: any): void {
    switch (input.readByte()) {
      case CURVE_STEPPED:
        timeline.setStepped(frameIndex);
        break;
      case CURVE_BEZIER:
        timeline.setCurve(frameIndex, input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat());
        break;
    }
  }
}
