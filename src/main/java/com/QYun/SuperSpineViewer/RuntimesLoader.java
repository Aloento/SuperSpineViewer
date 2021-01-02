package com.QYun.SuperSpineViewer;

import com.QYun.Spine.FrostlTest;
import com.QYun.SuperSpineViewer.GUI.Controller;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglFXApplication;
import com.badlogic.gdx.files.FileHandle;
import javafx.application.Platform;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RuntimesLoader extends Controller {

    static String[] extraSuffixes = {"", ".txt", ".bytes"};
    static String[] dataSuffixes = {".json", ".skel"};
    static String[] atlasSuffixes = {".atlas", "-pro.atlas", "-ess.atlas"};
    public AtomicReference<String> spineVersion = new AtomicReference<>("null");
    public FileHandle skelFile;
    public AtomicBoolean isBinary = new AtomicBoolean(true);
    public AtomicBoolean loaded = new AtomicBoolean(false);
    LwjglFXApplication gdxApp;

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

    private void LibGDX() {
        Platform.runLater(() -> {
            System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true");
            LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
            gdxApp = new LwjglFXApplication(new FrostlTest(), spineRender, config, spineController);
        });
    }

    public void init(FileHandle skelFile) {
        LibGDX();
        // this.skelFile = skelFile;
        // FileHandle atlasFile = atlasFile(skelFile);
        // TextureAtlas atlas = new TextureAtlas(atlasFile);
        //
        // try {
        //     String extension = skelFile.extension();
        //     SkeletonData skeletonData;
        //
        //     if (extension.equalsIgnoreCase("json") || extension.equalsIgnoreCase("txt")) {
        //         SkeletonJson json = new SkeletonJson(atlas);
        //         skeletonData = json.readSkeletonData(skelFile);
        //         isBinary.set(false);
        //     } else {
        //         SkeletonBinary binary = new SkeletonBinary(atlas);
        //         skeletonData = binary.readSkeletonData(skelFile);
        //     }
        //
        //     spineVersion.set(skeletonData.getVersion());
        //     System.out.println("Spine Version : " + spineVersion.get() + "\n"
        //             + "isBinary : " + isBinary.get());
        //     loaded.set(true);
        //
        // } catch (Throwable e) {
        //     System.out.println("Spine文件加载失败");
        //     e.printStackTrace();
        // }

    }

}
