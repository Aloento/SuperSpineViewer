package com.esotericsoftware.spine37;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.*;
import com.esotericsoftware.spine37.Animation.*;
import com.esotericsoftware.spine37.BoneData.TransformMode;
import com.esotericsoftware.spine37.PathConstraintData.PositionMode;
import com.esotericsoftware.spine37.PathConstraintData.RotateMode;
import com.esotericsoftware.spine37.PathConstraintData.SpacingMode;
import com.esotericsoftware.spine37.SkeletonJson.LinkedMesh;
import com.esotericsoftware.spine37.attachments.*;

import java.io.EOFException;
import java.io.IOException;

public class SkeletonBinary {
    static public final int BONE_ROTATE = 0;
    static public final int BONE_TRANSLATE = 1;
    static public final int BONE_SCALE = 2;
    static public final int BONE_SHEAR = 3;
    static public final int SLOT_ATTACHMENT = 0;
    static public final int SLOT_COLOR = 1;
    static public final int SLOT_TWO_COLOR = 2;
    static public final int PATH_POSITION = 0;
    static public final int PATH_SPACING = 1;
    static public final int PATH_MIX = 2;
    static public final int CURVE_LINEAR = 0;
    static public final int CURVE_STEPPED = 1;
    static public final int CURVE_BEZIER = 2;
    static private final Color tempColor1 = new Color(), tempColor2 = new Color();
    private final AttachmentLoader attachmentLoader;
    private final Array<LinkedMesh> linkedMeshes = new Array<>();
    private float scale = 1;

    public SkeletonBinary(TextureAtlas atlas) {
        attachmentLoader = new AtlasAttachmentLoader(atlas);
    }

