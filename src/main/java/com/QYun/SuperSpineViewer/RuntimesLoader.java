package com.QYun.SuperSpineViewer;

import com.QYun.Spine.*;
import com.QYun.SuperSpineViewer.GUI.Controller;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglFXApplication;
import com.badlogic.gdx.files.FileHandle;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class RuntimesLoader extends Controller {

    private static LwjglFXApplication gdxApp;
    private final String[] extraSuffixes = {"", ".txt", ".bytes"};
    private final String[] dataSuffixes = {"", ".json", ".skel"};
    private final String[] atlasSuffixes = {".atlas", "-pro.atlas", "-ess.atlas", "-pma.atlas"};
    private final SimpleIntegerProperty spineVersion = new SimpleIntegerProperty(0);
    LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
    private boolean shouldReload = false;

    private boolean binaryVersion(File skelFile) {
        try {
            String fistLine = new BufferedReader(new FileReader(skelFile)).readLine();
            System.out.println(fistLine);

            if (fistLine.contains("4.0"))
                spineVersion.set(40);
            else if (fistLine.contains("3.8"))
                spineVersion.set(38);
            else if (fistLine.contains("3.7"))
                spineVersion.set(37);
            else if (fistLine.contains("3.6"))
                spineVersion.set(36);
            else if (fistLine.contains("3.5"))
                spineVersion.set(35);
            else if (fistLine.contains("3.4"))
                spineVersion.set(34);
            else if (fistLine.contains("3.1"))
                spineVersion.set(31);
            else if (fistLine.contains("2.1"))
                spineVersion.set(21);
            else {
                System.out.println("Spine二进制版本判断失败");
                return false;
            }

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
            String json = FileUtils.readFileToString(skelFile, "UTF-8");

            if (json.contains("\"spine\": \"4.0"))
                spineVersion.set(40);
            else if (json.contains("\"spine\": \"3.8"))
                spineVersion.set(38);
            else if (json.contains("\"spine\": \"3.7"))
                spineVersion.set(37);
            else if (json.contains("\"spine\": \"3.6"))
                spineVersion.set(36);
            else if (json.contains("\"spine\": \"3.5"))
                spineVersion.set(35);
            else if (json.contains("\"spine\": \"3.4"))
                spineVersion.set(34);
            else if (json.contains("\"spine\": \"3.1"))
                spineVersion.set(31);
            else if (json.contains("\"spine\": \"2.1"))
                spineVersion.set(21);
            else {
                System.out.println("SpineJson版本判断失败");
                return false;
            }

            System.out.println("SpineJson版本：" + spineVersion.get());
            SuperSpine.isBinary = false;
        } catch (IOException e) {
            System.out.println("SpineJson读取失败");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean initLibDGX() {
        try {
            switch (spineVersion.get()) {
                case 40 -> gdxApp = new LwjglFXApplication(new Spine40(), spineRender, config);
                case 38 -> gdxApp = new LwjglFXApplication(new Spine38(), spineRender, config);
                case 37 -> gdxApp = new LwjglFXApplication(new Spine37(), spineRender, config);
                case 36 -> gdxApp = new LwjglFXApplication(new Spine36(), spineRender, config);
                case 35 -> gdxApp = new LwjglFXApplication(new Spine35(), spineRender, config);
                case 34 -> gdxApp = new LwjglFXApplication(new Spine34(), spineRender, config);
                case 31 -> gdxApp = new LwjglFXApplication(new Spine31(), spineRender, config);
                case 21 -> gdxApp = new LwjglFXApplication(new Spine21(), spineRender, config);
                default -> {
                    return false;
                }
            }
            isLoad.set(true);
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
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
        if (!isLoad.get()) {
            spineVersion.addListener((observable, oldValue, newValue) -> {
                if (!newValue.equals(oldValue)) {
                    shouldReload = true;
                    if (gdxApp != null)
                        gdxApp.exit();
                }
            });
        }

        FileHandle skelFile = new FileHandle(new File(file.getAbsolutePath()));
        SuperSpine.atlasFile = atlasFile(skelFile);
        SuperSpine.skelFile = skelFile;
        String extension = skelFile.extension();

        if (!requestReload || shouldReload) {
            if (extension.equalsIgnoreCase("json") || extension.equalsIgnoreCase("txt")) {
                if (jsonVersion(file))
                    return initLibDGX();
            } else {
                if (binaryVersion(file))
                    return initLibDGX();
            }
            shouldReload = false;
        } else {
            if (extension.equalsIgnoreCase("json") || extension.equalsIgnoreCase("txt"))
                jsonVersion(file);
            else binaryVersion(file);

            SuperSpine.isReload.set(true);
            requestReload = false;
            return true;
        }
        return false;
    }
}
