package com.esotericsoftware.SpineLegacy;

import com.badlogic.gdx.math.Vector2;

import static com.badlogic.gdx.math.MathUtils.*;

public class Bone implements Updatable {
    final BoneData data;
    final Skeleton skeleton;
    final Bone parent;
    float x, y, rotation, scaleX, scaleY, shearX, shearY;
    float appliedRotation, appliedScaleX, appliedScaleY;
    float a, b, worldX;
    float c, d, worldY;
    float worldSignX, worldSignY;

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
        appliedRotation = rotation;
        appliedScaleX = scaleX;
        appliedScaleY = scaleY;
        float rotationY = rotation + 90 + shearY;
        float la = cosDeg(rotation + shearX) * scaleX, lb = cosDeg(rotationY) * scaleY;
        float lc = sinDeg(rotation + shearX) * scaleX, ld = sinDeg(rotationY) * scaleY;
        Bone parent = this.parent;
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
            if (data.inheritRotation) {
                pa = 1;
                pb = 0;
                pc = 0;
                pd = 1;
                do {
                    float cos = cosDeg(parent.appliedRotation), sin = sinDeg(parent.appliedRotation);
                    float temp = pa * cos + pb * sin;
                    pb = pa * -sin + pb * cos;
                    pa = temp;
                    temp = pc * cos + pd * sin;
                    pd = pc * -sin + pd * cos;
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
                    float r = parent.appliedRotation, cos = cosDeg(r), sin = sinDeg(r);
                    float psx = parent.appliedScaleX, psy = parent.appliedScaleY;
                    float za = cos * psx, zb = -sin * psy, zc = sin * psx, zd = cos * psy;
                    float temp = pa * za + pb * zc;
                    pb = pa * zb + pb * zd;
                    pa = temp;
                    temp = pc * za + pd * zc;
                    pd = pc * zb + pd * zd;
                    pc = temp;
                    if (psx < 0) r = -r;
                    cos = cosDeg(-r);
                    sin = sinDeg(-r);
                    temp = pa * cos + pb * sin;
                    pb = pa * -sin + pb * cos;
                    pa = temp;
                    temp = pc * cos + pd * sin;
                    pd = pc * -sin + pd * cos;
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

    public float getWorldRotationX() {
        return atan2(c, a) * radDeg;
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