    public SkeletonBinary(AttachmentLoader attachmentLoader) {
        if (attachmentLoader == null) throw new IllegalArgumentException("attachmentLoader cannot be null.");
        this.attachmentLoader = attachmentLoader;
    }

    
    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public SkeletonData readSkeletonData(FileHandle file) {
        if (file == null) throw new IllegalArgumentException("file cannot be null.");
        float scale = this.scale;
        SkeletonData skeletonData = new SkeletonData();
        skeletonData.name = file.nameWithoutExtension();

        try (DataInput input = new DataInput(file.read(512)) {
            private char[] chars = new char[32];

            public String readString() throws IOException {
                int byteCount = readInt(true);
                switch (byteCount) {
                    case 0:
                        return null;
                    case 1:
                        return "";
                }
                byteCount--;
                if (chars.length < byteCount) chars = new char[byteCount];
                char[] chars = this.chars;
                int charCount = 0;
                for (int i = 0; i < byteCount; ) {
                    int b = read();
                    switch (b >> 4) {
                        case -1 -> throw new EOFException();
                        case 12, 13 -> {
                            chars[charCount++] = (char) ((b & 0x1F) << 6 | read() & 0x3F);
                            i += 2;
                        }
                        case 14 -> {
                            chars[charCount++] = (char) ((b & 0x0F) << 12 | (read() & 0x3F) << 6 | read() & 0x3F);
                            i += 3;
                        }
                        default -> {
                            chars[charCount++] = (char) b;
                            i++;
                        }
                    }
                }
                return new String(chars, 0, charCount);
            }
        }) {
            skeletonData.hash = input.readString();
            if (skeletonData.hash.isEmpty()) skeletonData.hash = null;
            skeletonData.version = input.readString();
            if (skeletonData.version.isEmpty()) skeletonData.version = null;
            skeletonData.width = input.readFloat();
            skeletonData.height = input.readFloat();

            boolean nonessential = input.readBoolean();

            if (nonessential) {
                skeletonData.fps = input.readFloat();

                skeletonData.imagesPath = input.readString();
                if (skeletonData.imagesPath.isEmpty()) skeletonData.imagesPath = null;

                skeletonData.audioPath = input.readString();
                if (skeletonData.audioPath.isEmpty()) skeletonData.audioPath = null;
            }


            for (int i = 0, n = input.readInt(true); i < n; i++) {
                String name = input.readString();
                BoneData parent = i == 0 ? null : skeletonData.bones.get(input.readInt(true));
                BoneData data = new BoneData(i, name, parent);
                data.rotation = input.readFloat();
                data.x = input.readFloat() * scale;
                data.y = input.readFloat() * scale;
                data.scaleX = input.readFloat();
                data.scaleY = input.readFloat();
                data.shearX = input.readFloat();
                data.shearY = input.readFloat();
                data.length = input.readFloat() * scale;
                data.transformMode = TransformMode.values[input.readInt(true)];
                if (nonessential) Color.rgba8888ToColor(data.color, input.readInt());
                skeletonData.bones.add(data);
            }

            for (int i = 0, n = input.readInt(true); i < n; i++) {
                String slotName = input.readString();
                BoneData boneData = skeletonData.bones.get(input.readInt(true));
                SlotData data = new SlotData(i, slotName, boneData);
                Color.rgba8888ToColor(data.color, input.readInt());

                int darkColor = input.readInt();
                if (darkColor != -1) Color.rgb888ToColor(data.darkColor = new Color(), darkColor);

                data.attachmentName = input.readString();
                data.blendMode = BlendMode.values[input.readInt(true)];
                skeletonData.slots.add(data);
            }

            for (int i = 0, n = input.readInt(true); i < n; i++) {
                IkConstraintData data = new IkConstraintData(input.readString());
                data.order = input.readInt(true);
                for (int ii = 0, nn = input.readInt(true); ii < nn; ii++)
                    data.bones.add(skeletonData.bones.get(input.readInt(true)));
                data.target = skeletonData.bones.get(input.readInt(true));
                data.mix = input.readFloat();
                data.bendDirection = input.readByte();
                data.compress = input.readBoolean();
                data.stretch = input.readBoolean();
                data.uniform = input.readBoolean();
                skeletonData.ikConstraints.add(data);
            }

            for (int i = 0, n = input.readInt(true); i < n; i++) {
                TransformConstraintData data = new TransformConstraintData(input.readString());
                data.order = input.readInt(true);
                for (int ii = 0, nn = input.readInt(true); ii < nn; ii++)
                    data.bones.add(skeletonData.bones.get(input.readInt(true)));
                data.target = skeletonData.bones.get(input.readInt(true));
                data.local = input.readBoolean();
                data.relative = input.readBoolean();
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
                skeletonData.transformConstraints.add(data);
            }

            for (int i = 0, n = input.readInt(true); i < n; i++) {
                PathConstraintData data = new PathConstraintData(input.readString());
                data.order = input.readInt(true);
                for (int ii = 0, nn = input.readInt(true); ii < nn; ii++)
                    data.bones.add(skeletonData.bones.get(input.readInt(true)));
                data.target = skeletonData.slots.get(input.readInt(true));
                data.positionMode = PositionMode.values[input.readInt(true)];
                data.spacingMode = SpacingMode.values[input.readInt(true)];
                data.rotateMode = RotateMode.values[input.readInt(true)];
                data.offsetRotation = input.readFloat();
                data.position = input.readFloat();
                if (data.positionMode == PositionMode.fixed) data.position *= scale;
                data.spacing = input.readFloat();
                if (data.spacingMode == SpacingMode.length || data.spacingMode == SpacingMode.fixed)
                    data.spacing *= scale;
                data.rotateMix = input.readFloat();
                data.translateMix = input.readFloat();
                skeletonData.pathConstraints.add(data);
            }

            Skin defaultSkin = readSkin(input, skeletonData, "default", nonessential);
            if (defaultSkin != null) {
                skeletonData.defaultSkin = defaultSkin;
                skeletonData.skins.add(defaultSkin);
            }

            for (int i = 0, n = input.readInt(true); i < n; i++)
                skeletonData.skins.add(readSkin(input, skeletonData, input.readString(), nonessential));

            for (int i = 0, n = linkedMeshes.size; i < n; i++) {
                LinkedMesh linkedMesh = linkedMeshes.get(i);
                Skin skin = linkedMesh.skin == null ? skeletonData.getDefaultSkin() : skeletonData.findSkin(linkedMesh.skin);
                if (skin == null) throw new SerializationException("Skin not found: " + linkedMesh.skin);
                Attachment parent = skin.getAttachment(linkedMesh.slotIndex, linkedMesh.parent);
                if (parent == null) throw new SerializationException("Parent mesh not found: " + linkedMesh.parent);
                linkedMesh.mesh.setParentMesh((MeshAttachment) parent);
                linkedMesh.mesh.updateUVs();
            }
            linkedMeshes.clear();

            for (int i = 0, n = input.readInt(true); i < n; i++) {
                EventData data = new EventData(input.readString());
                data.intValue = input.readInt(false);
                data.floatValue = input.readFloat();
                data.stringValue = input.readString();
                data.audioPath = input.readString();
                if (data.audioPath != null) {
                    data.volume = input.readFloat();
                    data.balance = input.readFloat();
                }
                skeletonData.events.add(data);
            }

            for (int i = 0, n = input.readInt(true); i < n; i++)
                readAnimation(input, input.readString(), skeletonData);

        } catch (IOException ex) {
            throw new SerializationException("Error reading skeleton file.", ex);
        }
        skeletonData.bones.shrink();
        skeletonData.slots.shrink();
        skeletonData.skins.shrink();
        skeletonData.events.shrink();
        skeletonData.animations.shrink();
        skeletonData.ikConstraints.shrink();
        return skeletonData;
    }

    
    private Skin readSkin(DataInput input, SkeletonData skeletonData, String skinName, boolean nonessential) throws IOException {
        int slotCount = input.readInt(true);
        if (slotCount == 0) return null;
        Skin skin = new Skin(skinName);
        for (int i = 0; i < slotCount; i++) {
            int slotIndex = input.readInt(true);
            for (int ii = 0, nn = input.readInt(true); ii < nn; ii++) {
                String name = input.readString();
                Attachment attachment = readAttachment(input, skeletonData, skin, slotIndex, name, nonessential);
                if (attachment != null) skin.addAttachment(slotIndex, name, attachment);
            }
        }
        return skin;
    }

