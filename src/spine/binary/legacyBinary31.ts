import { RuntimeError } from '../types';

/**
 * 3.0–3.2 的 .skel 读取器：这一代二进制布局与 3.3+ 完全不同，且三个 minor 版本之间也有出入，
 * 按声明版本的 minor 分支：
 * - 3.0：骨骼 parent 是 1-based（0=null），无 shear（agent/reference/spine/SkeletonBinary-3.0-late.cs）
 * - 3.1：parent 直接索引（i==0 不读），x,y,sx,sy,rot,len（agent/reference/spine/SkeletonBinary-3.1.07.java）
 * - 3.2：rot 在 x,y 前且带 shearX/shearY，时间线编号整体重排（SkeletonBinary-3.2.cs，spine-csharp 3.2 分支）
 * 产出的对象全部来自 vendored spine-js 3.1.07 的类。
 */

// 3.0/3.1 的时间线类型编号
const T30_SCALE = 0;
const T30_ROTATE = 1;
const T30_TRANSLATE = 2;
const T30_ATTACHMENT = 3;
const T30_COLOR = 4;
// 3.2 重排过，并新增 shear
const T32_ROTATE = 0;
const T32_TRANSLATE = 1;
const T32_SCALE = 2;
const T32_SHEAR = 3;
const T32_ATTACHMENT = 4;
const T32_COLOR = 5;
const CURVE_STEPPED = 1;
const CURVE_BEZIER = 2;

// 3.1 的附件类型编号
const TYPE_REGION = 0;
const TYPE_BOUNDINGBOX = 1;
const TYPE_MESH = 2;
const TYPE_WEIGHTED_MESH = 3;
const TYPE_LINKED_MESH = 4;
const TYPE_WEIGHTED_LINKED_MESH = 5;

class BinaryInput31 {
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

interface LinkedMesh31 {
  mesh: any;
  skin: string | null;
  slotIndex: number;
  parent: string;
}

export function readLegacy31SkeletonData(spine: any, bytes: Uint8Array, attachmentLoader: any): any {
  const scale = 1;
  const skeletonData = new spine.SkeletonData();
  const linkedMeshes: LinkedMesh31[] = [];
  const input = new BinaryInput31(bytes);

  skeletonData.hash = input.readString();
  const version = input.readString();
  if (!version) throw new RuntimeError('parseInvalid', 'legacy31-no-version');
  skeletonData.version = version;
  const matched = /^(\d+)\.(\d+)/.exec(version);
  if (!matched || matched[1] !== '3' || Number(matched[2]) > 2) {
    throw new RuntimeError('parseInvalid', 'legacy31-version=' + version);
  }
  const minor = Number(matched[2]);

  skeletonData.width = input.readFloat();
  skeletonData.height = input.readFloat();
  const nonessential = input.readBoolean();
  if (nonessential) skeletonData.imagesPath = input.readString();

  // 骨骼：字段顺序按 minor 分支（3.0 的 parent 是 1-based；3.2 的 rotation 在前且带 shear）
  for (let i = 0, n = input.readInt(true); i < n; i++) {
    const name = input.readString()!;
    let parent: any = null;
    if (minor === 0) {
      const parentIndex = input.readInt(true) - 1;
      if (parentIndex !== -1) parent = skeletonData.bones[parentIndex];
    } else if (i !== 0) {
      parent = skeletonData.bones[input.readInt(true)];
    }
    const data = new spine.BoneData(name, parent);
    if (minor === 2) {
      data.rotation = input.readFloat();
      data.x = input.readFloat() * scale;
      data.y = input.readFloat() * scale;
      data.scaleX = input.readFloat();
      data.scaleY = input.readFloat();
      // 3.1 的 JS core 没有 shear 字段，字节仍需消费
      input.readFloat();
      input.readFloat();
      data.length = input.readFloat() * scale;
      data.inheritRotation = input.readBoolean();
      data.inheritScale = input.readBoolean();
    } else {
      data.x = input.readFloat() * scale;
      data.y = input.readFloat() * scale;
      data.scaleX = input.readFloat();
      data.scaleY = input.readFloat();
      data.rotation = input.readFloat();
      data.length = input.readFloat() * scale;
      data.inheritScale = input.readBoolean();
      data.inheritRotation = input.readBoolean();
    }
    // bone color 是编辑器装饰，3.1 的 JS core 没有对应字段，字节仍需消费
    if (nonessential) input.readInt32();
    skeletonData.bones.push(data);
  }

  // 3.1 的约束在 slots 之前：IK → transform
  for (let i = 0, n = input.readInt(true); i < n; i++) {
    const data = new spine.IkConstraintData(input.readString()!);
    for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
      data.bones.push(skeletonData.bones[input.readInt(true)]);
    }
    data.target = skeletonData.bones[input.readInt(true)];
    data.mix = input.readFloat();
    data.bendDirection = input.readByte();
    skeletonData.ikConstraints.push(data);
  }

