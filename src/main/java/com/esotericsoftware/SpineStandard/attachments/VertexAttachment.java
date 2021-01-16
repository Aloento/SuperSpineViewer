package com.esotericsoftware.SpineStandard.attachments;

import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.SpineStandard.Bone;
import com.esotericsoftware.SpineStandard.Skeleton;
import com.esotericsoftware.SpineStandard.Slot;

import static com.esotericsoftware.SpineStandard.utils.SpineUtils.arraycopy;

abstract public class VertexAttachment extends Attachment {
    static private int nextID;

    private final int id = (nextID() & 65535) << 11;
    int[] bones;
    float[] vertices;
    int worldVerticesLength;
    VertexAttachment deformAttachment = this;

    public VertexAttachment(String name) {
        super(name);
    }

    static private synchronized int nextID() {
        return nextID++;
    }

    public void computeWorldVertices(Slot slot, int start, int count, float[] worldVertices, int offset, int stride) {
        count = offset + (count >> 1) * stride;
        Skeleton skeleton = slot.getSkeleton();
        FloatArray deformArray = slot.getDeform();
        float[] vertices = this.vertices;
        int[] bones = this.bones;
        if (bones == null) {
            if (deformArray.size > 0) vertices = deformArray.items;
            Bone bone = slot.getBone();
            float x = bone.getWorldX(), y = bone.getWorldY();
            float a = bone.getA(), b = bone.getB(), c = bone.getC(), d = bone.getD();
            for (int v = start, w = offset; w < count; v += 2, w += stride) {
                float vx = vertices[v], vy = vertices[v + 1];
                worldVertices[w] = vx * a + vy * b + x;
                worldVertices[w + 1] = vx * c + vy * d + y;
            }
            return;
        }
        int v = 0, skip = 0;
        for (int i = 0; i < start; i += 2) {
            int n = bones[v];
            v += n + 1;
            skip += n;
        }
        Object[] skeletonBones = skeleton.getBones().items;
        if (deformArray.size == 0) {
            for (int w = offset, b = skip * 3; w < count; w += stride) {
                float wx = 0, wy = 0;
                int n = bones[v++];
                n += v;
                for (; v < n; v++, b += 3) {
                    Bone bone = (Bone) skeletonBones[bones[v]];
                    float vx = vertices[b], vy = vertices[b + 1], weight = vertices[b + 2];
                    wx += (vx * bone.getA() + vy * bone.getB() + bone.getWorldX()) * weight;
                    wy += (vx * bone.getC() + vy * bone.getD() + bone.getWorldY()) * weight;
                }
                worldVertices[w] = wx;
                worldVertices[w + 1] = wy;
            }
        } else {
            float[] deform = deformArray.items;
            for (int w = offset, b = skip * 3, f = skip << 1; w < count; w += stride) {
                float wx = 0, wy = 0;
                int n = bones[v++];
                n += v;
                for (; v < n; v++, b += 3, f += 2) {
                    Bone bone = (Bone) skeletonBones[bones[v]];
                    float vx = vertices[b] + deform[f], vy = vertices[b + 1] + deform[f + 1], weight = vertices[b + 2];
                    wx += (vx * bone.getA() + vy * bone.getB() + bone.getWorldX()) * weight;
                    wy += (vx * bone.getC() + vy * bone.getD() + bone.getWorldY()) * weight;
                }
                worldVertices[w] = wx;
                worldVertices[w + 1] = wy;
            }
        }
    }

    public boolean applyDeform(VertexAttachment sourceAttachment) {
        return this == sourceAttachment;
    }

    public VertexAttachment getDeformAttachment() {
        return deformAttachment;
    }

    public void setDeformAttachment(VertexAttachment deformAttachment) {
        this.deformAttachment = deformAttachment;
    }

    public int[] getBones() {
        return bones;
    }
    
    public void setBones(int[] bones) {
        this.bones = bones;
    }
    
    public float[] getVertices() {
        return vertices;
    }

    public void setVertices(float[] vertices) {
        this.vertices = vertices;
    }
    
    public int getWorldVerticesLength() {
        return worldVerticesLength;
    }

    public void setWorldVerticesLength(int worldVerticesLength) {
        this.worldVerticesLength = worldVerticesLength;
    }

    public int getId() {
        return id;
    }

    void copyTo(VertexAttachment attachment) {
        if (bones != null) {
            attachment.bones = new int[bones.length];
            arraycopy(bones, 0, attachment.bones, 0, bones.length);
        } else
            attachment.bones = null;

        if (vertices != null) {
            attachment.vertices = new float[vertices.length];
            arraycopy(vertices, 0, attachment.vertices, 0, vertices.length);
        } else
            attachment.vertices = null;

        attachment.worldVerticesLength = worldVerticesLength;
        attachment.deformAttachment = deformAttachment;
    }
}
