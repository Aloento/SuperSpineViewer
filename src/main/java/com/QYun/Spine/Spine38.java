package com.QYun.Spine;

import com.badlogic.gdx.files.FileHandle;

public class Spine38 extends SuperSpine {

    FileHandle skelFile;
    FileHandle atlasFile;
    boolean isBinary;

    public Spine38(FileHandle skelFile, FileHandle atlasFile, boolean isBinary) {
        this.skelFile = skelFile;
        this.atlasFile = atlasFile;
        this.isBinary = isBinary;
    }

    private void loadSkel() {

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
