package com.esotericsoftware.SpinePreview;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.*;
import com.esotericsoftware.SpinePreview.Animation.*;
import com.esotericsoftware.SpinePreview.BoneData.TransformMode;
import com.esotericsoftware.SpinePreview.PathConstraintData.PositionMode;
import com.esotericsoftware.SpinePreview.PathConstraintData.RotateMode;
import com.esotericsoftware.SpinePreview.PathConstraintData.SpacingMode;
import com.esotericsoftware.SpinePreview.attachments.*;

import static com.esotericsoftware.SpinePreview.utils.SpineUtils.arraycopy;

public class SkeletonJson extends SkeletonLoader {
    // public SkeletonJson(AttachmentLoader attachmentLoader) {
    //     super(attachmentLoader);
    // }

    public SkeletonJson(TextureAtlas atlas) {
        super(atlas);
    }

    protected JsonValue parse(FileHandle file) {
        if (file == null) throw new IllegalArgumentException("file cannot be null.");
        return new JsonReader().parse(file);
    }

    public SkeletonData readSkeletonData(FileHandle file) {
        if (file == null) throw new IllegalArgumentException("file cannot be null.");
        float scale = this.scale;
        SkeletonData skeletonData = new SkeletonData();
        skeletonData.name = file.nameWithoutExtension();
        JsonValue root = parse(file);
        JsonValue skeletonMap = root.get("skeleton");
        if (skeletonMap != null) {
            skeletonData.hash = skeletonMap.getString("hash", null);
            skeletonData.version = skeletonMap.getString("spine", null);
            skeletonData.x = skeletonMap.getFloat("x", 0);
            skeletonData.y = skeletonMap.getFloat("y", 0);
            skeletonData.width = skeletonMap.getFloat("width", 0);
            skeletonData.height = skeletonMap.getFloat("height", 0);
            skeletonData.fps = skeletonMap.getFloat("fps", 30);
            skeletonData.imagesPath = skeletonMap.getString("images", null);
            skeletonData.audioPath = skeletonMap.getString("audio", null);
        }
        for (JsonValue boneMap = root.getChild("bones"); boneMap != null; boneMap = boneMap.next) {
            BoneData parent = null;
            String parentName = boneMap.getString("parent", null);
            if (parentName != null) {
                parent = skeletonData.findBone(parentName);
                if (parent == null) throw new SerializationException("Parent bone not found: " + parentName);
            }
            BoneData data = new BoneData(skeletonData.bones.size, boneMap.getString("name"), parent);
            data.length = boneMap.getFloat("length", 0) * scale;
            data.x = boneMap.getFloat("x", 0) * scale;
            data.y = boneMap.getFloat("y", 0) * scale;
            data.rotation = boneMap.getFloat("rotation", 0);
            data.scaleX = boneMap.getFloat("scaleX", 1);
            data.scaleY = boneMap.getFloat("scaleY", 1);
            data.shearX = boneMap.getFloat("shearX", 0);
            data.shearY = boneMap.getFloat("shearY", 0);
            data.transformMode = TransformMode.valueOf(boneMap.getString("transform", TransformMode.normal.name()));
            data.skinRequired = boneMap.getBoolean("skin", false);
            String color = boneMap.getString("color", null);
            if (color != null) Color.valueOf(color, data.getColor());
            skeletonData.bones.add(data);
        }
        for (JsonValue slotMap = root.getChild("slots"); slotMap != null; slotMap = slotMap.next) {
            String slotName = slotMap.getString("name");
            String boneName = slotMap.getString("bone");
            BoneData boneData = skeletonData.findBone(boneName);
            if (boneData == null) throw new SerializationException("Slot bone not found: " + boneName);
            SlotData data = new SlotData(skeletonData.slots.size, slotName, boneData);
            String color = slotMap.getString("color", null);
            if (color != null) Color.valueOf(color, data.getColor());
            String dark = slotMap.getString("dark", null);
            if (dark != null) data.setDarkColor(Color.valueOf(dark));
            data.attachmentName = slotMap.getString("attachment", null);
            data.blendMode = BlendMode.valueOf(slotMap.getString("blend", BlendMode.normal.name()));
            skeletonData.slots.add(data);
        }
        for (JsonValue constraintMap = root.getChild("ik"); constraintMap != null; constraintMap = constraintMap.next) {
            IkConstraintData data = new IkConstraintData(constraintMap.getString("name"));
            data.order = constraintMap.getInt("order", 0);
            data.skinRequired = constraintMap.getBoolean("skin", false);
            for (JsonValue entry = constraintMap.getChild("bones"); entry != null; entry = entry.next) {
                BoneData bone = skeletonData.findBone(entry.asString());
                if (bone == null) throw new SerializationException("IK bone not found: " + entry);
                data.bones.add(bone);
            }
            String targetName = constraintMap.getString("target");
            data.target = skeletonData.findBone(targetName);
            if (data.target == null) throw new SerializationException("IK target bone not found: " + targetName);
            data.mix = constraintMap.getFloat("mix", 1);
            data.softness = constraintMap.getFloat("softness", 0) * scale;
            data.bendDirection = constraintMap.getBoolean("bendPositive", true) ? 1 : -1;
            data.compress = constraintMap.getBoolean("compress", false);
            data.stretch = constraintMap.getBoolean("stretch", false);
            data.uniform = constraintMap.getBoolean("uniform", false);
            skeletonData.ikConstraints.add(data);
        }
        for (JsonValue constraintMap = root.getChild("transform"); constraintMap != null; constraintMap = constraintMap.next) {
            TransformConstraintData data = new TransformConstraintData(constraintMap.getString("name"));
            data.order = constraintMap.getInt("order", 0);
            data.skinRequired = constraintMap.getBoolean("skin", false);
            for (JsonValue entry = constraintMap.getChild("bones"); entry != null; entry = entry.next) {
                BoneData bone = skeletonData.findBone(entry.asString());
                if (bone == null) throw new SerializationException("Transform constraint bone not found: " + entry);
                data.bones.add(bone);
            }
            String targetName = constraintMap.getString("target");
            data.target = skeletonData.findBone(targetName);
            if (data.target == null)
                throw new SerializationException("Transform constraint target bone not found: " + targetName);
            data.local = constraintMap.getBoolean("local", false);
            data.relative = constraintMap.getBoolean("relative", false);
            data.offsetRotation = constraintMap.getFloat("rotation", 0);
            data.offsetX = constraintMap.getFloat("x", 0) * scale;
            data.offsetY = constraintMap.getFloat("y", 0) * scale;
            data.offsetScaleX = constraintMap.getFloat("scaleX", 0);
            data.offsetScaleY = constraintMap.getFloat("scaleY", 0);
            data.offsetShearY = constraintMap.getFloat("shearY", 0);
            data.mixRotate = constraintMap.getFloat("mixRotate", 1);
            data.mixX = constraintMap.getFloat("mixX", 1);
            data.mixY = constraintMap.getFloat("mixY", data.mixX);
            data.mixScaleX = constraintMap.getFloat("mixScaleX", 1);
            data.mixScaleY = constraintMap.getFloat("mixScaleY", data.mixScaleX);
            data.mixShearY = constraintMap.getFloat("mixShearY", 1);
            skeletonData.transformConstraints.add(data);
        }
        for (JsonValue constraintMap = root.getChild("outPath"); constraintMap != null; constraintMap = constraintMap.next) {
            PathConstraintData data = new PathConstraintData(constraintMap.getString("name"));
            data.order = constraintMap.getInt("order", 0);
            data.skinRequired = constraintMap.getBoolean("skin", false);
            for (JsonValue entry = constraintMap.getChild("bones"); entry != null; entry = entry.next) {
                BoneData bone = skeletonData.findBone(entry.asString());
                if (bone == null) throw new SerializationException("Path bone not found: " + entry);
                data.bones.add(bone);
            }
            String targetName = constraintMap.getString("target");
            data.target = skeletonData.findSlot(targetName);
            if (data.target == null) throw new SerializationException("Path target slot not found: " + targetName);
            data.positionMode = PositionMode.valueOf(constraintMap.getString("positionMode", "percent"));
            data.spacingMode = SpacingMode.valueOf(constraintMap.getString("spacingMode", "length"));
            data.rotateMode = RotateMode.valueOf(constraintMap.getString("rotateMode", "tangent"));
            data.offsetRotation = constraintMap.getFloat("rotation", 0);
            data.position = constraintMap.getFloat("position", 0);
            if (data.positionMode == PositionMode.fixed) data.position *= scale;
            data.spacing = constraintMap.getFloat("spacing", 0);
            if (data.spacingMode == SpacingMode.length || data.spacingMode == SpacingMode.fixed) data.spacing *= scale;
            data.mixRotate = constraintMap.getFloat("mixRotate", 1);
            data.mixX = constraintMap.getFloat("mixX", 1);
            data.mixY = constraintMap.getFloat("mixY", 1);
            skeletonData.pathConstraints.add(data);
        }
        for (JsonValue skinMap = root.getChild("skins"); skinMap != null; skinMap = skinMap.next) {
            Skin skin = new Skin(skinMap.getString("name"));
            for (JsonValue entry = skinMap.getChild("bones"); entry != null; entry = entry.next) {
                BoneData bone = skeletonData.findBone(entry.asString());
                if (bone == null) throw new SerializationException("Skin bone not found: " + entry);
                skin.bones.add(bone);
            }
            skin.bones.shrink();
            for (JsonValue entry = skinMap.getChild("ik"); entry != null; entry = entry.next) {
                IkConstraintData constraint = skeletonData.findIkConstraint(entry.asString());
                if (constraint == null) throw new SerializationException("Skin IK constraint not found: " + entry);
                skin.constraints.add(constraint);
            }
            for (JsonValue entry = skinMap.getChild("transform"); entry != null; entry = entry.next) {
                TransformConstraintData constraint = skeletonData.findTransformConstraint(entry.asString());
                if (constraint == null)
                    throw new SerializationException("Skin transform constraint not found: " + entry);
                skin.constraints.add(constraint);
            }
            for (JsonValue entry = skinMap.getChild("outPath"); entry != null; entry = entry.next) {
                PathConstraintData constraint = skeletonData.findPathConstraint(entry.asString());
                if (constraint == null) throw new SerializationException("Skin outPath constraint not found: " + entry);
                skin.constraints.add(constraint);
            }
            skin.constraints.shrink();
            for (JsonValue slotEntry = skinMap.getChild("attachments"); slotEntry != null; slotEntry = slotEntry.next) {
                SlotData slot = skeletonData.findSlot(slotEntry.name);
                if (slot == null) throw new SerializationException("Slot not found: " + slotEntry.name);
                for (JsonValue entry = slotEntry.child; entry != null; entry = entry.next) {
                    try {
                        Attachment attachment = readAttachment(entry, skin, slot.index, entry.name, skeletonData);
                        if (attachment != null) skin.setAttachment(slot.index, entry.name, attachment);
                    } catch (Throwable ex) {
                        throw new SerializationException("Error reading attachment: " + entry.name + ", skin: " + skin, ex);
                    }
                }
            }
            skeletonData.skins.add(skin);
            if (skin.name.equals("default")) skeletonData.defaultSkin = skin;
        }
        Object[] items = linkedMeshes.items;
        for (int i = 0, n = linkedMeshes.size; i < n; i++) {
            LinkedMesh linkedMesh = (LinkedMesh) items[i];
            Skin skin = linkedMesh.skin == null ? skeletonData.getDefaultSkin() : skeletonData.findSkin(linkedMesh.skin);
            if (skin == null) throw new SerializationException("Skin not found: " + linkedMesh.skin);
            Attachment parent = skin.getAttachment(linkedMesh.slotIndex, linkedMesh.parent);
            if (parent == null) throw new SerializationException("Parent mesh not found: " + linkedMesh.parent);
            linkedMesh.mesh.setDeformAttachment(linkedMesh.inheritDeform ? (VertexAttachment) parent : linkedMesh.mesh);
            linkedMesh.mesh.setParentMesh((MeshAttachment) parent);
            linkedMesh.mesh.updateUVs();
        }
        linkedMeshes.clear();
        for (JsonValue eventMap = root.getChild("events"); eventMap != null; eventMap = eventMap.next) {
            EventData data = new EventData(eventMap.name);
            data.intValue = eventMap.getInt("int", 0);
            data.floatValue = eventMap.getFloat("float", 0f);
            data.stringValue = eventMap.getString("string", "");
            data.audioPath = eventMap.getString("audio", null);
            if (data.audioPath != null) {
                data.volume = eventMap.getFloat("volume", 1);
                data.balance = eventMap.getFloat("balance", 0);
            }
            skeletonData.events.add(data);
        }
        for (JsonValue animationMap = root.getChild("animations"); animationMap != null; animationMap = animationMap.next) {
            try {
                readAnimation(animationMap, animationMap.name, skeletonData);
            } catch (Throwable ex) {
                throw new SerializationException("Error reading animation: " + animationMap.name, ex);
            }
        }
        skeletonData.bones.shrink();
        skeletonData.slots.shrink();
        skeletonData.skins.shrink();
        skeletonData.events.shrink();
        skeletonData.animations.shrink();
        skeletonData.ikConstraints.shrink();
        return skeletonData;
    }

