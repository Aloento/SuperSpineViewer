package com.QYun.SuperSpineViewer.Controller;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class About implements Initializable {
    @FXML
    private StackPane aboutPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    private void handleExitButtonClicked() {
        Stage aboutStage = (Stage) aboutPane.getScene().getWindow();
        aboutStage.close();
    }

    @FXML
    private void handleGitButtonClicked() {
        new Application() {
            @Override
            public void start(Stage stage) {
            }
        }.getHostServices().showDocument("https://github.com/soarteam/SuperSpineViewer");
    }
}
