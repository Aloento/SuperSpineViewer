package com.QYun.SuperSpineViewer.GUI;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements Initializable {

    @FXML
    private BorderPane aboutPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    private void handleExitButtonClicked(ActionEvent event) {
        Stage aboutStage = (Stage) aboutPane.getScene().getWindow();
        aboutStage.close();
        event.consume();
    }

    @FXML
    private void handleGitButtonClicked(ActionEvent event) {
        new Application() {
            @Override
            public void start(Stage stage) {
            }
        }.getHostServices().showDocument("https://github.com/soarteam/SuperSpineViewer");
        event.consume();
    }
}
