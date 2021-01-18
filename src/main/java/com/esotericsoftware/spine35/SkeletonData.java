package com.esotericsoftware.spine35;

import com.badlogic.gdx.utils.Array;

public class SkeletonData {
    final Array<BoneData> bones = new Array<>();
    final Array<SlotData> slots = new Array<>();
    final Array<Skin> skins = new Array<>();
    final Array<EventData> events = new Array<>();
    final Array<Animation> animations = new Array<>();
    final Array<IkConstraintData> ikConstraints = new Array<>();
    final Array<TransformConstraintData> transformConstraints = new Array<>();
    final Array<PathConstraintData> pathConstraints = new Array<>();
    String name;
    Skin defaultSkin;
    float width, height;
    String version, hash;


    float fps = 30;
    String imagesPath;



    
    public Array<BoneData> getBones() {
        return bones;
    }

    
    public BoneData findBone(String boneName) {
        if (boneName == null) throw new IllegalArgumentException("boneName cannot be null.");
        Array<BoneData> bones = this.bones;
        for (int i = 0, n = bones.size; i < n; i++) {
            BoneData bone = bones.get(i);
            if (bone.name.equals(boneName)) return bone;
        }
        return null;
    }



    
    public Array<SlotData> getSlots() {
        return slots;
    }

    
    public SlotData findSlot(String slotName) {
        if (slotName == null) throw new IllegalArgumentException("slotName cannot be null.");
        Array<SlotData> slots = this.slots;
        for (int i = 0, n = slots.size; i < n; i++) {
            SlotData slot = slots.get(i);
            if (slot.name.equals(slotName)) return slot;
        }
        return null;
    }



    
    public Skin getDefaultSkin() {
        return defaultSkin;
    }

    
    public void setDefaultSkin(Skin defaultSkin) {
        this.defaultSkin = defaultSkin;
    }

    
    public Skin findSkin(String skinName) {
        if (skinName == null) throw new IllegalArgumentException("skinName cannot be null.");
        for (Skin skin : skins)
            if (skin.name.equals(skinName)) return skin;
        return null;
    }

    
    public Array<Skin> getSkins() {
        return skins;
    }



    
    public EventData findEvent(String eventDataName) {
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

    
    public Animation findAnimation(String animationName) {
        if (animationName == null) throw new IllegalArgumentException("animationName cannot be null.");
        Array<Animation> animations = this.animations;
        for (int i = 0, n = animations.size; i < n; i++) {
            Animation animation = animations.get(i);
            if (animation.name.equals(animationName)) return animation;
        }
        return null;
    }



    
    public Array<IkConstraintData> getIkConstraints() {
        return ikConstraints;
    }

    
    public IkConstraintData findIkConstraint(String constraintName) {
        if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
        Array<IkConstraintData> ikConstraints = this.ikConstraints;
        for (int i = 0, n = ikConstraints.size; i < n; i++) {
            IkConstraintData constraint = ikConstraints.get(i);
            if (constraint.name.equals(constraintName)) return constraint;
        }
        return null;
    }



    
    public Array<TransformConstraintData> getTransformConstraints() {
        return transformConstraints;
    }

    
    public TransformConstraintData findTransformConstraint(String constraintName) {
        if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
        Array<TransformConstraintData> transformConstraints = this.transformConstraints;
        for (int i = 0, n = transformConstraints.size; i < n; i++) {
            TransformConstraintData constraint = transformConstraints.get(i);
            if (constraint.name.equals(constraintName)) return constraint;
        }
        return null;
    }



    
    public Array<PathConstraintData> getPathConstraints() {
        return pathConstraints;
    }

    
    public PathConstraintData findPathConstraint(String constraintName) {
        if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
        Array<PathConstraintData> pathConstraints = this.pathConstraints;
        for (int i = 0, n = pathConstraints.size; i < n; i++) {
            PathConstraintData constraint = pathConstraints.get(i);
            if (constraint.name.equals(constraintName)) return constraint;
        }
        return null;
    }



    
    public String getName() {
        return name;
    }

    
    public void setName(String name) {
        this.name = name;
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

    
    public String getVersion() {
        return version;
    }

    
    public void setVersion(String version) {
        this.version = version;
    }

    
    public String getHash() {
        return hash;
    }

    
    public void setHash(String hash) {
        this.hash = hash;
    }

    
    public String getImagesPath() {
        return imagesPath;
    }

    
    public void setImagesPath(String imagesPath) {
        this.imagesPath = imagesPath;
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
