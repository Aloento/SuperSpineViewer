package com.esotericsoftware.SpineStandard;

import com.QYun.SuperSpineViewer.Loader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.SpineStandard.attachments.Attachment;
import com.esotericsoftware.SpineStandard.attachments.PathAttachment;

import static com.esotericsoftware.SpineStandard.utils.SpineUtils.arraycopy;

public class Skeleton {
    final SkeletonData data;
    final Array<Bone> bones;
    final Array<Slot> slots;
    final Array<IkConstraint> ikConstraints;
    final Array<TransformConstraint> transformConstraints;
    final Array<PathConstraint> pathConstraints;
    final Array<Updatable> updateCache = new Array<>();
    final Array<Bone> updateCacheReset = new Array<>();
    final Color color;
    Array<Slot> drawOrder;
    Skin skin;
    float time;
    float scaleX = 1, scaleY = 1;
    boolean flipX, flipY; // Spine36/5/4
    float x, y;

    public Skeleton(SkeletonData data) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        this.data = data;

        bones = new Array<>(data.bones.size);
        for (BoneData boneData : data.bones) {
            Bone bone;
            if (boneData.parent == null)
                bone = new Bone(boneData, this, null);
            else {
                Bone parent = bones.get(boneData.parent.index);
                bone = new Bone(boneData, this, parent);
                parent.children.add(bone);
            }
            bones.add(bone);
        }

        slots = new Array<>(data.slots.size);
        drawOrder = new Array<>(data.slots.size);
        for (SlotData slotData : data.slots) {
            Bone bone = bones.get(slotData.boneData.index);
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

        pathConstraints = new Array<>(data.pathConstraints.size);
        for (PathConstraintData pathConstraintData : data.pathConstraints)
            pathConstraints.add(new PathConstraint(pathConstraintData, this));

        color = new Color(1, 1, 1, 1);
        updateCache();
    }

    // public Skeleton(Skeleton skeleton) {
    //     if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
    //     data = skeleton.data;
    //
    //     bones = new Array<>(skeleton.bones.size);
    //     for (Bone bone : skeleton.bones) {
    //         Bone newBone;
    //         if (bone.parent == null)
    //             newBone = new Bone(bone, this, null);
    //         else {
    //             Bone parent = bones.get(bone.parent.data.index);
    //             newBone = new Bone(bone, this, parent);
    //             parent.children.add(newBone);
    //         }
    //         bones.add(newBone);
    //     }
    //     slots = new Array<>(skeleton.slots.size);
    //     for (Slot slot : skeleton.slots) {
    //         Bone bone = bones.get(slot.bone.data.index);
    //         slots.add(new Slot(slot, bone));
    //     }
    //     drawOrder = new Array<>(slots.size);
    //     for (Slot slot : skeleton.drawOrder)
    //         drawOrder.add(slots.get(slot.data.index));
    //
    //     ikConstraints = new Array<>(skeleton.ikConstraints.size);
    //     for (IkConstraint ikConstraint : skeleton.ikConstraints)
    //         ikConstraints.add(new IkConstraint(ikConstraint, this));
    //
    //     transformConstraints = new Array<>(skeleton.transformConstraints.size);
    //     for (TransformConstraint transformConstraint : skeleton.transformConstraints)
    //         transformConstraints.add(new TransformConstraint(transformConstraint, this));
    //
    //     pathConstraints = new Array<>(skeleton.pathConstraints.size);
    //     for (PathConstraint pathConstraint : skeleton.pathConstraints)
    //         pathConstraints.add(new PathConstraint(pathConstraint, this));
    //
    //     skin = skeleton.skin;
    //     color = new Color(skeleton.color);
    //     time = skeleton.time;
    //     if (Loader.spineVersion > 36) {
    //         scaleX = skeleton.scaleX;
    //         scaleY = skeleton.scaleY;
    //     } else {
    //         flipX = skeleton.flipX;
    //         flipY = skeleton.flipY;
    //     }
    //     updateCache();
    // }

