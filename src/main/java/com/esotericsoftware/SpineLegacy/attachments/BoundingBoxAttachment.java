package com.esotericsoftware.SpineLegacy.attachments;

import com.esotericsoftware.SpineLegacy.Bone;
import com.esotericsoftware.SpineLegacy.Skeleton;

public class BoundingBoxAttachment extends Attachment {
    private float[] vertices;

    public BoundingBoxAttachment(String name) {
        super(name);
    }

    public void computeWorldVertices(Bone bone, float[] worldVertices) {
        Skeleton skeleton = bone.getSkeleton();
        float x = skeleton.getX() + bone.getWorldX(), y = skeleton.getY() + bone.getWorldY();
        float m00 = bone.getA();
        float m01 = bone.getB();
        float m10 = bone.getC();
        float m11 = bone.getD();
        float[] vertices = this.vertices;
        for (int i = 0, n = vertices.length; i < n; i += 2) {
            float px = vertices[i];
            float py = vertices[i + 1];
            worldVertices[i] = px * m00 + py * m01 + x;
            worldVertices[i + 1] = px * m10 + py * m11 + y;
        }
    }

    public float[] getVertices() {
        return vertices;
    }

    public void setVertices(float[] vertices) {
        this.vertices = vertices;
    }
}