    private Attachment readAttachment(DataInput input, SkeletonData skeletonData, Skin skin, int slotIndex, String attachmentName,
                                      boolean nonessential) throws IOException {
        float scale = this.scale;

        String name = input.readString();
        if (name == null) name = attachmentName;

        AttachmentType type = AttachmentType.values[input.readByte()];
        switch (type) {
            case region -> {
                String path = input.readString();
                float rotation = input.readFloat();
                float x = input.readFloat();
                float y = input.readFloat();
                float scaleX = input.readFloat();
                float scaleY = input.readFloat();
                float width = input.readFloat();
                float height = input.readFloat();
                int color = input.readInt();

                if (path == null) path = name;
                RegionAttachment region = attachmentLoader.newRegionAttachment(skin, name, path);
                if (region == null) return null;
                region.setPath(path);
                region.setX(x * scale);
                region.setY(y * scale);
                region.setScaleX(scaleX);
                region.setScaleY(scaleY);
                region.setRotation(rotation);
                region.setWidth(width * scale);
                region.setHeight(height * scale);
                Color.rgba8888ToColor(region.getColor(), color);
                region.updateOffset();
                return region;
            }
            case boundingbox -> {
                int vertexCount = input.readInt(true);
                Vertices vertices = readVertices(input, vertexCount);
                int color = nonessential ? input.readInt() : 0;

                BoundingBoxAttachment box = attachmentLoader.newBoundingBoxAttachment(skin, name);
                if (box == null) return null;
                box.setWorldVerticesLength(vertexCount << 1);
                box.setVertices(vertices.vertices);
                box.setBones(vertices.bones);
                if (nonessential) Color.rgba8888ToColor(box.getColor(), color);
                return box;
            }
            case mesh -> {
                String path = input.readString();
                int color = input.readInt();
                int vertexCount = input.readInt(true);
                float[] uvs = readFloatArray(input, vertexCount << 1, 1);
                short[] triangles = readShortArray(input);
                Vertices vertices = readVertices(input, vertexCount);
                int hullLength = input.readInt(true);
                short[] edges = null;
                float width = 0, height = 0;
                if (nonessential) {
                    edges = readShortArray(input);
                    width = input.readFloat();
                    height = input.readFloat();
                }

                if (path == null) path = name;
                MeshAttachment mesh = attachmentLoader.newMeshAttachment(skin, name, path);
                if (mesh == null) return null;
                mesh.setPath(path);
                Color.rgba8888ToColor(mesh.getColor(), color);
                mesh.setBones(vertices.bones);
                mesh.setVertices(vertices.vertices);
                mesh.setWorldVerticesLength(vertexCount << 1);
                mesh.setTriangles(triangles);
                mesh.setRegionUVs(uvs);
                mesh.updateUVs();
                mesh.setHullLength(hullLength << 1);
                if (nonessential) {
                    mesh.setEdges(edges);
                    mesh.setWidth(width * scale);
                    mesh.setHeight(height * scale);
                }
                return mesh;
            }
            case linkedmesh -> {
                String path = input.readString();
                int color = input.readInt();
                String skinName = input.readString();
                String parent = input.readString();
                boolean inheritDeform = input.readBoolean();
                float width = 0, height = 0;
                if (nonessential) {
                    width = input.readFloat();
                    height = input.readFloat();
                }

                if (path == null) path = name;
                MeshAttachment mesh = attachmentLoader.newMeshAttachment(skin, name, path);
                if (mesh == null) return null;
                mesh.setPath(path);
                Color.rgba8888ToColor(mesh.getColor(), color);
                mesh.setInheritDeform(inheritDeform);
                if (nonessential) {
                    mesh.setWidth(width * scale);
                    mesh.setHeight(height * scale);
                }
                linkedMeshes.add(new LinkedMesh(mesh, skinName, slotIndex, parent));
                return mesh;
            }
            case path -> {
                boolean closed = input.readBoolean();
                boolean constantSpeed = input.readBoolean();
                int vertexCount = input.readInt(true);
                Vertices vertices = readVertices(input, vertexCount);
                float[] lengths = new float[vertexCount / 3];
                for (int i = 0, n = lengths.length; i < n; i++)
                    lengths[i] = input.readFloat() * scale;
                int color = nonessential ? input.readInt() : 0;

                PathAttachment path = attachmentLoader.newPathAttachment(skin, name);
                if (path == null) return null;
                path.setClosed(closed);
                path.setConstantSpeed(constantSpeed);
                path.setWorldVerticesLength(vertexCount << 1);
                path.setVertices(vertices.vertices);
                path.setBones(vertices.bones);
                path.setLengths(lengths);
                if (nonessential) Color.rgba8888ToColor(path.getColor(), color);
                return path;
            }
            case point -> {
                float rotation = input.readFloat();
                float x = input.readFloat();
                float y = input.readFloat();
                int color = nonessential ? input.readInt() : 0;

                PointAttachment point = attachmentLoader.newPointAttachment(skin, name);
                if (point == null) return null;
                point.setX(x * scale);
                point.setY(y * scale);
                point.setRotation(rotation);
                if (nonessential) Color.rgba8888ToColor(point.getColor(), color);
                return point;
            }
            case clipping -> {
                int endSlotIndex = input.readInt(true);
                int vertexCount = input.readInt(true);
                Vertices vertices = readVertices(input, vertexCount);
                int color = nonessential ? input.readInt() : 0;

                ClippingAttachment clip = attachmentLoader.newClippingAttachment(skin, name);
                if (clip == null) return null;
                clip.setEndSlot(skeletonData.slots.get(endSlotIndex));
                clip.setWorldVerticesLength(vertexCount << 1);
                clip.setVertices(vertices.vertices);
                clip.setBones(vertices.bones);
                if (nonessential) Color.rgba8888ToColor(clip.getColor(), color);
                return clip;
            }
        }
        return null;
    }

