package com.esotericsoftware.SpinePreview;

import com.badlogic.gdx.utils.Array;

import static com.esotericsoftware.SpinePreview.utils.SpineUtils.*;

public class IkConstraint implements Updatable {
    final IkConstraintData data;
    final Array<Bone> bones;
    Bone target;
    int bendDirection;
    boolean compress, stretch;
    float mix, softness;
    boolean active;

    public IkConstraint(IkConstraintData data, Skeleton skeleton) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        this.data = data;
        mix = data.mix;
        softness = data.softness;
        bendDirection = data.bendDirection;
        compress = data.compress;
        stretch = data.stretch;
        bones = new Array<>(data.bones.size);
        for (BoneData boneData : data.bones)
            bones.add(skeleton.findBone(boneData.name));
        target = skeleton.findBone(data.target.name);
    }

    public IkConstraint(IkConstraint constraint, Skeleton skeleton) {
        if (constraint == null) throw new IllegalArgumentException("constraint cannot be null.");
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        data = constraint.data;
        bones = new Array<>(constraint.bones.size);
        for (Bone bone : constraint.bones)
            bones.add(skeleton.bones.get(bone.data.index));
        target = skeleton.bones.get(constraint.target.data.index);
        mix = constraint.mix;
        softness = constraint.softness;
        bendDirection = constraint.bendDirection;
        compress = constraint.compress;
        stretch = constraint.stretch;
    }

    static public void apply(Bone bone, float targetX, float targetY, boolean compress, boolean stretch, boolean uniform,
                             float alpha) {
        if (bone == null) throw new IllegalArgumentException("bone cannot be null.");
        if (!bone.appliedValid) bone.updateAppliedTransform();
        Bone p = bone.parent;
        float pa = p.a, pb = p.b, pc = p.c, pd = p.d;
        float rotationIK = -bone.ashearX - bone.arotation, tx, ty;
        switch (bone.data.transformMode) {
            case onlyTranslation:
                tx = targetX - bone.worldX;
                ty = targetY - bone.worldY;
                break;
            case noRotationOrReflection:
                float s = Math.abs(pa * pd - pb * pc) / (pa * pa + pc * pc);
                float sa = pa / bone.skeleton.scaleX;
                float sc = pc / bone.skeleton.scaleY;
                pb = -sc * s * bone.skeleton.scaleX;
                pd = sa * s * bone.skeleton.scaleY;
                rotationIK += atan2(sc, sa) * radDeg;
            default:
                float x = targetX - p.worldX, y = targetY - p.worldY;
                float d = pa * pd - pb * pc;
                tx = (x * pd - y * pb) / d - bone.ax;
                ty = (y * pa - x * pc) / d - bone.ay;
        }
        rotationIK += atan2(ty, tx) * radDeg;
        if (bone.ascaleX < 0) rotationIK += 180;
        if (rotationIK > 180)
            rotationIK -= 360;
        else if (rotationIK < -180)
            rotationIK += 360;
        float sx = bone.ascaleX, sy = bone.ascaleY;
        if (compress || stretch) {
            switch (bone.data.transformMode) {
                case noScale, noScaleOrReflection -> {
                    tx = targetX - bone.worldX;
                    ty = targetY - bone.worldY;
                }
            }
            float b = bone.data.length * sx, dd = (float) Math.sqrt(tx * tx + ty * ty);
            if ((compress && dd < b) || (stretch && dd > b) && b > 0.0001f) {
                float s = (dd / b - 1) * alpha + 1;
                sx *= s;
                if (uniform) sy *= s;
            }
        }
        bone.updateWorldTransform(bone.ax, bone.ay, bone.arotation + rotationIK * alpha, sx, sy, bone.ashearX, bone.ashearY);
    }

    static public void apply(Bone parent, Bone child, float targetX, float targetY, int bendDir, boolean stretch, float softness,
                             float alpha) {
        if (parent == null) throw new IllegalArgumentException("parent cannot be null.");
        if (child == null) throw new IllegalArgumentException("child cannot be null.");
        if (!parent.appliedValid) parent.updateAppliedTransform();
        if (!child.appliedValid) child.updateAppliedTransform();
        float px = parent.ax, py = parent.ay, psx = parent.ascaleX, sx = psx, psy = parent.ascaleY, csx = child.ascaleX;
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
        if (csx < 0) {
            csx = -csx;
            os2 = 180;
        } else
            os2 = 0;
        float cx = child.ax, cy, cwx, cwy, a = parent.a, b = parent.b, c = parent.c, d = parent.d;
        boolean u = Math.abs(psx - psy) <= 0.0001f;
        if (!u) {
            cy = 0;
            cwx = a * cx + parent.worldX;
            cwy = c * cx + parent.worldY;
        } else {
            cy = child.ay;
            cwx = a * cx + b * cy + parent.worldX;
            cwy = c * cx + d * cy + parent.worldY;
        }
        Bone pp = parent.parent;
        a = pp.a;
        b = pp.b;
        c = pp.c;
        d = pp.d;
        float id = 1 / (a * d - b * c), x = cwx - pp.worldX, y = cwy - pp.worldY;
        float dx = (x * d - y * b) * id - px, dy = (y * a - x * c) * id - py;
        float l1 = (float) Math.sqrt(dx * dx + dy * dy), l2 = child.data.length * csx, a1, a2;
        if (l1 < 0.0001f) {
            apply(parent, targetX, targetY, false, stretch, false, alpha);
            child.updateWorldTransform(cx, cy, 0, child.ascaleX, child.ascaleY, child.ashearX, child.ashearY);
            return;
        }
        x = targetX - pp.worldX;
        y = targetY - pp.worldY;
        float tx = (x * d - y * b) * id - px, ty = (y * a - x * c) * id - py;
        float dd = tx * tx + ty * ty;
        if (softness != 0) {
            softness *= psx * (csx + 1) / 2;
            float td = (float) Math.sqrt(dd), sd = td - l1 - l2 * psx + softness;
            if (sd > 0) {
                float p = Math.min(1, sd / (softness * 2)) - 1;
                p = (sd - softness * (1 - p * p)) / td;
                tx -= p * tx;
                ty -= p * ty;
                dd = tx * tx + ty * ty;
            }
        }
        outer:
        if (u) {
            l2 *= psx;
            float cos = (dd - l1 * l1 - l2 * l2) / (2 * l1 * l2);
            if (cos < -1)
                cos = -1;
            else if (cos > 1) {
                cos = 1;
                if (stretch) sx *= ((float) Math.sqrt(dd) / (l1 + l2) - 1) * alpha + 1;
            }
            a2 = (float) Math.acos(cos) * bendDir;
            a = l1 + l2 * cos;
            b = l2 * sin(a2);
            a1 = atan2(ty * a - tx * b, tx * a + ty * b);
        } else {
            a = psx * l2;
            b = psy * l2;
            float aa = a * a, bb = b * b, ta = atan2(ty, tx);
            c = bb * l1 * l1 + aa * dd - aa * bb;
            float c1 = -2 * bb * l1, c2 = bb - aa;
            d = c1 * c1 - 4 * c2 * c;
            if (d >= 0) {
                float q = (float) Math.sqrt(d);
                if (c1 < 0) q = -q;
                q = -(c1 + q) / 2;
                float r0 = q / c2, r1 = c / q;
                float r = Math.abs(r0) < Math.abs(r1) ? r0 : r1;
                if (r * r <= dd) {
                    y = (float) Math.sqrt(dd - r * r) * bendDir;
                    a1 = ta - atan2(y, r);
                    a2 = atan2(y / psy, (r - l1) / psx);
                    break outer;
                }
            }
            float minAngle = PI, minX = l1 - a, minDist = minX * minX, minY = 0;
            float maxAngle = 0, maxX = l1 + a, maxDist = maxX * maxX, maxY = 0;
            c = -a * l1 / (aa - bb);
            if (c >= -1 && c <= 1) {
                c = (float) Math.acos(c);
                x = a * cos(c) + l1;
                y = b * sin(c);
                d = x * x + y * y;
                if (d < minDist) {
                    minAngle = c;
                    minDist = d;
                    minX = x;
                    minY = y;
                }
                if (d > maxDist) {
                    maxAngle = c;
                    maxDist = d;
                    maxX = x;
                    maxY = y;
                }
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
        float rotation = parent.arotation;
        a1 = (a1 - os) * radDeg + os1 - rotation;
        if (a1 > 180)
            a1 -= 360;
        else if (a1 < -180) a1 += 360;
        parent.updateWorldTransform(px, py, rotation + a1 * alpha, sx, parent.ascaleY, 0, 0);
        rotation = child.arotation;
        a2 = ((a2 + os) * radDeg - child.ashearX) * s2 + os2 - rotation;
        if (a2 > 180)
            a2 -= 360;
        else if (a2 < -180) a2 += 360;
        child.updateWorldTransform(cx, cy, rotation + a2 * alpha, child.ascaleX, child.ascaleY, child.ashearX, child.ashearY);
    }

    public void update() {
        if (mix == 0) return;
        Bone target = this.target;
        Object[] bones = this.bones.items;
        switch (this.bones.size) {
            case 1 -> apply((Bone) bones[0], target.worldX, target.worldY, compress, stretch, data.uniform, mix);
            case 2 -> apply((Bone) bones[0], (Bone) bones[1], target.worldX, target.worldY, bendDirection, stretch, softness, mix);
        }
    }

    public Array<Bone> getBones() {
        return bones;
    }

    public Bone getTarget() {
        return target;
    }

    public void setTarget(Bone target) {
        if (target == null) throw new IllegalArgumentException("target cannot be null.");
        this.target = target;
    }

    public float getMix() {
        return mix;
    }

    public void setMix(float mix) {
        this.mix = mix;
    }

    public float getSoftness() {
        return softness;
    }

    public void setSoftness(float softness) {
        this.softness = softness;
    }

    public int getBendDirection() {
        return bendDirection;
    }

    public void setBendDirection(int bendDirection) {
        this.bendDirection = bendDirection;
    }

    public boolean getCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    public boolean getStretch() {
        return stretch;
    }

    public void setStretch(boolean stretch) {
        this.stretch = stretch;
    }

    public boolean isActive() {
        return active;
    }

    public IkConstraintData getData() {
        return data;
    }

    public String toString() {
        return data.name;
    }
}
