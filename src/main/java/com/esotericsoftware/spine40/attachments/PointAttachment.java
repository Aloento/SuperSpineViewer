package com.esotericsoftware.spine40.attachments;

import static com.badlogic.gdx.math.MathUtils.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import com.esotericsoftware.spine40.Bone;

/** An attachment which is a single point and a rotation. This can be used to spawn projectiles, particles, etc. A bone can be
 * used in similar ways, but a PointAttachment is slightly less expensive to compute and can be hidden, shown, and placed in a
 * skin.
 * <p>
 * See <a href="http://esotericsoftware.com/spine-point-attachments">Point Attachments</a> in the Spine User Guide. */
public class PointAttachment extends Attachment {
	float x, y, rotation;

	// Nonessential.
	final Color color = new Color(0.9451f, 0.9451f, 0, 1); // f1f100ff

	public PointAttachment (String name) {
		super(name);
	}

	public float getX () {
		return x;
	}

	public void setX (float x) {
		this.x = x;
	}

	public float getY () {
		return y;
	}

	public void setY (float y) {
		this.y = y;
	}

	public float getRotation () {
		return rotation;
	}

	public void setRotation (float rotation) {
		this.rotation = rotation;
	}

	/** The color of the point attachment as it was in Spine, or a default clor if nonessential data was not exported. Point
	 * attachments are not usually rendered at runtime. */
	public Color getColor () {
		return color;
	}

	public Vector2 computeWorldPosition (Bone bone, Vector2 point) {
		point.x = x * bone.getA() + y * bone.getB() + bone.getWorldX();
		point.y = x * bone.getC() + y * bone.getD() + bone.getWorldY();
		return point;
	}

	public float computeWorldRotation (Bone bone) {
		float cos = cosDeg(rotation), sin = sinDeg(rotation);
		float x = cos * bone.getA() + sin * bone.getB();
		float y = cos * bone.getC() + sin * bone.getD();
		return (float)Math.atan2(y, x) * radDeg;
	}

	public Attachment copy () {
		PointAttachment copy = new PointAttachment(name);
		copy.x = x;
		copy.y = y;
		copy.rotation = rotation;
		copy.color.set(color);
		return copy;
	}
}
