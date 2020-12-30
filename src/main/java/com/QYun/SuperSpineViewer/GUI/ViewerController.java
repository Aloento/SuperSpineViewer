package com.QYun.SuperSpineViewer.GUI;

import com.jfoenix.controls.JFXSpinner;
import io.datafx.controller.FXMLController;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

@FXMLController(value = "/UI/Viewer.fxml", title = "SpineViewer")
public class ViewerController {

    @FXML
    private StackPane Viewer;

    @FXML
    private ImageView SpineRender;

    @FXML
    private StackPane loadPane;

    @FXML
    private JFXSpinner purpleSpinner;

    @FXML
    private JFXSpinner blueSpinner;

    @FXML
    private JFXSpinner cyanSpinner;

    @FXML
    private JFXSpinner greenSpinner;

    @FXML
    private JFXSpinner yellowSpinner;

    @FXML
    private JFXSpinner orangeSpinner;

    @FXML
    private JFXSpinner redSpinner;

    @FXML
    void initialize() {

    }

}
