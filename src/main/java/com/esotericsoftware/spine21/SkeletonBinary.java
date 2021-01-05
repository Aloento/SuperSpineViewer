package com.esotericsoftware.spine21;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.*;
import com.esotericsoftware.spine21.Animation.*;
import com.esotericsoftware.spine21.attachments.*;

import java.io.IOException;

public class SkeletonBinary {
    static public final int TIMELINE_SCALE = 0;
    static public final int TIMELINE_ROTATE = 1;
    static public final int TIMELINE_TRANSLATE = 2;
    static public final int TIMELINE_ATTACHMENT = 3;
    static public final int TIMELINE_COLOR = 4;
    static public final int TIMELINE_FLIPX = 5;
    static public final int TIMELINE_FLIPY = 6;

    static public final int CURVE_LINEAR = 0;
    static public final int CURVE_STEPPED = 1;
    static public final int CURVE_BEZIER = 2;

    static private final Color tempColor = new Color();

    private final AttachmentLoader attachmentLoader;
    private float scale = 1;

    public SkeletonBinary(TextureAtlas atlas) {
        attachmentLoader = new AtlasAttachmentLoader(atlas);
    }

    public SkeletonBinary(AttachmentLoader attachmentLoader) {
        this.attachmentLoader = attachmentLoader;
    }

    public float getScale() {
        return scale;
    }

    /**
     * Scales the bones, images, and animations as they are loaded.
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    public SkeletonData readSkeletonData(FileHandle file) {
        if (file == null) throw new IllegalArgumentException("file cannot be null.");

        float scale = this.scale;

        SkeletonData skeletonData = new SkeletonData();
        skeletonData.name = file.nameWithoutExtension();

        try (DataInput input = new DataInput(file.read(512))) {
            skeletonData.hash = input.readString();
            if (skeletonData.hash.isEmpty()) skeletonData.hash = null;
            skeletonData.version = input.readString();
            if (skeletonData.version.isEmpty()) skeletonData.version = null;
            skeletonData.width = input.readFloat();
            skeletonData.height = input.readFloat();

            boolean nonessential = input.readBoolean();

            if (nonessential) {
                skeletonData.imagesPath = input.readString();
                if (skeletonData.imagesPath.isEmpty()) skeletonData.imagesPath = null;
            }

            // Bones.
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                String name = input.readString();
                BoneData parent = null;
                int parentIndex = input.readInt(true) - 1;
                if (parentIndex != -1) parent = skeletonData.bones.get(parentIndex);
                BoneData boneData = new BoneData(name, parent);
                boneData.x = input.readFloat() * scale;
                boneData.y = input.readFloat() * scale;
                boneData.scaleX = input.readFloat();
                boneData.scaleY = input.readFloat();
                boneData.rotation = input.readFloat();
                boneData.length = input.readFloat() * scale;
                boneData.flipX = input.readBoolean();
                boneData.flipY = input.readBoolean();
                boneData.inheritScale = input.readBoolean();
                boneData.inheritRotation = input.readBoolean();
                if (nonessential) Color.rgba8888ToColor(boneData.color, input.readInt());
                skeletonData.bones.add(boneData);
            }

            // IK constraints.
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                IkConstraintData ikConstraintData = new IkConstraintData(input.readString());
                for (int ii = 0, nn = input.readInt(true); ii < nn; ii++)
                    ikConstraintData.bones.add(skeletonData.bones.get(input.readInt(true)));
                ikConstraintData.target = skeletonData.bones.get(input.readInt(true));
                ikConstraintData.mix = input.readFloat();
                ikConstraintData.bendDirection = input.readByte();
                skeletonData.ikConstraints.add(ikConstraintData);
            }

            // Slots.
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                String slotName = input.readString();
                BoneData boneData = skeletonData.bones.get(input.readInt(true));
                SlotData slotData = new SlotData(slotName, boneData);
                Color.rgba8888ToColor(slotData.color, input.readInt());
                slotData.attachmentName = input.readString();
                slotData.additiveBlending = input.readBoolean();
                skeletonData.slots.add(slotData);
            }

            // Default skin.
            Skin defaultSkin = readSkin(input, "default", nonessential);
            if (defaultSkin != null) {
                skeletonData.defaultSkin = defaultSkin;
                skeletonData.skins.add(defaultSkin);
            }

            // Skins.
            for (int i = 0, n = input.readInt(true); i < n; i++)
                skeletonData.skins.add(readSkin(input, input.readString(), nonessential));

            // Events.
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                EventData eventData = new EventData(input.readString());
                eventData.intValue = input.readInt(false);
                eventData.floatValue = input.readFloat();
                eventData.stringValue = input.readString();
                skeletonData.events.add(eventData);
            }

            // Animations.
            for (int i = 0, n = input.readInt(true); i < n; i++)
                readAnimation(input.readString(), input, skeletonData);

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

    /**
     * @return May be null.
     */
    private Skin readSkin(DataInput input, String skinName, boolean nonessential) throws IOException {
        int slotCount = input.readInt(true);
        if (slotCount == 0) return null;
        Skin skin = new Skin(skinName);
        for (int i = 0; i < slotCount; i++) {
            int slotIndex = input.readInt(true);
            for (int ii = 0, nn = input.readInt(true); ii < nn; ii++) {
                String name = input.readString();
                skin.addAttachment(slotIndex, name, readAttachment(input, skin, name, nonessential));
            }
        }
        return skin;
    }

