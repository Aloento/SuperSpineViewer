package com.esotericsoftware.spine31;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine31.attachments.*;

public class SkeletonMeshRenderer extends SkeletonRenderer<PolygonSpriteBatch> {
    static private final short[] quadTriangles = {0, 1, 2, 2, 3, 0};

    @SuppressWarnings("null")
    public void draw(PolygonSpriteBatch batch, Skeleton skeleton) {
        boolean premultipliedAlpha = this.premultipliedAlpha;
        BlendMode blendMode = null;

        float[] vertices = null;
        short[] triangles = null;
        Array<Slot> drawOrder = skeleton.drawOrder;
        for (int i = 0, n = drawOrder.size; i < n; i++) {
            Slot slot = drawOrder.get(i);
            Attachment attachment = slot.attachment;
            Texture texture = null;
            if (attachment instanceof RegionAttachment) {
                RegionAttachment region = (RegionAttachment) attachment;
                vertices = region.updateWorldVertices(slot, premultipliedAlpha);
                triangles = quadTriangles;
                texture = region.getRegion().getTexture();

            } else if (attachment instanceof MeshAttachment) {
                MeshAttachment mesh = (MeshAttachment) attachment;
                vertices = mesh.updateWorldVertices(slot, premultipliedAlpha);
                triangles = mesh.getTriangles();
                texture = mesh.getRegion().getTexture();

            } else if (attachment instanceof WeightedMeshAttachment) {
                WeightedMeshAttachment mesh = (WeightedMeshAttachment) attachment;
                vertices = mesh.updateWorldVertices(slot, premultipliedAlpha);
                triangles = mesh.getTriangles();
                texture = mesh.getRegion().getTexture();

            } else if (attachment instanceof SkeletonAttachment) {
                Skeleton attachmentSkeleton = ((SkeletonAttachment) attachment).getSkeleton();
                if (attachmentSkeleton == null) continue;
                Bone bone = slot.getBone();
                Bone rootBone = attachmentSkeleton.getRootBone();
                float oldScaleX = rootBone.getScaleX();
                float oldScaleY = rootBone.getScaleY();
                float oldRotation = rootBone.getRotation();
                attachmentSkeleton.setPosition(skeleton.getX() + bone.getWorldX(), skeleton.getY() + bone.getWorldY());
                // rootBone.setScaleX(1 + bone.getWorldScaleX() - oldScaleX);
                // rootBone.setScaleY(1 + bone.getWorldScaleY() - oldScaleY);
                rootBone.setRotation(oldRotation + bone.getWorldRotationX());
                attachmentSkeleton.updateWorldTransform();

                draw(batch, attachmentSkeleton);

                attachmentSkeleton.setPosition(0, 0);
                rootBone.setScaleX(oldScaleX);
                rootBone.setScaleY(oldScaleY);
                rootBone.setRotation(oldRotation);
            }

            if (texture != null) {
                BlendMode slotBlendMode = slot.data.getBlendMode();
                if (slotBlendMode != blendMode) {
                    blendMode = slotBlendMode;
                    batch.setBlendFunction(blendMode.getSource(premultipliedAlpha), blendMode.getDest());
                }
                batch.draw(texture, vertices, 0, vertices.length, triangles, 0, triangles.length);
            }
        }
    }
}
