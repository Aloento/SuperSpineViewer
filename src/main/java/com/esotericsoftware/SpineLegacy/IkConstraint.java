package com.esotericsoftware.SpineLegacy;

import to.aloen.ssv.Loader;
import com.badlogic.gdx.utils.Array;

import static com.badlogic.gdx.math.MathUtils.*;

public class IkConstraint implements Updatable {
    final IkConstraintData data;
    final Array<Bone> bones;
    Bone target;
    float mix;
    int bendDirection;

    public IkConstraint(IkConstraintData data, Skeleton skeleton) {
        this.data = data;
        mix = data.mix;
        bendDirection = data.bendDirection;
        bones = new Array<>(data.bones.size);
        if (skeleton != null) {
            for (BoneData boneData : data.bones)
                bones.add(skeleton.findBone(boneData.name));
            target = skeleton.findBone(data.target.name);
        }
    }

    // public IkConstraint(IkConstraint ikConstraint, Skeleton skeleton) {
    //     data = ikConstraint.data;
    //     bones = new Array<>(ikConstraint.bones.size);
    //     for (Bone bone : ikConstraint.bones)
    //         bones.add(skeleton.bones.get(bone.skeleton.bones.indexOf(bone, true)));
    //     target = skeleton.bones.get(ikConstraint.target.skeleton.bones.indexOf(ikConstraint.target, true));
    //     mix = ikConstraint.mix;
    //     bendDirection = ikConstraint.bendDirection;
    // }

    static public void apply(Bone bone, float targetX, float targetY, float alpha) {
        if (Loader.spineVersion == 32) {
            Bone pp = bone.parent;
            float id = 1 / (pp.a * pp.d - pp.b * pp.c);
            float x = targetX - pp.worldX, y = targetY - pp.worldY;
            float tx = (x * pp.d - y * pp.b) * id - bone.x, ty = (y * pp.a - x * pp.c) * id - bone.y;
            float rotationIK = atan2(ty, tx) * radDeg - bone.shearX;
            if (bone.scaleX < 0) rotationIK += 180;
            if (rotationIK > 180)
                rotationIK -= 360;
            else if (rotationIK < -180) rotationIK += 360;
            bone.updateWorldTransform(bone.x, bone.y, bone.rotation + (rotationIK - bone.rotation) * alpha, bone.appliedScaleX,
                    bone.appliedScaleY, bone.shearX, bone.shearY);
        } else {
            float parentRotation = bone.parent == null ? 0 : bone.parent.getWorldRotationX();
            float rotation = bone.rotation;
            float rotationIK = atan2(targetY - bone.worldY, targetX - bone.worldX) * radDeg - parentRotation;
            if ((bone.worldSignX != bone.worldSignY) == (bone.skeleton.flipX == bone.skeleton.flipY))
                rotationIK = 360 - rotationIK;
            if (rotationIK > 180)
                rotationIK -= 360;
            else if (rotationIK < -180) rotationIK += 360;
            bone.updateWorldTransform(bone.x, bone.y, rotation + (rotationIK - rotation) * alpha, bone.appliedScaleX,
                    bone.appliedScaleY, 0, 0);
        }
    }

