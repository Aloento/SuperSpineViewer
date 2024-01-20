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
import com.esotericsoftware.spine31.*;
import com.esotericsoftware.spine31.AnimationState.TrackEntry;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import to.aloen.ssv.Loader;
import to.aloen.ssv.Main;

public class Spine31 extends Spine {

    private PolygonSpriteBatch batch;

    private OrthographicCamera camera;

    private SkeletonMeshRenderer renderer;

    private Skeleton skeleton;

    private AnimationState state;

    private float trackTime;

    private ChangeListener<String> skinListener = (_, _, newValue) -> {
        if (newValue == null)
            skeleton.setSkin((Skin) null);
        else
            skeleton.setSkin(newValue);

        skeleton.setSlotsToSetupPose();
    };

    private ChangeListener<String> animateListener = (_, _, newValue) -> {
        if (newValue != null) {
            state.setAnimation(0, newValue, isLoop.get());
            isPlay.set(true);
        } else
            isPlay.set(false);
    };

    private ChangeListener<Boolean> isLoopListener = (_, _, _) -> {
        if (isPlay.get()) {
            isPlay.set(false);
            isPlay.set(true);
        }
    };

    private ChangeListener<Boolean> isPlayListener = (_, oldValue, newValue) -> {
        if (!newValue.equals(oldValue)) {
            if (newValue) {
                if (animate.get() == null)
                    state.setAnimation(0, animatesList.getFirst(), isLoop.get());
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

    private ChangeListener<Number> scaleListener = (_, _, _) -> {
        Gdx.app.postRunnable(this::loadSkeleton);

        if (animate.get() != null) {
            state.setAnimation(0, animate.get(), isLoop.get());
            isPlay.set(true);
        }
    };

    private ChangeListener<Number> XListener = (_, _, _) -> {
        Gdx.app.postRunnable(this::loadSkeleton);

        if (animate.get() != null) {
            state.setAnimation(0, animate.get(), isLoop.get());
            isPlay.set(true);
        }
    };

    private ChangeListener<Number> YListener = (_, _, _) -> {
        Gdx.app.postRunnable(this::loadSkeleton);

        if (animate.get() != null) {
            state.setAnimation(0, animate.get(), isLoop.get());
            isPlay.set(true);
        }
    };

    private ChangeListener<Number> speedListener = (_, _, _) -> state.setTimeScale(speed.get());

    private void lists(Array<Skin> skins, Array<Animation> animations) {
        for (Skin skin : skins)
            skinsList.add(skin.getName());

        for (Animation animation : animations)
            animatesList.add(animation.getName());
    }

    private boolean loadSkeleton() {
        try {
            TextureAtlasData atlasData = new TextureAtlasData(atlasFile, atlasFile.parent(), false);

            boolean linear = true;
            for (int i = 0, n = atlasData.getPages().size; i < n; i++) {
                TextureAtlasData.Page page = atlasData.getPages().get(i);
                if (page.pma) {
                    renderA.set(true);
                    batchA.set(true);
                }

                if (page.minFilter != TextureFilter.Linear || page.magFilter != TextureFilter.Linear) {
                    linear = false;
                    break;
                }
            }

            TextureAtlas atlas = getAtlas(linear, atlasData);

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
        } catch (Exception e) {
            e.printStackTrace();
            projectName.set("Failed to Load");
            return false;
        }
    }

    private TextureAtlas getAtlas(boolean linear, TextureAtlasData atlasData) {
        TextureFilter filter = linear ? TextureFilter.Linear : TextureFilter.Nearest;

        return new TextureAtlas(atlasData) {
            public AtlasRegion findRegion(String name) {
                AtlasRegion region = super.findRegion(name);

                if (region == null) {
                    FileHandle file = skelFile.sibling(STR."\{name}.png");

                    if (file.exists()) {
                        Texture texture = new Texture(file);
                        texture.setFilter(filter, filter);
                        region = new AtlasRegion(texture, 0, 0, texture.getWidth(), texture.getHeight());
                        region.name = name;
                    }
                }

                return region;
            }
        };
    }

    public void reload() {
        super.reload();

        if (Loader.spineVersion != currentVersion) {
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
        } else
            Gdx.app.postRunnable(this::loadSkeleton);
    }

    public void create() {
        batch = new PolygonSpriteBatch();
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        renderer = new SkeletonMeshRenderer();
        renderer.setPremultipliedAlpha(renderA.get());

        if (loadSkeleton()) {
            skin.addListener(skinListener);
            animate.addListener(animateListener);
            isLoop.addListener(isLoopListener);
            isPlay.addListener(isPlayListener);
            scale.addListener(scaleListener);
            X.addListener(XListener);
            Y.addListener(YListener);
            speed.addListener(speedListener);
        }
    }

    public void render() {
        state.update(Gdx.graphics.getDeltaTime());
        state.apply(skeleton);
        skeleton.updateWorldTransform();

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.graphics.setTitle(STR."FPS : \{Gdx.graphics.getFramesPerSecond()}");

        renderer.setPremultipliedAlpha(renderA.get());

        camera.update();
        batch.getProjectionMatrix().set(camera.combined);
        batch.begin();
        renderer.draw(batch, skeleton);
        batch.end();

        TrackEntry entry = state.getCurrent(0);

        if (entry != null) {
            percent = entry.getTime() / entry.getEndTime();

            if (isPlay.get())
                Platform.runLater(() -> Main.progressBar.setProgress(percent));

            if (percent >= 1 && !isLoop.get())
                isPlay.set(false);
        }
    }

    public void resize() {
        float x = camera.position.x, y = camera.position.y;
        camera.setToOrtho(false);
        camera.position.set(x, y, 0);
    }
}
