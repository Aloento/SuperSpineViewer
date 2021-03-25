package com.QYun.SuperSpineViewer.Controller;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class About {
    @FXML
    private StackPane aboutPane;

    @FXML
    private void handleExitButtonClicked() {
        ((Stage) aboutPane.getScene().getWindow()).close();
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
