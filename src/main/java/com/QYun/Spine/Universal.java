package com.QYun.Spine;

import com.QYun.SuperSpineViewer.RuntimesLoader;
import com.badlogic.gdx.ApplicationAdapter;

public class Universal extends ApplicationAdapter {
    private SuperSpine Runtimes;

    @Override
    public void create() {
        if (RuntimesLoader.spineVersion > 38)
            Runtimes = new Preview();
        else if (RuntimesLoader.spineVersion < 34)
            Runtimes = new Latency();
        else Runtimes = new Standard();

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
