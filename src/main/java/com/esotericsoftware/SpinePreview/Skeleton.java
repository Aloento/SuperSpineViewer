package com.esotericsoftware.SpinePreview;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Null;
import com.esotericsoftware.SpinePreview.Skin.SkinEntry;
import com.esotericsoftware.SpinePreview.attachments.Attachment;
import com.esotericsoftware.SpinePreview.attachments.MeshAttachment;
import com.esotericsoftware.SpinePreview.attachments.PathAttachment;
import com.esotericsoftware.SpinePreview.attachments.RegionAttachment;

import static com.esotericsoftware.SpinePreview.utils.SpineUtils.*;

public class Skeleton {
    final SkeletonData data;
    final Array<Bone> bones;
    final Array<Slot> slots;
    final Array<IkConstraint> ikConstraints;
    final Array<TransformConstraint> transformConstraints;
    final Array<PathConstraint> pathConstraints;
    final Array<Updatable> updateCache = new Array();
    final Color color;
    Array<Slot> drawOrder;
    @Null
    Skin skin;
    float time;
    float scaleX = 1, scaleY = 1;
    float x, y;

    public Skeleton(SkeletonData data) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        this.data = data;
        bones = new Array(data.bones.size);
        Object[] bones = this.bones.items;
        for (BoneData boneData : data.bones) {
            Bone bone;
            if (boneData.parent == null)
                bone = new Bone(boneData, this, null);
            else {
                Bone parent = (Bone) bones[boneData.parent.index];
                bone = new Bone(boneData, this, parent);
                parent.children.add(bone);
            }
            this.bones.add(bone);
        }
        slots = new Array(data.slots.size);
        drawOrder = new Array(data.slots.size);
        for (SlotData slotData : data.slots) {
            Bone bone = (Bone) bones[slotData.boneData.index];
            Slot slot = new Slot(slotData, bone);
            slots.add(slot);
            drawOrder.add(slot);
        }
        ikConstraints = new Array(data.ikConstraints.size);
        for (IkConstraintData ikConstraintData : data.ikConstraints)
            ikConstraints.add(new IkConstraint(ikConstraintData, this));
        transformConstraints = new Array(data.transformConstraints.size);
        for (TransformConstraintData transformConstraintData : data.transformConstraints)
            transformConstraints.add(new TransformConstraint(transformConstraintData, this));
        pathConstraints = new Array(data.pathConstraints.size);
        for (PathConstraintData pathConstraintData : data.pathConstraints)
            pathConstraints.add(new PathConstraint(pathConstraintData, this));
        color = new Color(1, 1, 1, 1);
        updateCache();
    }

    public Skeleton(Skeleton skeleton) {
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        data = skeleton.data;
        bones = new Array(skeleton.bones.size);
        for (Bone bone : skeleton.bones) {
            Bone newBone;
            if (bone.parent == null)
                newBone = new Bone(bone, this, null);
            else {
                Bone parent = bones.get(bone.parent.data.index);
                newBone = new Bone(bone, this, parent);
                parent.children.add(newBone);
            }
            bones.add(newBone);
        }
        slots = new Array(skeleton.slots.size);
        for (Slot slot : skeleton.slots) {
            Bone bone = bones.get(slot.bone.data.index);
            slots.add(new Slot(slot, bone));
        }
        drawOrder = new Array(slots.size);
        for (Slot slot : skeleton.drawOrder)
            drawOrder.add(slots.get(slot.data.index));
        ikConstraints = new Array(skeleton.ikConstraints.size);
        for (IkConstraint ikConstraint : skeleton.ikConstraints)
            ikConstraints.add(new IkConstraint(ikConstraint, this));
        transformConstraints = new Array(skeleton.transformConstraints.size);
        for (TransformConstraint transformConstraint : skeleton.transformConstraints)
            transformConstraints.add(new TransformConstraint(transformConstraint, this));
        pathConstraints = new Array(skeleton.pathConstraints.size);
        for (PathConstraint pathConstraint : skeleton.pathConstraints)
            pathConstraints.add(new PathConstraint(pathConstraint, this));
        skin = skeleton.skin;
        color = new Color(skeleton.color);
        time = skeleton.time;
        scaleX = skeleton.scaleX;
        scaleY = skeleton.scaleY;
        updateCache();
    }

    public void updateCache() {
        Array<Updatable> updateCache = this.updateCache;
        updateCache.clear();
        int boneCount = bones.size;
        Object[] bones = this.bones.items;
        for (int i = 0; i < boneCount; i++) {
            Bone bone = (Bone) bones[i];
            bone.sorted = bone.data.skinRequired;
            bone.active = !bone.sorted;
        }
        if (skin != null) {
            Object[] skinBones = skin.bones.items;
            for (int i = 0, n = skin.bones.size; i < n; i++) {
                Bone bone = (Bone) bones[((BoneData) skinBones[i]).index];
                do {
                    bone.sorted = false;
                    bone.active = true;
                    bone = bone.parent;
                } while (bone != null);
            }
        }
        int ikCount = ikConstraints.size, transformCount = transformConstraints.size, pathCount = pathConstraints.size;
        Object[] ikConstraints = this.ikConstraints.items;
        Object[] transformConstraints = this.transformConstraints.items;
        Object[] pathConstraints = this.pathConstraints.items;
        int constraintCount = ikCount + transformCount + pathCount;
        outer:
        for (int i = 0; i < constraintCount; i++) {
            for (int ii = 0; ii < ikCount; ii++) {
                IkConstraint constraint = (IkConstraint) ikConstraints[ii];
                if (constraint.data.order == i) {
                    sortIkConstraint(constraint);
                    continue outer;
                }
            }
            for (int ii = 0; ii < transformCount; ii++) {
                TransformConstraint constraint = (TransformConstraint) transformConstraints[ii];
                if (constraint.data.order == i) {
                    sortTransformConstraint(constraint);
                    continue outer;
                }
            }
            for (int ii = 0; ii < pathCount; ii++) {
                PathConstraint constraint = (PathConstraint) pathConstraints[ii];
                if (constraint.data.order == i) {
                    sortPathConstraint(constraint);
                    continue outer;
                }
            }
        }
        for (int i = 0; i < boneCount; i++)
            sortBone((Bone) bones[i]);
    }

    private void sortIkConstraint(IkConstraint constraint) {
        constraint.active = constraint.target.active
                && (!constraint.data.skinRequired || (skin != null && skin.constraints.contains(constraint.data, true)));
        if (!constraint.active) return;
        Bone target = constraint.target;
        sortBone(target);
        Array<Bone> constrained = constraint.bones;
        Bone parent = constrained.first();
        sortBone(parent);
        if (constrained.size == 1) {
            updateCache.add(constraint);
            sortReset(parent.children);
        } else {
            Bone child = constrained.peek();
            sortBone(child);
            updateCache.add(constraint);
            sortReset(parent.children);
            child.sorted = true;
        }
    }

    private void sortPathConstraint(PathConstraint constraint) {
        constraint.active = constraint.target.bone.active
                && (!constraint.data.skinRequired || (skin != null && skin.constraints.contains(constraint.data, true)));
        if (!constraint.active) return;
        Slot slot = constraint.target;
        int slotIndex = slot.getData().index;
        Bone slotBone = slot.bone;
        if (skin != null) sortPathConstraintAttachment(skin, slotIndex, slotBone);
        if (data.defaultSkin != null && data.defaultSkin != skin)
            sortPathConstraintAttachment(data.defaultSkin, slotIndex, slotBone);
        Attachment attachment = slot.attachment;
        if (attachment instanceof PathAttachment) sortPathConstraintAttachment(attachment, slotBone);
        Object[] constrained = constraint.bones.items;
        int boneCount = constraint.bones.size;
        for (int i = 0; i < boneCount; i++)
            sortBone((Bone) constrained[i]);
        updateCache.add(constraint);
        for (int i = 0; i < boneCount; i++)
            sortReset(((Bone) constrained[i]).children);
        for (int i = 0; i < boneCount; i++)
            ((Bone) constrained[i]).sorted = true;
    }

    private void sortTransformConstraint(TransformConstraint constraint) {
        constraint.active = constraint.target.active
                && (!constraint.data.skinRequired || (skin != null && skin.constraints.contains(constraint.data, true)));
        if (!constraint.active) return;
        sortBone(constraint.target);
        Object[] constrained = constraint.bones.items;
        int boneCount = constraint.bones.size;
        if (constraint.data.local) {
            for (int i = 0; i < boneCount; i++) {
                Bone child = (Bone) constrained[i];
                sortBone(child.parent);
                sortBone(child);
            }
        } else {
            for (int i = 0; i < boneCount; i++)
                sortBone((Bone) constrained[i]);
        }
        updateCache.add(constraint);
        for (int i = 0; i < boneCount; i++)
            sortReset(((Bone) constrained[i]).children);
        for (int i = 0; i < boneCount; i++)
            ((Bone) constrained[i]).sorted = true;
    }

    private void sortPathConstraintAttachment(Skin skin, int slotIndex, Bone slotBone) {
        Object[] entries = skin.attachments.orderedItems().items;
        for (int i = 0, n = skin.attachments.size; i < n; i++) {
            SkinEntry entry = (SkinEntry) entries[i];
            if (entry.slotIndex == slotIndex) sortPathConstraintAttachment(entry.attachment, slotBone);
        }
    }

    private void sortPathConstraintAttachment(Attachment attachment, Bone slotBone) {
        if (!(attachment instanceof PathAttachment)) return;
        int[] pathBones = ((PathAttachment) attachment).getBones();
        if (pathBones == null)
            sortBone(slotBone);
        else {
            Object[] bones = this.bones.items;
            for (int i = 0, n = pathBones.length; i < n; ) {
                int nn = pathBones[i++];
                nn += i;
                while (i < nn)
                    sortBone((Bone) bones[pathBones[i++]]);
            }
        }
    }

    private void sortBone(Bone bone) {
        if (bone.sorted) return;
        Bone parent = bone.parent;
        if (parent != null) sortBone(parent);
        bone.sorted = true;
        updateCache.add(bone);
    }

    private void sortReset(Array<Bone> bones) {
        Object[] items = bones.items;
        for (int i = 0, n = bones.size; i < n; i++) {
            Bone bone = (Bone) items[i];
            if (!bone.active) continue;
            if (bone.sorted) sortReset(bone.children);
            bone.sorted = false;
        }
    }

    public void updateWorldTransform() {
        Object[] updateCache = this.updateCache.items;
        for (int i = 0, n = this.updateCache.size; i < n; i++)
            ((Updatable) updateCache[i]).update();
    }

    public void updateWorldTransform(Bone parent) {
        if (parent == null) throw new IllegalArgumentException("parent cannot be null.");
        Bone rootBone = getRootBone();
        float pa = parent.a, pb = parent.b, pc = parent.c, pd = parent.d;
        rootBone.worldX = pa * x + pb * y + parent.worldX;
        rootBone.worldY = pc * x + pd * y + parent.worldY;
        float rotationY = rootBone.rotation + 90 + rootBone.shearY;
        float la = cosDeg(rootBone.rotation + rootBone.shearX) * rootBone.scaleX;
        float lb = cosDeg(rotationY) * rootBone.scaleY;
        float lc = sinDeg(rootBone.rotation + rootBone.shearX) * rootBone.scaleX;
        float ld = sinDeg(rotationY) * rootBone.scaleY;
        rootBone.a = (pa * la + pb * lc) * scaleX;
        rootBone.b = (pa * lb + pb * ld) * scaleX;
        rootBone.c = (pc * la + pd * lc) * scaleY;
        rootBone.d = (pc * lb + pd * ld) * scaleY;
        Object[] updateCache = this.updateCache.items;
        for (int i = 0, n = this.updateCache.size; i < n; i++) {
            Updatable updatable = (Updatable) updateCache[i];
            if (updatable != rootBone) updatable.update();
        }
    }

    public void setToSetupPose() {
        setBonesToSetupPose();
        setSlotsToSetupPose();
    }

    public void setBonesToSetupPose() {
        Object[] bones = this.bones.items;
        for (int i = 0, n = this.bones.size; i < n; i++)
            ((Bone) bones[i]).setToSetupPose();
        Object[] ikConstraints = this.ikConstraints.items;
        for (int i = 0, n = this.ikConstraints.size; i < n; i++) {
            IkConstraint constraint = (IkConstraint) ikConstraints[i];
            constraint.mix = constraint.data.mix;
            constraint.softness = constraint.data.softness;
            constraint.bendDirection = constraint.data.bendDirection;
            constraint.compress = constraint.data.compress;
            constraint.stretch = constraint.data.stretch;
        }
        Object[] transformConstraints = this.transformConstraints.items;
        for (int i = 0, n = this.transformConstraints.size; i < n; i++) {
            TransformConstraint constraint = (TransformConstraint) transformConstraints[i];
            TransformConstraintData data = constraint.data;
            constraint.mixRotate = data.mixRotate;
            constraint.mixX = data.mixX;
            constraint.mixY = data.mixY;
            constraint.mixScaleX = data.mixScaleX;
            constraint.mixScaleY = data.mixScaleY;
            constraint.mixShearY = data.mixShearY;
        }
        Object[] pathConstraints = this.pathConstraints.items;
        for (int i = 0, n = this.pathConstraints.size; i < n; i++) {
            PathConstraint constraint = (PathConstraint) pathConstraints[i];
            PathConstraintData data = constraint.data;
            constraint.position = data.position;
            constraint.spacing = data.spacing;
            constraint.mixRotate = data.mixRotate;
            constraint.mixX = data.mixX;
            constraint.mixY = data.mixY;
        }
    }

    public void setSlotsToSetupPose() {
        Object[] slots = this.slots.items;
        int n = this.slots.size;
        arraycopy(slots, 0, drawOrder.items, 0, n);
        for (int i = 0; i < n; i++)
            ((Slot) slots[i]).setToSetupPose();
    }

    public SkeletonData getData() {
        return data;
    }

    public Array<Bone> getBones() {
        return bones;
    }

    public Array<Updatable> getUpdateCache() {
        return updateCache;
    }

    public Bone getRootBone() {
        return bones.size == 0 ? null : bones.first();
    }

    public @Null
    Bone findBone(String boneName) {
        if (boneName == null) throw new IllegalArgumentException("boneName cannot be null.");
        Object[] bones = this.bones.items;
        for (int i = 0, n = this.bones.size; i < n; i++) {
            Bone bone = (Bone) bones[i];
            if (bone.data.name.equals(boneName)) return bone;
        }
        return null;
    }

    public Array<Slot> getSlots() {
        return slots;
    }

    public @Null
    Slot findSlot(String slotName) {
        if (slotName == null) throw new IllegalArgumentException("slotName cannot be null.");
        Object[] slots = this.slots.items;
        for (int i = 0, n = this.slots.size; i < n; i++) {
            Slot slot = (Slot) slots[i];
            if (slot.data.name.equals(slotName)) return slot;
        }
        return null;
    }

    public Array<Slot> getDrawOrder() {
        return drawOrder;
    }

    public void setDrawOrder(Array<Slot> drawOrder) {
        if (drawOrder == null) throw new IllegalArgumentException("drawOrder cannot be null.");
        this.drawOrder = drawOrder;
    }

    public @Null
    Skin getSkin() {
        return skin;
    }

    public void setSkin(String skinName) {
        Skin skin = data.findSkin(skinName);
        if (skin == null) throw new IllegalArgumentException("Skin not found: " + skinName);
        setSkin(skin);
    }

    public void setSkin(@Null Skin newSkin) {
        if (newSkin == skin) return;
        if (newSkin != null) {
            if (skin != null)
                newSkin.attachAll(this, skin);
            else {
                Object[] slots = this.slots.items;
                for (int i = 0, n = this.slots.size; i < n; i++) {
                    Slot slot = (Slot) slots[i];
                    String name = slot.data.attachmentName;
                    if (name != null) {
                        Attachment attachment = newSkin.getAttachment(i, name);
                        if (attachment != null) slot.setAttachment(attachment);
                    }
                }
            }
        }
        skin = newSkin;
        updateCache();
    }

    public @Null
    Attachment getAttachment(String slotName, String attachmentName) {
        SlotData slot = data.findSlot(slotName);
        if (slot == null) throw new IllegalArgumentException("Slot not found: " + slotName);
        return getAttachment(slot.getIndex(), attachmentName);
    }

    public @Null
    Attachment getAttachment(int slotIndex, String attachmentName) {
        if (attachmentName == null) throw new IllegalArgumentException("attachmentName cannot be null.");
        if (skin != null) {
            Attachment attachment = skin.getAttachment(slotIndex, attachmentName);
            if (attachment != null) return attachment;
        }
        if (data.defaultSkin != null) return data.defaultSkin.getAttachment(slotIndex, attachmentName);
        return null;
    }

    public void setAttachment(String slotName, @Null String attachmentName) {
        if (slotName == null) throw new IllegalArgumentException("slotName cannot be null.");
        Slot slot = findSlot(slotName);
        if (slot == null) throw new IllegalArgumentException("Slot not found: " + slotName);
        Attachment attachment = null;
        if (attachmentName != null) {
            attachment = getAttachment(slot.data.index, attachmentName);
            if (attachment == null)
                throw new IllegalArgumentException("Attachment not found: " + attachmentName + ", for slot: " + slotName);
        }
        slot.setAttachment(attachment);
    }

    public Array<IkConstraint> getIkConstraints() {
        return ikConstraints;
    }

    public @Null
    IkConstraint findIkConstraint(String constraintName) {
        if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
        Object[] ikConstraints = this.ikConstraints.items;
        for (int i = 0, n = this.ikConstraints.size; i < n; i++) {
            IkConstraint ikConstraint = (IkConstraint) ikConstraints[i];
            if (ikConstraint.data.name.equals(constraintName)) return ikConstraint;
        }
        return null;
    }

    public Array<TransformConstraint> getTransformConstraints() {
        return transformConstraints;
    }

    public @Null
    TransformConstraint findTransformConstraint(String constraintName) {
        if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
        Object[] transformConstraints = this.transformConstraints.items;
        for (int i = 0, n = this.transformConstraints.size; i < n; i++) {
            TransformConstraint constraint = (TransformConstraint) transformConstraints[i];
            if (constraint.data.name.equals(constraintName)) return constraint;
        }
        return null;
    }

    public Array<PathConstraint> getPathConstraints() {
        return pathConstraints;
    }

    public @Null
    PathConstraint findPathConstraint(String constraintName) {
        if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
        Object[] pathConstraints = this.pathConstraints.items;
        for (int i = 0, n = this.pathConstraints.size; i < n; i++) {
            PathConstraint constraint = (PathConstraint) pathConstraints[i];
            if (constraint.data.name.equals(constraintName)) return constraint;
        }
        return null;
    }

    public void getBounds(Vector2 offset, Vector2 size, FloatArray temp) {
        if (offset == null) throw new IllegalArgumentException("offset cannot be null.");
        if (size == null) throw new IllegalArgumentException("size cannot be null.");
        if (temp == null) throw new IllegalArgumentException("temp cannot be null.");
        Object[] drawOrder = this.drawOrder.items;
        float minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0, n = this.drawOrder.size; i < n; i++) {
            Slot slot = (Slot) drawOrder[i];
            if (!slot.bone.active) continue;
            int verticesLength = 0;
            float[] vertices = null;
            Attachment attachment = slot.attachment;
            if (attachment instanceof RegionAttachment) {
                verticesLength = 8;
                vertices = temp.setSize(8);
                ((RegionAttachment) attachment).computeWorldVertices(slot.getBone(), vertices, 0, 2);
            } else if (attachment instanceof MeshAttachment) {
                MeshAttachment mesh = (MeshAttachment) attachment;
                verticesLength = mesh.getWorldVerticesLength();
                vertices = temp.setSize(verticesLength);
                mesh.computeWorldVertices(slot, 0, verticesLength, vertices, 0, 2);
            }
            if (vertices != null) {
                for (int ii = 0; ii < verticesLength; ii += 2) {
                    float x = vertices[ii], y = vertices[ii + 1];
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        offset.set(minX, minY);
        size.set(maxX - minX, maxY - minY);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        if (color == null) throw new IllegalArgumentException("color cannot be null.");
        this.color.set(color);
    }

    public void setColor(float r, float g, float b, float a) {
        color.set(r, g, b, a);
    }

    public float getScaleX() {
        return scaleX;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }

    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public void update(float delta) {
        time += delta;
    }

    public String toString() {
        return data.name != null ? data.name : super.toString();
    }
}