  for (let i = 0, n = input.readInt(true); i < n; i++) {
    const data = new spine.TransformConstraintData(input.readString()!);
    data.bone = skeletonData.bones[input.readInt(true)];
    data.target = skeletonData.bones[input.readInt(true)];
    if (minor === 2) {
      // 3.2 的 transform 约束含 10 个 offset/mix 浮点；3.1 的 core 只有 translateMix/x/y，
      // 其余字段消费字节后丢弃
      input.readFloat(); // offsetRotation
      data.x = input.readFloat(); // offsetX
      data.y = input.readFloat(); // offsetY
      input.readFloat(); // offsetScaleX
      input.readFloat(); // offsetScaleY
      input.readFloat(); // offsetShearY
      input.readFloat(); // rotateMix
      data.translateMix = input.readFloat(); // translateMix
      input.readFloat(); // scaleMix
      input.readFloat(); // shearMix
    } else {
      data.translateMix = input.readFloat();
      data.x = input.readFloat();
      data.y = input.readFloat();
    }
    skeletonData.transformConstraints.push(data);
  }

  for (let i = 0, n = input.readInt(true); i < n; i++) {
    const slotName = input.readString()!;
    const boneData = skeletonData.bones[input.readInt(true)];
    const data = new spine.SlotData(slotName, boneData);
    rgba(data, input.readInt32());
    data.attachmentName = input.readString();
    data.blendMode = input.readInt(true);
    skeletonData.slots.push(data);
  }

  const defaultSkin = readSkin(input, spine, attachmentLoader, linkedMeshes, 'default', nonessential, minor);
  if (defaultSkin != null) {
    skeletonData.defaultSkin = defaultSkin;
    skeletonData.skins.push(defaultSkin);
  }
  for (let i = 0, n = input.readInt(true); i < n; i++) {
    skeletonData.skins.push(readSkin(input, spine, attachmentLoader, linkedMeshes, input.readString()!, nonessential, minor));
  }

  for (const linkedMesh of linkedMeshes) {
    const skin = linkedMesh.skin == null ? skeletonData.defaultSkin : skeletonData.findSkin(linkedMesh.skin);
    if (skin == null) throw new Error('Skin not found: ' + linkedMesh.skin);
    const parent = skin.getAttachment(linkedMesh.slotIndex, linkedMesh.parent);
    if (parent == null) throw new Error('Parent mesh not found: ' + linkedMesh.parent);
    linkedMesh.mesh.setParentMesh(parent);
    linkedMesh.mesh.updateUVs();
  }

  for (let i = 0, n = input.readInt(true); i < n; i++) {
    const data = new spine.EventData(input.readString()!);
    data.intValue = input.readInt(false);
    data.floatValue = input.readFloat();
    data.stringValue = input.readString();
    skeletonData.events.push(data);
  }

  for (let i = 0, n = input.readInt(true); i < n; i++) {
    readAnimation(input, spine, input.readString()!, skeletonData, minor);
  }
  return skeletonData;
}

// 3.1 的 core 没有 spine.Color，颜色直接落在对象的 r/g/b/a 上
function rgba(target: any, value: number): void {
  target.r = ((value & 0xff000000) >>> 24) / 255;
  target.g = ((value & 0x00ff0000) >>> 16) / 255;
  target.b = ((value & 0x0000ff00) >>> 8) / 255;
  target.a = (value & 0x000000ff) / 255;
}

