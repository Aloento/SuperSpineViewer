package com.esotericsoftware.SpineStandard;

import com.QYun.SuperSpineViewer.Loader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.ShortArray;
import com.esotericsoftware.SpineStandard.attachments.*;
import com.esotericsoftware.SpineStandard.utils.SkeletonClipping;
import com.esotericsoftware.SpineStandard.utils.TwoColorPolygonBatch;

public class SkeletonRenderer {
    static private final short[] quadTriangles = {0, 1, 2, 2, 3, 0};
    private final FloatArray vertices = new FloatArray(32);
    private final SkeletonClipping clipper = new SkeletonClipping();
    // private final Vector2 temp = new Vector2();
    // private final Vector2 temp2 = new Vector2();
    // private final Color temp3 = new Color();
    // private final Color temp4 = new Color();
    // private final Color temp5 = new Color();
    // private final Color temp6 = new Color();
    private boolean premultipliedAlpha;
    // private VertexEffect vertexEffect;

    public void draw(Batch batch, Skeleton skeleton) {
        // if (batch instanceof TwoColorPolygonBatch) {
        //     draw((TwoColorPolygonBatch) batch, skeleton);
        //     return;
        // }
        // if (batch instanceof PolygonSpriteBatch) {
        //     draw((PolygonSpriteBatch) batch, skeleton);
        //     return;
        // }
        // if (batch == null) throw new IllegalArgumentException("batch cannot be null.");
        // if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        // VertexEffect vertexEffect = this.vertexEffect;
        // if (vertexEffect != null) vertexEffect.begin(skeleton);
        // boolean premultipliedAlpha = this.premultipliedAlpha;
        // BlendMode blendMode = null;
        // float[] vertices = this.vertices.items;
        // Color skeletonColor = skeleton.color;
        // float r = skeletonColor.r, g = skeletonColor.g, b = skeletonColor.b, a = skeletonColor.a;
        // Array<Slot> drawOrder = skeleton.drawOrder;
        // for (int i = 0, n = drawOrder.size; i < n; i++) {
        //     Slot slot = drawOrder.get(i);
        //     if (!slot.bone.active && Loader.spineVersion == 38) {
        //         clipper.clipEnd(slot);
        //         continue;
        //     }
        //     Attachment attachment = slot.attachment;
        //     if (attachment instanceof RegionAttachment) {
        //         RegionAttachment region = (RegionAttachment) attachment;
        //         region.computeWorldVertices(slot.getBone(), vertices, 0, 5);
        //         Color color = region.getColor(), slotColor = slot.getColor();
        //         float alpha = a * slotColor.a * color.a * 255;
        //         float multiplier = premultipliedAlpha ? alpha : 255;
        //         BlendMode slotBlendMode = slot.data.getBlendMode();
        //         if (slotBlendMode != blendMode) {
        //             if (slotBlendMode == BlendMode.additive && premultipliedAlpha) {
        //                 slotBlendMode = BlendMode.normal;
        //                 alpha = 0;
        //             }
        //             blendMode = slotBlendMode;
        //             batch.setBlendFunction(blendMode.getSource(premultipliedAlpha), blendMode.getDest());
        //         }
        //         float c = NumberUtils.intToFloatColor(((int) alpha << 24)
        //                 | ((int) (b * slotColor.b * color.b * multiplier) << 16)
        //                 | ((int) (g * slotColor.g * color.g * multiplier) << 8)
        //                 | (int) (r * slotColor.r * color.r * multiplier));
        //         float[] uvs = region.getUVs();
        //         for (int u = 0, v = 2; u < 8; u += 2, v += 5) {
        //             vertices[v] = c;
        //             vertices[v + 1] = uvs[u];
        //             vertices[v + 2] = uvs[u + 1];
        //         }
        //         if (vertexEffect != null) applyVertexEffect(vertices, 20, 5, c, 0);
        //         batch.draw(region.getRegion().getTexture(), vertices, 0, 20);
        //     } else if (attachment instanceof ClippingAttachment) {
        //         clipper.clipStart(slot, (ClippingAttachment) attachment);
        //         continue;
        //     } else if (attachment instanceof MeshAttachment) {
        //         throw new RuntimeException(batch.getClass().getSimpleName()
        //                 + " cannot render meshes, PolygonSpriteBatch or TwoColorPolygonBatch is required.");
        //     } else if (attachment instanceof SkeletonAttachment) {
        //         Skeleton attachmentSkeleton = ((SkeletonAttachment) attachment).getSkeleton();
        //         if (attachmentSkeleton != null) draw(batch, attachmentSkeleton);
        //     }
        //     clipper.clipEnd(slot);
        // }
        // clipper.clipEnd();
        // if (vertexEffect != null) vertexEffect.end();
    }

