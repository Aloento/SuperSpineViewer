package com.esotericsoftware.SpinePreview;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;

public class SkeletonData {
    final Array<BoneData> bones = new Array();
    final Array<SlotData> slots = new Array();
    final Array<Skin> skins = new Array();
    final Array<EventData> events = new Array();
    final Array<Animation> animations = new Array();
    final Array<IkConstraintData> ikConstraints = new Array();
    final Array<TransformConstraintData> transformConstraints = new Array();
    final Array<PathConstraintData> pathConstraints = new Array();
    @Null
    String name;
    @Null
    Skin defaultSkin;
    float x, y, width, height;
    @Null
    String version, hash;
    float fps = 30;
    @Null
    String imagesPath, audioPath;

    public SkeletonData() {
        super();
    }

    public Array<BoneData> getBones() {
        return bones;
    }

    public @Null
    BoneData findBone(String boneName) {
        if (boneName == null) throw new IllegalArgumentException("boneName cannot be null.");
        Object[] bones = this.bones.items;
        for (int i = 0, n = this.bones.size; i < n; i++) {
            BoneData bone = (BoneData) bones[i];
            if (bone.name.equals(boneName)) return bone;
        }
        return null;
    }

    public Array<SlotData> getSlots() {
        return slots;
    }

    public @Null
    SlotData findSlot(String slotName) {
        if (slotName == null) throw new IllegalArgumentException("slotName cannot be null.");
        Object[] slots = this.slots.items;
        for (int i = 0, n = this.slots.size; i < n; i++) {
            SlotData slot = (SlotData) slots[i];
            if (slot.name.equals(slotName)) return slot;
        }
        return null;
    }

    public @Null
    Skin getDefaultSkin() {
        return defaultSkin;
    }

    public void setDefaultSkin(@Null Skin defaultSkin) {
        this.defaultSkin = defaultSkin;
    }

    public @Null
    Skin findSkin(String skinName) {
        if (skinName == null) throw new IllegalArgumentException("skinName cannot be null.");
        for (Skin skin : skins)
            if (skin.name.equals(skinName)) return skin;
        return null;
    }

    public Array<Skin> getSkins() {
        return skins;
    }

    public @Null
    EventData findEvent(String eventDataName) {
        if (eventDataName == null) throw new IllegalArgumentException("eventDataName cannot be null.");
        for (EventData eventData : events)
            if (eventData.name.equals(eventDataName)) return eventData;
        return null;
    }

    public Array<EventData> getEvents() {
        return events;
    }

    public Array<Animation> getAnimations() {
        return animations;
    }

    public @Null
    Animation findAnimation(String animationName) {
        if (animationName == null) throw new IllegalArgumentException("animationName cannot be null.");
        Object[] animations = this.animations.items;
        for (int i = 0, n = this.animations.size; i < n; i++) {
            Animation animation = (Animation) animations[i];
            if (animation.name.equals(animationName)) return animation;
        }
        return null;
    }

    public Array<IkConstraintData> getIkConstraints() {
        return ikConstraints;
    }

    public @Null
    IkConstraintData findIkConstraint(String constraintName) {
        if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
        Object[] ikConstraints = this.ikConstraints.items;
        for (int i = 0, n = this.ikConstraints.size; i < n; i++) {
            IkConstraintData constraint = (IkConstraintData) ikConstraints[i];
            if (constraint.name.equals(constraintName)) return constraint;
        }
        return null;
    }

    public Array<TransformConstraintData> getTransformConstraints() {
        return transformConstraints;
    }

    public @Null
    TransformConstraintData findTransformConstraint(String constraintName) {
        if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
        Object[] transformConstraints = this.transformConstraints.items;
        for (int i = 0, n = this.transformConstraints.size; i < n; i++) {
            TransformConstraintData constraint = (TransformConstraintData) transformConstraints[i];
            if (constraint.name.equals(constraintName)) return constraint;
        }
        return null;
    }

    public Array<PathConstraintData> getPathConstraints() {
        return pathConstraints;
    }

    public @Null
    PathConstraintData findPathConstraint(String constraintName) {
        if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
        Object[] pathConstraints = this.pathConstraints.items;
        for (int i = 0, n = this.pathConstraints.size; i < n; i++) {
            PathConstraintData constraint = (PathConstraintData) pathConstraints[i];
            if (constraint.name.equals(constraintName)) return constraint;
        }
        return null;
    }

    public @Null
    String getName() {
        return name;
    }

    public void setName(@Null String name) {
        this.name = name;
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

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public @Null
    String getVersion() {
        return version;
    }

    public void setVersion(@Null String version) {
        this.version = version;
    }

    public @Null
    String getHash() {
        return hash;
    }

    public void setHash(@Null String hash) {
        this.hash = hash;
    }

    public @Null
    String getImagesPath() {
        return imagesPath;
    }

    public void setImagesPath(@Null String imagesPath) {
        this.imagesPath = imagesPath;
    }

    public @Null
    String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(@Null String audioPath) {
        this.audioPath = audioPath;
    }

    public float getFps() {
        return fps;
    }

    public void setFps(float fps) {
        this.fps = fps;
    }

    public String toString() {
        return name != null ? name : super.toString();
    }
}
