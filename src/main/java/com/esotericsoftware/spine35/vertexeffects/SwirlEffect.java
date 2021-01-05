
package com.esotericsoftware.spine35.vertexeffects;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.spine35.Skeleton;
import com.esotericsoftware.spine35.SkeletonRenderer.VertexEffect;
import com.esotericsoftware.spine35.utils.SpineUtils;

public class SwirlEffect implements VertexEffect {
	private float worldX, worldY, radius, angle;
	private Interpolation interpolation = Interpolation.pow2Out;
	private float centerX, centerY;

	public SwirlEffect (float radius) {
		this.radius = radius;
	}

	public void begin (Skeleton skeleton) {
		worldX = skeleton.getX() + centerX;
		worldY = skeleton.getY() + centerY;
	}

	public void transform (Vector2 vertex) {
		float x = vertex.x - worldX;
		float y = vertex.y - worldY;
		float dist = (float)Math.sqrt(x * x + y * y);
		if (dist < radius) {
			float theta = interpolation.apply(0, angle, (radius - dist) / radius);
			float cos = SpineUtils.cos(theta), sin = SpineUtils.sin(theta);
			vertex.x = cos * x - sin * y + worldX;
			vertex.y = sin * x + cos * y + worldY;
		}
	}

	public void end () {
	}

	public void setRadius (float radius) {
		this.radius = radius;
	}

	public void setCenter (float centerX, float centerY) {
		this.centerX = centerX;
		this.centerY = centerY;
	}

	public void setCenterX (float centerX) {
		this.centerX = centerX;
	}

	public void setCenterY (float centerY) {
		this.centerY = centerY;
	}

	public void setAngle (float degrees) {
		this.angle = degrees * MathUtils.degRad;
	}

	public Interpolation getInterpolation () {
		return interpolation;
	}

	public void setInterpolation (Interpolation interpolation) {
		this.interpolation = interpolation;
	}
}