    private Vertices readVertices(DataInput input, int vertexCount) throws IOException {
        int verticesLength = vertexCount << 1;
        Vertices vertices = new Vertices();
        if (!input.readBoolean()) {
            vertices.vertices = readFloatArray(input, verticesLength, scale);
            return vertices;
        }
        FloatArray weights = new FloatArray(verticesLength * 3 * 3);
        IntArray bonesArray = new IntArray(verticesLength * 3);
        for (int i = 0; i < vertexCount; i++) {
            int boneCount = input.readInt(true);
            bonesArray.add(boneCount);
            for (int ii = 0; ii < boneCount; ii++) {
                bonesArray.add(input.readInt(true));
                weights.add(input.readFloat() * scale);
                weights.add(input.readFloat() * scale);
                weights.add(input.readFloat());
            }
        }
        vertices.vertices = weights.toArray();
        vertices.bones = bonesArray.toArray();
        return vertices;
    }

    private float[] readFloatArray(DataInput input, int n, float scale) throws IOException {
        float[] array = new float[n];
        if (scale == 1) {
            for (int i = 0; i < n; i++)
                array[i] = input.readFloat();
        } else {
            for (int i = 0; i < n; i++)
                array[i] = input.readFloat() * scale;
        }
        return array;
    }

    private short[] readShortArray(DataInput input) throws IOException {
        int n = input.readInt(true);
        short[] array = new short[n];
        for (int i = 0; i < n; i++)
            array[i] = input.readShort();
        return array;
    }

