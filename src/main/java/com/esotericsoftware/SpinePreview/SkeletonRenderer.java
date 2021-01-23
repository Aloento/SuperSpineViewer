package com.esotericsoftware.SpinePreview;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.ShortArray;
import com.esotericsoftware.SpinePreview.attachments.*;
import com.esotericsoftware.SpinePreview.utils.SkeletonClipping;
import com.esotericsoftware.SpinePreview.utils.TwoColorPolygonBatch;

public class SkeletonRenderer {
    static private final short[] quadTriangles = {0, 1, 2, 2, 3, 0};
    private final FloatArray vertices = new FloatArray(32);
    private final SkeletonClipping clipper = new SkeletonClipping();
    private final Vector2 temp = new Vector2();
    private final Vector2 temp2 = new Vector2();
    private final Color temp3 = new Color();
    private final Color temp4 = new Color();
    private final Color temp5 = new Color();
    private final Color temp6 = new Color();
    private boolean pmaColors, pmaBlendModes;
    // private @Null
    // VertexEffect vertexEffect;

    // public void draw(Batch batch, Skeleton skeleton) {
    //     if (batch instanceof TwoColorPolygonBatch) {
    //         draw((TwoColorPolygonBatch) batch, skeleton);
    //         return;
    //     }
    //     if (batch instanceof PolygonSpriteBatch) {
    //         draw((PolygonSpriteBatch) batch, skeleton);
    //         return;
    //     }
    //     if (batch == null) throw new IllegalArgumentException("batch cannot be null.");
    //     if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
    //     VertexEffect vertexEffect = this.vertexEffect;
    //     if (vertexEffect != null) vertexEffect.begin(skeleton);
    //     boolean pmaColors = this.pmaColors, pmaBlendModes = this.pmaBlendModes;
    //     BlendMode blendMode = null;
    //     float[] vertices = this.vertices.items;
    //     Color skeletonColor = skeleton.color;
    //     float r = skeletonColor.r, g = skeletonColor.g, b = skeletonColor.b, a = skeletonColor.a;
    //     Object[] drawOrder = skeleton.drawOrder.items;
    //     for (int i = 0, n = skeleton.drawOrder.size; i < n; i++) {
    //         Slot slot = (Slot) drawOrder[i];
    //         if (!slot.bone.active) {
    //             clipper.clipEnd(slot);
    //             continue;
    //         }
    //         Attachment attachment = slot.attachment;
    //         if (attachment instanceof RegionAttachment) {
    //             RegionAttachment region = (RegionAttachment) attachment;
    //             region.computeWorldVertices(slot.getBone(), vertices, 0, 5);
    //             Color color = region.getColor(), slotColor = slot.getColor();
    //             float alpha = a * slotColor.a * color.a * 255;
    //             float multiplier = pmaColors ? alpha : 255;
    //             BlendMode slotBlendMode = slot.data.getBlendMode();
    //             if (slotBlendMode != blendMode) {
    //                 if (slotBlendMode == BlendMode.additive && pmaColors) {
    //                     slotBlendMode = BlendMode.normal;
    //                     alpha = 0;
    //                 }
    //                 blendMode = slotBlendMode;
    //                 blendMode.apply(batch, pmaBlendModes);
    //             }
    //             float c = NumberUtils.intToFloatColor((int) alpha << 24
    //                     | (int) (b * slotColor.b * color.b * multiplier) << 16
    //                     | (int) (g * slotColor.g * color.g * multiplier) << 8
    //                     | (int) (r * slotColor.r * color.r * multiplier));
    //             float[] uvs = region.getUVs();
    //             for (int u = 0, v = 2; u < 8; u += 2, v += 5) {
    //                 vertices[v] = c;
    //                 vertices[v + 1] = uvs[u];
    //                 vertices[v + 2] = uvs[u + 1];
    //             }
    //             if (vertexEffect != null) applyVertexEffect(vertices, 20, 5, c, 0);
    //             batch.draw(region.getRegion().getTexture(), vertices, 0, 20);
    //         } else if (attachment instanceof ClippingAttachment) {
    //             clipper.clipStart(slot, (ClippingAttachment) attachment);
    //             continue;
    //         } else if (attachment instanceof MeshAttachment) {
    //             throw new RuntimeException(batch.getClass().getSimpleName()
    //                     + " cannot render meshes, PolygonSpriteBatch or TwoColorPolygonBatch is required.");
    //         } else if (attachment instanceof SkeletonAttachment) {
    //             Skeleton attachmentSkeleton = ((SkeletonAttachment) attachment).getSkeleton();
    //             if (attachmentSkeleton != null) draw(batch, attachmentSkeleton);
    //         }
    //         clipper.clipEnd(slot);
    //     }
    //     clipper.clipEnd();
    //     if (vertexEffect != null) vertexEffect.end();
    // }

