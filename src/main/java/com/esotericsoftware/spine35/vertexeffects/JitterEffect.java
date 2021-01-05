
package com.esotericsoftware.spine35.vertexeffects;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.spine35.Skeleton;
import com.esotericsoftware.spine35.SkeletonRenderer.VertexEffect;

public class JitterEffect implements VertexEffect {
	private float x, y;

	public JitterEffect (float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void begin (Skeleton skeleton) {
	}

	public void transform (Vector2 vertex) {
		vertex.x += MathUtils.randomTriangular(-x, y);
		vertex.y += MathUtils.randomTriangular(-x, y);
	}

	public void end () {
	}

	public void setJitter (float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void setJitterX (float x) {
		this.x = x;
	}

	public void setJitterY (float y) {
		this.y = y;
	}
}
