package com.esotericsoftware.SpineLatency;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Pool;
import com.esotericsoftware.SpineLatency.attachments.Attachment;
import com.esotericsoftware.SpineLatency.attachments.BoundingBoxAttachment;

public class SkeletonBounds {
    private final Array<BoundingBoxAttachment> boundingBoxes = new Array<>();
    private final Array<FloatArray> polygons = new Array<>();
    private final Pool<FloatArray> polygonPool = new Pool<>() {
        protected FloatArray newObject() {
            return new FloatArray();
        }
    };
    private float minX, minY, maxX, maxY;

    public void update(Skeleton skeleton, boolean updateAabb) {
        Array<BoundingBoxAttachment> boundingBoxes = this.boundingBoxes;
        Array<FloatArray> polygons = this.polygons;
        Array<Slot> slots = skeleton.slots;
        int slotCount = slots.size;
        boundingBoxes.clear();
        polygonPool.freeAll(polygons);
        polygons.clear();
        for (int i = 0; i < slotCount; i++) {
            Slot slot = slots.get(i);
            Attachment attachment = slot.attachment;
            if (attachment instanceof BoundingBoxAttachment) {
                BoundingBoxAttachment boundingBox = (BoundingBoxAttachment) attachment;
                boundingBoxes.add(boundingBox);
                FloatArray polygon = polygonPool.obtain();
                polygons.add(polygon);
                int vertexCount = boundingBox.getVertices().length;
                polygon.ensureCapacity(vertexCount);
                polygon.size = vertexCount;
                boundingBox.computeWorldVertices(slot.bone, polygon.items);
            }
        }
        if (updateAabb) aabbCompute();
    }

    private void aabbCompute() {
        float minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        Array<FloatArray> polygons = this.polygons;
        for (int i = 0, n = polygons.size; i < n; i++) {
            FloatArray polygon = polygons.get(i);
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

    public Array<FloatArray> getPolygons() {
        return polygons;
    }

}