    // public void draw(PolygonSpriteBatch batch, Skeleton skeleton) {
    //     if (batch == null) throw new IllegalArgumentException("batch cannot be null.");
    //     if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
    //     Vector2 tempPosition = this.temp, tempUV = this.temp2;
    //     Color tempLight1 = this.temp3, tempDark1 = this.temp4;
    //     Color tempLight2 = this.temp5, tempDark2 = this.temp6;
    //     VertexEffect vertexEffect = this.vertexEffect;
    //     if (vertexEffect != null) vertexEffect.begin(skeleton);
    //     boolean pmaColors = this.pmaColors, pmaBlendModes = this.pmaBlendModes;
    //     BlendMode blendMode = null;
    //     int verticesLength = 0;
    //     float[] vertices = null, uvs = null;
    //     short[] triangles = null;
    //     Color color = null, skeletonColor = skeleton.color;
    //     float r = skeletonColor.r, g = skeletonColor.g, b = skeletonColor.b, a = skeletonColor.a;
    //     Object[] drawOrder = skeleton.drawOrder.items;
    //     for (int i = 0, n = skeleton.drawOrder.size; i < n; i++) {
    //         Slot slot = (Slot) drawOrder[i];
    //         if (!slot.bone.active) {
    //             clipper.clipEnd(slot);
    //             continue;
    //         }
    //         Texture texture = null;
    //         int vertexSize = clipper.isClipping() ? 2 : 5;
    //         Attachment attachment = slot.attachment;
    //         if (attachment instanceof RegionAttachment) {
    //             RegionAttachment region = (RegionAttachment) attachment;
    //             verticesLength = vertexSize << 2;
    //             vertices = this.vertices.items;
    //             region.computeWorldVertices(slot.getBone(), vertices, 0, vertexSize);
    //             triangles = quadTriangles;
    //             texture = region.getRegion().getTexture();
    //             uvs = region.getUVs();
    //             color = region.getColor();
    //         } else if (attachment instanceof MeshAttachment) {
    //             MeshAttachment mesh = (MeshAttachment) attachment;
    //             int count = mesh.getWorldVerticesLength();
    //             verticesLength = (count >> 1) * vertexSize;
    //             vertices = this.vertices.setSize(verticesLength);
    //             mesh.computeWorldVertices(slot, 0, count, vertices, 0, vertexSize);
    //             triangles = mesh.getTriangles();
    //             texture = mesh.getRegion().getTexture();
    //             uvs = mesh.getUVs();
    //             color = mesh.getColor();
    //         } else if (attachment instanceof ClippingAttachment) {
    //             ClippingAttachment clip = (ClippingAttachment) attachment;
    //             clipper.clipStart(slot, clip);
    //             continue;
    //         } else if (attachment instanceof SkeletonAttachment) {
    //             Skeleton attachmentSkeleton = ((SkeletonAttachment) attachment).getSkeleton();
    //             if (attachmentSkeleton != null) draw(batch, attachmentSkeleton);
    //         }
    //         if (texture != null) {
    //             Color slotColor = slot.getColor();
    //             float alpha = a * slotColor.a * color.a * 255;
    //             float multiplier = pmaColors ? alpha : 255;
    //             BlendMode slotBlendMode = slot.data.getBlendMode();
    //             if (slotBlendMode != blendMode) {
    //                 if (slotBlendMode == BlendMode.additive && pmaColors) {
    //                     slotBlendMode = BlendMode.normal;
    //                     alpha = 0;
    //                 }
    //                 blendMode = slotBlendMode;
    //                 blendMode.apply(batch, pmaBlendModes);
    //             }
    //             float c = NumberUtils.intToFloatColor((int) alpha << 24
    //                     | (int) (b * slotColor.b * color.b * multiplier) << 16
    //                     | (int) (g * slotColor.g * color.g * multiplier) << 8
    //                     | (int) (r * slotColor.r * color.r * multiplier));
    //             if (clipper.isClipping()) {
    //                 clipper.clipTriangles(vertices, verticesLength, triangles, triangles.length, uvs, c, 0, false);
    //                 FloatArray clippedVertices = clipper.getClippedVertices();
    //                 ShortArray clippedTriangles = clipper.getClippedTriangles();
    //                 if (vertexEffect != null) applyVertexEffect(clippedVertices.items, clippedVertices.size, 5, c, 0);
    //                 batch.draw(texture, clippedVertices.items, 0, clippedVertices.size, clippedTriangles.items, 0,
    //                         clippedTriangles.size);
    //             } else {
    //                 if (vertexEffect != null) {
    //                     tempLight1.set(NumberUtils.floatToIntColor(c));
    //                     tempDark1.set(0);
    //                     for (int v = 0, u = 0; v < verticesLength; v += 5, u += 2) {
    //                         tempPosition.x = vertices[v];
    //                         tempPosition.y = vertices[v + 1];
    //                         tempLight2.set(tempLight1);
    //                         tempDark2.set(tempDark1);
    //                         tempUV.x = uvs[u];
    //                         tempUV.y = uvs[u + 1];
    //                         vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2);
    //                         vertices[v] = tempPosition.x;
    //                         vertices[v + 1] = tempPosition.y;
    //                         vertices[v + 2] = tempLight2.toFloatBits();
    //                         vertices[v + 3] = tempUV.x;
    //                         vertices[v + 4] = tempUV.y;
    //                     }
    //                 } else {
    //                     for (int v = 2, u = 0; v < verticesLength; v += 5, u += 2) {
    //                         vertices[v] = c;
    //                         vertices[v + 1] = uvs[u];
    //                         vertices[v + 2] = uvs[u + 1];
    //                     }
    //                 }
    //                 batch.draw(texture, vertices, 0, verticesLength, triangles, 0, triangles.length);
    //             }
    //         }
    //         clipper.clipEnd(slot);
    //     }
    //     clipper.clipEnd();
    //     if (vertexEffect != null) vertexEffect.end();
    // }

