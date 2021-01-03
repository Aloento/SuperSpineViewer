package com.QYun.SuperSpineViewer;

import com.QYun.Spine.*;
import com.QYun.SuperSpineViewer.GUI.Controller;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglFXApplication;
import com.badlogic.gdx.files.FileHandle;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class RuntimesLoader extends Controller {

    private final String[] extraSuffixes = {"", ".txt", ".bytes"};
    private final String[] dataSuffixes = {"", ".json", ".skel"};
    private final String[] atlasSuffixes = {".atlas", "-pro.atlas", "-ess.atlas", "-pma.atlas"};
    LwjglFXApplication gdxApp;
    LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
    private int spineVersion;
    private boolean isBinary = true;
    private boolean isLoad = false;

    private boolean binaryVersion(File skelFile) {
        try {
            String fistLine = new BufferedReader(new FileReader(skelFile)).readLine();
            System.out.println(fistLine);

            if (fistLine.contains("4.0"))
                spineVersion = 40;
            else if (fistLine.contains("3.8"))
                spineVersion = 38;
            else if (fistLine.contains("3.7"))
                spineVersion = 37;
            else if (fistLine.contains("3.6"))
                spineVersion = 36;
            else if (fistLine.contains("3.5"))
                spineVersion = 35;
            else if (fistLine.contains("3.4"))
                spineVersion = 34;
            else if (fistLine.contains("3.1"))
                spineVersion = 31;
            else if (fistLine.contains("2.1"))
                spineVersion = 21;
            else {
                System.out.println("Spine二进制版本判断失败");
                return false;
            }

            System.out.println("Spine二进制版本：" + spineVersion);
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
                spineVersion = 40;
            else if (json.contains("\"spine\": \"3.8"))
                spineVersion = 38;
            else if (json.contains("\"spine\": \"3.7"))
                spineVersion = 37;
            else if (json.contains("\"spine\": \"3.6"))
                spineVersion = 36;
            else if (json.contains("\"spine\": \"3.5"))
                spineVersion = 35;
            else if (json.contains("\"spine\": \"3.4"))
                spineVersion = 34;
            else if (json.contains("\"spine\": \"3.1"))
                spineVersion = 31;
            else if (json.contains("\"spine\": \"2.1"))
                spineVersion = 21;
            else {
                System.out.println("SpineJson版本判断失败");
                return false;
            }

            System.out.println("SpineJson版本：" + spineVersion);
            isBinary = false;
        } catch (IOException e) {
            System.out.println("SpineJson读取失败");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean initLibDGX(FileHandle skelFile, FileHandle atlasFile) {
        try {
            switch (spineVersion) {
                case 40 -> gdxApp = new LwjglFXApplication(new Spine40(), spineRender, config);
                case 38 -> gdxApp = new LwjglFXApplication(new Spine38(skelFile, atlasFile, isBinary), spineRender, config);
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
            isLoad = true;
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
        if (isLoad)
            gdxApp.exit();

        FileHandle skelFile = new FileHandle(new File(file.getAbsolutePath()));
        FileHandle atlasFile = atlasFile(skelFile);
        String extension = skelFile.extension();

        if (extension.equalsIgnoreCase("json") || extension.equalsIgnoreCase("txt")) {
            if (jsonVersion(file))
                return initLibDGX(skelFile, atlasFile);
        } else {
            if (binaryVersion(file))
                return initLibDGX(skelFile, atlasFile);
        }

        return false;
    }

}
