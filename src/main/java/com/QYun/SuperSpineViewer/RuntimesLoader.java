package com.QYun.SuperSpineViewer;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.esotericsoftware.spine40.SkeletonBinary;
import com.esotericsoftware.spine40.SkeletonData;
import com.esotericsoftware.spine40.SkeletonJson;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RuntimesLoader {

    static String[] extraSuffixes = {"", ".txt", ".bytes"};
    static String[] dataSuffixes = {".json", ".skel"};
    static String[] atlasSuffixes = {".atlas", "-pro.atlas", "-ess.atlas"};
    public AtomicReference<String> spineVersion = new AtomicReference<>("null");
    public TextureAtlas atlas;
    public AtomicBoolean isBinary = new AtomicBoolean(true);

    private FileHandle atlasFile(FileHandle skelFile, String baseName) {
        for (String extraSuffix : extraSuffixes) {
            for (String suffix : atlasSuffixes) {
                FileHandle file = skelFile.sibling(baseName + suffix + extraSuffix);
                if (file.exists()) return file;
            }
        }
        return null;
    }

    private FileHandle atlasFile(FileHandle skelFile) {
        String baseName = skelFile.name();
        for (String extraSuffix : extraSuffixes) {
            for (String dataSuffix : dataSuffixes) {
                String suffix = dataSuffix + extraSuffix;
                if (baseName.endsWith(suffix)) {
                    FileHandle file = atlasFile(skelFile, baseName.substring(0, baseName.length() - suffix.length()));
                    if (file != null) return file;
                }
            }
        }
        return atlasFile(skelFile, baseName);
    }

    public boolean init(FileHandle skelFile) {
        try {
            FileHandle atlasFile = atlasFile(skelFile);
            TextureAtlasData atlasData = new TextureAtlasData(atlasFile, atlasFile.parent(), false);

            Pixmap pixmap = new Pixmap(32, 32, Format.RGBA8888);
            pixmap.setColor(new Color(1, 1, 1, 0.33f));
            pixmap.fill();
            final AtlasRegion fake = new AtlasRegion(new Texture(pixmap), 0, 0, 32, 32);
            pixmap.dispose();

            atlas = new TextureAtlas(atlasData) {
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
                    return region != null ? region : fake;
                }
            };

            String extension = skelFile.extension();
            SkeletonData skeletonData;

            if (extension.equalsIgnoreCase("json") || extension.equalsIgnoreCase("txt")) {
                SkeletonJson json = new SkeletonJson(atlas);
                skeletonData = json.readSkeletonData(skelFile);
                isBinary.set(false);
            } else {
                SkeletonBinary binary = new SkeletonBinary(atlas);
                skeletonData = binary.readSkeletonData(skelFile);
            }

            spineVersion.set(skeletonData.getVersion());
            return true;

        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }

    }

}
