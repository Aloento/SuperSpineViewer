package com.QYun.SuperSpineViewer;

import com.QYun.Spine.SuperSpine;
import com.QYun.Spine.Universal;
import com.QYun.SuperSpineViewer.GUI.Controller;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglFXApplication;
import com.badlogic.gdx.files.FileHandle;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

public class RuntimesLoader extends Controller {
    public static byte spineVersion = 0;
    private final LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
    private final String[] extraSuffixes = {"", ".txt", ".bytes"};
    private final String[] dataSuffixes = {"", ".json", ".skel"};
    private final String[] atlasSuffixes = {".atlas", "-pro.atlas", "-ess.atlas", "-pma.atlas"};
    private final SuperSpine spine = new SuperSpine();
    private final Universal universal = new Universal();
    private LwjglFXApplication gdxApp;

    private void whichVersion(String skel) {
        if (skel.contains("4.0."))
            spineVersion = 40;
        else if (skel.contains("3.8."))
            spineVersion = 38;
        else if (skel.contains("3.7."))
            spineVersion = 37;
        else if (skel.contains("3.6."))
            spineVersion = 36;
        else if (skel.contains("3.5."))
            spineVersion = 35;
        else if (skel.contains("3.4.") || skel.contains("3.3."))
            spineVersion = 34;
        else if (skel.contains("3.2."))
            spineVersion = 32;
        else if (skel.contains("3.1."))
            spineVersion = 31;
        else {
            spineVersion = 31;
            System.out.println("Spine版本过老或判断失败");
        }
    }

    private void skelVersion(File skelFile) {
        try {
            if (spine.isIsBinary()) {
                whichVersion(new BufferedReader(new FileReader(skelFile)).readLine());
                System.out.println("Spine二进制版本：" + spineVersion);
            } else {
                whichVersion(Files.readString(skelFile.toPath()));
                System.out.println("SpineJson版本：" + spineVersion);
            }
        } catch (IOException e) {
            System.out.println("文件读取失败");
            e.printStackTrace();
        }

        if (spineVersion > 38)
            Universal.Range = 2;
        else if (spineVersion < 34)
            Universal.Range = 0;
        else Universal.Range = 1;
    }

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

    public void init() {
        FileHandle handle = new FileHandle(new File(openPath));
        spine.setAtlasFile(atlasFile(handle));
        spine.setSkelFile(handle);

        Platform.runLater(() -> {
            config.samples = 16;
            LwjglApplicationConfiguration.disableAudio = true;
            Atlas.setText("Atlas : " + spine.getAtlasFile().name());
            Skel.setText("Skel : " + handle.name());
        });

        spine.setIsBinary(!handle.extension().equalsIgnoreCase("json") && !handle.extension().equalsIgnoreCase("txt"));
        byte tmp = Universal.Range;
        skelVersion(new File(openPath));
        universal.reload();
        if (isLoad) {
            if (tmp != Universal.Range) {
                gdxApp.exit();
                gdxApp = new LwjglFXApplication(universal, spineRender, config);
            }
        } else {
            gdxApp = new LwjglFXApplication(universal, spineRender, config);
            spineController.isLoaded();
        }
    }
}
