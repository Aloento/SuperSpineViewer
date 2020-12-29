package com.QYun.Spine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine38.*;
import com.esotericsoftware.spine38.utils.TwoColorPolygonBatch;

public class FrostlTest extends ApplicationAdapter {
    OrthographicCamera camera;
    TwoColorPolygonBatch batch;
    SkeletonRenderer renderer;
    Skeleton Frostl_Build;
    AnimationState Frostl_BuildState;

    public FrostlTest() {
    }

    public void create() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        this.camera = new OrthographicCamera(w, h);
        this.batch = new TwoColorPolygonBatch();
        this.renderer = new SkeletonRenderer();
        this.renderer.setPremultipliedAlpha(true);
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("Frostl_Build/Frostl_Build.atlas"));
        SkeletonBinary binary = new SkeletonBinary(atlas);
        binary.setScale(4.0F);
        SkeletonData skeletonData = binary.readSkeletonData(Gdx.files.internal("Frostl_Build/Frostl_Build.skel"));
        this.Frostl_Build = new Skeleton(skeletonData);
        this.Frostl_Build.setPosition(0.0F, -400F);
        AnimationStateData stateData = new AnimationStateData(skeletonData);
        this.Frostl_BuildState = new AnimationState(stateData);
        this.Frostl_BuildState.addAnimation(0, "run", true, 0.0F);
    }

    public void render() {
        this.Frostl_BuildState.update(Gdx.graphics.getDeltaTime());
        this.Frostl_BuildState.apply(this.Frostl_Build);
        this.Frostl_Build.updateWorldTransform();
        Gdx.gl.glClear(16384);
        Gdx.graphics.setTitle("FPS: " + Gdx.graphics.getFramesPerSecond());

        this.camera.update();
        this.batch.getProjectionMatrix().set(this.camera.combined);
        this.batch.begin();
        this.renderer.draw(this.batch, this.Frostl_Build);
        this.batch.end();
    }

    public void resize(int width, int height) {
        this.camera.setToOrtho(false);
    }
}
