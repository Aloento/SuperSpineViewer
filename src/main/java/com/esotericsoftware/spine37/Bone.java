package com.esotericsoftware.spine37;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine37.BoneData.TransformMode;

import static com.badlogic.gdx.math.Matrix3.*;
import static com.esotericsoftware.spine37.utils.SpineUtils.*;


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

    boolean sorted;

    
    public Bone(BoneData data, Skeleton skeleton, Bone parent) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        this.data = data;
        this.skeleton = skeleton;
        this.parent = parent;
        setToSetupPose();
    }

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
        if (parent == null) {
            Skeleton skeleton = this.skeleton;
            float rotationY = rotation + 90 + shearY, sx = skeleton.scaleX, sy = skeleton.scaleY;
            a = cosDeg(rotation + shearX) * scaleX * sx;
            b = cosDeg(rotationY) * scaleY * sy;
            c = sinDeg(rotation + shearX) * scaleX * sx;
            d = sinDeg(rotationY) * scaleY * sy;
            worldX = x * sx + skeleton.x;
            worldY = y * sy + skeleton.y;
            return;
        }

        float pa = parent.a, pb = parent.b, pc = parent.c, pd = parent.d;
        worldX = pa * x + pb * y + parent.worldX;
        worldY = pc * x + pd * y + parent.worldY;

        switch (data.transformMode) {
            case normal -> {
                float rotationY = rotation + 90 + shearY;
                float la = cosDeg(rotation + shearX) * scaleX;
                float lb = cosDeg(rotationY) * scaleY;
                float lc = sinDeg(rotation + shearX) * scaleX;
                float ld = sinDeg(rotationY) * scaleY;
                a = pa * la + pb * lc;
                b = pa * lb + pb * ld;
                c = pc * la + pd * lc;
                d = pc * lb + pd * ld;
                return;
            }
            case onlyTranslation -> {
                float rotationY = rotation + 90 + shearY;
                a = cosDeg(rotation + shearX) * scaleX;
                b = cosDeg(rotationY) * scaleY;
                c = sinDeg(rotation + shearX) * scaleX;
                d = sinDeg(rotationY) * scaleY;
            }
            case noRotationOrReflection -> {
                float s = pa * pa + pc * pc, prx;
                if (s > 0.0001f) {
                    s = Math.abs(pa * pd - pb * pc) / s;
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
                float la = cosDeg(rx) * scaleX;
                float lb = cosDeg(ry) * scaleY;
                float lc = sinDeg(rx) * scaleX;
                float ld = sinDeg(ry) * scaleY;
                a = pa * la - pb * lc;
                b = pa * lb - pb * ld;
                c = pc * la + pd * lc;
                d = pc * lb + pd * ld;
            }
            case noScale, noScaleOrReflection -> {
                float cos = cosDeg(rotation), sin = sinDeg(rotation);
                float za = (pa * cos + pb * sin) / skeleton.scaleX;
                float zc = (pc * cos + pd * sin) / skeleton.scaleY;
                float s = (float) Math.sqrt(za * za + zc * zc);
                if (s > 0.00001f) s = 1 / s;
                za *= s;
                zc *= s;
                s = (float) Math.sqrt(za * za + zc * zc);
                if (data.transformMode == TransformMode.noScale
                        && (pa * pd - pb * pc < 0) == (skeleton.scaleX < 0 == skeleton.scaleY < 0)) s = -s;
                float r = PI / 2 + atan2(zc, za);
                float zb = cos(r) * s;
                float zd = sin(r) * s;
                float la = cosDeg(shearX) * scaleX;
                float lb = cosDeg(90 + shearY) * scaleY;
                float lc = sinDeg(shearX) * scaleX;
                float ld = sinDeg(90 + shearY) * scaleY;
                a = za * la + zb * lc;
                b = za * lb + zb * ld;
                c = zc * la + zd * lc;
                d = zc * lb + zd * ld;
            }
        }
        a *= skeleton.scaleX;
        b *= skeleton.scaleX;
        c *= skeleton.scaleY;
        d *= skeleton.scaleY;
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
    
    public float getAX() {
        return ax;
    }

    public void setAX(float ax) {
        this.ax = ax;
    }

    public float getAY() {
        return ay;
    }

    public void setAY(float ay) {
        this.ay = ay;
    }

    public float getARotation() {
        return arotation;
    }

    public void setARotation(float arotation) {
        this.arotation = arotation;
    }

    public float getAScaleX() {
        return ascaleX;
    }

    public void setAScaleX(float ascaleX) {
        this.ascaleX = ascaleX;
    }

    public float getAScaleY() {
        return ascaleY;
    }

    public void setAScaleY(float ascaleY) {
        this.ascaleY = ascaleY;
    }

    public float getAShearX() {
        return ashearX;
    }

    public void setAShearX(float ashearX) {
        this.ashearX = ashearX;
    }

    public float getAShearY() {
        return ashearY;
    }

    public void setAShearY(float ashearY) {
        this.ashearY = ashearY;
    }

    public boolean isAppliedValid() {
        return appliedValid;
    }

    public void setAppliedValid(boolean appliedValid) {
        this.appliedValid = appliedValid;
    }

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

    public void setA(float a) {
        this.a = a;
    }

    public float getB() {
        return b;
    }

    public void setB(float b) {
        this.b = b;
    }

    public float getC() {
        return c;
    }

    public void setC(float c) {
        this.c = c;
    }

    public float getD() {
        return d;
    }

    public void setD(float d) {
        this.d = d;
    }

    public float getWorldX() {
        return worldX;
    }

    public void setWorldX(float worldX) {
        this.worldX = worldX;
    }

    public float getWorldY() {
        return worldY;
    }

    public void setWorldY(float worldY) {
        this.worldY = worldY;
    }

    public float getWorldRotationX() {
        return atan2(c, a) * radDeg;
    }

    public float getWorldRotationY() {
        return atan2(d, b) * radDeg;
    }

    
    public float getWorldScaleX() {
        return (float) Math.sqrt(a * a + c * c);
    }

    public float getWorldScaleY() {
        return (float) Math.sqrt(b * b + d * d);
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
        float invDet = 1 / (a * d - b * c);
        float x = world.x - worldX, y = world.y - worldY;
        world.x = x * d * invDet - y * b * invDet;
        world.y = y * a * invDet - x * c * invDet;
        return world;
    }

    
    public Vector2 localToWorld(Vector2 local) {
        float x = local.x, y = local.y;
        local.x = x * a + y * b + worldX;
        local.y = x * c + y * d + worldY;
        return local;
    }

    
    public float worldToLocalRotation(float worldRotation) {
        float sin = sinDeg(worldRotation), cos = cosDeg(worldRotation);
        return atan2(a * sin - c * cos, d * cos - b * sin) * radDeg + rotation - shearX;
    }

    
    public float localToWorldRotation(float localRotation) {
        localRotation -= rotation - shearX;
        float sin = sinDeg(localRotation), cos = cosDeg(localRotation);
        return atan2(cos * c + sin * d, cos * a + sin * b) * radDeg;
    }

    
    public void rotateWorld(float degrees) {
        float cos = cosDeg(degrees), sin = sinDeg(degrees);
        a = cos * a - sin * c;
        b = cos * b - sin * d;
        c = sin * a + cos * c;
        d = sin * b + cos * d;
        appliedValid = false;
    }



    public String toString() {
        return data.name;
    }
}