    @SuppressWarnings("null")
    public void draw(PolygonSpriteBatch batch, Skeleton skeleton) {
        // if (batch == null) throw new IllegalArgumentException("batch cannot be null.");
        // if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        // Vector2 tempPosition = this.temp, tempUV = this.temp2;
        // Color tempLight1 = this.temp3, tempDark1 = this.temp4;
        // Color tempLight2 = this.temp5, tempDark2 = this.temp6;
        // // Spine37/6
        // Vector2 tempPos = this.temp, tempUv = this.temp2;
        // Color tempLight = this.temp3, tempDark = this.temp4;
        // Color temp5 = this.temp5, temp6 = this.temp6;
        // VertexEffect vertexEffect = this.vertexEffect;
        // if (vertexEffect != null) vertexEffect.begin(skeleton);
        // boolean premultipliedAlpha = this.premultipliedAlpha;
        // BlendMode blendMode = null;
        // int verticesLength = 0;
        // float[] vertices = null, uvs = null;
        // short[] triangles = null;
        // Color color = null, skeletonColor = skeleton.color;
        // float r = skeletonColor.r, g = skeletonColor.g, b = skeletonColor.b, a = skeletonColor.a;
        // Array<Slot> drawOrder = skeleton.drawOrder;
        // for (int i = 0, n = drawOrder.size; i < n; i++) {
        //     Slot slot = drawOrder.get(i);
        //     if (!slot.bone.active && Loader.spineVersion == 38) {
        //         clipper.clipEnd(slot);
        //         continue;
        //     }
        //     Texture texture = null;
        //     int vertexSize = clipper.isClipping() ? 2 : 5;
        //     Attachment attachment = slot.attachment;
        //     if (attachment instanceof RegionAttachment) {
        //         RegionAttachment region = (RegionAttachment) attachment;
        //         verticesLength = vertexSize << 2;
        //         vertices = this.vertices.items;
        //         region.computeWorldVertices(slot.getBone(), vertices, 0, vertexSize);
        //         triangles = quadTriangles;
        //         texture = region.getRegion().getTexture();
        //         uvs = region.getUVs();
        //         color = region.getColor();
        //     } else if (attachment instanceof MeshAttachment) {
        //         MeshAttachment mesh = (MeshAttachment) attachment;
        //         int count = mesh.getWorldVerticesLength();
        //         verticesLength = (count >> 1) * vertexSize;
        //         vertices = this.vertices.setSize(verticesLength);
        //         mesh.computeWorldVertices(slot, 0, count, vertices, 0, vertexSize);
        //         triangles = mesh.getTriangles();
        //         texture = mesh.getRegion().getTexture();
        //         uvs = mesh.getUVs();
        //         color = mesh.getColor();
        //     } else if (attachment instanceof ClippingAttachment) {
        //         ClippingAttachment clip = (ClippingAttachment) attachment;
        //         clipper.clipStart(slot, clip);
        //         continue;
        //     } else if (attachment instanceof SkeletonAttachment) {
        //         Skeleton attachmentSkeleton = ((SkeletonAttachment) attachment).getSkeleton();
        //         if (attachmentSkeleton != null) draw(batch, attachmentSkeleton);
        //     }
        //     if (texture != null) {
        //         Color slotColor = slot.getColor();
        //         float alpha = a * slotColor.a * color.a * 255;
        //         float multiplier = premultipliedAlpha ? alpha : 255;
        //         BlendMode slotBlendMode = slot.data.getBlendMode();
        //         if (slotBlendMode != blendMode) {
        //             if (slotBlendMode == BlendMode.additive && premultipliedAlpha) {
        //                 slotBlendMode = BlendMode.normal;
        //                 alpha = 0;
        //             }
        //             blendMode = slotBlendMode;
        //             batch.setBlendFunction(blendMode.getSource(premultipliedAlpha), blendMode.getDest());
        //         }
        //         float c = NumberUtils.intToFloatColor(((int) alpha << 24)
        //                 | ((int) (b * slotColor.b * color.b * multiplier) << 16)
        //                 | ((int) (g * slotColor.g * color.g * multiplier) << 8)
        //                 | (int) (r * slotColor.r * color.r * multiplier));
        //         if (clipper.isClipping()) {
        //             clipper.clipTriangles(vertices, verticesLength, triangles, triangles.length, uvs, c, 0, false);
        //             FloatArray clippedVertices = clipper.getClippedVertices();
        //             ShortArray clippedTriangles = clipper.getClippedTriangles();
        //             if (vertexEffect != null)
        //                 applyVertexEffect(clippedVertices.items, clippedVertices.size, 5, c, 0);
        //             batch.draw(texture, clippedVertices.items, 0, clippedVertices.size, clippedTriangles.items, 0,
        //                     clippedTriangles.size);
        //         } else {
        //             if (vertexEffect != null) {
        //                 if (Loader.spineVersion > 37) {
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
        //                     temp5.set(NumberUtils.floatToIntColor(c));
        //                     temp6.set(0);
        //                     for (int v = 0, u = 0; v < verticesLength; v += 5, u += 2) {
        //                         tempPos.x = vertices[v];
        //                         tempPos.y = vertices[v + 1];
        //                         tempLight.set(temp5);
        //                         tempDark.set(temp6);
        //                         tempUv.x = uvs[u];
        //                         tempUv.y = uvs[u + 1];
        //                         vertexEffect.transform(tempPos, tempUv, tempLight, tempDark);
        //                         vertices[v] = tempPos.x;
        //                         vertices[v + 1] = tempPos.y;
        //                         vertices[v + 2] = tempLight.toFloatBits();
        //                         vertices[v + 3] = tempUv.x;
        //                         vertices[v + 4] = tempUv.y;
        //                     }
        //                 }
        //             } else {
        //                 for (int v = 2, u = 0; v < verticesLength; v += 5, u += 2) {
        //                     vertices[v] = c;
        //                     vertices[v + 1] = uvs[u];
        //                     vertices[v + 2] = uvs[u + 1];
        //                 }
        //             }
        //             batch.draw(texture, vertices, 0, verticesLength, triangles, 0, triangles.length);
        //         }
        //     }
        //     clipper.clipEnd(slot);
        // }
        // clipper.clipEnd();
        // if (vertexEffect != null) vertexEffect.end();
    }

