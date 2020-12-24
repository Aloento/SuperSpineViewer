package com.QYun.SuperSpineViewer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.*;
import com.esotericsoftware.spine.utils.TwoColorPolygonBatch;

public class FrostlTest extends ApplicationAdapter {
    OrthographicCamera camera;
    TwoColorPolygonBatch batch;
    SkeletonRenderer renderer;

    Skeleton Frostl_Build;
    AnimationState Frostl_BuildState;

    public void create () {
        camera = new OrthographicCamera();
        batch = new TwoColorPolygonBatch();
        renderer = new SkeletonRenderer();
        renderer.setPremultipliedAlpha(true);

        {
            TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("Frostl_Build/Frostl_Build.atlas"));
            SkeletonBinary binary = new SkeletonBinary(atlas);
            binary.setScale(2.0f);
            SkeletonData skeletonData = binary.readSkeletonData(Gdx.files.internal("Frostl_Build/Frostl_Build.skel"));
            Frostl_Build = new Skeleton(skeletonData);
            Frostl_Build.setPosition(320, 20);

            AnimationStateData stateData = new AnimationStateData(skeletonData);
            Frostl_BuildState = new AnimationState(stateData);
            Frostl_BuildState.addAnimation(0, "run", true, 0);
        }
    }

    public void render () {
        Frostl_BuildState.update(Gdx.graphics.getDeltaTime());
        Frostl_BuildState.apply(Frostl_Build);
        Frostl_Build.updateWorldTransform();

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.getProjectionMatrix().set(camera.combined);
        batch.begin();
        renderer.draw(batch, Frostl_Build);
        batch.end();
    }

    public void resize (int width, int height) {
        camera.setToOrtho(false);
    }
}