    public void draw(TwoColorPolygonBatch batch, Skeleton skeleton) {
        if (batch == null) throw new IllegalArgumentException("batch cannot be null.");
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        // Vector2 tempPosition = this.temp, tempUV = this.temp2;
        // Color tempLight1 = this.temp3, tempDark1 = this.temp4;
        // Color tempLight2 = this.temp5, tempDark2 = this.temp6;
        // VertexEffect vertexEffect = this.vertexEffect;
        // if (vertexEffect != null) vertexEffect.begin(skeleton);
        boolean pmaColors = this.pmaColors, pmaBlendModes = this.pmaBlendModes;
        batch.setPremultipliedAlpha(pmaColors);
        BlendMode blendMode = null;
        int verticesLength = 0;
        float[] vertices = null, uvs = null;
        short[] triangles = null;
        Color color = null, skeletonColor = skeleton.color;
        float r = skeletonColor.r, g = skeletonColor.g, b = skeletonColor.b, a = skeletonColor.a;
        Object[] drawOrder = skeleton.drawOrder.items;
        for (int i = 0, n = skeleton.drawOrder.size; i < n; i++) {
            Slot slot = (Slot) drawOrder[i];
            if (!slot.bone.active) {
                clipper.clipEnd(slot);
                continue;
            }
            Texture texture = null;
            int vertexSize = clipper.isClipping() ? 2 : 6;
            Attachment attachment = slot.attachment;
            if (attachment instanceof RegionAttachment) {
                RegionAttachment region = (RegionAttachment) attachment;
                verticesLength = vertexSize << 2;
                vertices = this.vertices.items;
                region.computeWorldVertices(slot.getBone(), vertices, 0, vertexSize);
                triangles = quadTriangles;
                texture = region.getRegion().getTexture();
                uvs = region.getUVs();
                color = region.getColor();
            } else if (attachment instanceof MeshAttachment) {
                MeshAttachment mesh = (MeshAttachment) attachment;
                int count = mesh.getWorldVerticesLength();
                verticesLength = (count >> 1) * vertexSize;
                vertices = this.vertices.setSize(verticesLength);
                mesh.computeWorldVertices(slot, 0, count, vertices, 0, vertexSize);
                triangles = mesh.getTriangles();
                texture = mesh.getRegion().getTexture();
                uvs = mesh.getUVs();
                color = mesh.getColor();
            } else if (attachment instanceof ClippingAttachment) {
                ClippingAttachment clip = (ClippingAttachment) attachment;
                clipper.clipStart(slot, clip);
                continue;
            } else if (attachment instanceof SkeletonAttachment) {
                Skeleton attachmentSkeleton = ((SkeletonAttachment) attachment).getSkeleton();
                if (attachmentSkeleton != null) draw(batch, attachmentSkeleton);
            }
            if (texture != null) {
                Color lightColor = slot.getColor();
                float alpha = a * lightColor.a * color.a * 255;
                float multiplier = pmaColors ? alpha : 255;
                BlendMode slotBlendMode = slot.data.getBlendMode();
                if (slotBlendMode != blendMode) {
                    if (slotBlendMode == BlendMode.additive && pmaColors) {
                        slotBlendMode = BlendMode.normal;
                        alpha = 0;
                    }
                    blendMode = slotBlendMode;
                    blendMode.apply(batch, pmaBlendModes);
                }
                float red = r * color.r * multiplier;
                float green = g * color.g * multiplier;
                float blue = b * color.b * multiplier;
                float light = NumberUtils.intToFloatColor((int) alpha << 24
                        | (int) (blue * lightColor.b) << 16
                        | (int) (green * lightColor.g) << 8
                        | (int) (red * lightColor.r));
                Color darkColor = slot.getDarkColor();
                float dark = darkColor == null ? 0
                        : NumberUtils.intToFloatColor((int) (blue * darkColor.b) << 16
                        | (int) (green * darkColor.g) << 8
                        | (int) (red * darkColor.r));
                if (clipper.isClipping()) {
                    clipper.clipTriangles(vertices, verticesLength, triangles, triangles.length, uvs, light, dark, true);
                    FloatArray clippedVertices = clipper.getClippedVertices();
                    ShortArray clippedTriangles = clipper.getClippedTriangles();
                    // if (vertexEffect != null)
                    //     applyVertexEffect(clippedVertices.items, clippedVertices.size, 6, light, dark);
                    batch.drawTwoColor(texture, clippedVertices.items, 0, clippedVertices.size, clippedTriangles.items, 0,
                            clippedTriangles.size);
                } else {
                    // if (vertexEffect != null) {
                    //     tempLight1.set(NumberUtils.floatToIntColor(light));
                    //     tempDark1.set(NumberUtils.floatToIntColor(dark));
                    //     for (int v = 0, u = 0; v < verticesLength; v += 6, u += 2) {
                    //         tempPosition.x = vertices[v];
                    //         tempPosition.y = vertices[v + 1];
                    //         tempLight2.set(tempLight1);
                    //         tempDark2.set(tempDark1);
                    //         tempUV.x = uvs[u];
                    //         tempUV.y = uvs[u + 1];
                    //         vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2);
                    //         vertices[v] = tempPosition.x;
                    //         vertices[v + 1] = tempPosition.y;
                    //         vertices[v + 2] = tempLight2.toFloatBits();
                    //         vertices[v + 3] = tempDark2.toFloatBits();
                    //         vertices[v + 4] = tempUV.x;
                    //         vertices[v + 5] = tempUV.y;
                    //     }
                    // } else {
                    for (int v = 2, u = 0; v < verticesLength; v += 6, u += 2) {
                        vertices[v] = light;
                        vertices[v + 1] = dark;
                        vertices[v + 2] = uvs[u];
                        vertices[v + 3] = uvs[u + 1];
                        // }
                    }
                    batch.drawTwoColor(texture, vertices, 0, verticesLength, triangles, 0, triangles.length);
                }
            }
            clipper.clipEnd(slot);
        }
        clipper.clipEnd();
        // if (vertexEffect != null) vertexEffect.end();
    }

