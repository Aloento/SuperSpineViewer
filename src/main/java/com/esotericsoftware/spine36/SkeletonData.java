package com.esotericsoftware.spine36;

import com.badlogic.gdx.utils.Array;

/** Stores the setup pose and all of the stateless data for a skeleton.
 * <p>
 * See <a href="http://esotericsoftware.com/spine-runtime-architecture#Data-objects">Data objects</a> in the Spine Runtimes
 * Guide. */
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
	String version, hash;

	// Nonessential.
	float fps = 30;
	String imagesPath;

	// --- Bones.

	/** The skeleton's bones, sorted parent first. The root bone is always the first bone. */
	public Array<BoneData> getBones () {
		return bones;
	}

	/** Finds a bone by comparing each bone's name. It is more efficient to cache the results of this method than to call it
	 * multiple times.
	 * @return May be null. */
	public BoneData findBone (String boneName) {
		if (boneName == null) throw new IllegalArgumentException("boneName cannot be null.");
		Array<BoneData> bones = this.bones;
		for (int i = 0, n = bones.size; i < n; i++) {
			BoneData bone = bones.get(i);
			if (bone.name.equals(boneName)) return bone;
		}
		return null;
	}

	// --- Slots.

	/** The skeleton's slots. */
	public Array<SlotData> getSlots () {
		return slots;
	}

	/** Finds a slot by comparing each slot's name. It is more efficient to cache the results of this method than to call it
	 * multiple times.
	 * @return May be null. */
	public SlotData findSlot (String slotName) {
		if (slotName == null) throw new IllegalArgumentException("slotName cannot be null.");
		Array<SlotData> slots = this.slots;
		for (int i = 0, n = slots.size; i < n; i++) {
			SlotData slot = slots.get(i);
			if (slot.name.equals(slotName)) return slot;
		}
		return null;
	}

	// --- Skins.

	/** The skeleton's default skin. By default this skin contains all attachments that were not in a skin in Spine.
	 * <p>
	 * See {@link Skeleton#getAttachment(int, String)}.
	 * @return May be null. */
	public Skin getDefaultSkin () {
		return defaultSkin;
	}

	/** @param defaultSkin May be null. */
	public void setDefaultSkin (Skin defaultSkin) {
		this.defaultSkin = defaultSkin;
	}

	/** Finds a skin by comparing each skin's name. It is more efficient to cache the results of this method than to call it
	 * multiple times.
	 * @return May be null. */
	public Skin findSkin (String skinName) {
		if (skinName == null) throw new IllegalArgumentException("skinName cannot be null.");
		for (Skin skin : skins)
			if (skin.name.equals(skinName)) return skin;
		return null;
	}

	/** All skins, including the default skin. */
	public Array<Skin> getSkins () {
		return skins;
	}

	// --- Events.

	/** Finds an event by comparing each events's name. It is more efficient to cache the results of this method than to call it
	 * multiple times.
	 * @return May be null. */
	public EventData findEvent (String eventDataName) {
		if (eventDataName == null) throw new IllegalArgumentException("eventDataName cannot be null.");
		for (EventData eventData : events)
			if (eventData.name.equals(eventDataName)) return eventData;
		return null;
	}

	/** The skeleton's events. */
	public Array<EventData> getEvents () {
		return events;
	}

	// --- Animations.

	/** The skeleton's animations. */
	public Array<Animation> getAnimations () {
		return animations;
	}

	/** Finds an animation by comparing each animation's name. It is more efficient to cache the results of this method than to
	 * call it multiple times.
	 * @return May be null. */
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

	/** The skeleton's IK constraints. */
	public Array<IkConstraintData> getIkConstraints () {
		return ikConstraints;
	}

	/** Finds an IK constraint by comparing each IK constraint's name. It is more efficient to cache the results of this method
	 * than to call it multiple times.
	 * @return May be null. */
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

	/** The skeleton's transform constraints. */
	public Array<TransformConstraintData> getTransformConstraints () {
		return transformConstraints;
	}

	/** Finds a transform constraint by comparing each transform constraint's name. It is more efficient to cache the results of
	 * this method than to call it multiple times.
	 * @return May be null. */
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

	/** The skeleton's path constraints. */
	public Array<PathConstraintData> getPathConstraints () {
		return pathConstraints;
	}

	/** Finds a path constraint by comparing each path constraint's name. It is more efficient to cache the results of this method
	 * than to call it multiple times.
	 * @return May be null. */
	public PathConstraintData findPathConstraint (String constraintName) {
		if (constraintName == null) throw new IllegalArgumentException("constraintName cannot be null.");
		Array<PathConstraintData> pathConstraints = this.pathConstraints;
		for (int i = 0, n = pathConstraints.size; i < n; i++) {
			PathConstraintData constraint = pathConstraints.get(i);
			if (constraint.name.equals(constraintName)) return constraint;
		}
		return null;
	}

	// ---

	/** The skeleton's name, which by default is the name of the skeleton data file, if possible.
	 * @return May be null. */
	public String getName () {
		return name;
	}

	/** @param name May be null. */
	public void setName (String name) {
		this.name = name;
	}

	/** The width of the skeleton's axis aligned bounding box in the setup pose. */
	public float getWidth () {
		return width;
	}

	public void setWidth (float width) {
		this.width = width;
	}

	/** The height of the skeleton's axis aligned bounding box in the setup pose. */
	public float getHeight () {
		return height;
	}

	public void setHeight (float height) {
		this.height = height;
	}

	/** The Spine version used to export the skeleton data, or null. */
	public String getVersion () {
		return version;
	}

	/** @param version May be null. */
	public void setVersion (String version) {
		this.version = version;
	}

	/** The skeleton data hash. This value will change if any of the skeleton data has changed.
	 * @return May be null. */
	public String getHash () {
		return hash;
	}

	/** @param hash May be null. */
	public void setHash (String hash) {
		this.hash = hash;
	}

	/** The path to the image directory as defined in Spine. Available only when nonessential data was exported.
	 * @return May be null. */
	public String getImagesPath () {
		return imagesPath;
	}

	/** @param imagesPath May be null. */
	public void setImagesPath (String imagesPath) {
		this.imagesPath = imagesPath;
	}

	/** The dopesheet FPS in Spine. Available only when nonessential data was exported. */
	public float getFps () {
		return fps;
	}

	public void setFps (float fps) {
		this.fps = fps;
	}

	public String toString () {
		return name != null ? name : super.toString();
	}
}
