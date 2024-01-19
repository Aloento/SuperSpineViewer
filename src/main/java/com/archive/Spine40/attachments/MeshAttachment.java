package com.archive.Spine40.attachments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Null;

import static com.archive.Spine40.utils.SpineUtils.arraycopy;

/**
 * An attachment that displays a textured mesh. A mesh has hull vertices and internal vertices within the hull. Holes are not
 * supported. Each vertex has UVs (texture coordinates) and triangles are used to map an image on to the mesh.
 * <p>
 * See <a href="http://esotericsoftware.com/spine-meshes">Mesh attachments</a> in the Spine User Guide.
 */
public class MeshAttachment extends VertexAttachment {
    private final Color color = new Color(1, 1, 1, 1);
    private TextureRegion region;
    private String path;
    private float[] regionUVs, uvs;
    private short[] triangles;
    private int hullLength;
    private @Null MeshAttachment parentMesh;

    // Nonessential.
    private @Null short[] edges;
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

    /**
     * Calculates {@link #uvs} using {@link #regionUVs} and the {@link #region}. Must be called after changing the region UVs or
     * region.
     */
    public void updateUVs() {
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
                case 90:
                    u -= (region.originalHeight - region.offsetY - region.packedWidth) / textureWidth;
                    v -= (region.originalWidth - region.offsetX - region.packedHeight) / textureHeight;
                    width = region.originalHeight / textureWidth;
                    height = region.originalWidth / textureHeight;
                    for (int i = 0; i < n; i += 2) {
                        uvs[i] = u + regionUVs[i + 1] * width;
                        uvs[i + 1] = v + (1 - regionUVs[i]) * height;
                    }
                    return;
                case 180:
                    u -= (region.originalWidth - region.offsetX - region.packedWidth) / textureWidth;
                    v -= region.offsetY / textureHeight;
                    width = region.originalWidth / textureWidth;
                    height = region.originalHeight / textureHeight;
                    for (int i = 0; i < n; i += 2) {
                        uvs[i] = u + (1 - regionUVs[i]) * width;
                        uvs[i + 1] = v + (1 - regionUVs[i + 1]) * height;
                    }
                    return;
                case 270:
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

    /**
     * Triplets of vertex indices which describe the mesh's triangulation.
     */
    public short[] getTriangles() {
        return triangles;
    }

    public void setTriangles(short[] triangles) {
        this.triangles = triangles;
    }

    /**
     * The UV pair for each vertex, normalized within the texture region.
     */
    public float[] getRegionUVs() {
        return regionUVs;
    }

    /**
     * Sets the texture coordinates for the region. The values are u,v pairs for each vertex.
     */
    public void setRegionUVs(float[] regionUVs) {
        this.regionUVs = regionUVs;
    }

    /**
     * The UV pair for each vertex, normalized within the entire texture.
     * <p>
     * See {@link #updateUVs}.
     */
    public float[] getUVs() {
        return uvs;
    }

    public void setUVs(float[] uvs) {
        this.uvs = uvs;
    }

    /**
     * The color to tint the mesh.
     */
    public Color getColor() {
        return color;
    }

    /**
     * The name of the texture region for this attachment.
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * The number of entries at the beginning of {@link #vertices} that make up the mesh hull.
     */
    public int getHullLength() {
        return hullLength;
    }

    public void setHullLength(int hullLength) {
        this.hullLength = hullLength;
    }

    /**
     * Vertex index pairs describing edges for controlling triangulation, or be null if nonessential data was not exported. Mesh
     * triangles will never cross edges. Triangulation is not performed at runtime.
     */
    public @Null short[] getEdges() {
        return edges;
    }

    public void setEdges(short[] edges) {
        this.edges = edges;
    }

    /**
     * The width of the mesh's image, or zero if nonessential data was not exported.
     */
    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    /**
     * The height of the mesh's image, or zero if nonessential data was not exported.
     */
    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    /**
     * The parent mesh if this is a linked mesh, else null. A linked mesh shares the {@link #bones}, {@link #vertices},
     * {@link #regionUVs}, {@link #triangles}, {@link #hullLength}, {@link #edges}, {@link #width}, and {@link #height} with the
     * parent mesh, but may have a different {@link #name} or {@link #path} (and therefore a different texture).
     */
    public @Null MeshAttachment getParentMesh() {
        return parentMesh;
    }

    public void setParentMesh(@Null MeshAttachment parentMesh) {
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

        // Nonessential.
        if (edges != null) {
            copy.edges = new short[edges.length];
            arraycopy(edges, 0, copy.edges, 0, edges.length);
        }
        copy.width = width;
        copy.height = height;
        return copy;
    }

    /**
     * Returns a new mesh with the {@link #parentMesh} set to this mesh's parent mesh, if any, else to this mesh.
     */
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
