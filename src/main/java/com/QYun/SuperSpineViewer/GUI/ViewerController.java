package com.QYun.SuperSpineViewer.GUI;

import com.jfoenix.controls.JFXSpinner;
import io.datafx.controller.FXMLController;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.util.Duration;

import javax.annotation.PostConstruct;

@FXMLController(value = "/UI/Viewer.fxml", title = "SpineViewer")
public class ViewerController {

    @FXML
    private JFXSpinner blueSpinner;
    @FXML
    private JFXSpinner greenSpinner;

    @PostConstruct
    public void init() {
        Timeline timeline = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(blueSpinner.progressProperty(), 0),
                new KeyValue(greenSpinner.progressProperty(), 0)
            ),
            new KeyFrame(
                Duration.seconds(0.5),
                new KeyValue(greenSpinner.progressProperty(), 0.5)
            ),
            new KeyFrame(
                Duration.seconds(2),
                new KeyValue(blueSpinner.progressProperty(), 1),
                new KeyValue(greenSpinner.progressProperty(), 1)
            )
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }


}
