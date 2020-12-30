package com.QYun.SuperSpineViewer.GUI;

import io.datafx.controller.FXMLController;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

@FXMLController(value = "/UI/Spine.fxml", title = "SpineController")
public class SpineController {

    @FXML
    private StackPane Spine;

    @FXML
    private BorderPane spineBorder;

    @FXML
    void initialize() {

    }
}