    private Attachment readAttachment(DataInput input, Skin skin, String attachmentName, boolean nonessential) throws IOException {
        float scale = this.scale;

        String name = input.readString();
        if (name == null) name = attachmentName;

        switch (AttachmentType.values()[input.readByte()]) {
            case region -> {
                String path = input.readString();
                if (path == null) path = name;
                RegionAttachment region = attachmentLoader.newRegionAttachment(skin, name, path);
                if (region == null) return null;
                region.setPath(path);
                region.setX(input.readFloat() * scale);
                region.setY(input.readFloat() * scale);
                region.setScaleX(input.readFloat());
                region.setScaleY(input.readFloat());
                region.setRotation(input.readFloat());
                region.setWidth(input.readFloat() * scale);
                region.setHeight(input.readFloat() * scale);
                Color.rgba8888ToColor(region.getColor(), input.readInt());
                region.updateOffset();
                return region;
            }
            case boundingbox -> {
                BoundingBoxAttachment box = attachmentLoader.newBoundingBoxAttachment(skin, name);
                if (box == null) return null;
                box.setVertices(readFloatArray(input, scale));
                return box;
            }
            case mesh -> {
                String path = input.readString();
                if (path == null) path = name;
                MeshAttachment mesh = attachmentLoader.newMeshAttachment(skin, name, path);
                if (mesh == null) return null;
                mesh.setPath(path);
                float[] uvs = readFloatArray(input, 1);
                short[] triangles = readShortArray(input);
                float[] vertices = readFloatArray(input, scale);
                mesh.setVertices(vertices);
                mesh.setTriangles(triangles);
                mesh.setRegionUVs(uvs);
                mesh.updateUVs();
                Color.rgba8888ToColor(mesh.getColor(), input.readInt());
                mesh.setHullLength(input.readInt(true) * 2);
                if (nonessential) {
                    mesh.setEdges(readIntArray(input));
                    mesh.setWidth(input.readFloat() * scale);
                    mesh.setHeight(input.readFloat() * scale);
                }
                return mesh;
            }
            case skinnedmesh -> {
                String path = input.readString();
                if (path == null) path = name;
                SkinnedMeshAttachment mesh = attachmentLoader.newSkinnedMeshAttachment(skin, name, path);
                if (mesh == null) return null;
                mesh.setPath(path);
                float[] uvs = readFloatArray(input, 1);
                short[] triangles = readShortArray(input);

                int vertexCount = input.readInt(true);
                FloatArray weights = new FloatArray(uvs.length * 3 * 3);
                IntArray bones = new IntArray(uvs.length * 3);
                for (int i = 0; i < vertexCount; i++) {
                    int boneCount = (int) input.readFloat();
                    bones.add(boneCount);
                    for (int nn = i + boneCount * 4; i < nn; i += 4) {
                        bones.add((int) input.readFloat());
                        weights.add(input.readFloat() * scale);
                        weights.add(input.readFloat() * scale);
                        weights.add(input.readFloat());
                    }
                }
                mesh.setBones(bones.toArray());
                mesh.setWeights(weights.toArray());
                mesh.setTriangles(triangles);
                mesh.setRegionUVs(uvs);
                mesh.updateUVs();
                Color.rgba8888ToColor(mesh.getColor(), input.readInt());
                mesh.setHullLength(input.readInt(true) * 2);
                if (nonessential) {
                    mesh.setEdges(readIntArray(input));
                    mesh.setWidth(input.readFloat() * scale);
                    mesh.setHeight(input.readFloat() * scale);
                }
                return mesh;
            }
        }
        return null;
    }

