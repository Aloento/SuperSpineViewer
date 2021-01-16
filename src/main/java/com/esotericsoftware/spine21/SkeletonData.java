package com.esotericsoftware.spine21;

import com.badlogic.gdx.utils.Array;

public class SkeletonData {
    final Array<BoneData> bones = new Array();
    final Array<SlotData> slots = new Array();
    final Array<Skin> skins = new Array();
    final Array<EventData> events = new Array();
    final Array<Animation> animations = new Array();
    final Array<IkConstraintData> ikConstraints = new Array();
    String name;
    Skin defaultSkin;
    float width, height;
    String version, hash, imagesPath;



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


    public int findBoneIndex(String boneName) {
        if (boneName == null) throw new IllegalArgumentException("boneName cannot be null.");
        Array<BoneData> bones = this.bones;
        for (int i = 0, n = bones.size; i < n; i++)
            if (bones.get(i).name.equals(boneName)) return i;
        return -1;
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


    public int findSlotIndex(String slotName) {
        if (slotName == null) throw new IllegalArgumentException("slotName cannot be null.");
        Array<SlotData> slots = this.slots;
        for (int i = 0, n = slots.size; i < n; i++)
            if (slots.get(i).name.equals(slotName)) return i;
        return -1;
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


    public IkConstraintData findIkConstraint(String ikConstraintName) {
        if (ikConstraintName == null) throw new IllegalArgumentException("ikConstraintName cannot be null.");
        Array<IkConstraintData> ikConstraints = this.ikConstraints;
        for (int i = 0, n = ikConstraints.size; i < n; i++) {
            IkConstraintData ikConstraint = ikConstraints.get(i);
            if (ikConstraint.name.equals(ikConstraintName)) return ikConstraint;
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

    public String toString() {
        return name != null ? name : super.toString();
    }
}
