package com.esotericsoftware.SpinePreview;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Pool;
import com.esotericsoftware.SpinePreview.attachments.Attachment;
import com.esotericsoftware.SpinePreview.attachments.BoundingBoxAttachment;


public class SkeletonBounds {
    private final Array<BoundingBoxAttachment> boundingBoxes = new Array();
    private final Array<FloatArray> polygons = new Array();
    private final Pool<FloatArray> polygonPool = new Pool() {
        protected Object newObject() {
            return new FloatArray();
        }
    };
    private float minX, minY, maxX, maxY;


    public void update(Skeleton skeleton, boolean updateAabb) {
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        Array<BoundingBoxAttachment> boundingBoxes = this.boundingBoxes;
        Array<FloatArray> polygons = this.polygons;
        Object[] slots = skeleton.slots.items;
        int slotCount = skeleton.slots.size;

        boundingBoxes.clear();
        polygonPool.freeAll(polygons);
        polygons.clear();

        for (int i = 0; i < slotCount; i++) {
            Slot slot = (Slot) slots[i];
            if (!slot.bone.active) continue;
            Attachment attachment = slot.attachment;
            if (attachment instanceof BoundingBoxAttachment) {
                BoundingBoxAttachment boundingBox = (BoundingBoxAttachment) attachment;
                boundingBoxes.add(boundingBox);

                FloatArray polygon = polygonPool.obtain();
                polygons.add(polygon);
                boundingBox.computeWorldVertices(slot, 0, boundingBox.getWorldVerticesLength(),
                        polygon.setSize(boundingBox.getWorldVerticesLength()), 0, 2);
            }
        }

        if (updateAabb)
            aabbCompute();
        else {
            minX = Integer.MIN_VALUE;
            minY = Integer.MIN_VALUE;
            maxX = Integer.MAX_VALUE;
            maxY = Integer.MAX_VALUE;
        }
    }

    private void aabbCompute() {
        float minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        Object[] polygons = this.polygons.items;
        for (int i = 0, n = this.polygons.size; i < n; i++) {
            FloatArray polygon = (FloatArray) polygons[i];
            float[] vertices = polygon.items;
            for (int ii = 0, nn = polygon.size; ii < nn; ii += 2) {
                float x = vertices[ii];
                float y = vertices[ii + 1];
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }


    public boolean aabbContainsPoint(float x, float y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }


    public boolean aabbIntersectsSegment(float x1, float y1, float x2, float y2) {
        float minX = this.minX;
        float minY = this.minY;
        float maxX = this.maxX;
        float maxY = this.maxY;
        if ((x1 <= minX && x2 <= minX) || (y1 <= minY && y2 <= minY) || (x1 >= maxX && x2 >= maxX) || (y1 >= maxY && y2 >= maxY))
            return false;
        float m = (y2 - y1) / (x2 - x1);
        float y = m * (minX - x1) + y1;
        if (y > minY && y < maxY) return true;
        y = m * (maxX - x1) + y1;
        if (y > minY && y < maxY) return true;
        float x = (minY - y1) / m + x1;
        if (x > minX && x < maxX) return true;
        x = (maxY - y1) / m + x1;
        return x > minX && x < maxX;
    }


    public boolean aabbIntersectsSkeleton(SkeletonBounds bounds) {
        if (bounds == null) throw new IllegalArgumentException("bounds cannot be null.");
        return minX < bounds.maxX && maxX > bounds.minX && minY < bounds.maxY && maxY > bounds.minY;
    }


    public @Null
    BoundingBoxAttachment containsPoint(float x, float y) {
        Object[] polygons = this.polygons.items;
        for (int i = 0, n = this.polygons.size; i < n; i++)
            if (containsPoint((FloatArray) polygons[i], x, y)) return boundingBoxes.get(i);
        return null;
    }


    public boolean containsPoint(FloatArray polygon, float x, float y) {
        if (polygon == null) throw new IllegalArgumentException("polygon cannot be null.");
        float[] vertices = polygon.items;
        int nn = polygon.size;

        int prevIndex = nn - 2;
        boolean inside = false;
        for (int ii = 0; ii < nn; ii += 2) {
            float vertexY = vertices[ii + 1];
            float prevY = vertices[prevIndex + 1];
            if ((vertexY < y && prevY >= y) || (prevY < y && vertexY >= y)) {
                float vertexX = vertices[ii];
                if (vertexX + (y - vertexY) / (prevY - vertexY) * (vertices[prevIndex] - vertexX) < x) inside = !inside;
            }
            prevIndex = ii;
        }
        return inside;
    }


    public @Null
    BoundingBoxAttachment intersectsSegment(float x1, float y1, float x2, float y2) {
        Object[] polygons = this.polygons.items;
        for (int i = 0, n = this.polygons.size; i < n; i++)
            if (intersectsSegment((FloatArray) polygons[i], x1, y1, x2, y2)) return boundingBoxes.get(i);
        return null;
    }


    public boolean intersectsSegment(FloatArray polygon, float x1, float y1, float x2, float y2) {
        if (polygon == null) throw new IllegalArgumentException("polygon cannot be null.");
        float[] vertices = polygon.items;
        int nn = polygon.size;

        float width12 = x1 - x2, height12 = y1 - y2;
        float det1 = x1 * y2 - y1 * x2;
        float x3 = vertices[nn - 2], y3 = vertices[nn - 1];
        for (int ii = 0; ii < nn; ii += 2) {
            float x4 = vertices[ii], y4 = vertices[ii + 1];
            float det2 = x3 * y4 - y3 * x4;
            float width34 = x3 - x4, height34 = y3 - y4;
            float det3 = width12 * height34 - height12 * width34;
            float x = (det1 * width34 - width12 * det2) / det3;
            if (((x >= x3 && x <= x4) || (x >= x4 && x <= x3)) && ((x >= x1 && x <= x2) || (x >= x2 && x <= x1))) {
                float y = (det1 * height34 - height12 * det2) / det3;
                if (((y >= y3 && y <= y4) || (y >= y4 && y <= y3)) && ((y >= y1 && y <= y2) || (y >= y2 && y <= y1)))
                    return true;
            }
            x3 = x4;
            y3 = y4;
        }
        return false;
    }


    public float getMinX() {
        return minX;
    }


    public float getMinY() {
        return minY;
    }


    public float getMaxX() {
        return maxX;
    }


    public float getMaxY() {
        return maxY;
    }


    public float getWidth() {
        return maxX - minX;
    }


    public float getHeight() {
        return maxY - minY;
    }


    public Array<BoundingBoxAttachment> getBoundingBoxes() {
        return boundingBoxes;
    }


    public Array<FloatArray> getPolygons() {
        return polygons;
    }


    public @Null
    FloatArray getPolygon(BoundingBoxAttachment boundingBox) {
        if (boundingBox == null) throw new IllegalArgumentException("boundingbox cannot be null.");
        int index = boundingBoxes.indexOf(boundingBox, true);
        return index == -1 ? null : polygons.get(index);
    }
}
