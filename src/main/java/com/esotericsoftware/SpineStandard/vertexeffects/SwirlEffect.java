package com.esotericsoftware.SpineStandard.vertexeffects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.SpineStandard.Skeleton;
import com.esotericsoftware.SpineStandard.SkeletonRenderer.VertexEffect;
import com.esotericsoftware.SpineStandard.utils.SpineUtils;

public class SwirlEffect implements VertexEffect {
    private float worldX, worldY, radius, angle;
    private final Interpolation interpolation = Interpolation.pow2Out;
    private float centerX, centerY;

    public void begin(Skeleton skeleton) {
        worldX = skeleton.getX() + centerX;
        worldY = skeleton.getY() + centerY;
    }

    public void transform(Vector2 position, Vector2 uv, Color light, Color dark) {
        float x = position.x - worldX;
        float y = position.y - worldY;
        float dist = (float) Math.sqrt(x * x + y * y);
        if (dist < radius) {
            float theta = interpolation.apply(0, angle, (radius - dist) / radius);
            float cos = SpineUtils.cos(theta), sin = SpineUtils.sin(theta);
            position.x = cos * x - sin * y + worldX;
            position.y = sin * x + cos * y + worldY;
        }
    }

    public void end() {
    }

    public void setAngle(float degrees) {
        this.angle = degrees * MathUtils.degRad;
    }

}
