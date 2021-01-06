package com.esotericsoftware.spine36.attachments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine36.Bone;

/**
 * An attachment that displays a textured quadrilateral.
 * <p>
 * See <a href="http://esotericsoftware.com/spine-regions">Region attachments</a> in the Spine User Guide.
 */
public class RegionAttachment extends Attachment {
    static public final int BLX = 0;
    static public final int BLY = 1;
    static public final int ULX = 2;
    static public final int ULY = 3;
    static public final int URX = 4;
    static public final int URY = 5;
    static public final int BRX = 6;
    static public final int BRY = 7;
    private final float[] uvs = new float[8];
    private final float[] offset = new float[8];
    private final Color color = new Color(1, 1, 1, 1);
    private TextureRegion region;
    private String path;
    private float x, y, scaleX = 1, scaleY = 1, rotation, width, height;

    public RegionAttachment(String name) {
        super(name);
    }

    /**
     * Calculates the {@link #offset} using the region settings. Must be called after changing region settings.
     */
    public void updateOffset() {
        float width = getWidth();
        float height = getHeight();
        float localX2 = width / 2;
        float localY2 = height / 2;
        float localX = -localX2;
        float localY = -localY2;
        if (region instanceof AtlasRegion) {
            AtlasRegion region = (AtlasRegion) this.region;
            if (region.rotate) {
                localX += region.offsetX / region.originalWidth * width;
                localY += region.offsetY / region.originalHeight * height;
                localX2 -= (region.originalWidth - region.offsetX - region.packedHeight) / region.originalWidth * width;
                localY2 -= (region.originalHeight - region.offsetY - region.packedWidth) / region.originalHeight * height;
            } else {
                localX += region.offsetX / region.originalWidth * width;
                localY += region.offsetY / region.originalHeight * height;
                localX2 -= (region.originalWidth - region.offsetX - region.packedWidth) / region.originalWidth * width;
                localY2 -= (region.originalHeight - region.offsetY - region.packedHeight) / region.originalHeight * height;
            }
        }
        float scaleX = getScaleX();
        float scaleY = getScaleY();
        localX *= scaleX;
        localY *= scaleY;
        localX2 *= scaleX;
        localY2 *= scaleY;
        float rotation = getRotation();
        float cos = (float) Math.cos(MathUtils.degRad * rotation);
        float sin = (float) Math.sin(MathUtils.degRad * rotation);
        float x = getX();
        float y = getY();
        float localXCos = localX * cos + x;
        float localXSin = localX * sin;
        float localYCos = localY * cos + y;
        float localYSin = localY * sin;
        float localX2Cos = localX2 * cos + x;
        float localX2Sin = localX2 * sin;
        float localY2Cos = localY2 * cos + y;
        float localY2Sin = localY2 * sin;
        float[] offset = this.offset;
        offset[BLX] = localXCos - localYSin;
        offset[BLY] = localYCos + localXSin;
        offset[ULX] = localXCos - localY2Sin;
        offset[ULY] = localY2Cos + localXSin;
        offset[URX] = localX2Cos - localY2Sin;
        offset[URY] = localY2Cos + localX2Sin;
        offset[BRX] = localX2Cos - localYSin;
        offset[BRY] = localYCos + localX2Sin;
    }

    public TextureRegion getRegion() {
        if (region == null) throw new IllegalStateException("Region has not been set: " + this);
        return region;
    }

    public void setRegion(TextureRegion region) {
        if (region == null) throw new IllegalArgumentException("region cannot be null.");
        this.region = region;
        float[] uvs = this.uvs;
        if (region instanceof AtlasRegion && ((AtlasRegion) region).rotate) {
            uvs[URX] = region.getU();
            uvs[URY] = region.getV2();
            uvs[BRX] = region.getU();
            uvs[BRY] = region.getV();
            uvs[BLX] = region.getU2();
            uvs[BLY] = region.getV();
            uvs[ULX] = region.getU2();
            uvs[ULY] = region.getV2();
        } else {
            uvs[ULX] = region.getU();
            uvs[ULY] = region.getV2();
            uvs[URX] = region.getU();
            uvs[URY] = region.getV();
            uvs[BRX] = region.getU2();
            uvs[BRY] = region.getV();
            uvs[BLX] = region.getU2();
            uvs[BLY] = region.getV2();
        }
    }

    /**
     * Transforms the attachment's four vertices to world coordinates.
     * <p>
     * See <a href="http://esotericsoftware.com/spine-runtime-skeletons#World-transforms">World transforms</a> in the Spine
     * Runtimes Guide.
     *
     * @param worldVertices The output world vertices. Must have a length >= <code>offset</code> + 8.
     * @param offset        The <code>worldVertices</code> index to begin writing values.
     * @param stride        The number of <code>worldVertices</code> entries between the value pairs written.
     */
    public void computeWorldVertices(Bone bone, float[] worldVertices, int offset, int stride) {
        float[] vertexOffset = this.offset;
        float x = bone.getWorldX(), y = bone.getWorldY();
        float a = bone.getA(), b = bone.getB(), c = bone.getC(), d = bone.getD();
        float offsetX, offsetY;

        offsetX = vertexOffset[BRX];
        offsetY = vertexOffset[BRY];
        worldVertices[offset] = offsetX * a + offsetY * b + x; // br
        worldVertices[offset + 1] = offsetX * c + offsetY * d + y;
        offset += stride;

        offsetX = vertexOffset[BLX];
        offsetY = vertexOffset[BLY];
        worldVertices[offset] = offsetX * a + offsetY * b + x; // bl
        worldVertices[offset + 1] = offsetX * c + offsetY * d + y;
        offset += stride;

        offsetX = vertexOffset[ULX];
        offsetY = vertexOffset[ULY];
        worldVertices[offset] = offsetX * a + offsetY * b + x; // ul
        worldVertices[offset + 1] = offsetX * c + offsetY * d + y;
        offset += stride;

        offsetX = vertexOffset[URX];
        offsetY = vertexOffset[URY];
        worldVertices[offset] = offsetX * a + offsetY * b + x; // ur
        worldVertices[offset + 1] = offsetX * c + offsetY * d + y;
    }

    /**
     * For each of the 4 vertices, a pair of <code>x,y</code> values that is the local position of the vertex.
     * <p>
     * See {@link #updateOffset()}.
     */
    public float[] getOffset() {
        return offset;
    }

    public float[] getUVs() {
        return uvs;
    }

    /**
     * The local x translation.
     */
    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    /**
     * The local y translation.
     */
    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    /**
     * The local scaleX.
     */
    public float getScaleX() {
        return scaleX;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }

    /**
     * The local scaleY.
     */
    public float getScaleY() {
        return scaleY;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }

    /**
     * The local rotation.
     */
    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    /**
     * The width of the region attachment in Spine.
     */
    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    /**
     * The height of the region attachment in Spine.
     */
    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    /**
     * The color to tint the region attachment.
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
}
