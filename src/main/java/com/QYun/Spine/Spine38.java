package com.QYun.Spine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.files.FileHandle;

public class Spine38 extends ApplicationAdapter {

    FileHandle skelFile;
    FileHandle atlasFile;
    boolean isBinary;

    public Spine38(FileHandle skelFile, FileHandle atlasFile, boolean isBinary) {
        this.skelFile = skelFile;
        this.atlasFile = atlasFile;
        this.isBinary = isBinary;
    }

    @Override
    public void create() {
        super.create();
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void resize(int width, int height) {
    }

}
