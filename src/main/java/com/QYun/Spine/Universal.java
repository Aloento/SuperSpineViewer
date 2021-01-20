package com.QYun.Spine;

import com.badlogic.gdx.ApplicationAdapter;
import javafx.application.Platform;

public class Universal extends ApplicationAdapter {
    private SuperSpine Runtimes;
    public static byte Range;

    @Override
    public void create() {
        if (Universal.Range == 2)
            Runtimes = new Preview();
        else if (Universal.Range == 0)
            Runtimes = new Latency();
        else Runtimes = new Standard();

        Platform.runLater(() -> {
            SuperSpine.skinsList.clear();
            SuperSpine.animatesList.clear();
            SuperSpine.skin.set(null);
            SuperSpine.animate.set(null);
            System.gc();
        });

        Runtimes.create();
    }

    @Override
    public void render() {
        Runtimes.render();
    }

    @Override
    public void resize(int width, int height) {
        Runtimes.resize();
    }
}
