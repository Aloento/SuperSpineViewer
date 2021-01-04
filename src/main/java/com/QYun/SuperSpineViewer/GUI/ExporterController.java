package com.QYun.SuperSpineViewer.GUI;

import com.QYun.SuperSpineViewer.RuntimesLoader;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
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
    void B_Export(ActionEvent event) {

    }

    @FXML
    public void B_Preview(ActionEvent actionEvent) {

    }

    @FXML
    void B_Open(ActionEvent event) {
        RuntimesLoader runtimesLoader = new RuntimesLoader();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Skeleton");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Skeleton File", "*.json", "*.skel", "*.txt", "*.bytes")
        );

        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            if (runtimesLoader.init(file))
                System.out.println("初始化成功");
        }
    }

    @FXML
    void B_Path(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Sava Location");
        File direc = chooser.showDialog(new Stage());
        path = direc.getAbsolutePath() + File.separator;
        T_Path.setText(path);
    }

    @FXML
    void RB_GIF(ActionEvent event) {
        format = 2;
    }

    @FXML
    void RB_LibGDX(ActionEvent event) {
        isFX = false;
    }

    @FXML
    void RB_MOV(ActionEvent event) {
        format = 1;
    }

    @FXML
    void RB_OpenJFX(ActionEvent event) {
        isFX = true;
    }

    @FXML
    void RB_Sequence(ActionEvent event) {
        format = 3;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

}
