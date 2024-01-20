package to.aloen.ssv.controller;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import to.aloen.spine.Spine;
import to.aloen.ssv.Loader;
import to.aloen.ssv.Main;
import to.aloen.ssv.RecordFX;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ExporterController implements Initializable {
    @FXML
    private JFXCheckBox L_RenderA;

    @FXML
    private JFXCheckBox L_BatchA;

    @FXML

    private Label L_Version;

    @FXML

    private Label skeletonName;

    @FXML

    private Label atlasName;

    @FXML

    private Label L_FPS;

    @FXML

    private JFXTextField T_Path;

    @FXML
    private JFXProgressBar P_Export;

    @FXML
    void B_Export() {
        if (Main.outPath != null && Spine.animate.get() != null) {
            Spine.isPlay.set(false);
            Spine.isLoop.set(false);
            Spine.percent = 2;
            Spine.speed.set(Main.quality);
            Spine.isPlay.set(true);

            System.out.println("请求：开始录制");
            RecordFX.Start(STR."\{Spine.projectName.get()}_\{Spine.animate.get()}");
        }
    }

    @FXML
    void B_Open() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Skeleton");
        File last = new File(Main.Pref.get("lastOpen", System.getProperty("user.home")));

        if (!last.canRead())
            last = new File(System.getProperty("user.home"));

        fileChooser.setInitialDirectory(last);

        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Skeleton File", "*.json", "*.skel", "*.txt", "*.bytes")
        );

        File file = fileChooser.showOpenDialog(new Stage());

        if (file != null) {
            Main.openPath = file.getAbsolutePath();
            Loader.init();

            if (Main.isLoad)
                System.out.println("请求重载");
            else
                System.out.println("请求初始化");

            Main.Pref.put("lastOpen", file.getParent());
        }
    }

    @FXML
    void B_Path() {
        Platform.runLater(() -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File last = new File(Main.outPath);

            if (!last.canRead())
                last = new File(System.getProperty("user.home"));

            chooser.setInitialDirectory(last.getParentFile());
            chooser.setTitle("Save Location");

            Main.outPath = chooser.showDialog(new Stage()).getAbsolutePath() + File.separator;
            T_Path.setText(Main.outPath);
            Main.Pref.put("lastSave", Main.outPath);
        });
    }

    @FXML
    void RB_S() {
        Main.quality = 0.5f;
    }

    @FXML
    void RB_E() {
        Main.quality = 0.25f;
    }

    @FXML
    void RB_F() {
        Main.quality = 1f;
    }

    @FXML
    void RB_MOV() {
        Main.sequence = Byte.MIN_VALUE;
    }

    @FXML
    void RB_Sequence() {
        Main.sequence = Byte.MAX_VALUE;
    }

    @FXML
    void RenderA() {
        Spine.renderA.set(!Spine.renderA.get());
    }

    @FXML
    void BatchA() {
        Spine.batchA.set(!Spine.batchA.get());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Main.FPS = L_FPS;
        Main.SkeletonName = skeletonName;
        Main.AtlasName = atlasName;
        Main.progressBar = P_Export;
        T_Path.setText(Main.outPath);

        Spine.spineVersion.addListener(
            (_, _, newValue) -> Platform.runLater(
                () -> L_Version.setText(STR."Version : \{newValue}")
            ));

        Spine.renderA.addListener(
            (_, _, newValue) -> Platform.runLater(
                () -> L_RenderA.setSelected(newValue)
            ));

        Spine.batchA.addListener(
            (_, _, newValue) -> Platform.runLater(
                () -> L_BatchA.setSelected(newValue)
            ));

        System.out.println("SuperSpineViewer已启动");

        if (Main.openPath != null) {
            Platform.runLater(() -> {
                Loader.init();
                System.out.println("从命令行加载");
            });
        }
    }
}
