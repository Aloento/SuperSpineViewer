package com.esotericsoftware.SpineLegacy.attachments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.NumberUtils;
import com.esotericsoftware.SpineLegacy.Bone;
import com.esotericsoftware.SpineLegacy.Skeleton;
import com.esotericsoftware.SpineLegacy.Slot;

public class WeightedMeshAttachment extends Attachment implements FfdAttachment {
    private final Color color = new Color(1, 1, 1, 1);
    private TextureRegion region;
    private String path;
    private int[] bones;
    private float[] weights, regionUVs;
    private short[] triangles;
    private float[] worldVertices;
    private int hullLength;
    private WeightedMeshAttachment parentMesh;
    private boolean inheritFFD;
    private short[] edges;
    private float width, height;

    public WeightedMeshAttachment(String name) {
        super(name);
    }

    public TextureRegion getRegion() {
        if (region == null) throw new IllegalStateException("Region has not been set: " + this);
        return region;
    }

    public void setRegion(TextureRegion region) {
        if (region == null) throw new IllegalArgumentException("region cannot be null.");
        this.region = region;
    }

    public void updateUVs() {
        float[] regionUVs = this.regionUVs;
        int verticesLength = regionUVs.length;
        int worldVerticesLength = verticesLength / 2 * 5;
        if (worldVertices == null || worldVertices.length != worldVerticesLength)
            worldVertices = new float[worldVerticesLength];
        float u, v, width, height;
        if (region == null) {
            u = v = 0;
            width = height = 1;
        } else {
            u = region.getU();
            v = region.getV();
            width = region.getU2() - u;
            height = region.getV2() - v;
        }
        if (region instanceof AtlasRegion && ((AtlasRegion) region).rotate) {
            for (int i = 0, w = 3; i < verticesLength; i += 2, w += 5) {
                worldVertices[w] = u + regionUVs[i + 1] * width;
                worldVertices[w + 1] = v + height - regionUVs[i] * height;
            }
        } else {
            for (int i = 0, w = 3; i < verticesLength; i += 2, w += 5) {
                worldVertices[w] = u + regionUVs[i] * width;
                worldVertices[w + 1] = v + regionUVs[i + 1] * height;
            }
        }
    }

    public float[] updateWorldVertices(Slot slot, boolean premultipliedAlpha) {
        Skeleton skeleton = slot.getSkeleton();
        Color skeletonColor = skeleton.getColor();
        Color meshColor = slot.getColor();
        Color regionColor = color;
        float a = skeletonColor.a * meshColor.a * regionColor.a * 255;
        float multiplier = premultipliedAlpha ? a : 255;
        float color = NumberUtils.intToFloatColor(
                ((int) a << 24)
                        | ((int) (skeletonColor.b * meshColor.b * regionColor.b * multiplier) << 16)
                        | ((int) (skeletonColor.g * meshColor.g * regionColor.g * multiplier) << 8)
                        | (int) (skeletonColor.r * meshColor.r * regionColor.r * multiplier));
        float[] worldVertices = this.worldVertices;
        float x = skeleton.getX(), y = skeleton.getY();
        Object[] skeletonBones = skeleton.getBones().items;
        float[] weights = this.weights;
        int[] bones = this.bones;
        FloatArray ffdArray = slot.getAttachmentVertices();
        if (ffdArray.size == 0) {
            for (int w = 0, v = 0, b = 0, n = bones.length; v < n; w += 5) {
                float wx = 0, wy = 0;
                int nn = bones[v++] + v;
                for (; v < nn; v++, b += 3) {
                    Bone bone = (Bone) skeletonBones[bones[v]];
                    float vx = weights[b], vy = weights[b + 1], weight = weights[b + 2];
                    wx += (vx * bone.getA() + vy * bone.getB() + bone.getWorldX()) * weight;
                    wy += (vx * bone.getC() + vy * bone.getD() + bone.getWorldY()) * weight;
                }
                worldVertices[w] = wx + x;
                worldVertices[w + 1] = wy + y;
                worldVertices[w + 2] = color;
            }
        } else {
            float[] ffd = ffdArray.items;
            for (int w = 0, v = 0, b = 0, f = 0, n = bones.length; v < n; w += 5) {
                float wx = 0, wy = 0;
                int nn = bones[v++] + v;
                for (; v < nn; v++, b += 3, f += 2) {
                    Bone bone = (Bone) skeletonBones[bones[v]];
                    float vx = weights[b] + ffd[f], vy = weights[b + 1] + ffd[f + 1], weight = weights[b + 2];
                    wx += (vx * bone.getA() + vy * bone.getB() + bone.getWorldX()) * weight;
                    wy += (vx * bone.getC() + vy * bone.getD() + bone.getWorldY()) * weight;
                }
                worldVertices[w] = wx + x;
                worldVertices[w + 1] = wy + y;
                worldVertices[w + 2] = color;
            }
        }
        return worldVertices;
    }

    public boolean applyFFD(Attachment sourceAttachment) {
        return this == sourceAttachment || (inheritFFD && parentMesh == sourceAttachment);
    }

    // public float[] getWorldVertices() {
    //     return worldVertices;
    // }

    // public int[] getBones() {
    //     return bones;
    // }

    public void setBones(int[] bones) {
        this.bones = bones;
    }

    public float[] getWeights() {
        return weights;
    }

    public void setWeights(float[] weights) {
        this.weights = weights;
    }

    public short[] getTriangles() {
        return triangles;
    }

    public void setTriangles(short[] triangles) {
        this.triangles = triangles;
    }

    // public float[] getRegionUVs() {
    //     return regionUVs;
    // }

    public void setRegionUVs(float[] regionUVs) {
        this.regionUVs = regionUVs;
    }

    public Color getColor() {
        return color;
    }

    // public String getPath() {
    //     return path;
    // }

    public void setPath(String path) {
        this.path = path;
    }

    // public int getHullLength() {
    //     return hullLength;
    // }

    public void setHullLength(int hullLength) {
        this.hullLength = hullLength;
    }

    // public short[] getEdges() {
    //     return edges;
    // }

    public void setEdges(short[] edges) {
        this.edges = edges;
    }

    // public float getWidth() {
    //     return width;
    // }

    public void setWidth(float width) {
        this.width = width;
    }

    // public float getHeight() {
    //     return height;
    // }

    public void setHeight(float height) {
        this.height = height;
    }

    // public WeightedMeshAttachment getParentMesh() {
    //     return parentMesh;
    // }

    public void setParentMesh(WeightedMeshAttachment parentMesh) {
        this.parentMesh = parentMesh;
        if (parentMesh != null) {
            bones = parentMesh.bones;
            weights = parentMesh.weights;
            regionUVs = parentMesh.regionUVs;
            triangles = parentMesh.triangles;
            hullLength = parentMesh.hullLength;
            edges = parentMesh.edges;
            width = parentMesh.width;
            height = parentMesh.height;
        }
    }

    // public boolean getInheritFFD() {
    //     return inheritFFD;
    // }

    public void setInheritFFD(boolean inheritFFD) {
        this.inheritFFD = inheritFFD;
    }
}