    static public void apply(Bone parent, Bone child, float targetX, float targetY, int bendDir, float alpha) {
        if (alpha == 0) return;
        float px = parent.x, py = parent.y, psx = parent.appliedScaleX, psy = parent.appliedScaleY;
        int os1, os2, s2;
        if (psx < 0) {
            psx = -psx;
            os1 = 180;
            s2 = -1;
        } else {
            os1 = 0;
            s2 = 1;
        }
        if (psy < 0) {
            psy = -psy;
            s2 = -s2;
        }
        float cx = child.x, cy = child.y, csx = child.appliedScaleX;
        boolean u = Math.abs(psx - psy) <= 0.0001f;
        if (!u && cy != 0) {
            child.worldX = parent.a * cx + parent.worldX;
            child.worldY = parent.c * cx + parent.worldY;
            cy = 0;
        }
        if (csx < 0) {
            csx = -csx;
            os2 = 180;
        } else
            os2 = 0;
        Bone pp = parent.parent;
        float ppa = pp.a, ppb = pp.b, ppc = pp.c, ppd = pp.d, id = 1 / (ppa * ppd - ppb * ppc);
        float x = targetX - pp.worldX, y = targetY - pp.worldY;
        float tx = (x * ppd - y * ppb) * id - px, ty = (y * ppa - x * ppc) * id - py;
        x = child.worldX - pp.worldX;
        y = child.worldY - pp.worldY;
        float dx = (x * ppd - y * ppb) * id - px, dy = (y * ppa - x * ppc) * id - py;
        float l1 = (float) Math.sqrt(dx * dx + dy * dy), l2 = child.data.length * csx, a1, a2;
        outer:
        if (u) {
            l2 *= psx;
            float cos = (tx * tx + ty * ty - l1 * l1 - l2 * l2) / (2 * l1 * l2);
            if (cos < -1)
                cos = -1;
            else if (cos > 1) cos = 1;
            a2 = (float) Math.acos(cos) * bendDir;
            float a = l1 + l2 * cos, o = l2 * sin(a2);
            a1 = atan2(ty * a - tx * o, tx * a + ty * o);
        } else {
            float a = psx * l2, b = psy * l2, ta = atan2(ty, tx);
            float aa = a * a, bb = b * b, ll = l1 * l1, dd = tx * tx + ty * ty;
            float c0 = bb * ll + aa * dd - aa * bb, c1 = -2 * bb * l1, c2 = bb - aa;
            float d = c1 * c1 - 4 * c2 * c0;
            if (d >= 0) {
                float q = (float) Math.sqrt(d);
                if (c1 < 0) q = -q;
                q = -(c1 + q) / 2;
                float r0 = q / c2, r1 = c0 / q;
                float r = Math.abs(r0) < Math.abs(r1) ? r0 : r1;
                if (r * r <= dd) {
                    y = (float) Math.sqrt(dd - r * r) * bendDir;
                    a1 = ta - atan2(y, r);
                    a2 = atan2(y / psy, (r - l1) / psx);
                    break outer;
                }
            }
            float minAngle = 0, minDist = Float.MAX_VALUE, minX = 0, minY = 0;
            float maxAngle = 0, maxDist = 0, maxX = 0, maxY = 0;
            x = l1 + a;
            d = x * x;
            if (d > maxDist) {
                maxAngle = 0;
                maxDist = d;
                maxX = x;
            }
            x = l1 - a;
            d = x * x;
            if (d < minDist) {
                minAngle = PI;
                minDist = d;
                minX = x;
            }
            float angle = (float) Math.acos(-a * l1 / (aa - bb));
            x = a * cos(angle) + l1;
            y = b * sin(angle);
            d = x * x + y * y;
            if (d < minDist) {
                minAngle = angle;
                minDist = d;
                minX = x;
                minY = y;
            }
            if (d > maxDist) {
                maxAngle = angle;
                maxDist = d;
                maxX = x;
                maxY = y;
            }
            if (dd <= (minDist + maxDist) / 2) {
                a1 = ta - atan2(minY * bendDir, minX);
                a2 = minAngle * bendDir;
            } else {
                a1 = ta - atan2(maxY * bendDir, maxX);
                a2 = maxAngle * bendDir;
            }
        }
        float os = atan2(cy, cx) * s2;
        a1 = (a1 - os) * radDeg + os1;
        a2 = ((a2 + os) * radDeg - child.shearX) * s2 + os2;
        if (a1 > 180)
            a1 -= 360;
        else if (a1 < -180) a1 += 360;
        if (a2 > 180)
            a2 -= 360;
        else if (a2 < -180) a2 += 360;
        float rotation = parent.rotation;
        parent.updateWorldTransform(px, py, rotation + (a1 - rotation) * alpha, parent.appliedScaleX, parent.appliedScaleY, 0, 0);
        rotation = child.rotation;
        child.updateWorldTransform(cx, cy, rotation + (a2 - rotation) * alpha, child.appliedScaleX, child.appliedScaleY,
                child.shearX, child.shearY);
    }

    // public void apply() {
    //     update();
    // }

    public void update() {
        Bone target = this.target;
        Array<Bone> bones = this.bones;
        switch (bones.size) {
            case 1 -> apply(bones.first(), target.worldX, target.worldY, mix);
            case 2 -> apply(bones.first(), bones.get(1), target.worldX, target.worldY, bendDirection, mix);
        }
    }

    // public Array<Bone> getBones() {
    //     return bones;
    // }

    // public Bone getTarget() {
    //     return target;
    // }

    // public void setTarget(Bone target) {
    //     this.target = target;
    // }

    // public float getMix() {
    //     return mix;
    // }

    // public void setMix(float mix) {
    //     this.mix = mix;
    // }

    // public int getBendDirection() {
    //     return bendDirection;
    // }

    // public void setBendDirection(int bendDirection) {
    //     this.bendDirection = bendDirection;
    // }

    // public IkConstraintData getData() {
    //     return data;
    // }

    public String toString() {
        return data.name;
    }
}
