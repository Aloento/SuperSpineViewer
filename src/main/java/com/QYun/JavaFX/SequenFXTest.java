package com.QYun.JavaFX;

import javafx.animation.Transition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SequenFXTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label test = new Label("Aloento");
        AnchorPane pane = new AnchorPane(test);
        Button start = new Button("Start");
        Button stop = new Button("Stop");
        CheckBox export = new CheckBox("isSequenFX");

        test.setStyle("-fx-font-size: 40; -fx-text-fill: #ff0000; -fx-font-weight: BOLD");
        test.setLayoutX(120.0);
        test.setLayoutY(60.0);
        SetAnime setAnime = new SetAnime();
        Transition anime = setAnime.EGAnime(test);
        pane.setStyle("-fx-pref-width: 400px; -fx-pref-height: 200px");
        VBox vBox = new VBox(10, pane, new HBox(30, start, stop, export));

        primaryStage.setScene(new Scene(vBox));
        primaryStage.setTitle("SequenFX Test");
        primaryStage.setHeight(400);
        primaryStage.setWidth(400);
        primaryStage.show();

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);

        AtomicBoolean SequenFXLock = new AtomicBoolean(false);
        start.setOnAction(actionEvent -> {
            if (SequenFXLock.get()) {
                System.out.println("上一次导出未完成");
                return;
            }

            if(export.isSelected()) {
                new Thread("SequenFX_Capturing") {
                    @Override
                    public void run() {
                        try {
                            SequenFX.Capture(pane, (int) anime.getTotalDuration().toMillis() + 100,
                                    30, parameters, SequenFXLock, "C:/CaChe/CaChe/");
                            anime.playFromStart();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
            else anime.playFromStart();
        });
        stop.setOnAction(actionEvent -> anime.stop());

    }

    public static void main(String[] args) {
        launch(args);
    }
}
