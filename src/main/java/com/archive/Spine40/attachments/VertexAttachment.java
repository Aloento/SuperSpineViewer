package com.archive.Spine40.attachments;

import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Null;
import com.archive.Spine40.Bone;
import com.archive.Spine40.Skeleton;
import com.archive.Spine40.Slot;

import static com.archive.Spine40.utils.SpineUtils.arraycopy;

/**
 * Base class for an attachment with vertices that are transformed by one or more bones and can be deformed by a slot's
 * {@link Slot#getDeform()}.
 */
abstract public class VertexAttachment extends Attachment {
    static private int nextID;

    private final int id = nextID();
    @Null
    int[] bones;
    float[] vertices;
    int worldVerticesLength;
    @Null
    VertexAttachment deformAttachment = this;

    public VertexAttachment(String name) {
        super(name);
    }

    static private synchronized int nextID() {
        return nextID++;
    }

    /**
     * Transforms the attachment's local {@link #getVertices()} to world coordinates. If the slot's {@link Slot#getDeform()} is
     * not empty, it is used to deform the vertices.
     * <p>
     * See <a href="http://esotericsoftware.com/spine-runtime-skeletons#World-transforms">World transforms</a> in the Spine
     * Runtimes Guide.
     *
     * @param start         The index of the first {@link #getVertices()} value to transform. Each vertex has 2 values, x and y.
     * @param count         The number of world vertex values to output. Must be <= {@link #getWorldVerticesLength()} - <code>start</code>.
     * @param worldVertices The output world vertices. Must have a length >= <code>offset</code> + <code>count</code> *
     *                      <code>stride</code> / 2.
     * @param offset        The <code>worldVertices</code> index to begin writing values.
     * @param stride        The number of <code>worldVertices</code> entries between the value pairs written.
     */
    public void computeWorldVertices(Slot slot, int start, int count, float[] worldVertices, int offset, int stride) {
        count = offset + (count >> 1) * stride;
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
        Object[] skeletonBones = slot.getSkeleton().getBones().items;
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

    /**
     * Deform keys for the deform attachment are also applied to this attachment.
     *
     * @return May be null if no deform keys should be applied.
     */
    public @Null VertexAttachment getDeformAttachment() {
        return deformAttachment;
    }

    /**
     * @param deformAttachment May be null if no deform keys should be applied.
     */
    public void setDeformAttachment(@Null VertexAttachment deformAttachment) {
        this.deformAttachment = deformAttachment;
    }

    /**
     * The bones which affect the {@link #getVertices()}. The array entries are, for each vertex, the number of bones affecting
     * the vertex followed by that many bone indices, which is the index of the bone in {@link Skeleton#getBones()}. Will be null
     * if this attachment has no weights.
     */
    public @Null int[] getBones() {
        return bones;
    }

    /**
     * @param bones May be null if this attachment has no weights.
     */
    public void setBones(@Null int[] bones) {
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
     * The maximum number of world vertex values that can be output by
     * {@link #computeWorldVertices(Slot, int, int, float[], int, int)} using the <code>count</code> parameter.
     */
    public int getWorldVerticesLength() {
        return worldVerticesLength;
    }

    public void setWorldVerticesLength(int worldVerticesLength) {
        this.worldVerticesLength = worldVerticesLength;
    }

    /**
     * Returns a unique ID for this attachment.
     */
    public int getId() {
        return id;
    }

    /**
     * Does not copy id (generated) or name (set on construction).
     */
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
