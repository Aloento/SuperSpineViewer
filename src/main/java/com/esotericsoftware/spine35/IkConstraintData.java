package com.esotericsoftware.spine35;

import com.badlogic.gdx.utils.Array;

/** Stores the setup pose for an {@link IkConstraint}.
 * <p>
 * See <a href="http://esotericsoftware.com/spine-ik-constraints">IK constraints</a> in the Spine User Guide. */
public class IkConstraintData {
	final String name;
	int order;
	final Array<BoneData> bones = new Array();
	BoneData target;
	int bendDirection = 1;
	float mix = 1;

	public IkConstraintData (String name) {
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		this.name = name;
	}

	/** The IK constraint's name, which is unique within the skeleton. */
	public String getName () {
		return name;
	}

	/** See {@link Constraint#getOrder()}. */
	public int getOrder () {
		return order;
	}

	public void setOrder (int order) {
		this.order = order;
	}

	/** The bones that are constrained by this IK constraint. */
	public Array<BoneData> getBones () {
		return bones;
	}

	/** The bone that is the IK target. */
	public BoneData getTarget () {
		return target;
	}

	public void setTarget (BoneData target) {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		this.target = target;
	}

	/** Controls the bend direction of the IK bones, either 1 or -1. */
	public int getBendDirection () {
		return bendDirection;
	}

	public void setBendDirection (int bendDirection) {
		this.bendDirection = bendDirection;
	}

	/** A percentage (0-1) that controls the mix between the constrained and unconstrained rotations. */
	public float getMix () {
		return mix;
	}

	public void setMix (float mix) {
		this.mix = mix;
	}

	public String toString () {
		return name;
	}
}