    private float[] readFloatArray(DataInput input, float scale) throws IOException {
        int n = input.readInt(true);
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

    private int[] readIntArray(DataInput input) throws IOException {
        int n = input.readInt(true);
        int[] array = new int[n];
        for (int i = 0; i < n; i++)
            array[i] = input.readInt(true);
        return array;
    }

    private void readAnimation(String name, DataInput input, SkeletonData skeletonData) {
        Array<Timeline> timelines = new Array();
        float scale = this.scale;
        float duration = 0;

        try {
            // Slot timelines.
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                int slotIndex = input.readInt(true);
                for (int ii = 0, nn = input.readInt(true); ii < nn; ii++) {
                    int timelineType = input.readByte();
                    int frameCount = input.readInt(true);
                    switch (timelineType) {
                        case TIMELINE_COLOR -> {
                            ColorTimeline timeline = new ColorTimeline(frameCount);
                            timeline.slotIndex = slotIndex;
                            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                                float time = input.readFloat();
                                Color.rgba8888ToColor(tempColor, input.readInt());
                                timeline.setFrame(frameIndex, time, tempColor.r, tempColor.g, tempColor.b, tempColor.a);
                                if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, timeline.getFrames()[frameCount * 5 - 5]);
                        }
                        case TIMELINE_ATTACHMENT -> {
                            AttachmentTimeline timeline = new AttachmentTimeline(frameCount);
                            timeline.slotIndex = slotIndex;
                            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++)
                                timeline.setFrame(frameIndex, input.readFloat(), input.readString());
                            timelines.add(timeline);
                            duration = Math.max(duration, timeline.getFrames()[frameCount - 1]);
                        }
                    }
                }
            }

