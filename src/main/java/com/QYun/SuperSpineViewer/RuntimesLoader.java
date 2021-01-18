package com.QYun.SuperSpineViewer;

import com.QYun.Spine.*;
import com.QYun.SuperSpineViewer.GUI.Controller;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglFXApplication;
import com.badlogic.gdx.files.FileHandle;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

public class RuntimesLoader extends Controller {

    public static final SimpleIntegerProperty spineVersion = new SimpleIntegerProperty(0);
    private final LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
    private final String[] extraSuffixes = {"", ".txt", ".bytes"};
    private final String[] dataSuffixes = {"", ".json", ".skel"};
    private final String[] atlasSuffixes = {".atlas", "-pro.atlas", "-ess.atlas", "-pma.atlas"};
    private final SuperSpine spine = new SuperSpine();

    private void whichVersion(String skel) {
        if (skel.contains("4.0."))
            spineVersion.set(40);
        else if (skel.contains("3.8."))
            spineVersion.set(38);
        else if (skel.contains("3.7."))
            spineVersion.set(37);
        else if (skel.contains("3.6."))
            spineVersion.set(36);
        else if (skel.contains("3.5."))
            spineVersion.set(35);
        else if (skel.contains("3.4.") || skel.contains("3.3."))
            spineVersion.set(34);
        else if (skel.contains("3.2."))
            spineVersion.set(32);
        else if (skel.contains("3.1."))
            spineVersion.set(31);
        else if (skel.contains("2.1."))
            spineVersion.set(21);
    }

    private boolean binaryVersion(File skelFile) {
        try {
            whichVersion(new BufferedReader(new FileReader(skelFile)).readLine());
            if (spineVersion.get() < 20) {
                System.out.println("Spine二进制版本判断失败");
                return false;
            }

            spine.setIsBinary(true);
            System.out.println("Spine二进制版本：" + spineVersion.get());
        } catch (IOException e) {
            System.out.println("Spine二进制读取失败");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean jsonVersion(File skelFile) {
        try {
            whichVersion(Files.readString(skelFile.toPath()));
            if (spineVersion.get() < 20) {
                System.out.println("SpineJson版本判断失败");
                return false;
            }

            spine.setIsBinary(false);
            System.out.println("SpineJson版本：" + spineVersion.get());
        } catch (IOException e) {
            System.out.println("SpineJson读取失败");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean initLibDGX() {
        config.samples = 16;
        switch (spineVersion.get()) {
            case 40 -> new LwjglFXApplication(new Spine40(), spineRender, config);
            case 38, 37, 36, 35, 34 -> new LwjglFXApplication(new Standard(), spineRender, config);
            case 32 -> new LwjglFXApplication(new Spine32(), spineRender, config);
            case 31 -> new LwjglFXApplication(new Spine31(), spineRender, config);
            case 21 -> new LwjglFXApplication(new Spine21(), spineRender, config);
            default -> {
                return false;
            }
        }
        isLoad.set(true);
        return true;
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

    public boolean init(File file) {
        FileHandle skelFile = new FileHandle(new File(file.getAbsolutePath()));
        spine.setAtlasFile(atlasFile(skelFile));
        spine.setSkelFile(skelFile);
        String extension = skelFile.extension();

        Platform.runLater(() -> {
            Atlas.setText("Atlas : " + spine.getAtlasFile().name());
            Skel.setText("Skel : " + skelFile.name());
        });

        if (!requestReload) {
            if (extension.equalsIgnoreCase("json") || extension.equalsIgnoreCase("txt")) {
                if (jsonVersion(file))
                    return initLibDGX();
            } else {
                if (binaryVersion(file))
                    return initLibDGX();
            }
        } else {
            if (extension.equalsIgnoreCase("json") || extension.equalsIgnoreCase("txt"))
                jsonVersion(file);
            else binaryVersion(file);

            new SuperSpine().setIsReload(true);
            requestReload = false;
            return true;
        }
        return false;
    }
}