    public void updateCache() {
        Array<Updatable> updateCache = this.updateCache;
        updateCache.clear();
        updateCacheReset.clear();

        switch (Loader.spineVersion) {
            case 38 -> {
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
            case 37, 36, 35 -> {
                Array<Bone> bones = this.bones;
                for (int i = 0, n = bones.size; i < n; i++)
                    bones.get(i).sorted = false;

                Array<IkConstraint> ikConstraints = this.ikConstraints;
                Array<TransformConstraint> transformConstraints = this.transformConstraints;
                Array<PathConstraint> pathConstraints = this.pathConstraints;
                int ikCount = ikConstraints.size, transformCount = transformConstraints.size, pathCount = pathConstraints.size;
                int constraintCount = ikCount + transformCount + pathCount;
                outer:
                for (int i = 0; i < constraintCount; i++) {
                    for (int ii = 0; ii < ikCount; ii++) {
                        IkConstraint constraint = ikConstraints.get(ii);
                        if (constraint.data.order == i) {
                            sortIkConstraint(constraint);
                            continue outer;
                        }
                    }
                    for (int ii = 0; ii < transformCount; ii++) {
                        TransformConstraint constraint = transformConstraints.get(ii);
                        if (constraint.data.order == i) {
                            sortTransformConstraint(constraint);
                            continue outer;
                        }
                    }
                    for (int ii = 0; ii < pathCount; ii++) {
                        PathConstraint constraint = pathConstraints.get(ii);
                        if (constraint.data.order == i) {
                            sortPathConstraint(constraint);
                            continue outer;
                        }
                    }
                }

                for (int i = 0, n = bones.size; i < n; i++)
                    sortBone(bones.get(i));
            }
            case 34 -> {
                updateCache.clear();
                Array<Bone> bones = this.bones;
                for (int i = 0, n = bones.size; i < n; i++)
                    bones.get(i).sorted = false;
                Array<IkConstraint> ikConstraints = this.ikConstraints;
                ikConstraints.clear();
                ikConstraints.addAll(this.ikConstraints);
                int ikCount = ikConstraints.size;
                for (int i = 0, level; i < ikCount; i++) {
                    IkConstraint ik = ikConstraints.get(i);
                    Bone bone = ik.bones.first().parent;
                    for (level = 0; bone != null; level++)
                        bone = bone.parent;
                    ik.level = level;
                }
                for (int i = 1, ii; i < ikCount; i++) {
                    IkConstraint ik = ikConstraints.get(i);
                    int level = ik.level;
                    for (ii = i - 1; ii >= 0; ii--) {
                        IkConstraint other = ikConstraints.get(ii);
                        if (other.level < level) break;
                        ikConstraints.set(ii + 1, other);
                    }
                    ikConstraints.set(ii + 1, ik);
                }
                for (int i = 0, n = ikConstraints.size; i < n; i++) {
                    IkConstraint constraint = ikConstraints.get(i);
                    Bone target = constraint.target;
                    sortBone(target);

                    Array<Bone> constrained = constraint.bones;
                    Bone parent = constrained.first();
                    sortBone(parent);

                    updateCache.add(constraint);

                    sortReset(parent.children);
                    constrained.peek().sorted = true;
                }

                Array<PathConstraint> pathConstraints = this.pathConstraints;
                for (int i = 0, n = pathConstraints.size; i < n; i++) {
                    PathConstraint constraint = pathConstraints.get(i);

                    Slot slot = constraint.target;
                    int slotIndex = slot.getData().index;
                    Bone slotBone = slot.bone;
                    if (skin != null) sortPathConstraintAttachment(skin, slotIndex, slotBone);
                    if (data.defaultSkin != null && data.defaultSkin != skin)
                        sortPathConstraintAttachment(data.defaultSkin, slotIndex, slotBone);
                    for (int ii = 0, nn = data.skins.size; ii < nn; ii++)
                        sortPathConstraintAttachment(data.skins.get(ii), slotIndex, slotBone);

                    Attachment attachment = slot.attachment;
                    if (attachment instanceof PathAttachment) sortPathConstraintAttachment(attachment, slotBone);

                    Array<Bone> constrained = constraint.bones;
                    int boneCount = constrained.size;
                    for (int ii = 0; ii < boneCount; ii++)
                        sortBone(constrained.get(ii));

                    updateCache.add(constraint);

                    for (int ii = 0; ii < boneCount; ii++)
                        sortReset(constrained.get(ii).children);
                    for (int ii = 0; ii < boneCount; ii++)
                        constrained.get(ii).sorted = true;
                }

                Array<TransformConstraint> transformConstraints = this.transformConstraints;
                for (int i = 0, n = transformConstraints.size; i < n; i++) {
                    TransformConstraint constraint = transformConstraints.get(i);

                    sortBone(constraint.target);

                    Array<Bone> constrained = constraint.bones;
                    int boneCount = constrained.size;
                    for (int ii = 0; ii < boneCount; ii++)
                        sortBone(constrained.get(ii));

                    updateCache.add(constraint);

                    for (int ii = 0; ii < boneCount; ii++)
                        sortReset(constrained.get(ii).children);
                    for (int ii = 0; ii < boneCount; ii++)
                        constrained.get(ii).sorted = true;
                }

                for (int i = 0, n = bones.size; i < n; i++)
                    sortBone(bones.get(i));
            }
        }
    }

    private void sortIkConstraint(IkConstraint constraint) {
        if (Loader.spineVersion == 38) {
            constraint.active = constraint.target.active &&
                    (!constraint.data.skinRequired || (skin != null && skin.constraints.contains(constraint.data, true)));
            if (!constraint.active) return;
        }

        Bone target = constraint.target;
        sortBone(target);
        Array<Bone> constrained = constraint.bones;
        Bone parent = constrained.first();
        sortBone(parent);

        if (constrained.size > 1) {
            Bone child = constrained.peek();
            if (!updateCache.contains(child, true)) updateCacheReset.add(child);
        }

        updateCache.add(constraint);
        sortReset(parent.children);
        constrained.peek().sorted = true;
    }

    private void sortPathConstraint(PathConstraint constraint) {
        // if (Loader.spineVersion == 38) {
        //     constraint.active = constraint.target.bone.active &&
        //             (!constraint.data.skinRequired || (skin != null && skin.constraints.contains(constraint.data, true)));
        //     if (!constraint.active) return;
        // }
        //
        // Slot slot = constraint.target;
        // int slotIndex = slot.getData().index;
        // Bone slotBone = slot.bone;
        // if (skin != null) sortPathConstraintAttachment(skin, slotIndex, slotBone);
        // if (data.defaultSkin != null && data.defaultSkin != skin)
        //     sortPathConstraintAttachment(data.defaultSkin, slotIndex, slotBone);
        // if (Loader.spineVersion < 37) {
        //     for (int ii = 0, nn = data.skins.size; ii < nn; ii++)
        //         sortPathConstraintAttachment(data.skins.get(ii), slotIndex, slotBone);
        // }
        // Attachment attachment = slot.attachment;
        // if (attachment instanceof PathAttachment) sortPathConstraintAttachment(attachment, slotBone);
        // Array<Bone> constrained = constraint.bones;
        // int boneCount = constrained.size;
        // for (int i = 0; i < boneCount; i++)
        //     sortBone(constrained.get(i));
        // updateCache.add(constraint);
        // for (int i = 0; i < boneCount; i++)
        //     sortReset(constrained.get(i).children);
        // for (int i = 0; i < boneCount; i++)
        //     constrained.get(i).sorted = true;
    }

    private void sortTransformConstraint(TransformConstraint constraint) {
        if (Loader.spineVersion == 38) {
            constraint.active = constraint.target.active &&
                    (!constraint.data.skinRequired || (skin != null && skin.constraints.contains(constraint.data, true)));
            if (!constraint.active) return;
        }

        sortBone(constraint.target);
        Array<Bone> constrained = constraint.bones;
        int boneCount = constrained.size;
        if (Loader.spineVersion > 35) {
            if (constraint.data.local) {
                for (int i = 0; i < boneCount; i++) {
                    Bone child = constrained.get(i);
                    sortBone(child.parent);
                    if (!updateCache.contains(child, true)) updateCacheReset.add(child);
                }
            } else {
                for (int i = 0; i < boneCount; i++)
                    sortBone(constrained.get(i));
            }
        } else {
            for (int i = 0; i < boneCount; i++)
                sortBone(constrained.get(i));
        }
        updateCache.add(constraint);
        for (int i = 0; i < boneCount; i++)
            sortReset(constrained.get(i).children);
        for (int i = 0; i < boneCount; i++)
            constrained.get(i).sorted = true;
    }

    private void sortPathConstraintAttachment(Skin skin, int slotIndex, Bone slotBone) {
        // if (Loader.spineVersion > 37) {
        //     for (SkinEntry entry : skin.attachments.keys())
        //         if (entry.getSlotIndex() == slotIndex)
        //             sortPathConstraintAttachment(entry.getAttachment(), slotBone);
        // } else {
        //     for (Entry<Key, Attachment> entry : skin.O_attachments.entries())
        //         if (entry.key.slotIndex == slotIndex) sortPathConstraintAttachment(entry.value, slotBone);
        // }
    }

    private void sortPathConstraintAttachment(Attachment attachment, Bone slotBone) {
        // if (!(attachment instanceof PathAttachment)) return;
        // int[] pathBones = ((PathAttachment) attachment).getBones();
        // if (pathBones == null)
        //     sortBone(slotBone);
        // else {
        //     Array<Bone> bones = this.bones;
        //     for (int i = 0, n = pathBones.length; i < n; ) {
        //         int nn = pathBones[i++];
        //         nn += i;
        //         while (i < nn)
        //             sortBone(bones.get(pathBones[i++]));
        //     }
        // }
    }

    private void sortBone(Bone bone) {
        if (bone.sorted) return;
        Bone parent = bone.parent;
        if (parent != null) sortBone(parent);
        bone.sorted = true;
        updateCache.add(bone);
    }

    private void sortReset(Array<Bone> bones) {
        for (int i = 0, n = bones.size; i < n; i++) {
            Bone bone = bones.get(i);
            if (!bone.active && Loader.spineVersion == 38) continue;
            if (bone.sorted) sortReset(bone.children);
            bone.sorted = false;
        }
    }

    public void updateWorldTransform() {
        Array<Bone> updateCacheReset = this.updateCacheReset;
        for (int i = 0, n = updateCacheReset.size; i < n; i++) {
            Bone bone = updateCacheReset.get(i);
            bone.ax = bone.x;
            bone.ay = bone.y;
            bone.arotation = bone.rotation;
            bone.ascaleX = bone.scaleX;
            bone.ascaleY = bone.scaleY;
            bone.ashearX = bone.shearX;
            bone.ashearY = bone.shearY;
            bone.appliedValid = true;
        }
        Array<Updatable> updateCache = this.updateCache;
        for (int i = 0, n = updateCache.size; i < n; i++)
            updateCache.get(i).update();
    }

    public void updateWorldTransform(Bone parent) {
        // if (parent == null) throw new IllegalArgumentException("parent cannot be null.");
        // Array<Bone> updateCacheReset = this.updateCacheReset;
        // for (int i = 0, n = updateCacheReset.size; i < n; i++) {
        //     Bone bone = updateCacheReset.get(i);
        //     bone.ax = bone.x;
        //     bone.ay = bone.y;
        //     bone.arotation = bone.rotation;
        //     bone.ascaleX = bone.scaleX;
        //     bone.ascaleY = bone.scaleY;
        //     bone.ashearX = bone.shearX;
        //     bone.ashearY = bone.shearY;
        //     bone.appliedValid = true;
        // }
        // Bone rootBone = getRootBone();
        // float pa = parent.a, pb = parent.b, pc = parent.c, pd = parent.d;
        // rootBone.worldX = pa * x + pb * y + parent.worldX;
        // rootBone.worldY = pc * x + pd * y + parent.worldY;
        // float rotationY = rootBone.rotation + 90 + rootBone.shearY;
        // float la = cosDeg(rootBone.rotation + rootBone.shearX) * rootBone.scaleX;
        // float lb = cosDeg(rotationY) * rootBone.scaleY;
        // float lc = sinDeg(rootBone.rotation + rootBone.shearX) * rootBone.scaleX;
        // float ld = sinDeg(rotationY) * rootBone.scaleY;
        // if (Loader.spineVersion > 36) {
        //     rootBone.a = (pa * la + pb * lc) * scaleX;
        //     rootBone.b = (pa * lb + pb * ld) * scaleX;
        //     rootBone.c = (pc * la + pd * lc) * scaleY;
        //     rootBone.d = (pc * lb + pd * ld) * scaleY;
        // } else {
        //     rootBone.a = pa * la + pb * lc;
        //     rootBone.b = pa * lb + pb * ld;
        //     rootBone.c = pc * la + pd * lc;
        //     rootBone.d = pc * lb + pd * ld;
        //     if (flipY) {
        //         rootBone.a = -rootBone.a;
        //         rootBone.b = -rootBone.b;
        //     }
        //     if (flipX) {
        //         rootBone.c = -rootBone.c;
        //         rootBone.d = -rootBone.d;
        //     }
        // }
        // Array<Updatable> updateCache = this.updateCache;
        // for (int i = 0, n = updateCache.size; i < n; i++) {
        //     Updatable updatable = updateCache.get(i);
        //     if (updatable != rootBone) updatable.update();
        // }
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
            switch (Loader.spineVersion) {
                case 38:
                    constraint.softness = constraint.data.softness;
                case 37, 36:
                    constraint.compress = constraint.data.compress;
                    constraint.stretch = constraint.data.stretch;
            }
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
        Array<PathConstraint> pathConstraints = this.pathConstraints;
        for (int i = 0, n = pathConstraints.size; i < n; i++) {
            PathConstraint constraint = pathConstraints.get(i);
            PathConstraintData data = constraint.data;
            constraint.position = data.position;
            constraint.spacing = data.spacing;
            constraint.rotateMix = data.rotateMix;
            constraint.translateMix = data.translateMix;
        }
    }

    public void setSlotsToSetupPose() {
        Array<Slot> slots = this.slots;
        arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
        for (int i = 0, n = slots.size; i < n; i++)
            slots.get(i).setToSetupPose();
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

    public Slot findSlot(String slotName) {
        if (slotName == null) throw new IllegalArgumentException("slotName cannot be null.");
        Array<Slot> slots = this.slots;
        for (int i = 0, n = slots.size; i < n; i++) {
            Slot slot = slots.get(i);
            if (slot.data.name.equals(slotName)) return slot;
        }
        return null;
    }

    // public Array<Slot> getDrawOrder() {
    //     return drawOrder;
    // }

    // public void setDrawOrder(Array<Slot> drawOrder) {
    //     if (drawOrder == null) throw new IllegalArgumentException("drawOrder cannot be null.");
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
        if (newSkin == skin) return;
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
        if (Loader.spineVersion == 38)
            updateCache();
    }

    // public Attachment getAttachment(String slotName, String attachmentName) {
    //     SlotData slot = data.findSlot(slotName);
    //     if (slot == null) throw new IllegalArgumentException("Slot not found: " + slotName);
    //     return getAttachment(slot.getIndex(), attachmentName);
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

    public void setAttachment(String slotName, String attachmentName) {
        // if (slotName == null) throw new IllegalArgumentException("slotName cannot be null.");
        // Slot slot = findSlot(slotName);
        // if (slot == null) throw new IllegalArgumentException("Slot not found: " + slotName);
        // Attachment attachment = null;
        // if (attachmentName != null) {
        //     attachment = getAttachment(slot.data.index, attachmentName);
        //     if (attachment == null)
        //         throw new IllegalArgumentException("Attachment not found: " + attachmentName + ", for slot: " + slotName);
        // }
        // slot.setAttachment(attachment);
    }

    // public Array<IkConstraint> getIkConstraints() {
    //     return ikConstraints;
    // }

    // public Array<TransformConstraint> getTransformConstraints() {
    //     return transformConstraints;
    // }

    public Color getColor() {
        return color;
    }

    // public void setColor(Color color) {
    //     if (color == null) throw new IllegalArgumentException("color cannot be null.");
    //     this.color.set(color);
    // }

    // public float getScaleX() {
    //     return scaleX;
    // }

    // public void setScaleX(float scaleX) {
    //     this.scaleX = scaleX;
    // }

    // public float getScaleY() {
    //     return scaleY;
    // }

    // public void setScaleY(float scaleY) {
    //     this.scaleY = scaleY;
    // }

    // public void setScale(float scaleX, float scaleY) {
    //     this.scaleX = scaleX;
    //     this.scaleY = scaleY;
    // }

    // public float getX() {
    //     return x;
    // }

    // public void setX(float x) {
    //     this.x = x;
    // }

    // public float getY() {
    //     return y;
    // }

    // public void setY(float y) {
    //     this.y = y;
    // }

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
