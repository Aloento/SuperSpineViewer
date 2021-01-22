package com.esotericsoftware.SpineStandard;

import com.QYun.SuperSpineViewer.RuntimesLoader;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.SpineStandard.BoneData.TransformMode;

import static com.esotericsoftware.SpineStandard.utils.SpineUtils.*;

public class Bone implements Updatable {
    final BoneData data;
    final Skeleton skeleton;
    final Bone parent;
    final Array<Bone> children = new Array<>();
    float x, y, rotation, scaleX, scaleY, shearX, shearY;
    float ax, ay, arotation, ascaleX, ascaleY, ashearX, ashearY;
    boolean appliedValid;
    float a, b, worldX;
    float c, d, worldY;
    float worldSignX, worldSignY; // Spine34
    boolean sorted, active;

    public Bone(BoneData data, Skeleton skeleton, Bone parent) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        this.data = data;
        this.skeleton = skeleton;
        this.parent = parent;
        setToSetupPose();
    }

    // public Bone(Bone bone, Skeleton skeleton, Bone parent) {
    //     if (bone == null) throw new IllegalArgumentException("bone cannot be null.");
    //     if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
    //     this.skeleton = skeleton;
    //     this.parent = parent;
    //     data = bone.data;
    //     x = bone.x;
    //     y = bone.y;
    //     rotation = bone.rotation;
    //     scaleX = bone.scaleX;
    //     scaleY = bone.scaleY;
    //     shearX = bone.shearX;
    //     shearY = bone.shearY;
    // }

    public void update() {
        updateWorldTransform(x, y, rotation, scaleX, scaleY, shearX, shearY);
    }

    public void updateWorldTransform() {
        updateWorldTransform(x, y, rotation, scaleX, scaleY, shearX, shearY);
    }

    public void updateWorldTransform(float x, float y, float rotation, float scaleX, float scaleY, float shearX, float shearY) {
        ax = x;
        ay = y;
        arotation = rotation;
        ascaleX = scaleX;
        ascaleY = scaleY;
        ashearX = shearX;
        ashearY = shearY;
        appliedValid = true;

        Bone parent = this.parent;
        float rotationY = rotation + 90 + shearY;
        float la = cosDeg(rotation + shearX) * scaleX;
        float lb = cosDeg(rotationY) * scaleY;
        float lc = sinDeg(rotation + shearX) * scaleX;
        float ld = sinDeg(rotationY) * scaleY;

        if (RuntimesLoader.spineVersion > 36) {
            if (parent == null) {
                Skeleton skeleton = this.skeleton;
                float sx = skeleton.scaleX, sy = skeleton.scaleY;
                a = cosDeg(rotation + shearX) * scaleX * sx;
                if (RuntimesLoader.spineVersion > 37) {
                    b = cosDeg(rotationY) * scaleY * sx;
                    c = sinDeg(rotation + shearX) * scaleX * sy;
                } else {
                    b = cosDeg(rotationY) * scaleY * sy;
                    c = sinDeg(rotation + shearX) * scaleX * sx;
                }
                d = sinDeg(rotationY) * scaleY * sy;
                worldX = x * sx + skeleton.x;
                worldY = y * sy + skeleton.y;
                return;
            }
        } else {
            if (parent == null) {
                Skeleton skeleton = this.skeleton;
                if (skeleton.flipX) {
                    x = -x;
                    la = -la;
                    lb = -lb;
                }
                if (skeleton.flipY) {
                    y = -y;
                    lc = -lc;
                    ld = -ld;
                }
                a = la;
                b = lb;
                c = lc;
                d = ld;

                if (RuntimesLoader.spineVersion > 34) {
                    worldX = x + skeleton.x;
                    worldY = y + skeleton.y;
                } else {
                    worldX = x;
                    worldY = y;
                    worldSignX = Math.signum(scaleX);
                    worldSignY = Math.signum(scaleY);
                }
                return;
            }
        }

        float pa = parent.a, pb = parent.b, pc = parent.c, pd = parent.d;
        worldX = pa * x + pb * y + parent.worldX;
        worldY = pc * x + pd * y + parent.worldY;

        if (RuntimesLoader.spineVersion > 34) {
            switch (data.transformMode) {
                case normal -> {
                    a = pa * la + pb * lc;
                    b = pa * lb + pb * ld;
                    c = pc * la + pd * lc;
                    d = pc * lb + pd * ld;
                    return;
                }
                case onlyTranslation -> {
                    a = cosDeg(rotation + shearX) * scaleX;
                    b = cosDeg(rotationY) * scaleY;
                    c = sinDeg(rotation + shearX) * scaleX;
                    d = sinDeg(rotationY) * scaleY;
                }
                case noRotationOrReflection -> {
                    float s = pa * pa + pc * pc, prx;
                    if (s > 0.0001f) {
                        s = Math.abs(pa * pd - pb * pc) / s;
                        if (RuntimesLoader.spineVersion == 38) {
                            pa /= skeleton.scaleX;
                            pc /= skeleton.scaleY;
                        }
                        pb = pc * s;
                        pd = pa * s;
                        prx = atan2(pc, pa) * radDeg;
                    } else {
                        pa = 0;
                        pc = 0;
                        prx = 90 - atan2(pd, pb) * radDeg;
                    }
                    float rx = rotation + shearX - prx;
                    float ry = rotation + shearY - prx + 90;
                    la = cosDeg(rx) * scaleX;
                    lb = cosDeg(ry) * scaleY;
                    lc = sinDeg(rx) * scaleX;
                    ld = sinDeg(ry) * scaleY;
                    a = pa * la - pb * lc;
                    b = pa * lb - pb * ld;
                    c = pc * la + pd * lc;
                    d = pc * lb + pd * ld;
                }
                case noScale, noScaleOrReflection -> {
                    float cos = cosDeg(rotation), sin = sinDeg(rotation);
                    float za, zc;
                    if (RuntimesLoader.spineVersion > 36) {
                        za = (pa * cos + pb * sin) / skeleton.scaleX;
                        zc = (pc * cos + pd * sin) / skeleton.scaleY;
                    } else {
                        za = pa * cos + pb * sin;
                        zc = pc * cos + pd * sin;
                    }
                    float s = (float) Math.sqrt(za * za + zc * zc);
                    if (s > 0.00001f) s = 1 / s;
                    za *= s;
                    zc *= s;
                    s = (float) Math.sqrt(za * za + zc * zc);
                    boolean b1 = pa * pd - pb * pc < 0;

                    if (RuntimesLoader.spineVersion > 36 && data.transformMode == TransformMode.noScale
                            && b1 == (skeleton.scaleX < 0 == skeleton.scaleY < 0)) s = -s;

                    float r = PI / 2 + atan2(zc, za);
                    float zb = cos(r) * s;
                    float zd = sin(r) * s;
                    la = cosDeg(shearX) * scaleX;
                    lb = cosDeg(90 + shearY) * scaleY;
                    lc = sinDeg(shearX) * scaleX;
                    ld = sinDeg(90 + shearY) * scaleY;

                    if (RuntimesLoader.spineVersion == 36 &&
                            (data.transformMode != TransformMode.noScaleOrReflection ? b1 : skeleton.flipX != skeleton.flipY)) {
                        zb = -zb;
                        zd = -zd;
                    }

                    a = za * la + zb * lc;
                    b = za * lb + zb * ld;
                    c = zc * la + zd * lc;
                    d = zc * lb + zd * ld;

                    if (RuntimesLoader.spineVersion == 35 &&
                            (data.transformMode != TransformMode.noScaleOrReflection ? b1 : skeleton.flipX != skeleton.flipY)) {
                        b = -b;
                        d = -d;
                    }
                    return;
                }
            }
        } else {
            worldSignX = parent.worldSignX * Math.signum(scaleX);
            worldSignY = parent.worldSignY * Math.signum(scaleY);

            if (data.inheritRotation && data.inheritScale) {
                a = pa * la + pb * lc;
                b = pa * lb + pb * ld;
                c = pc * la + pd * lc;
                d = pc * lb + pd * ld;
            } else {
                if (data.inheritRotation) {
                    pa = 1;
                    pb = 0;
                    pc = 0;
                    pd = 1;
                    do {
                        float cos = cosDeg(parent.arotation), sin = sinDeg(parent.arotation);
                        float temp = pa * cos + pb * sin;
                        pb = pb * cos - pa * sin;
                        pa = temp;
                        temp = pc * cos + pd * sin;
                        pd = pd * cos - pc * sin;
                        pc = temp;

                        if (!parent.data.inheritRotation) break;
                        parent = parent.parent;
                    } while (parent != null);
                    a = pa * la + pb * lc;
                    b = pa * lb + pb * ld;
                    c = pc * la + pd * lc;
                    d = pc * lb + pd * ld;
                } else if (data.inheritScale) {
                    pa = 1;
                    pb = 0;
                    pc = 0;
                    pd = 1;
                    do {
                        float cos = cosDeg(parent.arotation), sin = sinDeg(parent.arotation);
                        float psx = parent.scaleX, psy = parent.scaleY;
                        float za = cos * psx, zb = sin * psy, zc = sin * psx, zd = cos * psy;
                        float temp = pa * za + pb * zc;
                        pb = pb * zd - pa * zb;
                        pa = temp;
                        temp = pc * za + pd * zc;
                        pd = pd * zd - pc * zb;
                        pc = temp;

                        if (psx >= 0) sin = -sin;
                        temp = pa * cos + pb * sin;
                        pb = pb * cos - pa * sin;
                        pa = temp;
                        temp = pc * cos + pd * sin;
                        pd = pd * cos - pc * sin;
                        pc = temp;

                        if (!parent.data.inheritScale) break;
                        parent = parent.parent;
                    } while (parent != null);
                    a = pa * la + pb * lc;
                    b = pa * lb + pb * ld;
                    c = pc * la + pd * lc;
                    d = pc * lb + pd * ld;
                } else {
                    a = la;
                    b = lb;
                    c = lc;
                    d = ld;
                }
            }
        }

        if (RuntimesLoader.spineVersion > 36) {
            a *= skeleton.scaleX;
            b *= skeleton.scaleX;
            c *= skeleton.scaleY;
            d *= skeleton.scaleY;
        } else {
            if (skeleton.flipX) {
                a = -a;
                b = -b;
            }
            if (skeleton.flipY) {
                c = -c;
                d = -d;
            }
        }
    }

    public void setToSetupPose() {
        BoneData data = this.data;
        x = data.x;
        y = data.y;
        rotation = data.rotation;
        scaleX = data.scaleX;
        scaleY = data.scaleY;
        shearX = data.shearX;
        shearY = data.shearY;
    }

    // public BoneData getData() {
    //     return data;
    // }

    // public Skeleton getSkeleton() {
    //     return skeleton;
    // }

    // public Bone getParent() {
    //     return parent;
    // }

    // public boolean isActive() {
    //     return active;
    // }

    // public float getX() {
    //     return x;
    // }

    // public void setX(float x) {
    //     this.x = x;
    // }

    // public float getY() {
    //     return y;
    // }

    // public void setY(float y) {
    //     this.y = y;
    // }

    // public void setPosition(float x, float y) {
    //     this.x = x;
    //     this.y = y;
    // }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
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

    // public void setScale(float scaleX, float scaleY) {
    //     this.scaleX = scaleX;
    //     this.scaleY = scaleY;
    // }

    // public void setScale(float scale) {
    //     scaleX = scale;
    //     scaleY = scale;
    // }

    // public float getShearX() {
    //     return shearX;
    // }

    // public void setShearX(float shearX) {
    //     this.shearX = shearX;
    // }

    // public float getShearY() {
    //     return shearY;
    // }

    // public void setShearY(float shearY) {
    //     this.shearY = shearY;
    // }

    public void updateAppliedTransform() {
        appliedValid = true;
        Bone parent = this.parent;
        if (parent == null) {
            ax = worldX;
            ay = worldY;
            arotation = atan2(c, a) * radDeg;
            ascaleX = (float) Math.sqrt(a * a + c * c);
            ascaleY = (float) Math.sqrt(b * b + d * d);
            ashearX = 0;
            ashearY = atan2(a * b + c * d, a * d - b * c) * radDeg;
            return;
        }
        float pa = parent.a, pb = parent.b, pc = parent.c, pd = parent.d;
        float pid = 1 / (pa * pd - pb * pc);
        float dx = worldX - parent.worldX, dy = worldY - parent.worldY;
        ax = (dx * pd * pid - dy * pb * pid);
        ay = (dy * pa * pid - dx * pc * pid);
        float ia = pid * pd;
        float id = pid * pa;
        float ib = pid * pb;
        float ic = pid * pc;
        float ra = ia * a - ib * c;
        float rb = ia * b - ib * d;
        float rc = id * c - ic * a;
        float rd = id * d - ic * b;
        ashearX = 0;
        ascaleX = (float) Math.sqrt(ra * ra + rc * rc);
        if (ascaleX > 0.0001f) {
            float det = ra * rd - rb * rc;
            ascaleY = det / ascaleX;
            ashearY = atan2(ra * rb + rc * rd, det) * radDeg;
            arotation = atan2(rc, ra) * radDeg;
        } else {
            ascaleX = 0;
            ascaleY = (float) Math.sqrt(rb * rb + rd * rd);
            ashearY = 0;
            arotation = 90 - atan2(rd, rb) * radDeg;
        }
    }

    public float getA() {
        return a;
    }

    // public void setA(float a) {
    //     this.a = a;
    // }

    public float getB() {
        return b;
    }

    // public void setB(float b) {
    //     this.b = b;
    // }

    public float getC() {
        return c;
    }

    // public void setC(float c) {
    //     this.c = c;
    // }

    public float getD() {
        return d;
    }

    // public void setD(float d) {
    //     this.d = d;
    // }

    public float getWorldX() {
        if (RuntimesLoader.spineVersion < 35)
            return skeleton.x + worldX;
        return worldX;
    }

    // public void setWorldX(float worldX) {
    //     this.worldX = worldX;
    // }

    public float getWorldY() {
        if (RuntimesLoader.spineVersion < 35)
            return skeleton.y + worldY;
        return worldY;
    }

    // public void setWorldY(float worldY) {
    //     this.worldY = worldY;
    // }

    public float getWorldRotationX() {
        return atan2(c, a) * radDeg;
    }

    public Vector2 localToWorld(Vector2 local) {
        if (local == null) throw new IllegalArgumentException("local cannot be null.");
        float x = local.x, y = local.y;
        local.x = x * a + y * b + worldX;
        local.y = x * c + y * d + worldY;
        return local;
    }

    public String toString() {
        return data.name;
    }
}
