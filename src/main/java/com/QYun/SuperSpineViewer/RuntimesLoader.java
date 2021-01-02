package com.QYun.SuperSpineViewer;

import com.QYun.Spine.FrostlTest;
import com.QYun.SuperSpineViewer.GUI.Controller;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglFXApplication;
import com.badlogic.gdx.files.FileHandle;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RuntimesLoader extends Controller {

    static String[] extraSuffixes = {"", ".txt", ".bytes"};
    static String[] dataSuffixes = {".json", ".skel"};
    static String[] atlasSuffixes = {".atlas", "-pro.atlas", "-ess.atlas"};
    public AtomicInteger spineVersion = new AtomicInteger();
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
            gdxApp = new LwjglFXApplication(new FrostlTest(), spineRender, config, new Controller());
        });
    }

    private void binaryVersion(File skelFile) {
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
            else System.out.println("Spine二进制版本判断失败");

            System.out.println("Spine二进制版本：" + spineVersion.get());
        } catch (IOException e) {
            System.out.println("Spine二进制读取失败");
            e.printStackTrace();
        }
    }

    private void jsonVersion(File skelFile) {

        isBinary.set(false);
    }

    public void init(File skelFile) {
        this.skelFile = new FileHandle(new File(skelFile.getAbsolutePath()));
        String extension = this.skelFile.extension();

        if (extension.equalsIgnoreCase("json") || extension.equalsIgnoreCase("txt"))
            jsonVersion(skelFile);
        else binaryVersion(skelFile);
    }

}
