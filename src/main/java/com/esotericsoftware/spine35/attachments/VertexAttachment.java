package com.esotericsoftware.spine35.attachments;

import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.spine35.Bone;
import com.esotericsoftware.spine35.Skeleton;
import com.esotericsoftware.spine35.Slot;

/**
 * Base class for an attachment with vertices that are transformed by one or more bones and can be deformed by a slot's
 * {@link Slot#getAttachmentVertices()}.
 */
public class VertexAttachment extends Attachment {
    int[] bones;
    float[] vertices;
    int worldVerticesLength;

    public VertexAttachment(String name) {
        super(name);
    }

    /**
     * Transforms the attachment's local {@link #getVertices()} to world coordinates, using 0 for <code>start</code> and
     * <code>offset</code>.
     * <p>
     * See {@link #computeWorldVertices(Slot, int, int, float[], int)}.
     */
    public void computeWorldVertices(Slot slot, float[] worldVertices) {
        computeWorldVertices(slot, 0, worldVerticesLength, worldVertices, 0);
    }

    /**
     * Transforms the attachment's local {@link #getVertices()} to world coordinates. If the slot has
     * {@link Slot#getAttachmentVertices()}, they are used to deform the vertices.
     * <p>
     * See <a href="http://esotericsoftware.com/spine-runtime-skeletons#World-transforms">World transforms</a> in the Spine
     * Runtimes Guide.
     *
     * @param start         The index of the first {@link #getVertices()} value to transform. Each vertex has 2 values, x and y.
     * @param count         The number of world vertex values to output. Must be <= {@link #getWorldVerticesLength()} - <code>start</code>.
     * @param worldVertices The output world vertices. Must have a length >= <code>offset</code> + <code>count</code>.
     * @param offset        The <code>worldVertices</code> index to begin writing values.
     */
    public void computeWorldVertices(Slot slot, int start, int count, float[] worldVertices, int offset) {
        count += offset;
        Skeleton skeleton = slot.getSkeleton();
        FloatArray deformArray = slot.getAttachmentVertices();
        float[] vertices = this.vertices;
        int[] bones = this.bones;
        if (bones == null) {
            if (deformArray.size > 0) vertices = deformArray.items;
            Bone bone = slot.getBone();
            float x = bone.getWorldX(), y = bone.getWorldY();
            float a = bone.getA(), b = bone.getB(), c = bone.getC(), d = bone.getD();
            for (int v = start, w = offset; w < count; v += 2, w += 2) {
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
            for (int w = offset, b = skip * 3; w < count; w += 2) {
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
            for (int w = offset, b = skip * 3, f = skip << 1; w < count; w += 2) {
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

    /**
     * Returns true if a deform originally applied to the specified attachment should be applied to this attachment. The default
     * implementation returns true only when <code>sourceAttachment</code> is this attachment.
     */
    public boolean applyDeform(VertexAttachment sourceAttachment) {
        return this == sourceAttachment;
    }

    /**
     * The bones which affect the {@link #getVertices()}. The array entries are, for each vertex, the number of bones affecting
     * the vertex followed by that many bone indices, which is the index of the bone in {@link Skeleton#getBones()}. Will be null
     * if this attachment has no weights.
     */
    public int[] getBones() {
        return bones;
    }

    /**
     * @param bones May be null if this attachment has no weights.
     */
    public void setBones(int[] bones) {
        this.bones = bones;
    }

    /**
     * The vertex positions in the bone's coordinate system. For a non-weighted attachment, the values are <code>x,y</code>
     * entries for each vertex. For a weighted attachment, the values are <code>x,y,weight</code> entries for each bone affecting
     * each vertex.
     */
    public float[] getVertices() {
        return vertices;
    }

    public void setVertices(float[] vertices) {
        this.vertices = vertices;
    }

    /**
     * The maximum length required of the <code>worldVertices</code> passed to
     * {@link #computeWorldVertices(Slot, int, int, float[], int)}.
     */
    public int getWorldVerticesLength() {
        return worldVerticesLength;
    }

    public void setWorldVerticesLength(int worldVerticesLength) {
        this.worldVerticesLength = worldVerticesLength;
    }
}