    private Attachment readAttachment(JsonValue map, Skin skin, int slotIndex, String name, SkeletonData skeletonData) {
        float scale = this.scale;
        name = map.getString("name", name);
        switch (AttachmentType.valueOf(map.getString("type", AttachmentType.region.name()))) {
            case region -> {
                String path = map.getString("outPath", name);
                RegionAttachment region = attachmentLoader.newRegionAttachment(skin, name, path);
                if (region == null) return null;
                region.setPath(path);
                region.setX(map.getFloat("x", 0) * scale);
                region.setY(map.getFloat("y", 0) * scale);
                region.setScaleX(map.getFloat("scaleX", 1));
                region.setScaleY(map.getFloat("scaleY", 1));
                region.setRotation(map.getFloat("rotation", 0));
                region.setWidth(map.getFloat("width") * scale);
                region.setHeight(map.getFloat("height") * scale);
                String color = map.getString("color", null);
                if (color != null) Color.valueOf(color, region.getColor());
                region.updateOffset();
                return region;
            }
            case boundingbox -> {
                BoundingBoxAttachment box = attachmentLoader.newBoundingBoxAttachment(skin, name);
                if (box == null) return null;
                readVertices(map, box, map.getInt("vertexCount") << 1);
                String color = map.getString("color", null);
                if (color != null) Color.valueOf(color, box.getColor());
                return box;
            }
            case mesh, linkedmesh -> {
                String path = map.getString("outPath", name);
                MeshAttachment mesh = attachmentLoader.newMeshAttachment(skin, name, path);
                if (mesh == null) return null;
                mesh.setPath(path);
                String color = map.getString("color", null);
                if (color != null) Color.valueOf(color, mesh.getColor());
                mesh.setWidth(map.getFloat("width", 0) * scale);
                mesh.setHeight(map.getFloat("height", 0) * scale);
                String parent = map.getString("parent", null);
                if (parent != null) {
                    linkedMeshes
                            .add(new LinkedMesh(mesh, map.getString("skin", null), slotIndex, parent, map.getBoolean("deform", true)));
                    return mesh;
                }
                float[] uvs = map.require("uvs").asFloatArray();
                readVertices(map, mesh, uvs.length);
                mesh.setTriangles(map.require("triangles").asShortArray());
                mesh.setRegionUVs(uvs);
                mesh.updateUVs();
                if (map.has("hull")) mesh.setHullLength(map.require("hull").asInt() << 1);
                if (map.has("edges")) mesh.setEdges(map.require("edges").asShortArray());
                return mesh;
            }
            case path -> {
                PathAttachment path = attachmentLoader.newPathAttachment(skin, name);
                if (path == null) return null;
                path.setClosed(map.getBoolean("closed", false));
                path.setConstantSpeed(map.getBoolean("constantSpeed", true));
                int vertexCount = map.getInt("vertexCount");
                readVertices(map, path, vertexCount << 1);
                float[] lengths = new float[vertexCount / 3];
                int i = 0;
                for (JsonValue curves = map.require("lengths").child; curves != null; curves = curves.next)
                    lengths[i++] = curves.asFloat() * scale;
                path.setLengths(lengths);
                String color = map.getString("color", null);
                if (color != null) Color.valueOf(color, path.getColor());
                return path;
            }
            case point -> {
                PointAttachment point = attachmentLoader.newPointAttachment(skin, name);
                if (point == null) return null;
                point.setX(map.getFloat("x", 0) * scale);
                point.setY(map.getFloat("y", 0) * scale);
                point.setRotation(map.getFloat("rotation", 0));
                String color = map.getString("color", null);
                if (color != null) Color.valueOf(color, point.getColor());
                return point;
            }
            case clipping -> {
                ClippingAttachment clip = attachmentLoader.newClippingAttachment(skin, name);
                if (clip == null) return null;
                String end = map.getString("end", null);
                if (end != null) {
                    SlotData slot = skeletonData.findSlot(end);
                    if (slot == null) throw new SerializationException("Clipping end slot not found: " + end);
                    clip.setEndSlot(slot);
                }
                readVertices(map, clip, map.getInt("vertexCount") << 1);
                String color = map.getString("color", null);
                if (color != null) Color.valueOf(color, clip.getColor());
                return clip;
            }
        }
        return null;
    }

