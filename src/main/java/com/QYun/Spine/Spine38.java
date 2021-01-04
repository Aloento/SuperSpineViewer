package com.QYun.Spine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
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
    TextureAtlas atlas;
    SkeletonData skeletonData;
    private Skeleton skeleton;
    private AnimationState state;

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
                state.setAnimation(0, animate.get(), newValue);
            }
        });

        isPlay.addListener((observable, oldValue, newValue) -> {
            if (state != null) {
                if (newValue) {
                    state.setTimeScale(speed.get());
                } else {
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

        atlas = new TextureAtlas(atlasFile);
        if (loadSkel()) {
            spineVersion.set(skeletonData.getVersion());
            skins(skeletonData.getSkins());
            animates(skeletonData.getAnimations());
            listeners();
        }
    }

    @Override
    public void render() {
        state.update(Gdx.graphics.getDeltaTime());
        state.apply(skeleton);
        skeleton.updateWorldTransform();

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.graphics.setTitle("FPS : " + Gdx.graphics.getFramesPerSecond());

        camera.update();
        batch.getProjectionMatrix().set(camera.combined);
        batch.begin();
        renderer.draw(batch, skeleton);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        float x = camera.position.x;
        float y = camera.position.y;
        camera.setToOrtho(false);
        camera.position.set(x, y, 0);
    }

}