    // private void applyVertexEffect(float[] vertices, int verticesLength, int stride, float light, float dark) {
    //     Vector2 tempPosition = this.temp, tempUV = this.temp2;
    //     Color tempLight1 = this.temp3, tempDark1 = this.temp4;
    //     Color tempLight2 = this.temp5, tempDark2 = this.temp6;
    //     // VertexEffect vertexEffect = this.vertexEffect;
    //     tempLight1.set(NumberUtils.floatToIntColor(light));
    //     tempDark1.set(NumberUtils.floatToIntColor(dark));
    //     if (stride == 5) {
    //         for (int v = 0; v < verticesLength; v += stride) {
    //             tempPosition.x = vertices[v];
    //             tempPosition.y = vertices[v + 1];
    //             tempUV.x = vertices[v + 3];
    //             tempUV.y = vertices[v + 4];
    //             tempLight2.set(tempLight1);
    //             tempDark2.set(tempDark1);
    //             // vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2);
    //             vertices[v] = tempPosition.x;
    //             vertices[v + 1] = tempPosition.y;
    //             vertices[v + 2] = tempLight2.toFloatBits();
    //             vertices[v + 3] = tempUV.x;
    //             vertices[v + 4] = tempUV.y;
    //         }
    //     } else {
    //         for (int v = 0; v < verticesLength; v += stride) {
    //             tempPosition.x = vertices[v];
    //             tempPosition.y = vertices[v + 1];
    //             tempUV.x = vertices[v + 4];
    //             tempUV.y = vertices[v + 5];
    //             tempLight2.set(tempLight1);
    //             tempDark2.set(tempDark1);
    //             // vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2);
    //             vertices[v] = tempPosition.x;
    //             vertices[v + 1] = tempPosition.y;
    //             vertices[v + 2] = tempLight2.toFloatBits();
    //             vertices[v + 3] = tempDark2.toFloatBits();
    //             vertices[v + 4] = tempUV.x;
    //             vertices[v + 5] = tempUV.y;
    //         }
    //     }
    // }

    // public boolean getPremultipliedAlphaColors() {
    //     return pmaColors;
    // }

    // public void setPremultipliedAlphaColors(boolean pmaColors) {
    //     this.pmaColors = pmaColors;
    // }

    // public boolean getPremultipliedAlphaBlendModes() {
    //     return pmaBlendModes;
    // }

    // public void setPremultipliedAlphaBlendModes(boolean pmaBlendModes) {
    //     this.pmaBlendModes = pmaBlendModes;
    // }

    public void setPremultipliedAlpha(boolean pmaColorsAndBlendModes) {
        pmaColors = pmaColorsAndBlendModes;
        pmaBlendModes = pmaColorsAndBlendModes;
    }

    // public @Null
    // VertexEffect getVertexEffect() {
    //     return vertexEffect;
    // }

    // public void setVertexEffect(@Null VertexEffect vertexEffect) {
    //     this.vertexEffect = vertexEffect;
    // }

    // public interface VertexEffect {
    //     void begin(Skeleton skeleton);
    //
    //     // void transform(Vector2 position, Vector2 uv, Color color, Color darkColor);
    //
    //     void end();
    // }
}