    private void readVertices(JsonValue map, VertexAttachment attachment, int verticesLength) {
        attachment.setWorldVerticesLength(verticesLength);
        float[] vertices = map.require("vertices").asFloatArray();
        if (verticesLength == vertices.length) {
            if (scale != 1) {
                for (int i = 0, n = vertices.length; i < n; i++)
                    vertices[i] *= scale;
            }
            attachment.setVertices(vertices);
            return;
        }
        FloatArray weights = new FloatArray(verticesLength * 3 * 3);
        IntArray bones = new IntArray(verticesLength * 3);
        for (int i = 0, n = vertices.length; i < n; ) {
            int boneCount = (int) vertices[i++];
            bones.add(boneCount);
            for (int nn = i + (boneCount << 2); i < nn; i += 4) {
                bones.add((int) vertices[i]);
                weights.add(vertices[i + 1] * scale);
                weights.add(vertices[i + 2] * scale);
                weights.add(vertices[i + 3]);
            }
        }
        attachment.setBones(bones.toArray());
        attachment.setVertices(weights.toArray());
    }

    private void readAnimation(JsonValue map, String name, SkeletonData skeletonData) {
        float scale = this.scale;
        Array<Timeline> timelines = new Array<>();
        for (JsonValue slotMap = map.getChild("slots"); slotMap != null; slotMap = slotMap.next) {
            SlotData slot = skeletonData.findSlot(slotMap.name);
            if (slot == null) throw new SerializationException("Slot not found: " + slotMap.name);
            for (JsonValue timelineMap = slotMap.child; timelineMap != null; timelineMap = timelineMap.next) {
                JsonValue keyMap = timelineMap.child;
                if (keyMap == null) continue;
                String timelineName = timelineMap.name;
                switch (timelineName) {
                    case "attachment" -> {
                        AttachmentTimeline timeline = new AttachmentTimeline(timelineMap.size, slot.index);
                        for (int frame = 0; keyMap != null; keyMap = keyMap.next, frame++)
                            timeline.setFrame(frame, keyMap.getFloat("time", 0), keyMap.getString("name"));
                        timelines.add(timeline);
                    }
                    case "rgba" -> {
                        RGBATimeline timeline = new RGBATimeline(timelineMap.size, timelineMap.size << 2, slot.index);
                        float time = keyMap.getFloat("time", 0);
                        String color = keyMap.getString("color");
                        float r = Integer.parseInt(color.substring(0, 2), 16) / 255f;
                        float g = Integer.parseInt(color.substring(2, 4), 16) / 255f;
                        float b = Integer.parseInt(color.substring(4, 6), 16) / 255f;
                        float a = Integer.parseInt(color.substring(6, 8), 16) / 255f;
                        for (int frame = 0, bezier = 0; ; frame++) {
                            timeline.setFrame(frame, time, r, g, b, a);
                            JsonValue nextMap = keyMap.next;
                            if (nextMap == null) {
                                timeline.shrink(bezier);
                                break;
                            }
                            float time2 = nextMap.getFloat("time", 0);
                            color = nextMap.getString("color");
                            float nr = Integer.parseInt(color.substring(0, 2), 16) / 255f;
                            float ng = Integer.parseInt(color.substring(2, 4), 16) / 255f;
                            float nb = Integer.parseInt(color.substring(4, 6), 16) / 255f;
                            float na = Integer.parseInt(color.substring(6, 8), 16) / 255f;
                            JsonValue curve = keyMap.get("curve");
                            if (curve != null) {
                                bezier = readCurve(curve, timeline, bezier, frame, 0, time, time2, r, nr, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 1, time, time2, g, ng, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 2, time, time2, b, nb, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 3, time, time2, a, na, 1);
                            }
                            time = time2;
                            r = nr;
                            g = ng;
                            b = nb;
                            a = na;
                            keyMap = nextMap;
                        }
                        timelines.add(timeline);
                    }
                    case "rgb" -> {
                        RGBTimeline timeline = new RGBTimeline(timelineMap.size, timelineMap.size * 3, slot.index);
                        float time = keyMap.getFloat("time", 0);
                        String color = keyMap.getString("color");
                        float r = Integer.parseInt(color.substring(0, 2), 16) / 255f;
                        float g = Integer.parseInt(color.substring(2, 4), 16) / 255f;
                        float b = Integer.parseInt(color.substring(4, 6), 16) / 255f;
                        for (int frame = 0, bezier = 0; ; frame++) {
                            timeline.setFrame(frame, time, r, g, b);
                            JsonValue nextMap = keyMap.next;
                            if (nextMap == null) {
                                timeline.shrink(bezier);
                                break;
                            }
                            float time2 = nextMap.getFloat("time", 0);
                            color = nextMap.getString("color");
                            float nr = Integer.parseInt(color.substring(0, 2), 16) / 255f;
                            float ng = Integer.parseInt(color.substring(2, 4), 16) / 255f;
                            float nb = Integer.parseInt(color.substring(4, 6), 16) / 255f;
                            JsonValue curve = keyMap.get("curve");
                            if (curve != null) {
                                bezier = readCurve(curve, timeline, bezier, frame, 0, time, time2, r, nr, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 1, time, time2, g, ng, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 2, time, time2, b, nb, 1);
                            }
                            time = time2;
                            r = nr;
                            g = ng;
                            b = nb;
                            keyMap = nextMap;
                        }
                        timelines.add(timeline);
                    }
                    case "alpha" -> timelines.add(readTimeline(keyMap, new AlphaTimeline(timelineMap.size, timelineMap.size, slot.index), 0, 1));
                    case "rgba2" -> {
                        RGBA2Timeline timeline = new RGBA2Timeline(timelineMap.size, timelineMap.size * 7, slot.index);
                        float time = keyMap.getFloat("time", 0);
                        String color = keyMap.getString("light");
                        float r = Integer.parseInt(color.substring(0, 2), 16) / 255f;
                        float g = Integer.parseInt(color.substring(2, 4), 16) / 255f;
                        float b = Integer.parseInt(color.substring(4, 6), 16) / 255f;
                        float a = Integer.parseInt(color.substring(6, 8), 16) / 255f;
                        color = keyMap.getString("dark");
                        float r2 = Integer.parseInt(color.substring(0, 2), 16) / 255f;
                        float g2 = Integer.parseInt(color.substring(2, 4), 16) / 255f;
                        float b2 = Integer.parseInt(color.substring(4, 6), 16) / 255f;
                        for (int frame = 0, bezier = 0; ; frame++) {
                            timeline.setFrame(frame, time, r, g, b, a, r2, g2, b2);
                            JsonValue nextMap = keyMap.next;
                            if (nextMap == null) {
                                timeline.shrink(bezier);
                                break;
                            }
                            float time2 = nextMap.getFloat("time", 0);
                            color = nextMap.getString("light");
                            float nr = Integer.parseInt(color.substring(0, 2), 16) / 255f;
                            float ng = Integer.parseInt(color.substring(2, 4), 16) / 255f;
                            float nb = Integer.parseInt(color.substring(4, 6), 16) / 255f;
                            float na = Integer.parseInt(color.substring(6, 8), 16) / 255f;
                            color = nextMap.getString("dark");
                            float nr2 = Integer.parseInt(color.substring(0, 2), 16) / 255f;
                            float ng2 = Integer.parseInt(color.substring(2, 4), 16) / 255f;
                            float nb2 = Integer.parseInt(color.substring(4, 6), 16) / 255f;
                            JsonValue curve = keyMap.get("curve");
                            if (curve != null) {
                                bezier = readCurve(curve, timeline, bezier, frame, 0, time, time2, r, nr, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 1, time, time2, g, ng, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 2, time, time2, b, nb, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 3, time, time2, a, na, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 4, time, time2, r2, nr2, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 5, time, time2, g2, ng2, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 6, time, time2, b2, nb2, 1);
                            }
                            time = time2;
                            r = nr;
                            g = ng;
                            b = nb;
                            a = na;
                            r2 = nr2;
                            g2 = ng2;
                            b2 = nb2;
                            keyMap = nextMap;
                        }
                        timelines.add(timeline);
                    }
                    case "rgb2" -> {
                        RGB2Timeline timeline = new RGB2Timeline(timelineMap.size, timelineMap.size * 6, slot.index);
                        float time = keyMap.getFloat("time", 0);
                        String color = keyMap.getString("light");
                        float r = Integer.parseInt(color.substring(0, 2), 16) / 255f;
                        float g = Integer.parseInt(color.substring(2, 4), 16) / 255f;
                        float b = Integer.parseInt(color.substring(4, 6), 16) / 255f;
                        color = keyMap.getString("dark");
                        float r2 = Integer.parseInt(color.substring(0, 2), 16) / 255f;
                        float g2 = Integer.parseInt(color.substring(2, 4), 16) / 255f;
                        float b2 = Integer.parseInt(color.substring(4, 6), 16) / 255f;
                        for (int frame = 0, bezier = 0; ; frame++) {
                            timeline.setFrame(frame, time, r, g, b, r2, g2, b2);
                            JsonValue nextMap = keyMap.next;
                            if (nextMap == null) {
                                timeline.shrink(bezier);
                                break;
                            }
                            float time2 = nextMap.getFloat("time", 0);
                            color = nextMap.getString("light");
                            float nr = Integer.parseInt(color.substring(0, 2), 16) / 255f;
                            float ng = Integer.parseInt(color.substring(2, 4), 16) / 255f;
                            float nb = Integer.parseInt(color.substring(4, 6), 16) / 255f;
                            color = nextMap.getString("dark");
                            float nr2 = Integer.parseInt(color.substring(0, 2), 16) / 255f;
                            float ng2 = Integer.parseInt(color.substring(2, 4), 16) / 255f;
                            float nb2 = Integer.parseInt(color.substring(4, 6), 16) / 255f;
                            JsonValue curve = keyMap.get("curve");
                            if (curve != null) {
                                bezier = readCurve(curve, timeline, bezier, frame, 0, time, time2, r, nr, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 1, time, time2, g, ng, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 2, time, time2, b, nb, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 3, time, time2, r2, nr2, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 4, time, time2, g2, ng2, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 5, time, time2, b2, nb2, 1);
                            }
                            time = time2;
                            r = nr;
                            g = ng;
                            b = nb;
                            r2 = nr2;
                            g2 = ng2;
                            b2 = nb2;
                            keyMap = nextMap;
                        }
                        timelines.add(timeline);
                    }
                    default -> throw new RuntimeException("Invalid timeline type for a slot: " + timelineName + " (" + slotMap.name + ")");
                }
            }
        }
        for (JsonValue boneMap = map.getChild("bones"); boneMap != null; boneMap = boneMap.next) {
            BoneData bone = skeletonData.findBone(boneMap.name);
            if (bone == null) throw new SerializationException("Bone not found: " + boneMap.name);
            for (JsonValue timelineMap = boneMap.child; timelineMap != null; timelineMap = timelineMap.next) {
                JsonValue keyMap = timelineMap.child;
                if (keyMap == null) continue;
                String timelineName = timelineMap.name;
                switch (timelineName) {
                    case "rotate" -> timelines.add(readTimeline(keyMap, new RotateTimeline(timelineMap.size, timelineMap.size, bone.index), 0, 1));
                    case "translate" -> {
                        TranslateTimeline timeline = new TranslateTimeline(timelineMap.size, timelineMap.size << 1, bone.index);
                        timelines.add(readTimeline(keyMap, timeline, 0, scale));
                    }
                    case "translatex" -> timelines
                            .add(readTimeline(keyMap, new TranslateXTimeline(timelineMap.size, timelineMap.size, bone.index), 0, scale));
                    case "translatey" -> timelines
                            .add(readTimeline(keyMap, new TranslateYTimeline(timelineMap.size, timelineMap.size, bone.index), 0, scale));
                    case "scale" -> {
                        ScaleTimeline timeline = new ScaleTimeline(timelineMap.size, timelineMap.size << 1, bone.index);
                        timelines.add(readTimeline(keyMap, timeline, 1, 1));
                    }
                    case "scalex" -> timelines.add(readTimeline(keyMap, new ScaleXTimeline(timelineMap.size, timelineMap.size, bone.index), 1, 1));
                    case "scaley" -> timelines.add(readTimeline(keyMap, new ScaleYTimeline(timelineMap.size, timelineMap.size, bone.index), 1, 1));
                    case "shear" -> {
                        ShearTimeline timeline = new ShearTimeline(timelineMap.size, timelineMap.size << 1, bone.index);
                        timelines.add(readTimeline(keyMap, timeline, 0, 1));
                    }
                    case "shearx" -> timelines.add(readTimeline(keyMap, new ShearXTimeline(timelineMap.size, timelineMap.size, bone.index), 0, 1));
                    case "sheary" -> timelines.add(readTimeline(keyMap, new ShearYTimeline(timelineMap.size, timelineMap.size, bone.index), 0, 1));
                    default -> throw new RuntimeException("Invalid timeline type for a bone: " + timelineName + " (" + boneMap.name + ")");
                }
            }
        }
        for (JsonValue timelineMap = map.getChild("ik"); timelineMap != null; timelineMap = timelineMap.next) {
            JsonValue keyMap = timelineMap.child;
            if (keyMap == null) continue;
            IkConstraintData constraint = skeletonData.findIkConstraint(timelineMap.name);
            IkConstraintTimeline timeline = new IkConstraintTimeline(timelineMap.size, timelineMap.size << 1,
                    skeletonData.getIkConstraints().indexOf(constraint, true));
            float time = keyMap.getFloat("time", 0);
            float mix = keyMap.getFloat("mix", 1), softness = keyMap.getFloat("softness", 0) * scale;
            for (int frame = 0, bezier = 0; ; frame++) {
                timeline.setFrame(frame, time, mix, softness, keyMap.getBoolean("bendPositive", true) ? 1 : -1,
                        keyMap.getBoolean("compress", false), keyMap.getBoolean("stretch", false));
                JsonValue nextMap = keyMap.next;
                if (nextMap == null) {
                    timeline.shrink(bezier);
                    break;
                }
                float time2 = nextMap.getFloat("time", 0);
                float mix2 = nextMap.getFloat("mix", 1), softness2 = nextMap.getFloat("softness", 0) * scale;
                JsonValue curve = keyMap.get("curve");
                if (curve != null) {
                    bezier = readCurve(curve, timeline, bezier, frame, 0, time, time2, mix, mix2, 1);
                    bezier = readCurve(curve, timeline, bezier, frame, 1, time, time2, softness, softness2, scale);
                }
                time = time2;
                mix = mix2;
                softness = softness2;
                keyMap = nextMap;
            }
            timelines.add(timeline);
        }
        for (JsonValue timelineMap = map.getChild("transform"); timelineMap != null; timelineMap = timelineMap.next) {
            JsonValue keyMap = timelineMap.child;
            if (keyMap == null) continue;
            TransformConstraintData constraint = skeletonData.findTransformConstraint(timelineMap.name);
            TransformConstraintTimeline timeline = new TransformConstraintTimeline(timelineMap.size, timelineMap.size << 2,
                    skeletonData.getTransformConstraints().indexOf(constraint, true));
            float time = keyMap.getFloat("time", 0);
            float mixRotate = keyMap.getFloat("mixRotate", 1), mixShearY = keyMap.getFloat("mixShearY", 1);
            float mixX = keyMap.getFloat("mixX", 1), mixY = keyMap.getFloat("mixY", mixX);
            float mixScaleX = keyMap.getFloat("mixScaleX", 1), mixScaleY = keyMap.getFloat("mixScaleY", mixScaleX);
            for (int frame = 0, bezier = 0; ; frame++) {
                timeline.setFrame(frame, time, mixRotate, mixX, mixY, mixScaleX, mixScaleY, mixShearY);
                JsonValue nextMap = keyMap.next;
                if (nextMap == null) {
                    timeline.shrink(bezier);
                    break;
                }
                float time2 = nextMap.getFloat("time", 0);
                float mixRotate2 = nextMap.getFloat("mixRotate", 1), mixShearY2 = nextMap.getFloat("mixShearY", 1);
                float mixX2 = nextMap.getFloat("mixX", 1), mixY2 = nextMap.getFloat("mixY", mixX2);
                float mixScaleX2 = nextMap.getFloat("mixScaleX", 1), mixScaleY2 = nextMap.getFloat("mixScaleY", mixScaleX2);
                JsonValue curve = keyMap.get("curve");
                if (curve != null) {
                    bezier = readCurve(curve, timeline, bezier, frame, 0, time, time2, mixRotate, mixRotate2, 1);
                    bezier = readCurve(curve, timeline, bezier, frame, 1, time, time2, mixX, mixX2, 1);
                    bezier = readCurve(curve, timeline, bezier, frame, 2, time, time2, mixY, mixY2, 1);
                    bezier = readCurve(curve, timeline, bezier, frame, 3, time, time2, mixScaleX, mixScaleX2, 1);
                    bezier = readCurve(curve, timeline, bezier, frame, 4, time, time2, mixScaleY, mixScaleY2, 1);
                    bezier = readCurve(curve, timeline, bezier, frame, 5, time, time2, mixShearY, mixShearY2, 1);
                }
                time = time2;
                mixRotate = mixRotate2;
                mixX = mixX2;
                mixY = mixY2;
                mixScaleY = mixScaleY2;
                mixScaleX = mixScaleX2;
                keyMap = nextMap;
            }
            timelines.add(timeline);
        }
        for (JsonValue constraintMap = map.getChild("outPath"); constraintMap != null; constraintMap = constraintMap.next) {
            PathConstraintData data = skeletonData.findPathConstraint(constraintMap.name);
            if (data == null) throw new SerializationException("Path constraint not found: " + constraintMap.name);
            int index = skeletonData.pathConstraints.indexOf(data, true);
            for (JsonValue timelineMap = constraintMap.child; timelineMap != null; timelineMap = timelineMap.next) {
                JsonValue keyMap = timelineMap.child;
                if (keyMap == null) continue;
                String timelineName = timelineMap.name;
                switch (timelineName) {
                    case "position" -> {
                        CurveTimeline1 timeline = new PathConstraintPositionTimeline(timelineMap.size, timelineMap.size, index);
                        timelines.add(readTimeline(keyMap, timeline, 0, data.positionMode == PositionMode.fixed ? scale : 1));
                    }
                    case "spacing" -> {
                        CurveTimeline1 timeline = new PathConstraintSpacingTimeline(timelineMap.size, timelineMap.size, index);
                        timelines.add(readTimeline(keyMap, timeline, 0,
                                data.spacingMode == SpacingMode.length || data.spacingMode == SpacingMode.fixed ? scale : 1));
                    }
                    case "mix" -> {
                        PathConstraintMixTimeline timeline = new PathConstraintMixTimeline(timelineMap.size, timelineMap.size * 3, index);
                        float time = keyMap.getFloat("time", 0);
                        float mixRotate = keyMap.getFloat("mixRotate", 1);
                        float mixX = keyMap.getFloat("mixX", 1), mixY = keyMap.getFloat("mixY", mixX);
                        for (int frame = 0, bezier = 0; ; frame++) {
                            timeline.setFrame(frame, time, mixRotate, mixX, mixY);
                            JsonValue nextMap = keyMap.next;
                            if (nextMap == null) {
                                timeline.shrink(bezier);
                                break;
                            }
                            float time2 = nextMap.getFloat("time", 0);
                            float mixRotate2 = nextMap.getFloat("mixRotate", 1);
                            float mixX2 = nextMap.getFloat("mixX", 1), mixY2 = nextMap.getFloat("mixY", mixX2);
                            JsonValue curve = keyMap.get("curve");
                            if (curve != null) {
                                bezier = readCurve(curve, timeline, bezier, frame, 0, time, time2, mixRotate, mixRotate2, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 1, time, time2, mixX, mixX2, 1);
                                bezier = readCurve(curve, timeline, bezier, frame, 2, time, time2, mixY, mixY2, 1);
                            }
                            time = time2;
                            mixRotate = mixRotate2;
                            mixX = mixX2;
                            mixY = mixY2;
                            keyMap = nextMap;
                        }
                        timelines.add(timeline);
                    }
                }
            }
        }
        for (JsonValue deformMap = map.getChild("deform"); deformMap != null; deformMap = deformMap.next) {
            Skin skin = skeletonData.findSkin(deformMap.name);
            if (skin == null) throw new SerializationException("Skin not found: " + deformMap.name);
            for (JsonValue slotMap = deformMap.child; slotMap != null; slotMap = slotMap.next) {
                SlotData slot = skeletonData.findSlot(slotMap.name);
                if (slot == null) throw new SerializationException("Slot not found: " + slotMap.name);
                for (JsonValue timelineMap = slotMap.child; timelineMap != null; timelineMap = timelineMap.next) {
                    JsonValue keyMap = timelineMap.child;
                    if (keyMap == null) continue;
                    VertexAttachment attachment = (VertexAttachment) skin.getAttachment(slot.index, timelineMap.name);
                    if (attachment == null)
                        throw new SerializationException("Deform attachment not found: " + timelineMap.name);
                    boolean weighted = attachment.getBones() != null;
                    float[] vertices = attachment.getVertices();
                    int deformLength = weighted ? (vertices.length / 3) << 1 : vertices.length;
                    DeformTimeline timeline = new DeformTimeline(timelineMap.size, timelineMap.size, slot.index, attachment);
                    float time = keyMap.getFloat("time", 0);
                    for (int frame = 0, bezier = 0; ; frame++) {
                        float[] deform;
                        JsonValue verticesValue = keyMap.get("vertices");
                        if (verticesValue == null)
                            deform = weighted ? new float[deformLength] : vertices;
                        else {
                            deform = new float[deformLength];
                            int start = keyMap.getInt("offset", 0);
                            arraycopy(verticesValue.asFloatArray(), 0, deform, start, verticesValue.size);
                            if (scale != 1) {
                                for (int i = start, n = i + verticesValue.size; i < n; i++)
                                    deform[i] *= scale;
                            }
                            if (!weighted) {
                                for (int i = 0; i < deformLength; i++)
                                    deform[i] += vertices[i];
                            }
                        }
                        timeline.setFrame(frame, time, deform);
                        JsonValue nextMap = keyMap.next;
                        if (nextMap == null) {
                            timeline.shrink(bezier);
                            break;
                        }
                        float time2 = nextMap.getFloat("time", 0);
                        JsonValue curve = keyMap.get("curve");
                        if (curve != null) bezier = readCurve(curve, timeline, bezier, frame, 0, time, time2, 0, 1, 1);
                        time = time2;
                        keyMap = nextMap;
                    }
                    timelines.add(timeline);
                }
            }
        }
        JsonValue drawOrdersMap = map.get("drawOrder");
        if (drawOrdersMap == null) drawOrdersMap = map.get("draworder");
        if (drawOrdersMap != null) {
            DrawOrderTimeline timeline = new DrawOrderTimeline(drawOrdersMap.size);
            int slotCount = skeletonData.slots.size;
            int frame = 0;
            for (JsonValue drawOrderMap = drawOrdersMap.child; drawOrderMap != null; drawOrderMap = drawOrderMap.next, frame++) {
                int[] drawOrder = null;
                JsonValue offsets = drawOrderMap.get("offsets");
                if (offsets != null) {
                    drawOrder = new int[slotCount];
                    for (int i = slotCount - 1; i >= 0; i--)
                        drawOrder[i] = -1;
                    int[] unchanged = new int[slotCount - offsets.size];
                    int originalIndex = 0, unchangedIndex = 0;
                    for (JsonValue offsetMap = offsets.child; offsetMap != null; offsetMap = offsetMap.next) {
                        SlotData slot = skeletonData.findSlot(offsetMap.getString("slot"));
                        if (slot == null)
                            throw new SerializationException("Slot not found: " + offsetMap.getString("slot"));
                        while (originalIndex != slot.index)
                            unchanged[unchangedIndex++] = originalIndex++;
                        drawOrder[originalIndex + offsetMap.getInt("offset")] = originalIndex++;
                    }
                    while (originalIndex < slotCount)
                        unchanged[unchangedIndex++] = originalIndex++;
                    for (int i = slotCount - 1; i >= 0; i--)
                        if (drawOrder[i] == -1) drawOrder[i] = unchanged[--unchangedIndex];
                }
                timeline.setFrame(frame, drawOrderMap.getFloat("time", 0), drawOrder);
            }
            timelines.add(timeline);
        }
        JsonValue eventsMap = map.get("events");
        if (eventsMap != null) {
            EventTimeline timeline = new EventTimeline(eventsMap.size);
            int frame = 0;
            for (JsonValue eventMap = eventsMap.child; eventMap != null; eventMap = eventMap.next, frame++) {
                EventData eventData = skeletonData.findEvent(eventMap.getString("name"));
                if (eventData == null)
                    throw new SerializationException("Event not found: " + eventMap.getString("name"));
                Event event = new Event(eventMap.getFloat("time", 0), eventData);
                event.intValue = eventMap.getInt("int", eventData.intValue);
                event.floatValue = eventMap.getFloat("float", eventData.floatValue);
                event.stringValue = eventMap.getString("string", eventData.stringValue);
                if (event.getData().audioPath != null) {
                    event.volume = eventMap.getFloat("volume", eventData.volume);
                    event.balance = eventMap.getFloat("balance", eventData.balance);
                }
                timeline.setFrame(frame, event);
            }
            timelines.add(timeline);
        }
        timelines.shrink();
        float duration = 0;
        Object[] items = timelines.items;
        for (int i = 0, n = timelines.size; i < n; i++)
            duration = Math.max(duration, ((Timeline) items[i]).getDuration());
        skeletonData.animations.add(new Animation(name, timelines, duration));
    }

