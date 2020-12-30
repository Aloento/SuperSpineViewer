package com.QYun.SuperSpineViewer.GUI;

import io.datafx.controller.FXMLController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

@FXMLController(value = "/UI/Spine.fxml", title = "SpineController")
public class SpineController implements Initializable {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private StackPane root;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }
}
