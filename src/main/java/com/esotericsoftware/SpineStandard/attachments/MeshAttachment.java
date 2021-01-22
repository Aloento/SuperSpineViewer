package com.esotericsoftware.SpineStandard.attachments;

import com.QYun.SuperSpineViewer.RuntimesLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.NumberUtils;
import com.esotericsoftware.SpineStandard.Bone;
import com.esotericsoftware.SpineStandard.Skeleton;
import com.esotericsoftware.SpineStandard.Slot;

public class MeshAttachment extends VertexAttachment {
    private final Color color = new Color(1, 1, 1, 1);
    private TextureRegion region;
    private String path;
    private float[] regionUVs, uvs, worldVertices; // Spine35
    private short[] triangles;
    private int hullLength;
    private MeshAttachment parentMesh;
    private boolean inheritDeform;
    private short[] edges;
    private float width, height;

    public MeshAttachment(String name) {
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
        switch (RuntimesLoader.spineVersion) {
            case 38 -> {
                float[] regionUVs = this.regionUVs;
                if (this.uvs == null || this.uvs.length != regionUVs.length) this.uvs = new float[regionUVs.length];
                float[] uvs = this.uvs;
                int n = uvs.length;
                float u, v, width, height;
                if (region instanceof AtlasRegion) {
                    u = region.getU();
                    v = region.getV();
                    AtlasRegion region = (AtlasRegion) this.region;
                    float textureWidth = region.getTexture().getWidth(), textureHeight = region.getTexture().getHeight();
                    switch (region.degrees) {
                        case 90 -> {
                            u -= (region.originalHeight - region.offsetY - region.packedWidth) / textureWidth;
                            v -= (region.originalWidth - region.offsetX - region.packedHeight) / textureHeight;
                            width = region.originalHeight / textureWidth;
                            height = region.originalWidth / textureHeight;
                            for (int i = 0; i < n; i += 2) {
                                uvs[i] = u + regionUVs[i + 1] * width;
                                uvs[i + 1] = v + (1 - regionUVs[i]) * height;
                            }
                            return;
                        }
                        case 180 -> {
                            u -= (region.originalWidth - region.offsetX - region.packedWidth) / textureWidth;
                            v -= region.offsetY / textureHeight;
                            width = region.originalWidth / textureWidth;
                            height = region.originalHeight / textureHeight;
                            for (int i = 0; i < n; i += 2) {
                                uvs[i] = u + (1 - regionUVs[i]) * width;
                                uvs[i + 1] = v + (1 - regionUVs[i + 1]) * height;
                            }
                            return;
                        }
                        case 270 -> {
                            u -= region.offsetY / textureWidth;
                            v -= region.offsetX / textureHeight;
                            width = region.originalHeight / textureWidth;
                            height = region.originalWidth / textureHeight;
                            for (int i = 0; i < n; i += 2) {
                                uvs[i] = u + (1 - regionUVs[i + 1]) * width;
                                uvs[i + 1] = v + regionUVs[i] * height;
                            }
                            return;
                        }
                    }
                    u -= region.offsetX / textureWidth;
                    v -= (region.originalHeight - region.offsetY - region.packedHeight) / textureHeight;
                    width = region.originalWidth / textureWidth;
                    height = region.originalHeight / textureHeight;
                } else if (region == null) {
                    u = v = 0;
                    width = height = 1;
                } else {
                    u = region.getU();
                    v = region.getV();
                    width = region.getU2() - u;
                    height = region.getV2() - v;
                }
                for (int i = 0; i < n; i += 2) {
                    uvs[i] = u + regionUVs[i] * width;
                    uvs[i + 1] = v + regionUVs[i + 1] * height;
                }
            }
            case 37 -> {
                float[] regionUVs = this.regionUVs;
                if (this.uvs == null || this.uvs.length != regionUVs.length) this.uvs = new float[regionUVs.length];
                float[] uvs = this.uvs;
                float u, v, width, height;
                if (region instanceof AtlasRegion) {
                    AtlasRegion region = (AtlasRegion) this.region;
                    float textureWidth = region.getTexture().getWidth(), textureHeight = region.getTexture().getHeight();
                    if (region.rotate) {
                        u = region.getU() - (region.originalHeight - region.offsetY - region.packedWidth) / textureWidth;
                        v = region.getV() - (region.originalWidth - region.offsetX - region.packedHeight) / textureHeight;
                        width = region.originalHeight / textureWidth;
                        height = region.originalWidth / textureHeight;
                        for (int i = 0, n = uvs.length; i < n; i += 2) {
                            uvs[i] = u + regionUVs[i + 1] * width;
                            uvs[i + 1] = v + height - regionUVs[i] * height;
                        }
                        return;
                    }
                    u = region.getU() - region.offsetX / textureWidth;
                    v = region.getV() - (region.originalHeight - region.offsetY - region.packedHeight) / textureHeight;
                    width = region.originalWidth / textureWidth;
                    height = region.originalHeight / textureHeight;
                } else if (region == null) {
                    u = v = 0;
                    width = height = 1;
                } else {
                    u = region.getU();
                    v = region.getV();
                    width = region.getU2() - u;
                    height = region.getV2() - v;
                }
                for (int i = 0, n = uvs.length; i < n; i += 2) {
                    uvs[i] = u + regionUVs[i] * width;
                    uvs[i + 1] = v + regionUVs[i + 1] * height;
                }
            }
            case 36 -> {
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
                float[] regionUVs = this.regionUVs;
                if (this.uvs == null || this.uvs.length != regionUVs.length) this.uvs = new float[regionUVs.length];
                float[] uvs = this.uvs;
                if (region instanceof AtlasRegion && ((AtlasRegion) region).rotate) {
                    for (int i = 0, n = uvs.length; i < n; i += 2) {
                        uvs[i] = u + regionUVs[i + 1] * width;
                        uvs[i + 1] = v + height - regionUVs[i] * height;
                    }
                } else {
                    for (int i = 0, n = uvs.length; i < n; i += 2) {
                        uvs[i] = u + regionUVs[i] * width;
                        uvs[i + 1] = v + regionUVs[i + 1] * height;
                    }
                }
            }
            case 35, 34 -> {
                float[] regionUVs = this.regionUVs;
                int verticesLength = regionUVs.length;
                int worldVerticesLength = (verticesLength >> 1) * 5;
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
        }
    }

    public boolean applyDeform(VertexAttachment sourceAttachment) {
        return this != sourceAttachment && (!inheritDeform || parentMesh != sourceAttachment);
    }

    public float[] updateWorldVertices(Slot slot, boolean premultipliedAlpha) { // Spine35
        Skeleton skeleton = slot.getSkeleton();
        Color skeletonColor = skeleton.getColor(), slotColor = slot.getColor(), meshColor = color;
        float alpha = skeletonColor.a * slotColor.a * meshColor.a * 255;
        float multiplier = premultipliedAlpha ? alpha : 255;
        float color = NumberUtils.intToFloatColor(
                ((int) alpha << 24)
                        | ((int) (skeletonColor.b * slotColor.b * meshColor.b * multiplier) << 16)
                        | ((int) (skeletonColor.g * slotColor.g * meshColor.g * multiplier) << 8)
                        | (int) (skeletonColor.r * slotColor.r * meshColor.r * multiplier));

        FloatArray deformArray = slot.getAttachmentVertices();
        float[] vertices = this.vertices, worldVertices = this.worldVertices;
        int[] bones = this.bones;
        if (bones == null) {
            int verticesLength = vertices.length;
            if (deformArray.size > 0) vertices = deformArray.items;
            Bone bone = slot.getBone();
            float x = bone.getWorldX(), y = bone.getWorldY();
            float a = bone.getA(), b = bone.getB(), c = bone.getC(), d = bone.getD();
            for (int v = 0, w = 0; v < verticesLength; v += 2, w += 5) {
                float vx = vertices[v], vy = vertices[v + 1];
                worldVertices[w] = vx * a + vy * b + x;
                worldVertices[w + 1] = vx * c + vy * d + y;
                worldVertices[w + 2] = color;
            }
            return worldVertices;
        }
        Object[] skeletonBones = skeleton.getBones().items;
        if (deformArray.size == 0) {
            for (int w = 0, v = 0, b = 0, n = bones.length; v < n; w += 5) {
                float wx = 0, wy = 0;
                int nn = bones[v++] + v;
                for (; v < nn; v++, b += 3) {
                    Bone bone = (Bone) skeletonBones[bones[v]];
                    float vx = vertices[b], vy = vertices[b + 1], weight = vertices[b + 2];
                    wx += (vx * bone.getA() + vy * bone.getB() + bone.getWorldX()) * weight;
                    wy += (vx * bone.getC() + vy * bone.getD() + bone.getWorldY()) * weight;
                }
                worldVertices[w] = wx;
                worldVertices[w + 1] = wy;
                worldVertices[w + 2] = color;
            }
        } else {
            float[] deform = deformArray.items;
            for (int w = 0, v = 0, b = 0, f = 0, n = bones.length; v < n; w += 5) {
                float wx = 0, wy = 0;
                int nn = bones[v++] + v;
                for (; v < nn; v++, b += 3, f += 2) {
                    Bone bone = (Bone) skeletonBones[bones[v]];
                    float vx = vertices[b] + deform[f], vy = vertices[b + 1] + deform[f + 1], weight = vertices[b + 2];
                    wx += (vx * bone.getA() + vy * bone.getB() + bone.getWorldX()) * weight;
                    wy += (vx * bone.getC() + vy * bone.getD() + bone.getWorldY()) * weight;
                }
                worldVertices[w] = wx;
                worldVertices[w + 1] = wy;
                worldVertices[w + 2] = color;
            }
        }
        return worldVertices;
    }

    public short[] getTriangles() {
        return triangles;
    }

    public void setTriangles(short[] triangles) {
        this.triangles = triangles;
    }

    public void setRegionUVs(float[] regionUVs) {
        this.regionUVs = regionUVs;
    }

    public float[] getUVs() {
        return uvs;
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

    public void setHullLength(int hullLength) {
        this.hullLength = hullLength;
    }

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

    public void setParentMesh(MeshAttachment parentMesh) {
        this.parentMesh = parentMesh;
        if (parentMesh != null) {
            bones = parentMesh.bones;
            vertices = parentMesh.vertices;
            regionUVs = parentMesh.regionUVs;
            triangles = parentMesh.triangles;
            hullLength = parentMesh.hullLength;
            worldVerticesLength = parentMesh.worldVerticesLength;
            edges = parentMesh.edges;
            width = parentMesh.width;
            height = parentMesh.height;
        }
    }

    // public Attachment copy() {
    //     if (parentMesh != null) return newLinkedMesh();
    //
    //     MeshAttachment copy = new MeshAttachment(name);
    //     copy.region = region;
    //     copy.path = path;
    //     copy.color.set(color);
    //
    //     copyTo(copy);
    //     copy.regionUVs = new float[regionUVs.length];
    //     arraycopy(regionUVs, 0, copy.regionUVs, 0, regionUVs.length);
    //     copy.uvs = new float[uvs.length];
    //     arraycopy(uvs, 0, copy.uvs, 0, uvs.length);
    //     copy.triangles = new short[triangles.length];
    //     arraycopy(triangles, 0, copy.triangles, 0, triangles.length);
    //     copy.hullLength = hullLength;
    //
    //     if (edges != null) {
    //         copy.edges = new short[edges.length];
    //         arraycopy(edges, 0, copy.edges, 0, edges.length);
    //     }
    //     copy.width = width;
    //     copy.height = height;
    //     return copy;
    // }

    public void setInheritDeform(boolean inheritDeform) {
        this.inheritDeform = inheritDeform;
    }

    // public MeshAttachment newLinkedMesh() {
    //     MeshAttachment mesh = new MeshAttachment(name);
    //     mesh.region = region;
    //     mesh.path = path;
    //     mesh.color.set(color);
    //     mesh.deformAttachment = deformAttachment;
    //     mesh.setParentMesh(parentMesh != null ? parentMesh : this);
    //     mesh.updateUVs();
    //     return mesh;
    // }
}
