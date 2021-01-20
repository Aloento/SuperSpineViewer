package com.QYun.Spine;

import com.badlogic.gdx.ApplicationAdapter;

public class Universal extends ApplicationAdapter {
    public static byte Range;
    private static SuperSpine Runtimes;

    public void reload() {
        if (Runtimes != null)
            Runtimes.reload();
    }

    @Override
    public void create() {
        if (Universal.Range == 2)
            Runtimes = new Preview();
        else if (Universal.Range == 0)
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
