package to.aloen.ssv;

import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglFXApplication;
import com.badlogic.gdx.files.FileHandle;
import to.aloen.spine.SpineAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

public class Loader extends Main {
    public static byte spineVersion = 0;
    private static LwjglFXApplication gdxApp;
    private final String[] startSuffixes = {"", "-pro", "-ess"};
    private final String[] dataSuffixes = {".json", ".skel"};
    private final String[] endSuffixes = {"", ".txt", ".bytes"};
    private final String[] atlasSuffixes = {".atlas", "-pma.atlas"};
    private final SpineAdapter adapter = new SpineAdapter();

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
            SpineAdapter.Range = 2;
        else if (spineVersion < 34)
            SpineAdapter.Range = 0;
        else SpineAdapter.Range = 1;
    }

    private FileHandle atlasFile(FileHandle skeletonFile) {
        String baseName = skeletonFile.name();
        for (String startSuffix : startSuffixes) {
            for (String endSuffix : endSuffixes) {
                for (String dataSuffix : dataSuffixes) {
                    String suffix = startSuffix + dataSuffix + endSuffix;
                    if (baseName.endsWith(suffix)) {
                        FileHandle file = findAtlasFile(skeletonFile, baseName.substring(0, baseName.length() - suffix.length()));
                        if (file != null) return file;
                    }
                }
            }
        }
        return findAtlasFile(skeletonFile, baseName);
    }

    private FileHandle findAtlasFile(FileHandle skeletonFile, String baseName) {
        for (String startSuffix : startSuffixes) {
            for (String endSuffix : endSuffixes) {
                for (String suffix : atlasSuffixes) {
                    FileHandle file = skeletonFile.sibling(baseName + startSuffix + suffix + endSuffix);
                    if (file.exists()) return file;
                }
            }
        }
        return null;
    }

    public void init() {
        FileHandle handle = new FileHandle(new File(openPath));
        spine.setAtlasFile(atlasFile(handle));
        spine.setSkelFile(handle);

        LwjglApplicationConfiguration.disableAudio = true;
        Atlas.setText("Atlas : " + spine.getAtlasFile().name());
        Skel.setText("Skel : " + handle.name());

        spine.setIsBinary(!handle.extension().equalsIgnoreCase("json") && !handle.extension().equalsIgnoreCase("txt"));
        byte tmp = SpineAdapter.Range;
        skelVersion(new File(openPath));
        adapter.reload();
        if (isLoad) {
            if (tmp != SpineAdapter.Range) {
                gdxApp.exit();
                gdxApp = new LwjglFXApplication(adapter, spineRender);
            }
        } else {
            new Thread(() -> {
                if (spineController.isLoaded()) {
                    gdxApp = new LwjglFXApplication(adapter, spineRender);
                    spineController = null;
                }
            }, "Loading").start();
        }
    }
}
