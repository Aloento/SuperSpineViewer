package com.badlogic.gdx.backends.lwjgl;

import com.QYun.SuperSpineViewer.Main;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.stream.StreamUtil;

public class LwjglFXApplication extends LwjglApplication {
    // final LwjglFXInput input;
    private boolean shouldRender;

    public LwjglFXApplication(ApplicationListener listener, ImageView target) {
        this(listener, target, new LwjglApplicationConfiguration());
    }

    public LwjglFXApplication(ApplicationListener listener, ImageView target, LwjglApplicationConfiguration config) {
        this(listener, config, new LwjglFXGraphics(config, target));
    }

    public LwjglFXApplication(ApplicationListener listener, LwjglApplicationConfiguration config, LwjglFXGraphics graphics) {
        super(listener, config, graphics);
        mainLoopThread.setPriority(Thread.MAX_PRIORITY);
        // input = new LwjglFXInput(graphics.target);
        // Gdx.input = input;
    }

    private void resize() {
        Gdx.app.postRunnable(() -> {
            graphics.resize = false;
            graphics.config.width = Main.width;
            graphics.config.height = Main.height;
            if (listener != null)
                listener.resize(Main.width, Main.height);
            shouldRender = true;
            graphics.requestRendering();
        });
    }

    @Override
    protected void mainLoop() {
        Array<LifecycleListener> lifecycleListeners = this.lifecycleListeners;
        ImageView target = ((LwjglFXGraphics) graphics).target;
        Stage stage = (Stage) target.getScene().getWindow();
        target.fitWidthProperty().addListener(e -> resize());
        target.fitHeightProperty().addListener(e -> resize());
        stage.setOnCloseRequest(e -> exit());
        LwjglToJavaFX toFX;
        try {
            graphics.setupDisplay();
            toFX = ((LwjglFXGraphics) graphics).toFX;
            toFX.setRenderStreamFactory(StreamUtil.getRenderStreamImplementations().get(1));
        } catch (LWJGLException e) {
            throw new GdxRuntimeException(e);
        }

        listener.create();
        graphics.resize = true;

        graphics.lastTime = System.nanoTime();
        boolean wasActive = true;
        while (running) {
            boolean isActive = stage.isFocused();
            if (wasActive && !isActive) { // if it's just recently minimized from active state
                wasActive = false;
                synchronized (lifecycleListeners) {
                    for (LifecycleListener listener : lifecycleListeners)
                        listener.pause();
                }
                listener.pause();
            }
            if (!wasActive && isActive) { // if it's just recently focused from minimized state
                wasActive = true;
                listener.resume();
                synchronized (lifecycleListeners) {
                    for (LifecycleListener listener : lifecycleListeners)
                        listener.resume();
                }
            }

            shouldRender = false;
            graphics.config.x = (int) ((LwjglFXGraphics) graphics).target.getLayoutX();
            graphics.config.y = (int) ((LwjglFXGraphics) graphics).target.getLayoutY();

            if (executeRunnables()) shouldRender = true;

            // If one of the runnables set running to false, for example after an exit().
            if (!running) break;
            shouldRender |= graphics.shouldRender();
            // input.processEvents();
            if (audio != null) audio.update();

            if (!isActive && graphics.config.backgroundFPS == -1) shouldRender = false;
            int frameRate = isActive ? graphics.config.foregroundFPS : graphics.config.backgroundFPS;
            if (shouldRender) {
                graphics.updateTime();
                toFX.begin();
                listener.render();
                toFX.end();
            } else {
                // Sleeps to avoid wasting CPU in an empty loop.
                if (frameRate == -1) frameRate = 10;
                if (frameRate == 0) frameRate = graphics.config.backgroundFPS;
                if (frameRate == 0) frameRate = 30;
            }
            if (frameRate > 0 && graphics.vsync) Display.sync(frameRate);
        }

        synchronized (lifecycleListeners) {
            for (LifecycleListener listener : lifecycleListeners) {
                listener.pause();
                listener.dispose();
            }
        }
        listener.pause();
        listener.dispose();
        toFX.dispose();
        if (audio != null) audio.dispose();
        // if (graphics.config.forceExit) System.exit(-1);
    }
}
