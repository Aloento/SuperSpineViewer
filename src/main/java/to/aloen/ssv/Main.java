package to.aloen.ssv;

import com.jfoenix.controls.JFXProgressBar;
import javafx.application.Application;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import to.aloen.spine.BaseSpine;
import to.aloen.ssv.controller.Launcher;
import to.aloen.ssv.controller.Spine;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.zip.Deflater;

public class Main {
    public static final BaseSpine spine = new BaseSpine();
    public static final Preferences Pref = Preferences.userRoot().node("/to/aloen/ssv");
    public static RecordFX recordFX;
    public static Spine spineController;
    public static JFXProgressBar progressBar;
    public static ImageView spineRender;
    public static Label FPS;
    public static Label Skel;
    public static Label Atlas;
    public static int width;
    public static int height;
    public static byte sequence = Deflater.BEST_COMPRESSION;
    public static float quality = 0.5f;
    public static boolean isLoad = false;
    public static boolean preA = true;
    public static boolean recording = false;
    public static String openPath;
    public static String outPath = Pref.get("lastSave", System.getProperty("user.home"));

    public static void main(String[] args) {
        org.burningwave.core.assembler.StaticComponentContainer.Modules.exportAllToAll();

        if (args.length > 0) {
            if (args[0].equals("reset")) {
                try {
                    Pref.clear();
                } catch (BackingStoreException ignored) {
                }
            } else openPath = args[0];
        }
        Application.launch(Launcher.class, args);
    }
}
