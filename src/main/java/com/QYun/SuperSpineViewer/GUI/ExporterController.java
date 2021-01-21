package com.QYun.SuperSpineViewer.GUI;

import com.QYun.Spine.SuperSpine;
import com.QYun.SuperSpineViewer.RecordFX;
import com.QYun.SuperSpineViewer.RuntimesLoader;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ExporterController extends Controller implements Initializable {
    private final RuntimesLoader runtimesLoader = new RuntimesLoader();
    private final SuperSpine spine = new SuperSpine();
    private final RecordFX recordFX = new RecordFX();

    @FXML
    private Label L_Version;
    @FXML
    private Label L_Skel;
    @FXML
    private Label L_Atlas;
    @FXML
    private Label L_FPS;
    @FXML
    private JFXTextField T_Path;
    @FXML
    private JFXProgressBar P_Export;

    @FXML
    void B_Export() {
        if (outPath != null && spine.getAnimate() != null) {
            spine.setIsPlay(false);
            spine.setIsLoop(false);
            spine.setSpeed(quality);
            recordFX.startRecording(spine.getProjectName() + "_" + spine.getAnimate());
            spine.setIsPlay(true);
        }
    }

    @FXML
    void B_Open() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Skeleton");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Skeleton File", "*.json", "*.skel", "*.txt", "*.bytes")
        );

        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            openPath = file.getAbsolutePath();
            runtimesLoader.init();
            if (isLoad) System.out.println("请求重载");
            else System.out.println("请求初始化");
        }
    }

    @FXML
    void B_Path() {
        Platform.runLater(() -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Sava Location");
            File direc = chooser.showDialog(new Stage());
            outPath = direc.getAbsolutePath() + File.separator;
            T_Path.setText(outPath);
        });
    }

    @FXML
    void RB_S() {
        quality = 0.5f;
    }

    @FXML
    void RB_E() {
        quality = 0.25f;
    }

    @FXML
    void RB_F() {
        quality = 1f;
    }

    @FXML
    void RB_N() {
        perform = 5;
    }

    @FXML
    void RB_H() {
        perform = 10;
    }

    @FXML
    void RB_L() {
        perform = 2;
    }

    @FXML
    void RB_MOV() {
        sequence = false;
    }

    @FXML
    void RB_Sequence() {
        sequence = true;
    }

    @FXML
    void PreA() {
        preA = !preA;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FPS = L_FPS;
        Skel = L_Skel;
        Atlas = L_Atlas;
        progressBar = P_Export;
        spine.spineVersionProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> L_Version.setText("Version : " + newValue)));

        if (openPath != null) {
            Platform.runLater(() -> {
                runtimesLoader.init();
                System.out.println("从命令行加载");
            });
        }
    }
}