            // Bone timelines.
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                int boneIndex = input.readInt(true);
                for (int ii = 0, nn = input.readInt(true); ii < nn; ii++) {
                    int timelineType = input.readByte();
                    int frameCount = input.readInt(true);
                    switch (timelineType) {
                        case TIMELINE_ROTATE -> {
                            RotateTimeline timeline = new RotateTimeline(frameCount);
                            timeline.boneIndex = boneIndex;
                            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                                timeline.setFrame(frameIndex, input.readFloat(), input.readFloat());
                                if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, timeline.getFrames()[frameCount * 2 - 2]);
                        }
                        case TIMELINE_TRANSLATE, TIMELINE_SCALE -> {
                            TranslateTimeline timeline;
                            float timelineScale = 1;
                            if (timelineType == TIMELINE_SCALE)
                                timeline = new ScaleTimeline(frameCount);
                            else {
                                timeline = new TranslateTimeline(frameCount);
                                timelineScale = scale;
                            }
                            timeline.boneIndex = boneIndex;
                            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                                timeline.setFrame(frameIndex, input.readFloat(), input.readFloat() * timelineScale, input.readFloat()
                                        * timelineScale);
                                if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, timeline.getFrames()[frameCount * 3 - 3]);
                        }
                        case TIMELINE_FLIPX, TIMELINE_FLIPY -> {
                            FlipXTimeline timeline = timelineType == TIMELINE_FLIPX ? new FlipXTimeline(frameCount) : new FlipYTimeline(
                                    frameCount);
                            timeline.boneIndex = boneIndex;
                            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++)
                                timeline.setFrame(frameIndex, input.readFloat(), input.readBoolean());
                            timelines.add(timeline);
                            duration = Math.max(duration, timeline.getFrames()[frameCount * 2 - 2]);
                        }
                    }
                }
            }

            // IK timelines.
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                IkConstraintData ikConstraint = skeletonData.ikConstraints.get(input.readInt(true));
                int frameCount = input.readInt(true);
                IkConstraintTimeline timeline = new IkConstraintTimeline(frameCount);
                timeline.ikConstraintIndex = skeletonData.getIkConstraints().indexOf(ikConstraint, true);
                for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                    timeline.setFrame(frameIndex, input.readFloat(), input.readFloat(), input.readByte());
                    if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                }
                timelines.add(timeline);
                duration = Math.max(duration, timeline.getFrames()[frameCount * 3 - 3]);
            }

            // FFD timelines.
            for (int i = 0, n = input.readInt(true); i < n; i++) {
                Skin skin = skeletonData.skins.get(input.readInt(true));
                for (int ii = 0, nn = input.readInt(true); ii < nn; ii++) {
                    int slotIndex = input.readInt(true);
                    for (int iii = 0, nnn = input.readInt(true); iii < nnn; iii++) {
                        Attachment attachment = skin.getAttachment(slotIndex, input.readString());
                        int frameCount = input.readInt(true);
                        FfdTimeline timeline = new FfdTimeline(frameCount);
                        timeline.slotIndex = slotIndex;
                        timeline.attachment = attachment;
                        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                            float time = input.readFloat();

                            float[] vertices;
                            int vertexCount;
                            if (attachment instanceof MeshAttachment)
                                vertexCount = ((MeshAttachment) attachment).getVertices().length;
                            else
                                vertexCount = ((SkinnedMeshAttachment) attachment).getWeights().length / 3 * 2;

                            int end = input.readInt(true);
                            if (end == 0) {
                                if (attachment instanceof MeshAttachment)
                                    vertices = ((MeshAttachment) attachment).getVertices();
                                else
                                    vertices = new float[vertexCount];
                            } else {
                                vertices = new float[vertexCount];
                                int start = input.readInt(true);
                                end += start;
                                if (scale == 1) {
                                    for (int v = start; v < end; v++)
                                        vertices[v] = input.readFloat();
                                } else {
                                    for (int v = start; v < end; v++)
                                        vertices[v] = input.readFloat() * scale;
                                }
                                if (attachment instanceof MeshAttachment) {
                                    float[] meshVertices = ((MeshAttachment) attachment).getVertices();
                                    for (int v = 0, vn = vertices.length; v < vn; v++)
                                        vertices[v] += meshVertices[v];
                                }
                            }

                            timeline.setFrame(frameIndex, time, vertices);
                            if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline);
                        }
                        timelines.add(timeline);
                        duration = Math.max(duration, timeline.getFrames()[frameCount - 1]);
                    }
                }
            }

            // Draw order timeline.
            int drawOrderCount = input.readInt(true);
            if (drawOrderCount > 0) {
                DrawOrderTimeline timeline = new DrawOrderTimeline(drawOrderCount);
                int slotCount = skeletonData.slots.size;
                for (int i = 0; i < drawOrderCount; i++) {
                    int offsetCount = input.readInt(true);
                    int[] drawOrder = new int[slotCount];
                    for (int ii = slotCount - 1; ii >= 0; ii--)
                        drawOrder[ii] = -1;
                    int[] unchanged = new int[slotCount - offsetCount];
                    int originalIndex = 0, unchangedIndex = 0;
                    for (int ii = 0; ii < offsetCount; ii++) {
                        int slotIndex = input.readInt(true);
                        // Collect unchanged items.
                        while (originalIndex != slotIndex)
                            unchanged[unchangedIndex++] = originalIndex++;
                        // Set changed items.
                        drawOrder[originalIndex + input.readInt(true)] = originalIndex++;
                    }
                    // Collect remaining unchanged items.
                    while (originalIndex < slotCount)
                        unchanged[unchangedIndex++] = originalIndex++;
                    // Fill in unchanged items.
                    for (int ii = slotCount - 1; ii >= 0; ii--)
                        if (drawOrder[ii] == -1) drawOrder[ii] = unchanged[--unchangedIndex];
                    timeline.setFrame(i, input.readFloat(), drawOrder);
                }
                timelines.add(timeline);
                duration = Math.max(duration, timeline.getFrames()[drawOrderCount - 1]);
            }

            // Event timeline.
            int eventCount = input.readInt(true);
            if (eventCount > 0) {
                EventTimeline timeline = new EventTimeline(eventCount);
                for (int i = 0; i < eventCount; i++) {
                    float time = input.readFloat();
                    EventData eventData = skeletonData.events.get(input.readInt(true));
                    Event event = new Event(eventData);
                    event.intValue = input.readInt(false);
                    event.floatValue = input.readFloat();
                    event.stringValue = input.readBoolean() ? input.readString() : eventData.stringValue;
                    timeline.setFrame(i, time, event);
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
}
