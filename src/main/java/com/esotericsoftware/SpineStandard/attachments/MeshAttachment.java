package com.esotericsoftware.SpineStandard.attachments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.esotericsoftware.CrossSpine;

import static com.esotericsoftware.SpineStandard.utils.SpineUtils.arraycopy;

public class MeshAttachment extends VertexAttachment {
    private final Color color = new Color(1, 1, 1, 1);
    private final String V = CrossSpine.V.get();
    private TextureRegion region;
    private String path;
    private float[] regionUVs, uvs;
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
        float[] regionUVs = this.regionUVs;
        if (this.uvs == null || this.uvs.length != regionUVs.length) this.uvs = new float[regionUVs.length];
        float[] uvs = this.uvs;
        int n = uvs.length;
        float u, v, width, height;
        u = region.getU();
        v = region.getV();
        if (region instanceof AtlasRegion) {
            AtlasRegion region = (AtlasRegion) this.region;
            float textureWidth = region.getTexture().getWidth(), textureHeight = region.getTexture().getHeight();
            if (V.equals("38")) {
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
            } else if (V.equals("37")) {
                if (region.rotate) {
                    u = region.getU() - (region.originalHeight - region.offsetY - region.packedWidth) / textureWidth;
                    v = region.getV() - (region.originalWidth - region.offsetX - region.packedHeight) / textureHeight;
                    width = region.originalHeight / textureWidth;
                    height = region.originalWidth / textureHeight;
                    for (int i = 0; i < n; i += 2) {
                        uvs[i] = u + regionUVs[i + 1] * width;
                        uvs[i + 1] = v + height - regionUVs[i] * height;
                    }
                    return;
                }
                u = region.getU() - region.offsetX / textureWidth;
                v = region.getV() - (region.originalHeight - region.offsetY - region.packedHeight) / textureHeight;
            }
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

    public boolean applyDeform(VertexAttachment sourceAttachment) {
        return this == sourceAttachment || (inheritDeform && parentMesh == sourceAttachment);
    }

    public short[] getTriangles() {
        return triangles;
    }

    public void setTriangles(short[] triangles) {
        this.triangles = triangles;
    }


    public float[] getRegionUVs() {
        return regionUVs;
    }


    public void setRegionUVs(float[] regionUVs) {
        this.regionUVs = regionUVs;
    }


    public float[] getUVs() {
        return uvs;
    }

    public void setUVs(float[] uvs) {
        this.uvs = uvs;
    }


    public Color getColor() {
        return color;
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public int getHullLength() {
        return hullLength;
    }

    public void setHullLength(int hullLength) {
        this.hullLength = hullLength;
    }


    public short[] getEdges() {
        return edges;
    }

    public void setEdges(short[] edges) {
        this.edges = edges;
    }


    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }


    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }


    public MeshAttachment getParentMesh() {
        return parentMesh;
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

    public Attachment copy() {
        if (parentMesh != null) return newLinkedMesh();

        MeshAttachment copy = new MeshAttachment(name);
        copy.region = region;
        copy.path = path;
        copy.color.set(color);

        copyTo(copy);
        copy.regionUVs = new float[regionUVs.length];
        arraycopy(regionUVs, 0, copy.regionUVs, 0, regionUVs.length);
        copy.uvs = new float[uvs.length];
        arraycopy(uvs, 0, copy.uvs, 0, uvs.length);
        copy.triangles = new short[triangles.length];
        arraycopy(triangles, 0, copy.triangles, 0, triangles.length);
        copy.hullLength = hullLength;


        if (edges != null) {
            copy.edges = new short[edges.length];
            arraycopy(edges, 0, copy.edges, 0, edges.length);
        }
        copy.width = width;
        copy.height = height;
        return copy;
    }

    public boolean getInheritDeform() {
        return inheritDeform;
    }

    public void setInheritDeform(boolean inheritDeform) {
        this.inheritDeform = inheritDeform;
    }

    public MeshAttachment newLinkedMesh() {
        MeshAttachment mesh = new MeshAttachment(name);
        mesh.region = region;
        mesh.path = path;
        mesh.color.set(color);
        mesh.deformAttachment = deformAttachment;
        mesh.setParentMesh(parentMesh != null ? parentMesh : this);
        mesh.updateUVs();
        return mesh;
    }
}
