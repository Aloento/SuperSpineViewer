package to.aloen.spine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.badlogic.gdx.utils.Array;
import com.archive.SpineLegacy.*;
import com.archive.SpineLegacy.AnimationState.TrackEntry;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import to.aloen.ssv.Main;

public class Legacy extends Spine {
    private PolygonSpriteBatch batch;
    private OrthographicCamera camera;
    private SkeletonMeshRenderer renderer;
    private Skeleton skeleton;
    private AnimationState state;
    private float trackTime;
    private ChangeListener<String> skinListener = (observable, oldValue, newValue) -> {
        if (newValue == null)
            skeleton.setSkin((Skin) null);
        else skeleton.setSkin(newValue);
        skeleton.setSlotsToSetupPose();
    };
    private ChangeListener<String> animateListener = (observable, oldValue, newValue) -> {
        if (newValue != null) {
            state.setAnimation(0, newValue, isLoop.get());
            isPlay.set(true);
        } else
            isPlay.set(false);
    };
    private ChangeListener<Boolean> isLoopListener = (observable, oldValue, newValue) -> {
        if (isPlay.get()) {
            isPlay.set(false);
            isPlay.set(true);
        }
    };
    private ChangeListener<Boolean> isPlayListener = (observable, oldValue, newValue) -> {
        if (!newValue.equals(oldValue)) {
            if (newValue) {
                if (animate.get() == null)
                    state.setAnimation(0, animatesList.get(0), isLoop.get());
                else
                    state.setAnimation(0, animate.get(), isLoop.get());
                state.setTimeScale(speed.get());
                if (percent < 1)
                    state.getCurrent(0).setTime(trackTime);
            } else {
                state.setTimeScale(0);
                trackTime = state.getCurrent(0).getTime();
            }
        }
    };
    private ChangeListener<Number> scaleListener = (observable, oldValue, newValue) -> {
        Gdx.app.postRunnable(this::loadSkel);
        if (animate.get() != null) {
            state.setAnimation(0, animate.get(), isLoop.get());
            isPlay.set(true);
        }
    };
    private ChangeListener<Number> XListener = (observable, oldValue, newValue) -> {
        Gdx.app.postRunnable(this::loadSkel);
        if (animate.get() != null) {
            state.setAnimation(0, animate.get(), isLoop.get());
            isPlay.set(true);
        }
    };
    private ChangeListener<Number> YListener = (observable, oldValue, newValue) -> {
        Gdx.app.postRunnable(this::loadSkel);
        if (animate.get() != null) {
            state.setAnimation(0, animate.get(), isLoop.get());
            isPlay.set(true);
        }
    };
    private ChangeListener<Number> speedListener = (observable, oldValue, newValue) -> state.setTimeScale(speed.get());

    private void lists(Array<Skin> skins, Array<Animation> animations) {
        for (Skin skin : skins)
            skinsList.add(skin.getName());

        for (Animation animation : animations)
            animatesList.add(animation.getName());
    }

    private boolean loadSkel() {
        TextureAtlas atlas = new TextureAtlas(new TextureAtlasData(atlasFile, atlasFile.parent(), false)) {
            public AtlasRegion findRegion(String name) {
                AtlasRegion region = super.findRegion(name);
                if (region == null) {
                    FileHandle file = skelFile.sibling(name + ".png");
                    if (file.exists()) {
                        Texture texture = new Texture(file);
                        texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
                        region = new AtlasRegion(texture, 0, 0, texture.getWidth(), texture.getHeight());
                        region.name = name;
                    }
                }
                return region;
            }
        };

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
        spineVersion.set(skeletonData.getVersion());
        projectName.set(skeletonData.getName());

        if (skinsList.isEmpty())
            lists(skeletonData.getSkins(), skeletonData.getAnimations());

        return true;
    }

    private void listeners() {
        skin.addListener(skinListener);
        animate.addListener(animateListener);
        isLoop.addListener(isLoopListener);
        isPlay.addListener(isPlayListener);
        scale.addListener(scaleListener);
        X.addListener(XListener);
        Y.addListener(YListener);
        speed.addListener(speedListener);
    }

    void reload() {
        super.reload();
        if (SpineAdapter.Range != 0) {
            batch = null;
            camera = null;
            renderer = null;
            skeleton = null;
            state = null;

            skin.removeListener(skinListener);
            animate.removeListener(animateListener);
            isLoop.removeListener(isLoopListener);
            isPlay.removeListener(isPlayListener);
            scale.removeListener(scaleListener);
            X.removeListener(XListener);
            Y.removeListener(YListener);
            speed.removeListener(speedListener);

            skinListener = null;
            animateListener = null;
            isLoopListener = null;
            isPlayListener = null;
            scaleListener = null;
            XListener = null;
            YListener = null;
            speedListener = null;
        } else Gdx.app.postRunnable(this::loadSkel);
    }

    void create() {
        batch = new PolygonSpriteBatch();
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        renderer = new SkeletonMeshRenderer();
        renderer.setPremultipliedAlpha(true);

        if (loadSkel())
            listeners();
    }

    void render() {
        state.update(Gdx.graphics.getDeltaTime());
        state.apply(skeleton);
        skeleton.updateWorldTransform();

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.graphics.setTitle("FPS : " + Gdx.graphics.getFramesPerSecond());
        renderer.setPremultipliedAlpha(Main.renderA);

        camera.update();
        batch.getProjectionMatrix().set(camera.combined);
        batch.begin();
        renderer.draw(batch, skeleton);
        batch.end();

        TrackEntry entry = state.getCurrent(0);
        if (entry != null) {
            percent = entry.getTime() / entry.getEndTime();
            if (entry.getLoop())
                percent %= 1;
            if (isPlay.get())
                Platform.runLater(() -> Main.progressBar.setProgress(percent));
            if (percent >= 1 && !isLoop.get())
                isPlay.set(false);
        }
    }

    void resize() {
        float x = camera.position.x, y = camera.position.y;
        camera.setToOrtho(false);
        camera.position.set(x, y, 0);
    }
}
