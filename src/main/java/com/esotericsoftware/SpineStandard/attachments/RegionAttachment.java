package com.esotericsoftware.SpineStandard.attachments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.SpineStandard.Bone;

import static com.esotericsoftware.SpineStandard.utils.SpineUtils.arraycopy;

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

    public void updateOffset() {
        float width = getWidth();
        float height = getHeight();
        float localX2 = width / 2;
        float localY2 = height / 2;
        float localX = -localX2;
        float localY = -localY2;
        if (region instanceof AtlasRegion) {
            AtlasRegion region = (AtlasRegion) this.region;
            localX += region.offsetX / region.originalWidth * width;
            localY += region.offsetY / region.originalHeight * height;
            if (region.rotate) {
                localX2 -= (region.originalWidth - region.offsetX - region.packedHeight) / region.originalWidth * width;
                localY2 -= (region.originalHeight - region.offsetY - region.packedWidth) / region.originalHeight * height;
            } else {
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

    public void computeWorldVertices(Bone bone, float[] worldVertices, int offset, int stride) {
        float[] vertexOffset = this.offset;
        float x = bone.getWorldX(), y = bone.getWorldY();
        float a = bone.getA(), b = bone.getB(), c = bone.getC(), d = bone.getD();
        float offsetX, offsetY;

        offsetX = vertexOffset[BRX];
        offsetY = vertexOffset[BRY];
        worldVertices[offset] = offsetX * a + offsetY * b + x;
        worldVertices[offset + 1] = offsetX * c + offsetY * d + y;
        offset += stride;

        offsetX = vertexOffset[BLX];
        offsetY = vertexOffset[BLY];
        worldVertices[offset] = offsetX * a + offsetY * b + x;
        worldVertices[offset + 1] = offsetX * c + offsetY * d + y;
        offset += stride;

        offsetX = vertexOffset[ULX];
        offsetY = vertexOffset[ULY];
        worldVertices[offset] = offsetX * a + offsetY * b + x;
        worldVertices[offset + 1] = offsetX * c + offsetY * d + y;
        offset += stride;

        offsetX = vertexOffset[URX];
        offsetY = vertexOffset[URY];
        worldVertices[offset] = offsetX * a + offsetY * b + x;
        worldVertices[offset + 1] = offsetX * c + offsetY * d + y;
    }

    public float[] getOffset() {
        return offset;
    }

    public float[] getUVs() {
        return uvs;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getScaleX() {
        return scaleX;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
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

    public Color getColor() {
        return color;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Attachment copy() {
        RegionAttachment copy = new RegionAttachment(name);
        copy.region = region;
        copy.path = path;
        copy.x = x;
        copy.y = y;
        copy.scaleX = scaleX;
        copy.scaleY = scaleY;
        copy.rotation = rotation;
        copy.width = width;
        copy.height = height;
        arraycopy(uvs, 0, copy.uvs, 0, 8);
        arraycopy(offset, 0, copy.offset, 0, 8);
        copy.color.set(color);
        return copy;
    }
}
