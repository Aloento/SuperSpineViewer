package com.esotericsoftware.spine21;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import static com.badlogic.gdx.math.MathUtils.*;

public class IkConstraint {
    static private final Vector2 temp = new Vector2();
    final IkConstraintData data;
    final Array<Bone> bones;
    Bone target;
    float mix;
    int bendDirection;

    public IkConstraint(IkConstraintData data, Skeleton skeleton) {
        this.data = data;
        mix = data.mix;
        bendDirection = data.bendDirection;
        bones = new Array(data.bones.size);
        if (skeleton != null) {
            for (BoneData boneData : data.bones)
                bones.add(skeleton.findBone(boneData.name));
            target = skeleton.findBone(data.target.name);
        }
    }

    public IkConstraint(IkConstraint ikConstraint, Array<Bone> bones, Bone target) {
        data = ikConstraint.data;
        this.bones = bones;
        this.target = target;
        mix = ikConstraint.mix;
        bendDirection = ikConstraint.bendDirection;
    }

    static public void apply(Bone bone, float targetX, float targetY, float alpha) {
        float parentRotation = (!bone.data.inheritRotation || bone.parent == null) ? 0 : bone.parent.worldRotation;
        float rotation = bone.rotation;
        float rotationIK = (float) Math.atan2(targetY - bone.worldY, targetX - bone.worldX) * radDeg - parentRotation;
        bone.rotationIK = rotation + (rotationIK - rotation) * alpha;
    }

    static public void apply(Bone parent, Bone child, float targetX, float targetY, int bendDirection, float alpha) {
        float childRotation = child.rotation, parentRotation = parent.rotation;
        if (alpha == 0) {
            child.rotationIK = childRotation;
            parent.rotationIK = parentRotation;
            return;
        }
        Vector2 position = temp;
        Bone parentParent = parent.parent;
        if (parentParent != null) {
            parentParent.worldToLocal(position.set(targetX, targetY));
            targetX = (position.x - parent.x) * parentParent.worldScaleX;
            targetY = (position.y - parent.y) * parentParent.worldScaleY;
        } else {
            targetX -= parent.x;
            targetY -= parent.y;
        }
        if (child.parent == parent)
            position.set(child.x, child.y);
        else
            parent.worldToLocal(child.parent.localToWorld(position.set(child.x, child.y)));
        float childX = position.x * parent.worldScaleX, childY = position.y * parent.worldScaleY;
        float offset = (float) Math.atan2(childY, childX);
        float len1 = (float) Math.sqrt(childX * childX + childY * childY), len2 = child.data.length * child.worldScaleX;
        float cosDenom = 2 * len1 * len2;
        if (cosDenom < 0.0001f) {
            child.rotationIK = childRotation + ((float) Math.atan2(targetY, targetX) * radDeg - parentRotation - childRotation)
                    * alpha;
            return;
        }
        float cos = clamp((targetX * targetX + targetY * targetY - len1 * len1 - len2 * len2) / cosDenom, -1, 1);
        float childAngle = (float) Math.acos(cos) * bendDirection;
        float adjacent = len1 + len2 * cos, opposite = len2 * sin(childAngle);
        float parentAngle = (float) Math.atan2(targetY * adjacent - targetX * opposite, targetX * adjacent + targetY * opposite);
        float rotation = (parentAngle - offset) * radDeg - parentRotation;
        if (rotation > 180)
            rotation -= 360;
        else if (rotation < -180)
            rotation += 360;
        parent.rotationIK = parentRotation + rotation * alpha;
        rotation = (childAngle + offset) * radDeg - childRotation;
        if (rotation > 180)
            rotation -= 360;
        else if (rotation < -180)
            rotation += 360;
        child.rotationIK = childRotation + (rotation + parent.worldRotation - child.parent.worldRotation) * alpha;
    }

    public void apply() {
        Bone target = this.target;
        Array<Bone> bones = this.bones;
        switch (bones.size) {
            case 1 -> apply(bones.first(), target.worldX, target.worldY, mix);
            case 2 -> apply(bones.first(), bones.get(1), target.worldX, target.worldY, bendDirection, mix);
        }
    }

    public Array<Bone> getBones() {
        return bones;
    }

    public Bone getTarget() {
        return target;
    }

    public void setTarget(Bone target) {
        this.target = target;
    }

    public float getMix() {
        return mix;
    }

    public void setMix(float mix) {
        this.mix = mix;
    }

    public int getBendDirection() {
        return bendDirection;
    }

    public void setBendDirection(int bendDirection) {
        this.bendDirection = bendDirection;
    }

    public IkConstraintData getData() {
        return data;
    }

    public String toString() {
        return data.name;
    }
}
