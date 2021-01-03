package com.QYun.Spine;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine38.*;

import java.util.Objects;

public class Spine38 extends SuperSpine {

    private static FileHandle skelFile;
    private static FileHandle atlasFile;
    private final boolean isBinary;
    private SkeletonData skeletonData;
    private Skeleton skeleton;

    public Spine38(FileHandle skelFile, FileHandle atlasFile, boolean isBinary) {
        Spine38.skelFile = Objects.requireNonNull(skelFile);
        Spine38.atlasFile = Objects.requireNonNull(atlasFile);
        this.isBinary = isBinary;
    }

    private boolean loadSkel() {
        TextureAtlas atlas = new TextureAtlas(atlasFile);

        if (isBinary) {
            SkeletonBinary binary = new SkeletonBinary(atlas);
            binary.setScale(scale);
            skeletonData = binary.readSkeletonData(skelFile);
        } else {
            SkeletonJson json = new SkeletonJson(atlas);
            json.setScale(scale);
            skeletonData = json.readSkeletonData(skelFile);
        }
        if (skeletonData.getBones().size == 0) {
            System.out.println("骨骼为空");
            return false;
        }

        skeleton = new Skeleton(skeletonData);
        skeleton.updateWorldTransform();
        skeleton.setToSetupPose();
        skeleton.updateWorldTransform();

        version = skeletonData.getVersion();
        skins(skeletonData.getSkins());
        animates(skeletonData.getAnimations());

        return true;
    }

    private void skins (Array<Skin> skins) {
        for (Skin skin : skins)
            upSkins.add(skin.getName());
    }

    private void animates (Array<Animation> animations) {
        for (Animation animation : animations)
            upAnimates.add(animation.getName());
    }

    @Override
    public void create() {
        super.create();
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void resize(int width, int height) {
    }

}