    private void readAnimation(DataInput input, String name, SkeletonData skeletonData) {
        Array<Timeline> timelines = new Array<>();
        float scale = this.scale;
        float duration = 0;

        try {
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                int slotIndex = input.readInt(true);
                for (int ii = 0, nn = input.readInt(true); ii < nn; ii++) {
                    int timelineType = input.readByte();
                    int frameCount = input.readInt(true);
                    switch (timelineType) {
                        case SLOT_ATTACHMENT -> {
                            AttachmentTimeline timeline = new AttachmentTimeline(frameCount);
                            timeline.slotIndex = slotIndex;
                            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++)
                                timeline.setFrame(frameIndex, input.readFloat(), input.readString());
                            timelines.add(timeline);
                            duration = Math.max(duration, timeline.getFrames()[frameCount - 1]);
                        }
                        case SLOT_COLOR -> {
                            ColorTimeline timeline = new ColorTimeline(frameCount);
                            timeline.slotIndex = slotIndex;
                            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                                float time = input.readFloat();
                                Color.rgba8888ToColor(tempColor1, input.readInt());
                                timeline.setFrame(frameIndex, time, tempColor1.r, tempColor1.g, tempColor1.b, tempColor1.a);
                                if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, timeline.getFrames()[(frameCount - 1) * ColorTimeline.ENTRIES]);
                        }
                        case SLOT_TWO_COLOR -> {
                            TwoColorTimeline timeline = new TwoColorTimeline(frameCount);
                            timeline.slotIndex = slotIndex;
                            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                                float time = input.readFloat();
                                Color.rgba8888ToColor(tempColor1, input.readInt());
                                Color.rgb888ToColor(tempColor2, input.readInt());
                                timeline.setFrame(frameIndex, time, tempColor1.r, tempColor1.g, tempColor1.b, tempColor1.a, tempColor2.r,
                                        tempColor2.g, tempColor2.b);
                                if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, timeline.getFrames()[(frameCount - 1) * TwoColorTimeline.ENTRIES]);
                        }
                    }
                }
            }

            for (int i = 0, n = input.readInt(true); i < n; i++) {
                int boneIndex = input.readInt(true);
                for (int ii = 0, nn = input.readInt(true); ii < nn; ii++) {
                    int timelineType = input.readByte();
                    int frameCount = input.readInt(true);
                    switch (timelineType) {
                        case BONE_ROTATE -> {
                            RotateTimeline timeline = new RotateTimeline(frameCount);
                            timeline.boneIndex = boneIndex;
                            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                                timeline.setFrame(frameIndex, input.readFloat(), input.readFloat());
                                if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, timeline.getFrames()[(frameCount - 1) * RotateTimeline.ENTRIES]);
                        }
                        case BONE_TRANSLATE, BONE_SCALE, BONE_SHEAR -> {
                            TranslateTimeline timeline;
                            float timelineScale = 1;
                            if (timelineType == BONE_SCALE)
                                timeline = new ScaleTimeline(frameCount);
                            else if (timelineType == BONE_SHEAR)
                                timeline = new ShearTimeline(frameCount);
                            else {
                                timeline = new TranslateTimeline(frameCount);
                                timelineScale = scale;
                            }
                            timeline.boneIndex = boneIndex;
                            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                                timeline.setFrame(frameIndex, input.readFloat(), input.readFloat() * timelineScale,
                                        input.readFloat() * timelineScale);
                                if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, timeline.getFrames()[(frameCount - 1) * TranslateTimeline.ENTRIES]);
                        }
                    }
                }
            }
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                int index = input.readInt(true);
                int frameCount = input.readInt(true);
                IkConstraintTimeline timeline = new IkConstraintTimeline(frameCount);
                timeline.ikConstraintIndex = index;
                for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                    timeline.setFrame(frameIndex, input.readFloat(), input.readFloat(), input.readByte(), input.readBoolean(), input.readBoolean());
                    if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                }
                timelines.add(timeline);
                duration = Math.max(duration, timeline.getFrames()[(frameCount - 1) * IkConstraintTimeline.ENTRIES]);
            }
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                int index = input.readInt(true);
                int frameCount = input.readInt(true);
                TransformConstraintTimeline timeline = new TransformConstraintTimeline(frameCount);
                timeline.transformConstraintIndex = index;
                for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                    timeline.setFrame(frameIndex, input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat(),
                            input.readFloat());
                    if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                }
                timelines.add(timeline);
                duration = Math.max(duration, timeline.getFrames()[(frameCount - 1) * TransformConstraintTimeline.ENTRIES]);
            }
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                int index = input.readInt(true);
                PathConstraintData data = skeletonData.pathConstraints.get(index);
                for (int ii = 0, nn = input.readInt(true); ii < nn; ii++) {
                    int timelineType = input.readByte();
                    int frameCount = input.readInt(true);
                    switch (timelineType) {
                        case PATH_POSITION, PATH_SPACING -> {
                            PathConstraintPositionTimeline timeline;
                            float timelineScale = 1;
                            if (timelineType == PATH_SPACING) {
                                timeline = new PathConstraintSpacingTimeline(frameCount);
                                if (data.spacingMode == SpacingMode.length || data.spacingMode == SpacingMode.fixed)
                                    timelineScale = scale;
                            } else {
                                timeline = new PathConstraintPositionTimeline(frameCount);
                                if (data.positionMode == PositionMode.fixed) timelineScale = scale;
                            }
                            timeline.pathConstraintIndex = index;
                            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                                timeline.setFrame(frameIndex, input.readFloat(), input.readFloat() * timelineScale);
                                if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, timeline.getFrames()[(frameCount - 1) * PathConstraintPositionTimeline.ENTRIES]);
                        }
                        case PATH_MIX -> {
                            PathConstraintMixTimeline timeline = new PathConstraintMixTimeline(frameCount);
                            timeline.pathConstraintIndex = index;
                            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                                timeline.setFrame(frameIndex, input.readFloat(), input.readFloat(), input.readFloat());
                                if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, timeline.getFrames()[(frameCount - 1) * PathConstraintMixTimeline.ENTRIES]);
                        }
                    }
                }
            }
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                Skin skin = skeletonData.skins.get(input.readInt(true));
                for (int ii = 0, nn = input.readInt(true); ii < nn; ii++) {
                    int slotIndex = input.readInt(true);
                    for (int iii = 0, nnn = input.readInt(true); iii < nnn; iii++) {
                        VertexAttachment attachment = (VertexAttachment) skin.getAttachment(slotIndex, input.readString());
                        boolean weighted = attachment.getBones() != null;
                        float[] vertices = attachment.getVertices();
                        int deformLength = weighted ? vertices.length / 3 * 2 : vertices.length;

                        int frameCount = input.readInt(true);
                        DeformTimeline timeline = new DeformTimeline(frameCount);
                        timeline.slotIndex = slotIndex;
                        timeline.attachment = attachment;

                        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                            float time = input.readFloat();
                            float[] deform;
                            int end = input.readInt(true);
                            if (end == 0)
                                deform = weighted ? new float[deformLength] : vertices;
                            else {
                                deform = new float[deformLength];
                                int start = input.readInt(true);
                                end += start;
                                if (scale == 1) {
                                    for (int v = start; v < end; v++)
                                        deform[v] = input.readFloat();
                                } else {
                                    for (int v = start; v < end; v++)
                                        deform[v] = input.readFloat() * scale;
                                }
                                if (!weighted) {
                                    for (int v = 0, vn = deform.length; v < vn; v++)
                                        deform[v] += vertices[v];
                                }
                            }

                            timeline.setFrame(frameIndex, time, deform);
                            if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                        }
                        timelines.add(timeline);
                        duration = Math.max(duration, timeline.getFrames()[frameCount - 1]);
                    }
                }
            }
            int drawOrderCount = input.readInt(true);
            if (drawOrderCount > 0) {
                DrawOrderTimeline timeline = new DrawOrderTimeline(drawOrderCount);
                int slotCount = skeletonData.slots.size;
                for (int i = 0; i < drawOrderCount; i++) {
                    float time = input.readFloat();
                    int offsetCount = input.readInt(true);
                    int[] drawOrder = new int[slotCount];
                    for (int ii = slotCount - 1; ii >= 0; ii--)
                        drawOrder[ii] = -1;
                    int[] unchanged = new int[slotCount - offsetCount];
                    int originalIndex = 0, unchangedIndex = 0;
                    for (int ii = 0; ii < offsetCount; ii++) {
                        int slotIndex = input.readInt(true);

                        while (originalIndex != slotIndex)
                            unchanged[unchangedIndex++] = originalIndex++;

                        drawOrder[originalIndex + input.readInt(true)] = originalIndex++;
                    }

                    while (originalIndex < slotCount)
                        unchanged[unchangedIndex++] = originalIndex++;

                    for (int ii = slotCount - 1; ii >= 0; ii--)
                        if (drawOrder[ii] == -1) drawOrder[ii] = unchanged[--unchangedIndex];
                    timeline.setFrame(i, time, drawOrder);
                }
                timelines.add(timeline);
                duration = Math.max(duration, timeline.getFrames()[drawOrderCount - 1]);
            }
            int eventCount = input.readInt(true);
            if (eventCount > 0) {
                EventTimeline timeline = new EventTimeline(eventCount);
                for (int i = 0; i < eventCount; i++) {
                    float time = input.readFloat();
                    EventData eventData = skeletonData.events.get(input.readInt(true));
                    Event event = new Event(time, eventData);
                    event.intValue = input.readInt(false);
                    event.floatValue = input.readFloat();
                    event.stringValue = input.readBoolean() ? input.readString() : eventData.stringValue;
                    if (event.getData().audioPath != null) {
                        event.volume = input.readFloat();
                        event.balance = input.readFloat();
                    }
                    timeline.setFrame(i, event);
                }
                timelines.add(timeline);
                duration = Math.max(duration, timeline.getFrames()[eventCount - 1]);
            }
        } catch (IOException ex) {
            throw new SerializationException("Error reading skeleton file.", ex);
        }

        timelines.shrink();
        skeletonData.animations.add(new Animation(name, timelines, duration));

    }

    private void readCurve(DataInput input, int frameIndex, CurveTimeline timeline) throws IOException {
        switch (input.readByte()) {
            case CURVE_STEPPED -> timeline.setStepped(frameIndex);
            case CURVE_BEZIER -> setCurve(timeline, frameIndex, input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat());
        }
    }

    void setCurve(CurveTimeline timeline, int frameIndex, float cx1, float cy1, float cx2, float cy2) {
        timeline.setCurve(frameIndex, cx1, cy1, cx2, cy2);
    }

    static class Vertices {
        int[] bones;
        float[] vertices;
    }
}