    @SuppressWarnings("null")
    public void draw(TwoColorPolygonBatch batch, Skeleton skeleton) {
        if (Loader.spineVersion > 35) {
            if (batch == null) throw new IllegalArgumentException("batch cannot be null.");
            if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
            // Vector2 tempPosition = this.temp, tempUV = this.temp2;
            // Color tempLight1 = this.temp3, tempDark1 = this.temp4;
            // Color tempLight2 = this.temp5, tempDark2 = this.temp6;
            // // Spine37/6
            // Vector2 tempPos = this.temp, tempUv = this.temp2;
            // Color tempLight = this.temp3, tempDark = this.temp4;
            // Color temp5 = this.temp5, temp6 = this.temp6;

            // VertexEffect vertexEffect = this.vertexEffect;
            // if (vertexEffect != null) vertexEffect.begin(skeleton);
            boolean premultipliedAlpha = this.premultipliedAlpha;
            batch.setPremultipliedAlpha(premultipliedAlpha);
            BlendMode blendMode = null;
            int verticesLength = 0;
            float[] vertices = null, uvs = null;
            short[] triangles = null;
            Color color = null, skeletonColor = skeleton.color;
            float r = skeletonColor.r, g = skeletonColor.g, b = skeletonColor.b, a = skeletonColor.a;
            Array<Slot> drawOrder = skeleton.drawOrder;
            for (int i = 0, n = drawOrder.size; i < n; i++) {
                Slot slot = drawOrder.get(i);
                if (!slot.bone.active && Loader.spineVersion == 38) {
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
                    float multiplier = premultipliedAlpha ? alpha : 255;
                    BlendMode slotBlendMode = slot.data.getBlendMode();
                    if (slotBlendMode != blendMode) {
                        if (slotBlendMode == BlendMode.additive && premultipliedAlpha) {
                            slotBlendMode = BlendMode.normal;
                            alpha = 0;
                        }
                        blendMode = slotBlendMode;
                        batch.setBlendFunction(blendMode.getSource(premultipliedAlpha), blendMode.getDest());
                    }
                    float red = r * color.r * multiplier;
                    float green = g * color.g * multiplier;
                    float blue = b * color.b * multiplier;
                    float light = NumberUtils.intToFloatColor(((int) alpha << 24)
                            | ((int) (blue * lightColor.b) << 16)
                            | ((int) (green * lightColor.g) << 8)
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
                        //     applyVertexEffect(clippedVertices.items, clippedVertices.size, light, dark);
                        batch.drawTwoColor(texture, clippedVertices.items, 0, clippedVertices.size, clippedTriangles.items, 0,
                                clippedTriangles.size);
                    } else {
                        // if (vertexEffect != null) {
                            // if (Loader.spineVersion > 37) {
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
                            //     temp5.set(NumberUtils.floatToIntColor(light));
                            //     temp6.set(NumberUtils.floatToIntColor(dark));
                            //     for (int v = 0, u = 0; v < verticesLength; v += 6, u += 2) {
                            //         tempPos.x = vertices[v];
                            //         tempPos.y = vertices[v + 1];
                            //         tempLight.set(temp5);
                            //         tempDark.set(temp6);
                            //         tempUv.x = uvs[u];
                            //         tempUv.y = uvs[u + 1];
                            //         vertexEffect.transform(tempPos, tempUv, tempLight, tempDark);
                            //         vertices[v] = tempPos.x;
                            //         vertices[v + 1] = tempPos.y;
                            //         vertices[v + 2] = tempLight.toFloatBits();
                            //         vertices[v + 3] = tempDark.toFloatBits();
                            //         vertices[v + 4] = tempUv.x;
                            //         vertices[v + 5] = tempUv.y;
                            //     }
                            // }
                        // } else {
                            for (int v = 2, u = 0; v < verticesLength; v += 6, u += 2) {
                                vertices[v] = light;
                                vertices[v + 1] = dark;
                                vertices[v + 2] = uvs[u];
                                vertices[v + 3] = uvs[u + 1];
                            }
                        // }
                        batch.drawTwoColor(texture, vertices, 0, verticesLength, triangles, 0, triangles.length);
                    }
                }
                clipper.clipEnd(slot);
            }
            clipper.clipEnd();
            // if (vertexEffect != null) vertexEffect.end();
        } else {
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

                } else if (attachment instanceof SkeletonAttachment) {
                    Skeleton attachmentSkeleton = ((SkeletonAttachment) attachment).getSkeleton();
                    if (attachmentSkeleton == null) continue;
                    Bone bone = slot.getBone();
                    Bone rootBone = attachmentSkeleton.getRootBone();
                    float oldScaleX = rootBone.getScaleX();
                    float oldScaleY = rootBone.getScaleY();
                    float oldRotation = rootBone.getRotation();
                    attachmentSkeleton.setPosition(bone.getWorldX(), bone.getWorldY());
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

    // private void applyVertexEffect(float[] vertices, int verticesLength, float light, float dark) {
    //     Vector2 tempPos = this.temp, tempUv = this.temp2;
    //     Color tempLight = this.temp3, tempDark = this.temp4;
    //     Color temp5 = this.temp5, temp6 = this.temp6;
    //     // VertexEffect vertexEffect = this.vertexEffect;
    //     temp5.set(NumberUtils.floatToIntColor(light));
    //     temp6.set(NumberUtils.floatToIntColor(dark));
    //     // Spine38
    //     Vector2 tempPosition = this.temp, tempUV = this.temp2;
    //     Color tempLight1 = this.temp3, tempDark1 = this.temp4;
    //     Color tempLight2 = this.temp5, tempDark2 = this.temp6;
    //     tempLight1.set(NumberUtils.floatToIntColor(light));
    //     tempDark1.set(NumberUtils.floatToIntColor(dark));
    //     for (int v = 0; v < verticesLength; v += 6) {
    //         if (Loader.spineVersion > 37) {
    //             tempPosition.x = vertices[v];
    //             tempPosition.y = vertices[v + 1];
    //             tempUV.x = vertices[v + 4];
    //             tempUV.y = vertices[v + 5];
    //             tempLight2.set(tempLight1);
    //             tempDark2.set(tempDark1);
    //             vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2);
    //             vertices[v] = tempPosition.x;
    //             vertices[v + 1] = tempPosition.y;
    //             vertices[v + 2] = tempLight2.toFloatBits();
    //             vertices[v + 3] = tempDark2.toFloatBits();
    //             vertices[v + 4] = tempUV.x;
    //             vertices[v + 5] = tempUV.y;
    //         } else {
    //             tempPos.x = vertices[v];
    //             tempPos.y = vertices[v + 1];
    //             tempUv.x = vertices[v + 4];
    //             tempUv.y = vertices[v + 5];
    //             tempLight.set(temp5);
    //             tempDark.set(temp6);
    //             vertexEffect.transform(tempPos, tempUv, tempLight, tempDark);
    //             vertices[v] = tempPos.x;
    //             vertices[v + 1] = tempPos.y;
    //             vertices[v + 2] = tempLight.toFloatBits();
    //             vertices[v + 3] = tempDark.toFloatBits();
    //             vertices[v + 4] = tempUv.x;
    //             vertices[v + 5] = tempUv.y;
    //         }
    //     }
    // }

    // public boolean getPremultipliedAlpha() {
    //     return premultipliedAlpha;
    // }

    public void setPremultipliedAlpha(boolean premultipliedAlpha) {
        this.premultipliedAlpha = premultipliedAlpha;
    }

    // public interface VertexEffect {
    //     void begin(Skeleton skeleton);
    //
    //     void transform(Vector2 position, Vector2 uv, Color color, Color darkColor);
    //
    //     void end();
    // }
}
