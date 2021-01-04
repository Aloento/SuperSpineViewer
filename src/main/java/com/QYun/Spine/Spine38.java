package com.QYun.Spine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine38.*;
import com.esotericsoftware.spine38.utils.TwoColorPolygonBatch;

import java.util.Objects;

public class Spine38 extends SuperSpine {

    private final FileHandle skelFile;
    private final FileHandle atlasFile;
    private final boolean isBinary;
    TwoColorPolygonBatch batch;
    OrthographicCamera camera;
    SkeletonRenderer renderer;
    private Skeleton skeleton;
    private AnimationState state;
    private float time = 0;

    public Spine38(FileHandle skelFile, FileHandle atlasFile, boolean isBinary) {
        this.skelFile = Objects.requireNonNull(skelFile);
        this.atlasFile = Objects.requireNonNull(atlasFile);
        this.isBinary = isBinary;
    }

    private void skins(Array<Skin> skins) {
        for (Skin skin : skins)
            skinsList.add(skin.getName());
    }

    private void animates(Array<Animation> animations) {
        for (Animation animation : animations)
            animatesList.add(animation.getName());
    }

    private boolean loadSkel() {
        TextureAtlas atlas = new TextureAtlas(atlasFile);
        SkeletonData skeletonData;

        if (isBinary) {
            SkeletonBinary binary = new SkeletonBinary(atlas);
            binary.setScale(scale.get());
            skeletonData = binary.readSkeletonData(skelFile);
        } else {
            SkeletonJson json = new SkeletonJson(atlas);
            json.setScale(scale.get());
            skeletonData = json.readSkeletonData(skelFile);
        }
        if (skeletonData.getBones().size == 0) {
            System.out.println("骨骼为空");
            return false;
        }

        skeleton = new Skeleton(skeletonData);
        skeleton.updateWorldTransform();
        skeleton.setToSetupPose();
        skeleton.setPosition(X.get(), Y.get());

        state = new AnimationState(new AnimationStateData(skeletonData));
        if (animate.get() == null)
            state.setEmptyAnimation(0, 0);

        spineVersion.set(skeletonData.getVersion());
        skins(skeletonData.getSkins());
        animates(skeletonData.getAnimations());

        return true;
    }

    private void listeners() {

        skin.addListener((observable, oldValue, newValue) -> {
            if (skeleton != null) {
                if (newValue == null)
                    skeleton.setSkin((Skin) null);
                else skeleton.setSkin(newValue);
                skeleton.setSlotsToSetupPose();
            }
        });

        animate.addListener((observable, oldValue, newValue) -> {
            if (state != null) {
                state.setAnimation(0, newValue, isLoop.get());
            }
        });

        isLoop.addListener((observable, oldValue, newValue) -> {
            if (state != null) {
                state.setAnimation(0, animate.getName(), newValue);
            }
        });

        isPlay.addListener((observable, oldValue, newValue) -> {
            if (state != null) {
                if (newValue) {
                    state.getTracks().items[0].setTrackTime(time);
                    state.setTimeScale(speed.get());
                } else {
                    time = state.getTracks().items[0].getTrackTime();
                    state.setTimeScale(0);
                }
            }
        });

        scale.addListener((observable, oldValue, newValue) -> {
            if (state != null)
                loadSkel();
        });

        X.addListener((observable, oldValue, newValue) -> {
            if (state != null)
                loadSkel();
        });

        Y.addListener((observable, oldValue, newValue) -> {
            if (state != null)
                loadSkel();
        });

        speed.addListener((observable, oldValue, newValue) -> {
            if (state != null) {
                state.setTimeScale(speed.get());
            }
        });

    }

    @Override
    public void create() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        batch = new TwoColorPolygonBatch(3100);
        camera = new OrthographicCamera(w, h);
        renderer = new SkeletonRenderer();
        renderer.setPremultipliedAlpha(true);

        if (loadSkel())
            listeners();
    }

    @Override
    public void render() {

    }

    @Override
    public void resize(int width, int height) {
    }

}
