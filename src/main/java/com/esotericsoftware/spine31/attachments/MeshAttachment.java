package com.esotericsoftware.spine31.attachments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.NumberUtils;
import com.esotericsoftware.spine31.Bone;
import com.esotericsoftware.spine31.Skeleton;
import com.esotericsoftware.spine31.Slot;

/**
 * Attachment that displays a texture region.
 */
public class MeshAttachment extends Attachment implements FfdAttachment {
    private final Color color = new Color(1, 1, 1, 1);
    private TextureRegion region;
    private String path;
    private float[] vertices, regionUVs;
    private short[] triangles;
    private float[] worldVertices;
    private int hullLength;
    private MeshAttachment parentMesh;
    private boolean inheritFFD;

    // Nonessential.
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
        int verticesLength = vertices.length;
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
        float[] regionUVs = this.regionUVs;
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

    /**
     * @return The updated world vertices.
     */
    public float[] updateWorldVertices(Slot slot, boolean premultipliedAlpha) {
        Skeleton skeleton = slot.getSkeleton();
        Color skeletonColor = skeleton.getColor();
        Color slotColor = slot.getColor();
        Color meshColor = color;
        float a = skeletonColor.a * slotColor.a * meshColor.a * 255;
        float multiplier = premultipliedAlpha ? a : 255;
        float color = NumberUtils.intToFloatColor( //
                ((int) a << 24) //
                        | ((int) (skeletonColor.b * slotColor.b * meshColor.b * multiplier) << 16) //
                        | ((int) (skeletonColor.g * slotColor.g * meshColor.g * multiplier) << 8) //
                        | (int) (skeletonColor.r * slotColor.r * meshColor.r * multiplier));

        float[] worldVertices = this.worldVertices;
        FloatArray slotVertices = slot.getAttachmentVertices();
        float[] vertices = this.vertices;
        if (slotVertices.size == vertices.length) vertices = slotVertices.items;
        Bone bone = slot.getBone();
        float x = skeleton.getX() + bone.getWorldX(), y = skeleton.getY() + bone.getWorldY();
        float m00 = bone.getA(), m01 = bone.getB(), m10 = bone.getC(), m11 = bone.getD();
        for (int v = 0, w = 0, n = worldVertices.length; w < n; v += 2, w += 5) {
            float vx = vertices[v];
            float vy = vertices[v + 1];
            worldVertices[w] = vx * m00 + vy * m01 + x;
            worldVertices[w + 1] = vx * m10 + vy * m11 + y;
            worldVertices[w + 2] = color;
        }
        return worldVertices;
    }

    public boolean applyFFD(Attachment sourceAttachment) {
        return this == sourceAttachment || (inheritFFD && parentMesh == sourceAttachment);
    }

    public float[] getWorldVertices() {
        return worldVertices;
    }

    public float[] getVertices() {
        return vertices;
    }

    public void setVertices(float[] vertices) {
        this.vertices = vertices;
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

    /**
     * Returns the source mesh if this is a linked mesh, else returns null.
     */
    public MeshAttachment getParentMesh() {
        return parentMesh;
    }

    /**
     * @param parentMesh May be null.
     */
    public void setParentMesh(MeshAttachment parentMesh) {
        this.parentMesh = parentMesh;
        if (parentMesh != null) {
            vertices = parentMesh.vertices;
            regionUVs = parentMesh.regionUVs;
            triangles = parentMesh.triangles;
            hullLength = parentMesh.hullLength;
            edges = parentMesh.edges;
            width = parentMesh.width;
            height = parentMesh.height;
        }
    }

    public boolean getInheritFFD() {
        return inheritFFD;
    }

    public void setInheritFFD(boolean inheritFFD) {
        this.inheritFFD = inheritFFD;
    }
}
