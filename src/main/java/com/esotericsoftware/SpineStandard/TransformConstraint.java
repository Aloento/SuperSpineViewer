package com.esotericsoftware.SpineStandard;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import static com.esotericsoftware.SpineStandard.utils.SpineUtils.*;

public class TransformConstraint {
    final TransformConstraintData data;
    final Array<Bone> bones;
    final Vector2 temp = new Vector2();
    Bone target;
    float rotateMix, translateMix, scaleMix, shearMix;
    boolean active; // Spine38

    public TransformConstraint(TransformConstraintData data, Skeleton skeleton) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        this.data = data;
        rotateMix = data.rotateMix;
        translateMix = data.translateMix;
        scaleMix = data.scaleMix;
        shearMix = data.shearMix;
        bones = new Array<>(data.bones.size);
        for (BoneData boneData : data.bones)
            bones.add(skeleton.findBone(boneData.name));
        target = skeleton.findBone(data.target.name);
    }

    // public TransformConstraint(TransformConstraint constraint, Skeleton skeleton) {
    //     if (constraint == null) throw new IllegalArgumentException("constraint cannot be null.");
    //     if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
    //     data = constraint.data;
    //     bones = new Array<>(constraint.bones.size);
    //     for (Bone bone : constraint.bones)
    //         bones.add(skeleton.bones.get(bone.data.index));
    //     target = skeleton.bones.get(constraint.target.data.index);
    //     rotateMix = constraint.rotateMix;
    //     translateMix = constraint.translateMix;
    //     scaleMix = constraint.scaleMix;
    //     shearMix = constraint.shearMix;
    // }

    // public void apply() {
    //     update();
    // }

    public void update() {
        if (data.local) {
            if (data.relative)
                applyRelativeLocal();
            else
                applyAbsoluteLocal();
        } else {
            if (data.relative)
                applyRelativeWorld();
            else
                applyAbsoluteWorld();
        }
    }

    private void applyAbsoluteWorld() {
        float rotateMix = this.rotateMix, translateMix = this.translateMix, scaleMix = this.scaleMix, shearMix = this.shearMix;
        Bone target = this.target;
        float ta = target.a, tb = target.b, tc = target.c, td = target.d;
        float degRadReflect = ta * td - tb * tc > 0 ? degRad : -degRad;
        float offsetRotation = data.offsetRotation * degRadReflect, offsetShearY = data.offsetShearY * degRadReflect;
        Array<Bone> bones = this.bones;
        for (int i = 0, n = bones.size; i < n; i++) {
            Bone bone = bones.get(i);
            boolean modified = false;
            if (rotateMix != 0) {
                float a = bone.a, b = bone.b, c = bone.c, d = bone.d;
                float r = atan2(tc, ta) - atan2(c, a) + offsetRotation;
                if (r > PI)
                    r -= PI2;
                else if (r < -PI) r += PI2;
                r *= rotateMix;
                float cos = cos(r), sin = sin(r);
                bone.a = cos * a - sin * c;
                bone.b = cos * b - sin * d;
                bone.c = sin * a + cos * c;
                bone.d = sin * b + cos * d;
                modified = true;
            }
            if (translateMix != 0) {
                Vector2 temp = this.temp;
                target.localToWorld(temp.set(data.offsetX, data.offsetY));
                bone.worldX += (temp.x - bone.worldX) * translateMix;
                bone.worldY += (temp.y - bone.worldY) * translateMix;
                modified = true;
            }
            if (scaleMix > 0) {
                float s = (float) Math.sqrt(bone.a * bone.a + bone.c * bone.c);
                if (s != 0) s = (s + ((float) Math.sqrt(ta * ta + tc * tc) - s + data.offsetScaleX) * scaleMix) / s;
                bone.a *= s;
                bone.c *= s;
                s = (float) Math.sqrt(bone.b * bone.b + bone.d * bone.d);
                if (s != 0) s = (s + ((float) Math.sqrt(tb * tb + td * td) - s + data.offsetScaleY) * scaleMix) / s;
                bone.b *= s;
                bone.d *= s;
                modified = true;
            }
            if (shearMix > 0) {
                float b = bone.b, d = bone.d;
                float by = atan2(d, b);
                float r = atan2(td, tb) - atan2(tc, ta) - (by - atan2(bone.c, bone.a));
                if (r > PI)
                    r -= PI2;
                else if (r < -PI) r += PI2;
                r = by + (r + offsetShearY) * shearMix;
                float s = (float) Math.sqrt(b * b + d * d);
                bone.b = cos(r) * s;
                bone.d = sin(r) * s;
                modified = true;
            }
            if (modified) bone.appliedValid = false;
        }
    }

    private void applyRelativeWorld() {
        float rotateMix = this.rotateMix, translateMix = this.translateMix, scaleMix = this.scaleMix, shearMix = this.shearMix;
        Bone target = this.target;
        float ta = target.a, tb = target.b, tc = target.c, td = target.d;
        float degRadReflect = ta * td - tb * tc > 0 ? degRad : -degRad;
        float offsetRotation = data.offsetRotation * degRadReflect, offsetShearY = data.offsetShearY * degRadReflect;
        Array<Bone> bones = this.bones;
        for (int i = 0, n = bones.size; i < n; i++) {
            Bone bone = bones.get(i);
            boolean modified = false;
            if (rotateMix != 0) {
                float a = bone.a, b = bone.b, c = bone.c, d = bone.d;
                float r = atan2(tc, ta) + offsetRotation;
                if (r > PI)
                    r -= PI2;
                else if (r < -PI) r += PI2;
                r *= rotateMix;
                float cos = cos(r), sin = sin(r);
                bone.a = cos * a - sin * c;
                bone.b = cos * b - sin * d;
                bone.c = sin * a + cos * c;
                bone.d = sin * b + cos * d;
                modified = true;
            }
            if (translateMix != 0) {
                Vector2 temp = this.temp;
                target.localToWorld(temp.set(data.offsetX, data.offsetY));
                bone.worldX += temp.x * translateMix;
                bone.worldY += temp.y * translateMix;
                modified = true;
            }
            if (scaleMix > 0) {
                float s = ((float) Math.sqrt(ta * ta + tc * tc) - 1 + data.offsetScaleX) * scaleMix + 1;
                bone.a *= s;
                bone.c *= s;
                s = ((float) Math.sqrt(tb * tb + td * td) - 1 + data.offsetScaleY) * scaleMix + 1;
                bone.b *= s;
                bone.d *= s;
                modified = true;
            }
            if (shearMix > 0) {
                float r = atan2(td, tb) - atan2(tc, ta);
                if (r > PI)
                    r -= PI2;
                else if (r < -PI) r += PI2;
                float b = bone.b, d = bone.d;
                r = atan2(d, b) + (r - PI / 2 + offsetShearY) * shearMix;
                float s = (float) Math.sqrt(b * b + d * d);
                bone.b = cos(r) * s;
                bone.d = sin(r) * s;
                modified = true;
            }
            if (modified) bone.appliedValid = false;
        }
    }

    private void applyAbsoluteLocal() {
        float rotateMix = this.rotateMix, translateMix = this.translateMix, scaleMix = this.scaleMix, shearMix = this.shearMix;
        Bone target = this.target;
        if (!target.appliedValid) target.updateAppliedTransform();
        Array<Bone> bones = this.bones;
        for (int i = 0, n = bones.size; i < n; i++) {
            Bone bone = bones.get(i);
            if (!bone.appliedValid) bone.updateAppliedTransform();
            float rotation = bone.arotation;
            if (rotateMix != 0) {
                float r = target.arotation - rotation + data.offsetRotation;
                r -= (16384 - (int) (16384.499999999996 - r / 360)) * 360;
                rotation += r * rotateMix;
            }
            float x = bone.ax, y = bone.ay;
            if (translateMix != 0) {
                x += (target.ax - x + data.offsetX) * translateMix;
                y += (target.ay - y + data.offsetY) * translateMix;
            }
            float scaleX = bone.ascaleX, scaleY = bone.ascaleY;
            if (scaleMix != 0) {
                if (scaleX != 0) scaleX = (scaleX + (target.ascaleX - scaleX + data.offsetScaleX) * scaleMix) / scaleX;
                if (scaleY != 0) scaleY = (scaleY + (target.ascaleY - scaleY + data.offsetScaleY) * scaleMix) / scaleY;
            }
            float shearY = bone.ashearY;
            if (shearMix != 0) {
                float r = target.ashearY - shearY + data.offsetShearY;
                r -= (16384 - (int) (16384.499999999996 - r / 360)) * 360;
                shearY += r * shearMix;
            }
            bone.updateWorldTransform(x, y, rotation, scaleX, scaleY, bone.ashearX, shearY);
        }
    }

    private void applyRelativeLocal() {
        float rotateMix = this.rotateMix, translateMix = this.translateMix, scaleMix = this.scaleMix, shearMix = this.shearMix;
        Bone target = this.target;
        if (!target.appliedValid) target.updateAppliedTransform();
        Array<Bone> bones = this.bones;
        for (int i = 0, n = bones.size; i < n; i++) {
            Bone bone = bones.get(i);
            if (!bone.appliedValid) bone.updateAppliedTransform();
            float rotation = bone.arotation;
            if (rotateMix != 0) rotation += (target.arotation + data.offsetRotation) * rotateMix;
            float x = bone.ax, y = bone.ay;
            if (translateMix != 0) {
                x += (target.ax + data.offsetX) * translateMix;
                y += (target.ay + data.offsetY) * translateMix;
            }
            float scaleX = bone.ascaleX, scaleY = bone.ascaleY;
            if (scaleMix != 0) {
                scaleX *= ((target.ascaleX - 1 + data.offsetScaleX) * scaleMix) + 1;
                scaleY *= ((target.ascaleY - 1 + data.offsetScaleY) * scaleMix) + 1;
            }
            float shearY = bone.ashearY;
            if (shearMix != 0) shearY += (target.ashearY + data.offsetShearY) * shearMix;
            bone.updateWorldTransform(x, y, rotation, scaleX, scaleY, bone.ashearX, shearY);
        }
    }

    // public int getOrder() {
    //     return data.order;
    // }

    // public Array<Bone> getBones() {
    //     return bones;
    // }

    // public Bone getTarget() {
    //     return target;
    // }

    // public void setTarget(Bone target) {
    //     if (target == null) throw new IllegalArgumentException("target cannot be null.");
    //     this.target = target;
    // }

    // public float getRotateMix() {
    //     return rotateMix;
    // }

    // public void setRotateMix(float rotateMix) {
    //     this.rotateMix = rotateMix;
    // }

    // public float getTranslateMix() {
    //     return translateMix;
    // }

    // public void setTranslateMix(float translateMix) {
    //     this.translateMix = translateMix;
    // }

    // public float getScaleMix() {
    //     return scaleMix;
    // }

    // public void setScaleMix(float scaleMix) {
    //     this.scaleMix = scaleMix;
    // }

    // public float getShearMix() {
    //     return shearMix;
    // }

    // public void setShearMix(float shearMix) {
    //     this.shearMix = shearMix;
    // }

    // public boolean isActive() {
    //     return active;
    // }

    // public TransformConstraintData getData() {
    //     return data;
    // }

    public String toString() {
        return data.name;
    }
}
