package com.esotericsoftware.SpineLegacy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.SpineLegacy.attachments.Attachment;

public class Skeleton {
    final SkeletonData data;
    final Array<Bone> bones;
    final Array<Slot> slots;
    final Array<IkConstraint> ikConstraints;
    final Array<TransformConstraint> transformConstraints;
    final Color color;
    private final Array<Updatable> updateCache = new Array<>();
    Array<Slot> drawOrder;
    Skin skin;
    float time;
    boolean flipX, flipY;
    float x, y;

    public Skeleton(SkeletonData data) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        this.data = data;
        bones = new Array<>(data.bones.size);
        for (BoneData boneData : data.bones) {
            Bone parent = boneData.parent == null ? null : bones.get(data.bones.indexOf(boneData.parent, true));
            bones.add(new Bone(boneData, this, parent));
        }
        slots = new Array<>(data.slots.size);
        drawOrder = new Array<>(data.slots.size);
        for (SlotData slotData : data.slots) {
            Bone bone = bones.get(data.bones.indexOf(slotData.boneData, true));
            Slot slot = new Slot(slotData, bone);
            slots.add(slot);
            drawOrder.add(slot);
        }
        ikConstraints = new Array<>(data.ikConstraints.size);
        for (IkConstraintData ikConstraintData : data.ikConstraints)
            ikConstraints.add(new IkConstraint(ikConstraintData, this));
        transformConstraints = new Array<>(data.transformConstraints.size);
        for (TransformConstraintData transformConstraintData : data.transformConstraints)
            transformConstraints.add(new TransformConstraint(transformConstraintData, this));
        color = new Color(1, 1, 1, 1);
        updateCache();
    }

    // public Skeleton(Skeleton skeleton) {
    //     if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
    //     data = skeleton.data;
    //     bones = new Array<>(skeleton.bones.size);
    //     for (Bone bone : skeleton.bones) {
    //         Bone parent = bone.parent == null ? null : bones.get(skeleton.bones.indexOf(bone.parent, true));
    //         bones.add(new Bone(bone, this, parent));
    //     }
    //     slots = new Array<>(skeleton.slots.size);
    //     for (Slot slot : skeleton.slots) {
    //         Bone bone = bones.get(skeleton.bones.indexOf(slot.bone, true));
    //         slots.add(new Slot(slot, bone));
    //     }
    //     drawOrder = new Array<>(slots.size);
    //     for (Slot slot : skeleton.drawOrder)
    //         drawOrder.add(slots.get(skeleton.slots.indexOf(slot, true)));
    //     ikConstraints = new Array<>(skeleton.ikConstraints.size);
    //     for (IkConstraint ikConstraint : skeleton.ikConstraints)
    //         ikConstraints.add(new IkConstraint(ikConstraint, this));
    //     transformConstraints = new Array<>(skeleton.transformConstraints.size);
    //     for (TransformConstraint transformConstraint : skeleton.transformConstraints)
    //         transformConstraints.add(new TransformConstraint(transformConstraint, this));
    //     skin = skeleton.skin;
    //     color = new Color(skeleton.color);
    //     time = skeleton.time;
    //     flipX = skeleton.flipX;
    //     flipY = skeleton.flipY;
    //     updateCache();
    // }

    public void updateCache() {
        Array<Bone> bones = this.bones;
        Array<Updatable> updateCache = this.updateCache;
        Array<IkConstraint> ikConstraints = this.ikConstraints;
        Array<TransformConstraint> transformConstraints = this.transformConstraints;
        int ikConstraintsCount = ikConstraints.size;
        int transformConstraintsCount = transformConstraints.size;
        updateCache.clear();
        for (int i = 0, n = bones.size; i < n; i++) {
            Bone bone = bones.get(i);
            updateCache.add(bone);
            for (int ii = 0; ii < ikConstraintsCount; ii++) {
                IkConstraint ikConstraint = ikConstraints.get(ii);
                if (bone == ikConstraint.bones.peek()) {
                    updateCache.add(ikConstraint);
                    break;
                }
            }
        }
        for (int i = 0; i < transformConstraintsCount; i++) {
            TransformConstraint transformConstraint = transformConstraints.get(i);
            for (int ii = updateCache.size - 1; ii >= 0; ii--) {
                if (updateCache.get(ii) == transformConstraint.bone) {
                    updateCache.insert(ii + 1, transformConstraint);
                    break;
                }
            }
        }
    }

    public void updateWorldTransform() {
        Array<Updatable> updateCache = this.updateCache;
        for (int i = 0, n = updateCache.size; i < n; i++)
            updateCache.get(i).update();
    }

    public void setToSetupPose() {
        setBonesToSetupPose();
        setSlotsToSetupPose();
    }

    public void setBonesToSetupPose() {
        Array<Bone> bones = this.bones;
        for (int i = 0, n = bones.size; i < n; i++)
            bones.get(i).setToSetupPose();
        Array<IkConstraint> ikConstraints = this.ikConstraints;
        for (int i = 0, n = ikConstraints.size; i < n; i++) {
            IkConstraint constraint = ikConstraints.get(i);
            constraint.bendDirection = constraint.data.bendDirection;
            constraint.mix = constraint.data.mix;
        }
        Array<TransformConstraint> transformConstraints = this.transformConstraints;
        for (int i = 0, n = transformConstraints.size; i < n; i++) {
            TransformConstraint constraint = transformConstraints.get(i);
            TransformConstraintData data = constraint.data;
            constraint.rotateMix = data.rotateMix;
            constraint.translateMix = data.translateMix;
            constraint.scaleMix = data.scaleMix;
            constraint.shearMix = data.shearMix;
        }
    }

    public void setSlotsToSetupPose() {
        Array<Slot> slots = this.slots;
        System.arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
        for (int i = 0, n = slots.size; i < n; i++)
            slots.get(i).setToSetupPose(i);
    }

    // public SkeletonData getData() {
    //     return data;
    // }

    public Array<Bone> getBones() {
        return bones;
    }

    public Bone getRootBone() {
        if (bones.size == 0) return null;
        return bones.first();
    }

    public Bone findBone(String boneName) {
        if (boneName == null) throw new IllegalArgumentException("boneName cannot be null.");
        Array<Bone> bones = this.bones;
        for (int i = 0, n = bones.size; i < n; i++) {
            Bone bone = bones.get(i);
            if (bone.data.name.equals(boneName)) return bone;
        }
        return null;
    }

    // public Array<Slot> getSlots() {
    //     return slots;
    // }

    // public Slot findSlot(String slotName) {
    //     if (slotName == null) throw new IllegalArgumentException("slotName cannot be null.");
    //     Array<Slot> slots = this.slots;
    //     for (int i = 0, n = slots.size; i < n; i++) {
    //         Slot slot = slots.get(i);
    //         if (slot.data.name.equals(slotName)) return slot;
    //     }
    //     return null;
    // }

    // public Array<Slot> getDrawOrder() {
    //     return drawOrder;
    // }

    // public void setDrawOrder(Array<Slot> drawOrder) {
    //     this.drawOrder = drawOrder;
    // }

    // public Skin getSkin() {
    //     return skin;
    // }

    public void setSkin(String skinName) {
        Skin skin = data.findSkin(skinName);
        if (skin == null) throw new IllegalArgumentException("Skin not found: " + skinName);
        setSkin(skin);
    }

    public void setSkin(Skin newSkin) {
        if (newSkin != null) {
            if (skin != null)
                newSkin.attachAll(this, skin);
            else {
                Array<Slot> slots = this.slots;
                for (int i = 0, n = slots.size; i < n; i++) {
                    Slot slot = slots.get(i);
                    String name = slot.data.attachmentName;
                    if (name != null) {
                        Attachment attachment = newSkin.getAttachment(i, name);
                        if (attachment != null) slot.setAttachment(attachment);
                    }
                }
            }
        }
        skin = newSkin;
    }

    // public Attachment getAttachment(String slotName, String attachmentName) {
    //     return getAttachment(data.findSlotIndex(slotName), attachmentName);
    // }

    public Attachment getAttachment(int slotIndex, String attachmentName) {
        if (attachmentName == null) throw new IllegalArgumentException("attachmentName cannot be null.");
        if (skin != null) {
            Attachment attachment = skin.getAttachment(slotIndex, attachmentName);
            if (attachment != null) return attachment;
        }
        if (data.defaultSkin != null) return data.defaultSkin.getAttachment(slotIndex, attachmentName);
        return null;
    }

    // public void setAttachment(String slotName, String attachmentName) {
    //     if (slotName == null) throw new IllegalArgumentException("slotName cannot be null.");
    //     Array<Slot> slots = this.slots;
    //     for (int i = 0, n = slots.size; i < n; i++) {
    //         Slot slot = slots.get(i);
    //         if (slot.data.name.equals(slotName)) {
    //             Attachment attachment = null;
    //             if (attachmentName != null) {
    //                 attachment = getAttachment(i, attachmentName);
    //                 if (attachment == null)
    //                     throw new IllegalArgumentException("Attachment not found: " + attachmentName + ", for slot: " + slotName);
    //             }
    //             slot.setAttachment(attachment);
    //             return;
    //         }
    //     }
    //     throw new IllegalArgumentException("Slot not found: " + slotName);
    // }

    // public Array<IkConstraint> getIkConstraints() {
    //     return ikConstraints;
    // }

    // public IkConstraint findIkConstraint(String constraintName) {
    //     if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
    //     Array<IkConstraint> ikConstraints = this.ikConstraints;
    //     for (int i = 0, n = ikConstraints.size; i < n; i++) {
    //         IkConstraint ikConstraint = ikConstraints.get(i);
    //         if (ikConstraint.data.name.equals(constraintName)) return ikConstraint;
    //     }
    //     return null;
    // }

    // public Array<TransformConstraint> getTransformConstraints() {
    //     return transformConstraints;
    // }

    // public TransformConstraint findTransformConstraint(String constraintName) {
    //     if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
    //     Array<TransformConstraint> transformConstraints = this.transformConstraints;
    //     for (int i = 0, n = transformConstraints.size; i < n; i++) {
    //         TransformConstraint constraint = transformConstraints.get(i);
    //         if (constraint.data.name.equals(constraintName)) return constraint;
    //     }
    //     return null;
    // }

    // public void getBounds(Vector2 offset, Vector2 size) {
    //     Array<Slot> drawOrder = this.drawOrder;
    //     float minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
    //     for (int i = 0, n = drawOrder.size; i < n; i++) {
    //         Slot slot = drawOrder.get(i);
    //         float[] vertices = null;
    //         Attachment attachment = slot.attachment;
    //         if (attachment instanceof RegionAttachment) {
    //             vertices = ((RegionAttachment) attachment).updateWorldVertices(slot, false);
    //         } else if (attachment instanceof MeshAttachment) {
    //             vertices = ((MeshAttachment) attachment).updateWorldVertices(slot, true);
    //         } else if (attachment instanceof WeightedMeshAttachment) {
    //             vertices = ((WeightedMeshAttachment) attachment).updateWorldVertices(slot, true);
    //         }
    //         if (vertices != null) {
    //             for (int ii = 0, nn = vertices.length; ii < nn; ii += 5) {
    //                 float x = vertices[ii], y = vertices[ii + 1];
    //                 minX = Math.min(minX, x);
    //                 minY = Math.min(minY, y);
    //                 maxX = Math.max(maxX, x);
    //                 maxY = Math.max(maxY, y);
    //             }
    //         }
    //     }
    //     offset.set(minX, minY);
    //     size.set(maxX - minX, maxY - minY);
    // }

    public Color getColor() {
        return color;
    }

    // public void setColor(Color color) {
    //     this.color.set(color);
    // }

    // public void setFlip(boolean flipX, boolean flipY) {
    //     this.flipX = flipX;
    //     this.flipY = flipY;
    // }

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

    // public float getTime() {
    //     return time;
    // }

    // public void setTime(float time) {
    //     this.time = time;
    // }

    // public void update(float delta) {
    //     time += delta;
    // }

    public String toString() {
        return data.name != null ? data.name : super.toString();
    }
}