    private Timeline readTimeline(JsonValue keyMap, CurveTimeline1 timeline, float defaultValue, float scale) {
        float time = keyMap.getFloat("time", 0), value = keyMap.getFloat("value", defaultValue) * scale;
        int bezier = 0;
        for (int frame = 0; ; frame++) {
            timeline.setFrame(frame, time, value);
            JsonValue nextMap = keyMap.next;
            if (nextMap == null) break;
            float time2 = nextMap.getFloat("time", 0);
            float value2 = nextMap.getFloat("value", defaultValue) * scale;
            JsonValue curve = keyMap.get("curve");
            if (curve != null) bezier = readCurve(curve, timeline, bezier, frame, 0, time, time2, value, value2, scale);
            time = time2;
            value = value2;
            keyMap = nextMap;
        }
        timeline.shrink(bezier);
        return timeline;
    }

    private Timeline readTimeline(JsonValue keyMap, CurveTimeline2 timeline, float defaultValue,
                                  float scale) {
        float time = keyMap.getFloat("time", 0);
        float value1 = keyMap.getFloat("x", defaultValue) * scale, value2 = keyMap.getFloat("y", defaultValue) * scale;
        int bezier = 0;
        for (int frame = 0; ; frame++) {
            timeline.setFrame(frame, time, value1, value2);
            JsonValue nextMap = keyMap.next;
            if (nextMap == null) break;
            float time2 = nextMap.getFloat("time", 0);
            float nvalue1 = nextMap.getFloat("x", defaultValue) * scale, nvalue2 = nextMap.getFloat("y", defaultValue) * scale;
            JsonValue curve = keyMap.get("curve");
            if (curve != null) {
                bezier = readCurve(curve, timeline, bezier, frame, 0, time, time2, value1, nvalue1, scale);
                bezier = readCurve(curve, timeline, bezier, frame, 1, time, time2, value2, nvalue2, scale);
            }
            time = time2;
            value1 = nvalue1;
            value2 = nvalue2;
            keyMap = nextMap;
        }
        timeline.shrink(bezier);
        return timeline;
    }

