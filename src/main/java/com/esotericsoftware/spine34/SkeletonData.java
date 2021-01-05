package com.esotericsoftware.spine34;

import com.badlogic.gdx.utils.Array;

public class SkeletonData {
	String name;
	final Array<BoneData> bones = new Array(); // Ordered parents first.
	final Array<SlotData> slots = new Array(); // Setup pose draw order.
	final Array<Skin> skins = new Array();
	Skin defaultSkin;
	final Array<EventData> events = new Array();
	final Array<Animation> animations = new Array();
	final Array<IkConstraintData> ikConstraints = new Array();
	final Array<TransformConstraintData> transformConstraints = new Array();
	final Array<PathConstraintData> pathConstraints = new Array();
	float width, height;
	String version, hash, imagesPath;

	// --- Bones.

	public Array<BoneData> getBones () {
		return bones;
	}

	/** @return May be null. */
	public BoneData findBone (String boneName) {
		if (boneName == null) throw new IllegalArgumentException("boneName cannot be null.");
		Array<BoneData> bones = this.bones;
		for (int i = 0, n = bones.size; i < n; i++) {
			BoneData bone = bones.get(i);
			if (bone.name.equals(boneName)) return bone;
		}
		return null;
	}

	/** @return -1 if the bone was not found. */
	public int findBoneIndex (String boneName) {
		if (boneName == null) throw new IllegalArgumentException("boneName cannot be null.");
		Array<BoneData> bones = this.bones;
		for (int i = 0, n = bones.size; i < n; i++)
			if (bones.get(i).name.equals(boneName)) return i;
		return -1;
	}

	// --- Slots.

	public Array<SlotData> getSlots () {
		return slots;
	}

	/** @return May be null. */
	public SlotData findSlot (String slotName) {
		if (slotName == null) throw new IllegalArgumentException("slotName cannot be null.");
		Array<SlotData> slots = this.slots;
		for (int i = 0, n = slots.size; i < n; i++) {
			SlotData slot = slots.get(i);
			if (slot.name.equals(slotName)) return slot;
		}
		return null;
	}

	/** @return -1 if the slot was not found. */
	public int findSlotIndex (String slotName) {
		if (slotName == null) throw new IllegalArgumentException("slotName cannot be null.");
		Array<SlotData> slots = this.slots;
		for (int i = 0, n = slots.size; i < n; i++)
			if (slots.get(i).name.equals(slotName)) return i;
		return -1;
	}

	// --- Skins.

	/** @return May be null. */
	public Skin getDefaultSkin () {
		return defaultSkin;
	}

	/** @param defaultSkin May be null. */
	public void setDefaultSkin (Skin defaultSkin) {
		this.defaultSkin = defaultSkin;
	}

	/** @return May be null. */
	public Skin findSkin (String skinName) {
		if (skinName == null) throw new IllegalArgumentException("skinName cannot be null.");
		for (Skin skin : skins)
			if (skin.name.equals(skinName)) return skin;
		return null;
	}

	/** Returns all skins, including the default skin. */
	public Array<Skin> getSkins () {
		return skins;
	}

	// --- Events.

	/** @return May be null. */
	public EventData findEvent (String eventDataName) {
		if (eventDataName == null) throw new IllegalArgumentException("eventDataName cannot be null.");
		for (EventData eventData : events)
			if (eventData.name.equals(eventDataName)) return eventData;
		return null;
	}

	public Array<EventData> getEvents () {
		return events;
	}

	// --- Animations.

	public Array<Animation> getAnimations () {
		return animations;
	}

	/** @return May be null. */
	public Animation findAnimation (String animationName) {
		if (animationName == null) throw new IllegalArgumentException("animationName cannot be null.");
		Array<Animation> animations = this.animations;
		for (int i = 0, n = animations.size; i < n; i++) {
			Animation animation = animations.get(i);
			if (animation.name.equals(animationName)) return animation;
		}
		return null;
	}

	// --- IK constraints

	public Array<IkConstraintData> getIkConstraints () {
		return ikConstraints;
	}

	/** @return May be null. */
	public IkConstraintData findIkConstraint (String constraintName) {
		if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
		Array<IkConstraintData> ikConstraints = this.ikConstraints;
		for (int i = 0, n = ikConstraints.size; i < n; i++) {
			IkConstraintData constraint = ikConstraints.get(i);
			if (constraint.name.equals(constraintName)) return constraint;
		}
		return null;
	}

	// --- Transform constraints

	public Array<TransformConstraintData> getTransformConstraints () {
		return transformConstraints;
	}

	/** @return May be null. */
	public TransformConstraintData findTransformConstraint (String constraintName) {
		if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
		Array<TransformConstraintData> transformConstraints = this.transformConstraints;
		for (int i = 0, n = transformConstraints.size; i < n; i++) {
			TransformConstraintData constraint = transformConstraints.get(i);
			if (constraint.name.equals(constraintName)) return constraint;
		}
		return null;
	}

	// --- Path constraints

	public Array<PathConstraintData> getPathConstraints () {
		return pathConstraints;
	}

	/** @return May be null. */
	public PathConstraintData findPathConstraint (String constraintName) {
		if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
		Array<PathConstraintData> pathConstraints = this.pathConstraints;
		for (int i = 0, n = pathConstraints.size; i < n; i++) {
			PathConstraintData constraint = pathConstraints.get(i);
			if (constraint.name.equals(constraintName)) return constraint;
		}
		return null;
	}

	/** @return -1 if the path constraint was not found. */
	public int findPathConstraintIndex (String pathConstraintName) {
		if (pathConstraintName == null) throw new IllegalArgumentException("pathConstraintName cannot be null.");
		Array<PathConstraintData> pathConstraints = this.pathConstraints;
		for (int i = 0, n = pathConstraints.size; i < n; i++)
			if (pathConstraints.get(i).name.equals(pathConstraintName)) return i;
		return -1;
	}

	// ---

	/** @return May be null. */
	public String getName () {
		return name;
	}

	/** @param name May be null. */
	public void setName (String name) {
		this.name = name;
	}

	public float getWidth () {
		return width;
	}

	public void setWidth (float width) {
		this.width = width;
	}

	public float getHeight () {
		return height;
	}

	public void setHeight (float height) {
		this.height = height;
	}

	/** Returns the Spine version used to export this data, or null. */
	public String getVersion () {
		return version;
	}

	/** @param version May be null. */
	public void setVersion (String version) {
		this.version = version;
	}

	/** @return May be null. */
	public String getHash () {
		return hash;
	}

	/** @param hash May be null. */
	public void setHash (String hash) {
		this.hash = hash;
	}

	/** @return May be null. */
	public String getImagesPath () {
		return imagesPath;
	}

	/** @param imagesPath May be null. */
	public void setImagesPath (String imagesPath) {
		this.imagesPath = imagesPath;
	}

	public String toString () {
		return name != null ? name : super.toString();
	}
}
