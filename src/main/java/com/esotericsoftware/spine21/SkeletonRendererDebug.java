package com.esotericsoftware.spine21;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.spine21.attachments.Attachment;
import com.esotericsoftware.spine21.attachments.MeshAttachment;
import com.esotericsoftware.spine21.attachments.RegionAttachment;
import com.esotericsoftware.spine21.attachments.SkinnedMeshAttachment;

import static com.badlogic.gdx.graphics.g2d.Batch.*;

public class SkeletonRendererDebug {
    static private final Color boneLineColor = Color.RED;
    static private final Color boneOriginColor = Color.GREEN;
    static private final Color attachmentLineColor = new Color(0, 0, 1, 0.5f);
    static private final Color triangleLineColor = new Color(1, 0.64f, 0, 0.5f);
    static private final Color boundingBoxColor = new Color(0, 1, 0, 0.8f);
    static private final Color aabbColor = new Color(0, 1, 0, 0.5f);

    private final ShapeRenderer shapes;
    private final SkeletonBounds bounds = new SkeletonBounds();
    private boolean drawBones = true, drawRegionAttachments = true, drawBoundingBoxes = true;
    private boolean drawMeshHull = true, drawMeshTriangles = true;
    private float scale = 1;
    private boolean premultipliedAlpha;

    public SkeletonRendererDebug() {
        shapes = new ShapeRenderer();
    }

    public SkeletonRendererDebug(ShapeRenderer shapes) {
        this.shapes = shapes;
    }

    public void draw(Skeleton skeleton) {
        float skeletonX = skeleton.getX();
        float skeletonY = skeleton.getY();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        int srcFunc = premultipliedAlpha ? GL20.GL_ONE : GL20.GL_SRC_ALPHA;
        Gdx.gl.glBlendFunc(srcFunc, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ShapeRenderer shapes = this.shapes;

        Array<Bone> bones = skeleton.getBones();
        if (drawBones) {
            shapes.setColor(boneLineColor);
            shapes.begin(ShapeType.Filled);
            for (int i = 0, n = bones.size; i < n; i++) {
                Bone bone = bones.get(i);
                if (bone.parent == null) continue;
                float x = skeletonX + bone.data.length * bone.m00 + bone.worldX;
                float y = skeletonY + bone.data.length * bone.m10 + bone.worldY;
                float boneWidth = 2;
                shapes.rectLine(skeletonX + bone.worldX, skeletonY + bone.worldY, x, y, boneWidth * scale);
            }
            shapes.end();
            shapes.begin(ShapeType.Line);
            shapes.x(skeletonX, skeletonY, 4 * scale);
        } else
            shapes.begin(ShapeType.Line);

        if (drawRegionAttachments) {
            shapes.setColor(attachmentLineColor);
            Array<Slot> slots = skeleton.getSlots();
            for (int i = 0, n = slots.size; i < n; i++) {
                Slot slot = slots.get(i);
                Attachment attachment = slot.attachment;
                if (attachment instanceof RegionAttachment) {
                    RegionAttachment regionAttachment = (RegionAttachment) attachment;
                    regionAttachment.updateWorldVertices(slot, false);
                    float[] vertices = regionAttachment.getWorldVertices();
                    shapes.line(vertices[X1], vertices[Y1], vertices[X2], vertices[Y2]);
                    shapes.line(vertices[X2], vertices[Y2], vertices[X3], vertices[Y3]);
                    shapes.line(vertices[X3], vertices[Y3], vertices[X4], vertices[Y4]);
                    shapes.line(vertices[X4], vertices[Y4], vertices[X1], vertices[Y1]);
                }
            }
        }

        if (drawMeshHull || drawMeshTriangles) {
            Array<Slot> slots = skeleton.getSlots();
            for (int i = 0, n = slots.size; i < n; i++) {
                Slot slot = slots.get(i);
                Attachment attachment = slot.attachment;
                float[] vertices = null;
                short[] triangles = null;
                int hullLength = 0;
                if (attachment instanceof MeshAttachment) {
                    MeshAttachment mesh = (MeshAttachment) attachment;
                    mesh.updateWorldVertices(slot, false);
                    vertices = mesh.getWorldVertices();
                    triangles = mesh.getTriangles();
                    hullLength = mesh.getHullLength();
                } else if (attachment instanceof SkinnedMeshAttachment) {
                    SkinnedMeshAttachment mesh = (SkinnedMeshAttachment) attachment;
                    mesh.updateWorldVertices(slot, false);
                    vertices = mesh.getWorldVertices();
                    triangles = mesh.getTriangles();
                    hullLength = mesh.getHullLength();
                }
                if (vertices == null || triangles == null) continue;
                if (drawMeshTriangles) {
                    shapes.setColor(triangleLineColor);
                    for (int ii = 0, nn = triangles.length; ii < nn; ii += 3) {
                        int v1 = triangles[ii] * 5, v2 = triangles[ii + 1] * 5, v3 = triangles[ii + 2] * 5;
                        shapes.triangle(vertices[v1], vertices[v1 + 1],
                                vertices[v2], vertices[v2 + 1],
                                vertices[v3], vertices[v3 + 1]
                        );
                    }
                }
                if (drawMeshHull && hullLength > 0) {
                    shapes.setColor(attachmentLineColor);
                    hullLength = hullLength / 2 * 5;
                    float lastX = vertices[hullLength - 5], lastY = vertices[hullLength - 4];
                    for (int ii = 0, nn = hullLength; ii < nn; ii += 5) {
                        float x = vertices[ii], y = vertices[ii + 1];
                        shapes.line(x, y, lastX, lastY);
                        lastX = x;
                        lastY = y;
                    }
                }
            }
        }

        if (drawBoundingBoxes) {
            SkeletonBounds bounds = this.bounds;
            bounds.update(skeleton, true);
            shapes.setColor(aabbColor);
            shapes.rect(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
            shapes.setColor(boundingBoxColor);
            Array<FloatArray> polygons = bounds.getPolygons();
            for (int i = 0, n = polygons.size; i < n; i++) {
                FloatArray polygon = polygons.get(i);
                shapes.polygon(polygon.items, 0, polygon.size);
            }
        }

        shapes.end();
        shapes.begin(ShapeType.Filled);

        if (drawBones) {
            shapes.setColor(boneOriginColor);
            for (int i = 0, n = bones.size; i < n; i++) {
                Bone bone = bones.get(i);
                shapes.setColor(Color.GREEN);
                shapes.circle(skeletonX + bone.worldX, skeletonY + bone.worldY, 3 * scale, 8);
            }
        }

        shapes.end();
    }

    public ShapeRenderer getShapeRenderer() {
        return shapes;
    }

    public void setBones(boolean bones) {
        this.drawBones = bones;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setRegionAttachments(boolean regionAttachments) {
        this.drawRegionAttachments = regionAttachments;
    }

    public void setBoundingBoxes(boolean boundingBoxes) {
        this.drawBoundingBoxes = boundingBoxes;
    }

    public void setMeshHull(boolean meshHull) {
        this.drawMeshHull = meshHull;
    }

    public void setMeshTriangles(boolean meshTriangles) {
        this.drawMeshTriangles = meshTriangles;
    }

    public void setPremultipliedAlpha(boolean premultipliedAlpha) {
        this.premultipliedAlpha = premultipliedAlpha;
    }
}