    int readCurve(JsonValue curve, CurveTimeline timeline, int bezier, int frame, int value, float time1, float time2,
                  float value1, float value2, float scale) {
        if (curve.isString()) {
            if (value != 0) timeline.setStepped(frame);
        } else {
            curve = curve.get(value << 2);
            float cx1 = curve.asFloat();
            curve = curve.next;
            float cy1 = curve.asFloat() * scale;
            curve = curve.next;
            float cx2 = curve.asFloat();
            curve = curve.next;
            float cy2 = curve.asFloat() * scale;
            setBezier(timeline, frame, value, bezier++, time1, value1, cx1, cy1, cx2, cy2, time2, value2);
        }
        return bezier;
    }

    void setBezier(CurveTimeline timeline, int frame, int value, int bezier, float time1, float value1, float cx1, float cy1,
                   float cx2, float cy2, float time2, float value2) {
        timeline.setBezier(bezier, frame, value, time1, value1, cx1, cy1, cx2, cy2, time2, value2);
    }

    static class LinkedMesh {
        final String parent;
        final String skin;
        final int slotIndex;
        final MeshAttachment mesh;
        final boolean inheritDeform;

        public LinkedMesh(MeshAttachment mesh, String skin, int slotIndex, String parent, boolean inheritDeform) {
            this.mesh = mesh;
            this.skin = skin;
            this.slotIndex = slotIndex;
            this.parent = parent;
            this.inheritDeform = inheritDeform;
        }
    }
}