function readSkin(
  input: BinaryInput31,
  spine: any,
  attachmentLoader: any,
  linkedMeshes: LinkedMesh31[],
  skinName: string,
  nonessential: boolean,
  minor: number,
): any {
  const slotCount = input.readInt(true);
  if (slotCount === 0) return null;
  const skin = new spine.Skin(skinName);
  for (let i = 0; i < slotCount; i++) {
    const slotIndex = input.readInt(true);
    for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
      const name = input.readString()!;
      const attachment = readAttachment(input, attachmentLoader, linkedMeshes, skin, slotIndex, name, nonessential, minor);
      if (attachment != null) skin.addAttachment(slotIndex, name, attachment);
    }
  }
  return skin;
}

function readAttachment(
  input: BinaryInput31,
  attachmentLoader: any,
  linkedMeshes: LinkedMesh31[],
  skin: any,
  slotIndex: number,
  attachmentName: string,
  nonessential: boolean,
  minor: number,
): any {
  const scale = 1;
  const name = input.readString() ?? attachmentName;
  const typeIndex = input.readByte();

  switch (typeIndex) {
    case TYPE_REGION: {
      let path = input.readString();
      let rotation: number;
      let x: number;
      let y: number;
      let scaleX: number;
      let scaleY: number;
      if (minor === 2) {
        // 3.2：rotation 在 x,y 之前
        rotation = input.readFloat();
        x = input.readFloat();
        y = input.readFloat();
        scaleX = input.readFloat();
        scaleY = input.readFloat();
      } else {
        x = input.readFloat();
        y = input.readFloat();
        scaleX = input.readFloat();
        scaleY = input.readFloat();
        rotation = input.readFloat();
      }
      const width = input.readFloat();
      const height = input.readFloat();
      const color = input.readInt32();
      if (path == null) path = name;
      const region = attachmentLoader.newRegionAttachment(skin, name, path);
      if (region == null) return null;
      region.path = path;
      region.x = x * scale;
      region.y = y * scale;
      region.scaleX = scaleX;
      region.scaleY = scaleY;
      region.rotation = rotation;
      region.width = width * scale;
      region.height = height * scale;
      rgba(region, color);
      region.updateOffset();
      return region;
    }
    case TYPE_BOUNDINGBOX: {
      // 3.1 的 boundingbox 只有顶点，没有骨骼权重和颜色。
      // core 的 vertices 是长度为 0 的 Float32Array，官方 SkeletonJson 同样写不进去，
      // 这里保持与 JSON 路径一致的丢弃行为（渲染层不使用 boundingbox）
      const box = attachmentLoader.newBoundingBoxAttachment(skin, name);
      if (box == null) return null;
      // 3.0 的写入器把浮点总数直接写进 varint（3.1 起写的是顶点对数）
      const vertexFloats = readFloatArray(input, minor === 0 ? input.readInt(true) : input.readInt(true) * 2, scale);
      for (let i = 0; i < vertexFloats.length; i++) box.vertices[i] = vertexFloats[i];
      return box;
    }
    case TYPE_MESH: {
      let path = input.readString();
      const color = input.readInt32();
      const verticesLength = input.readInt(true) * 2;
      const uvs = readFloatArray(input, verticesLength, 1);
      const triangles = readShortArray(input);
      const vertices = readFloatArray(input, verticesLength, scale);
      const hullLength = input.readInt(true);
      let edges: number[] | null = null;
      let width = 0;
      let height = 0;
      if (nonessential) {
        edges = readShortArray(input);
        width = input.readFloat();
        height = input.readFloat();
      }
      if (path == null) path = name;
      const mesh = attachmentLoader.newMeshAttachment(skin, name, path);
      if (mesh == null) return null;
      mesh.path = path;
      rgba(mesh, color);
      mesh.vertices = vertices;
      mesh.triangles = triangles;
      mesh.regionUVs = uvs;
      mesh.updateUVs();
      mesh.hullLength = hullLength * 2;
      if (nonessential) {
        mesh.edges = edges;
        mesh.width = width * scale;
        mesh.height = height * scale;
      }
      return mesh;
    }
    case TYPE_LINKED_MESH: {
      let path = input.readString();
      const color = input.readInt32();
      const skinName = input.readString();
      const parent = input.readString()!;
      const inheritFFD = input.readBoolean();
      let width = 0;
      let height = 0;
      if (nonessential) {
        width = input.readFloat();
        height = input.readFloat();
      }
      if (path == null) path = name;
      const mesh = attachmentLoader.newMeshAttachment(skin, name, path);
      if (mesh == null) return null;
      mesh.path = path;
      rgba(mesh, color);
      mesh.inheritFFD = inheritFFD;
      if (nonessential) {
        mesh.width = width * scale;
        mesh.height = height * scale;
      }
      linkedMeshes.push({ mesh, skin: skinName, slotIndex, parent });
      return mesh;
    }
    case TYPE_WEIGHTED_MESH: {
      let path = input.readString();
      const color = input.readInt32();
      const vertexCount = input.readInt(true);
      const uvs = readFloatArray(input, vertexCount * 2, 1);
      const triangles = readShortArray(input);
      const weights: number[] = [];
      const bones: number[] = [];
      for (let i = 0; i < vertexCount; i++) {
        // 3.1 的格式：骨骼数与骨骼索引按 float 存储
        const boneCount = Math.trunc(input.readFloat());
        bones.push(boneCount);
        for (let ii = 0; ii < boneCount; ii++) {
          bones.push(Math.trunc(input.readFloat()));
          weights.push(input.readFloat() * scale);
          weights.push(input.readFloat() * scale);
          weights.push(input.readFloat());
        }
      }
      const hullLength = input.readInt(true);
      let edges: number[] | null = null;
      let width = 0;
      let height = 0;
      if (nonessential) {
        edges = readShortArray(input);
        width = input.readFloat();
        height = input.readFloat();
      }
      if (path == null) path = name;
      const mesh = attachmentLoader.newWeightedMeshAttachment(skin, name, path);
      if (mesh == null) return null;
      mesh.path = path;
      rgba(mesh, color);
      mesh.bones = bones;
      mesh.weights = weights;
      mesh.triangles = triangles;
      mesh.regionUVs = uvs;
      mesh.updateUVs();
      mesh.hullLength = hullLength * 2;
      if (nonessential) {
        mesh.edges = edges;
        mesh.width = width * scale;
        mesh.height = height * scale;
      }
      return mesh;
    }
    case TYPE_WEIGHTED_LINKED_MESH: {
      let path = input.readString();
      const color = input.readInt32();
      const skinName = input.readString();
      const parent = input.readString()!;
      const inheritFFD = input.readBoolean();
      let width = 0;
      let height = 0;
      if (nonessential) {
        width = input.readFloat();
        height = input.readFloat();
      }
      if (path == null) path = name;
      const mesh = attachmentLoader.newWeightedMeshAttachment(skin, name, path);
      if (mesh == null) return null;
      mesh.path = path;
      rgba(mesh, color);
      mesh.inheritFFD = inheritFFD;
      if (nonessential) {
        mesh.width = width * scale;
        mesh.height = height * scale;
      }
      linkedMeshes.push({ mesh, skin: skinName, slotIndex, parent });
      return mesh;
    }
    default:
      throw new RuntimeError('parseInvalid', 'legacy31-attachment-type=' + typeIndex);
  }
}

