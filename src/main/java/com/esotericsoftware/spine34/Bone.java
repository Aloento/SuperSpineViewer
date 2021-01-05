package com.esotericsoftware.spine34;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import static com.badlogic.gdx.math.MathUtils.*;
import static com.badlogic.gdx.math.Matrix3.*;

public class Bone implements Updatable {
    final BoneData data;
    final Skeleton skeleton;
    final Bone parent;
    final Array<Bone> children = new Array();
    float x, y, rotation, scaleX, scaleY, shearX, shearY;
    float appliedRotation;

    float a, b, worldX;
    float c, d, worldY;
    float worldSignX, worldSignY;

    boolean sorted;

    /**
     * @param parent May be null.
     */
    public Bone(BoneData data, Skeleton skeleton, Bone parent) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        this.data = data;
        this.skeleton = skeleton;
        this.parent = parent;
        setToSetupPose();
    }

    /**
     * Copy constructor. Does not copy the children bones.
     *
     * @param parent May be null.
     */
    public Bone(Bone bone, Skeleton skeleton, Bone parent) {
        if (bone == null) throw new IllegalArgumentException("bone cannot be null.");
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        this.skeleton = skeleton;
        this.parent = parent;
        data = bone.data;
        x = bone.x;
        y = bone.y;
        rotation = bone.rotation;
        scaleX = bone.scaleX;
        scaleY = bone.scaleY;
        shearX = bone.shearX;
        shearY = bone.shearY;
    }

    /**
     * Same as {@link #updateWorldTransform()}. This method exists for Bone to implement {@link Updatable}.
     */
    public void update() {
        updateWorldTransform(x, y, rotation, scaleX, scaleY, shearX, shearY);
    }

    /**
     * Computes the world transform using the parent bone and this bone's local transform.
     */
    public void updateWorldTransform() {
        updateWorldTransform(x, y, rotation, scaleX, scaleY, shearX, shearY);
    }

    /**
     * Computes the world transform using the parent bone and the specified local transform.
     */
    public void updateWorldTransform(float x, float y, float rotation, float scaleX, float scaleY, float shearX, float shearY) {
        appliedRotation = rotation;

        float rotationY = rotation + 90 + shearY;
        float la = cosDeg(rotation + shearX) * scaleX, lb = cosDeg(rotationY) * scaleY;
        float lc = sinDeg(rotation + shearX) * scaleX, ld = sinDeg(rotationY) * scaleY;

        Bone parent = this.parent;
        if (parent == null) { // Root bone.
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
            worldX = x;
            worldY = y;
            worldSignX = Math.signum(scaleX);
            worldSignY = Math.signum(scaleY);
            return;
        }

        float pa = parent.a, pb = parent.b, pc = parent.c, pd = parent.d;
        worldX = pa * x + pb * y + parent.worldX;
        worldY = pc * x + pd * y + parent.worldY;
        worldSignX = parent.worldSignX * Math.signum(scaleX);
        worldSignY = parent.worldSignY * Math.signum(scaleY);

        if (data.inheritRotation && data.inheritScale) {
            a = pa * la + pb * lc;
            b = pa * lb + pb * ld;
            c = pc * la + pd * lc;
            d = pc * lb + pd * ld;
        } else {
            if (data.inheritRotation) { // No scale inheritance.
                pa = 1;
                pb = 0;
                pc = 0;
                pd = 1;
                do {
                    float cos = cosDeg(parent.appliedRotation), sin = sinDeg(parent.appliedRotation);
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
            } else if (data.inheritScale) { // No rotation inheritance.
                pa = 1;
                pb = 0;
                pc = 0;
                pd = 1;
                do {
                    float cos = cosDeg(parent.appliedRotation), sin = sinDeg(parent.appliedRotation);
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

    public BoneData getData() {
        return data;
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public Bone getParent() {
        return parent;
    }

    public Array<Bone> getChildren() {
        return children;
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

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

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

    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public void setScale(float scale) {
        scaleX = scale;
        scaleY = scale;
    }

    public float getShearX() {
        return shearX;
    }

    public void setShearX(float shearX) {
        this.shearX = shearX;
    }

    public float getShearY() {
        return shearY;
    }

    public void setShearY(float shearY) {
        this.shearY = shearY;
    }

    public float getA() {
        return a;
    }

    public float getB() {
        return b;
    }

    public float getC() {
        return c;
    }

    public float getD() {
        return d;
    }

    public float getWorldX() {
        return worldX;
    }

    public float getWorldY() {
        return worldY;
    }

    public float getWorldSignX() {
        return worldSignX;
    }

    public float getWorldSignY() {
        return worldSignY;
    }

    public float getWorldRotationX() {
        return atan2(c, a) * radDeg;
    }

    public float getWorldRotationY() {
        return atan2(d, b) * radDeg;
    }

    public float getWorldScaleX() {
        return (float) Math.sqrt(a * a + b * b) * worldSignX;
    }

    public float getWorldScaleY() {
        return (float) Math.sqrt(c * c + d * d) * worldSignY;
    }

    public float worldToLocalRotationX() {
        Bone parent = this.parent;
        if (parent == null) return rotation;
        float pa = parent.a, pb = parent.b, pc = parent.c, pd = parent.d, a = this.a, c = this.c;
        return atan2(pa * c - pc * a, pd * a - pb * c) * radDeg;
    }

    public float worldToLocalRotationY() {
        Bone parent = this.parent;
        if (parent == null) return rotation;
        float pa = parent.a, pb = parent.b, pc = parent.c, pd = parent.d, b = this.b, d = this.d;
        return atan2(pa * d - pc * b, pd * b - pb * d) * radDeg;
    }

    public void rotateWorld(float degrees) {
        float a = this.a, b = this.b, c = this.c, d = this.d;
        float cos = cosDeg(degrees), sin = sinDeg(degrees);
        this.a = cos * a - sin * c;
        this.b = cos * b - sin * d;
        this.c = sin * a + cos * c;
        this.d = sin * b + cos * d;
    }

    /**
     * Computes the local transform from the world transform. This can be useful to perform processing on the local transform
     * after the world transform has been modified directly (eg, by a constraint).
     * <p>
     * Some redundant information is lost by the world transform, such as -1,-1 scale versus 180 rotation. The computed local
     * transform values may differ from the original values but are functionally the same.
     */
    public void updateLocalTransform() {
        Bone parent = this.parent;
        if (parent == null) {
            x = worldX;
            y = worldY;
            rotation = atan2(c, a) * radDeg;
            scaleX = (float) Math.sqrt(a * a + c * c);
            scaleY = (float) Math.sqrt(b * b + d * d);
            float det = a * d - b * c;
            shearX = 0;
            shearY = atan2(a * b + c * d, det) * radDeg;
            return;
        }
        float pa = parent.a, pb = parent.b, pc = parent.c, pd = parent.d;
        float pid = 1 / (pa * pd - pb * pc);
        float dx = worldX - parent.worldX, dy = worldY - parent.worldY;
        x = (dx * pd * pid - dy * pb * pid);
        y = (dy * pa * pid - dx * pc * pid);
        float ia = pid * pd;
        float id = pid * pa;
        float ib = pid * pb;
        float ic = pid * pc;
        float ra = ia * a - ib * c;
        float rb = ia * b - ib * d;
        float rc = id * c - ic * a;
        float rd = id * d - ic * b;
        shearX = 0;
        scaleX = (float) Math.sqrt(ra * ra + rc * rc);
        if (scaleX > 0.0001f) {
            float det = ra * rd - rb * rc;
            scaleY = det / scaleX;
            shearY = atan2(ra * rb + rc * rd, det) * radDeg;
            rotation = atan2(rc, ra) * radDeg;
        } else {
            scaleX = 0;
            scaleY = (float) Math.sqrt(rb * rb + rd * rd);
            shearY = 0;
            rotation = 90 - atan2(rd, rb) * radDeg;
        }
        appliedRotation = rotation;
    }

    public Matrix3 getWorldTransform(Matrix3 worldTransform) {
        if (worldTransform == null) throw new IllegalArgumentException("worldTransform cannot be null.");
        float[] val = worldTransform.val;
        val[M00] = a;
        val[M01] = b;
        val[M10] = c;
        val[M11] = d;
        val[M02] = worldX;
        val[M12] = worldY;
        val[M20] = 0;
        val[M21] = 0;
        val[M22] = 1;
        return worldTransform;
    }

    public Vector2 worldToLocal(Vector2 world) {
        float a = this.a, b = this.b, c = this.c, d = this.d;
        float invDet = 1 / (a * d - b * c);
        float x = world.x - worldX, y = world.y - worldY;
        world.x = (x * d * invDet - y * b * invDet);
        world.y = (y * a * invDet - x * c * invDet);
        return world;
    }

    public Vector2 localToWorld(Vector2 local) {
        float x = local.x, y = local.y;
        local.x = x * a + y * b + worldX;
        local.y = x * c + y * d + worldY;
        return local;
    }

    public String toString() {
        return data.name;
    }
}
