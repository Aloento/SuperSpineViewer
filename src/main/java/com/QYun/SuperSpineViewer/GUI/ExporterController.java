package com.QYun.SuperSpineViewer.GUI;

import com.QYun.Spine.SuperSpine;
import com.QYun.SuperSpineViewer.RuntimesLoader;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ExporterController extends Controller implements Initializable {

    final RuntimesLoader runtimesLoader = new RuntimesLoader();
    SuperSpine spine = new SuperSpine();
    @FXML
    private StackPane Exporter;
    @FXML
    private Label L_Version;
    @FXML
    private Label L_Skel;
    @FXML
    private Label L_Atlas;
    @FXML
    private Label L_FPS;
    @FXML
    private ToggleGroup Render;
    @FXML
    private ToggleGroup Format;
    @FXML
    private JFXTextField T_Path;
    @FXML
    private JFXProgressBar P_Export;

    @FXML
    void B_Export() {

    }

    @FXML
    public void B_Preview() {

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
            arg = file.getAbsolutePath();
            if (isLoad.get()) {
                requestReload = true;
                if (runtimesLoader.init(file))
                    System.out.println("请求重载成功");
            } else if (runtimesLoader.init(file))
                System.out.println("初始化成功");
        }
    }

    @FXML
    void B_Path() {
        Platform.runLater(() -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Sava Location");
            File direc = chooser.showDialog(new Stage());
            path = direc.getAbsolutePath() + File.separator;
            T_Path.setText(path);
        });
    }

    @FXML
    void RB_GIF() {
        format = 2;
    }

    @FXML
    void RB_LibGDX() {
        isFX = false;
    }

    @FXML
    void RB_MOV() {
        format = 1;
    }

    @FXML
    void RB_OpenJFX() {
        isFX = true;
    }

    @FXML
    void RB_Sequence() {
        format = 3;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        FPS = L_FPS;
        Skel = L_Skel;
        Atlas = L_Atlas;
        progressBar = P_Export;
        spine.spineVersionProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> L_Version.setText("Version : " + newValue)));

        if (arg != null) {
            Platform.runLater(() -> {
                File file = new File(arg);
                if (runtimesLoader.init(file)) {
                    System.out.println("初始化成功");
                    arg = null;
                }
            });
        }
    }

}