function readFloatArray(input: BinaryInput31, n: number, scale: number): number[] {
  const array = new Array<number>(n);
  if (scale === 1) {
    for (let i = 0; i < n; i++) array[i] = input.readFloat();
  } else {
    for (let i = 0; i < n; i++) array[i] = input.readFloat() * scale;
  }
  return array;
}

function readShortArray(input: BinaryInput31): number[] {
  const n = input.readInt(true);
  const array = new Array<number>(n);
  for (let i = 0; i < n; i++) array[i] = input.readShort();
  return array;
}

// 丢弃型曲线读取：只消费字节
function skipCurve(input: BinaryInput31): void {
  if (input.readByte() === CURVE_BEZIER) {
    input.readFloat();
    input.readFloat();
    input.readFloat();
    input.readFloat();
  }
}

// 3.1 的曲线设置在 timeline.curves（timeline 本体没有 setStepped/setCurve）
function readCurve(input: BinaryInput31, frameIndex: number, timeline: any): void {
  switch (input.readByte()) {
    case CURVE_STEPPED:
      timeline.curves.setStepped(frameIndex);
      break;
    case CURVE_BEZIER:
      timeline.curves.setCurve(frameIndex, input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat());
      break;
  }
}

function readAnimation(input: BinaryInput31, spine: any, name: string, skeletonData: any, minor: number): void {
  const scale = 1;
  const timelines: any[] = [];
  let duration = 0;
  // 时间线类型编号：3.2 重排过并新增 shear
  const T_ROTATE = minor === 2 ? T32_ROTATE : T30_ROTATE;
  const T_TRANSLATE = minor === 2 ? T32_TRANSLATE : T30_TRANSLATE;
  const T_SCALE = minor === 2 ? T32_SCALE : T30_SCALE;
  const T_SHEAR = T32_SHEAR;
  const T_ATTACHMENT = minor === 2 ? T32_ATTACHMENT : T30_ATTACHMENT;
  const T_COLOR = minor === 2 ? T32_COLOR : T30_COLOR;

  // slot 时间线：只有 color 与 attachment（3.1 没有 twoColor）
  for (let i = 0, n = input.readInt(true); i < n; i++) {
    const slotIndex = input.readInt(true);
    for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
      const timelineType = input.readByte();
      const frameCount = input.readInt(true);
      switch (timelineType) {
        case T_COLOR: {
          const timeline = new spine.ColorTimeline(frameCount);
          timeline.slotIndex = slotIndex;
          for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            const time = input.readFloat();
            const color = input.readInt32();
            const r = ((color & 0xff000000) >>> 24) / 255;
            const g = ((color & 0x00ff0000) >>> 16) / 255;
            const b = ((color & 0x0000ff00) >>> 8) / 255;
            const a = (color & 0x000000ff) / 255;
            timeline.setFrame(frameIndex, time, r, g, b, a);
            if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
          }
          timelines.push(timeline);
          duration = Math.max(duration, timeline.frames[frameCount * 5 - 5]);
          break;
        }
        case T_ATTACHMENT: {
          const timeline = new spine.AttachmentTimeline(frameCount);
          timeline.slotIndex = slotIndex;
          for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            timeline.setFrame(frameIndex, input.readFloat(), input.readString());
          }
          timelines.push(timeline);
          duration = Math.max(duration, timeline.frames[frameCount - 1]);
          break;
        }
      }
    }
  }

  // 骨骼时间线：rotate / translate / scale（3.1 无 shear）
  for (let i = 0, n = input.readInt(true); i < n; i++) {
    const boneIndex = input.readInt(true);
    for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
      const timelineType = input.readByte();
      const frameCount = input.readInt(true);
      switch (timelineType) {
        case T_ROTATE: {
          const timeline = new spine.RotateTimeline(frameCount);
          timeline.boneIndex = boneIndex;
          for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            timeline.setFrame(frameIndex, input.readFloat(), input.readFloat());
            if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
          }
          timelines.push(timeline);
          duration = Math.max(duration, timeline.frames[frameCount * 2 - 2]);
          break;
        }
        case T_SHEAR: {
          // 3.2 新增的剪切时间线：3.1 的 JS core 没有对应类型，按 3×3 布局消费字节并丢弃，
          // 官方 3.1 对 3.2 数据同样无解，剪切不参与 3.1 的骨骼变换合成
          for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            input.readFloat();
            input.readFloat();
            input.readFloat();
            if (frameIndex < frameCount - 1) skipCurve(input);
          }
          break;
        }
        case T_TRANSLATE:
        case T_SCALE: {
          let timeline: any;
          let timelineScale = 1;
          if (timelineType === T_SCALE) timeline = new spine.ScaleTimeline(frameCount);
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
            if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
          }
          timelines.push(timeline);
          duration = Math.max(duration, timeline.frames[frameCount * 3 - 3]);
          break;
        }
      }
    }
  }

  // IK 时间线
  for (let i = 0, n = input.readInt(true); i < n; i++) {
    const index = input.readInt(true);
    const frameCount = input.readInt(true);
    const timeline = new spine.IkConstraintTimeline(frameCount);
    timeline.ikConstraintIndex = index;
    for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
      timeline.setFrame(frameIndex, input.readFloat(), input.readFloat(), input.readByte());
      if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
    }
    timelines.push(timeline);
    duration = Math.max(duration, timeline.frames[frameCount * 3 - 3]);
  }

  // 3.2 在 IK 与 FFD 之间多了 transform 约束时间线（5 浮点/帧）；3.1 的 core 无对应类型，消费后丢弃
  if (minor === 2) {
    for (let i = 0, n = input.readInt(true); i < n; i++) {
      input.readInt(true); // constraint index
      const frameCount = input.readInt(true);
      for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
        input.readFloat(); // time
        input.readFloat(); // rotateMix
        input.readFloat(); // translateMix
        input.readFloat(); // scaleMix
        input.readFloat(); // shearMix
        if (frameIndex < frameCount - 1) skipCurve(input);
      }
    }
  }

  // FFD 时间线（3.1 的叫法，3.8 以后是 deform）
  for (let i = 0, n = input.readInt(true); i < n; i++) {
    const skin = skeletonData.skins[input.readInt(true)];
    for (let ii = 0, nn = input.readInt(true); ii < nn; ii++) {
      const slotIndex = input.readInt(true);
      for (let iii = 0, nnn = input.readInt(true); iii < nnn; iii++) {
        const attachment = skin.getAttachment(slotIndex, input.readString()!);
        // MeshAttachment 没有 bones 字段；WeightedMeshAttachment 有
        const isMesh = attachment.bones == null;
        const vertexCount = isMesh
          ? (attachment.vertices as ArrayLike<number>).length
          : ((attachment.weights as ArrayLike<number>).length / 3) * 2;
        const frameCount = input.readInt(true);
        const timeline = new spine.FfdTimeline(frameCount);
        timeline.slotIndex = slotIndex;
        timeline.attachment = attachment;
        for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
          const time = input.readFloat();
          let vertices: number[];
          const end = input.readInt(true);
          if (end === 0) {
            vertices = isMesh
              ? Array.from(attachment.vertices as ArrayLike<number>)
              : new Array<number>(vertexCount).fill(0);
          } else {
            vertices = new Array<number>(vertexCount).fill(0);
            const start = input.readInt(true);
            const stop = end + start;
            for (let v = start; v < stop; v++) vertices[v] = input.readFloat() * scale;
            if (isMesh) {
              const meshVertices = attachment.vertices as ArrayLike<number>;
              for (let v = 0, vn = vertices.length; v < vn; v++) vertices[v] += meshVertices[v];
            }
          }
          timeline.setFrame(frameIndex, time, vertices);
          if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
        }
        timelines.push(timeline);
        duration = Math.max(duration, timeline.frames[frameCount - 1]);
      }
    }
  }

  // 绘制顺序时间线
  const drawOrderCount = input.readInt(true);
  if (drawOrderCount > 0) {
    const timeline = new spine.DrawOrderTimeline(drawOrderCount);
    const slotCount = skeletonData.slots.length;
    for (let i = 0; i < drawOrderCount; i++) {
      // 3.0 的帧是「偏移数量+偏移对，时间在最末」，3.1 起才是「时间在前」
      const timeLast = minor === 0;
      const time = timeLast ? 0 : input.readFloat();
      const offsetCount = input.readInt(true);
      const drawOrder = new Array<number>(slotCount);
      for (let ii = slotCount - 1; ii >= 0; ii--) drawOrder[ii] = -1;
      const unchanged = new Array<number>(slotCount - offsetCount);
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
      timeline.setFrame(i, timeLast ? input.readFloat() : time, drawOrder);
    }
    timelines.push(timeline);
    duration = Math.max(duration, timeline.frames[drawOrderCount - 1]);
  }

  // 事件时间线
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
      timeline.setFrame(i, event);
    }
    timelines.push(timeline);
    duration = Math.max(duration, timeline.frames[eventCount - 1]);
  }

  skeletonData.animations.push(new spine.Animation(name, timelines, duration));
}
