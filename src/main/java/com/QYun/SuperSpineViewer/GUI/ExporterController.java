package com.QYun.SuperSpineViewer.GUI;

import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import io.datafx.controller.FXMLController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

@FXMLController(value = "/UI/Exporter.fxml", title = "SpineExporter")
public class ExporterController {

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
    void B_Open(ActionEvent event) {

    }

    @FXML
    void B_Path(ActionEvent event) {

    }

    @FXML
    void RB_GIF(ActionEvent event) {

    }

    @FXML
    void RB_LibGDX(ActionEvent event) {

    }

    @FXML
    void RB_MOV(ActionEvent event) {

    }

    @FXML
    void RB_OpenJFX(ActionEvent event) {

    }

    @FXML
    void RB_Sequence(ActionEvent event) {

    }

}
