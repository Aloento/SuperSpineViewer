package com.QYun.JavaFX;

import javafx.animation.*;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class GiFXTest extends Application {
    Transition anime;

    @Override
    public void start(Stage primaryStage) {
        Label test = new Label("Aloento");
        StackPane stackPane = new StackPane(test);
        Button start = new Button("Start");
        Button stop = new Button("Stop");
        CheckBox export = new CheckBox("isGiFX");

        test.setStyle("-fx-font-size: 30");
        createAnimations(test);
        stackPane.setStyle("-fx-pref-width: 400px; -fx-pref-height: 200px ;-fx-alignment: CENTER");
        VBox vBox = new VBox(10, stackPane, new HBox(30, start, stop, export));

        primaryStage.setScene(new Scene(vBox));
        primaryStage.setTitle("GiFX Test");
        primaryStage.setHeight(400);
        primaryStage.setWidth(400);
        primaryStage.show();

        AtomicBoolean GifFXLock = new AtomicBoolean(false);
        start.setOnAction(actionEvent -> {
            if (GifFXLock.get()) {
                System.out.println("上一次导出未完成");
                return;
            }

            if(export.isSelected()) {
                new Thread("GiFX_Capturing") {
                    @Override
                    public void run() {
                        try {
                            GiFX.Capture(stackPane, (int) anime.getTotalDuration().toMillis(),
                                    30, true, "C:/CaChe/GiFX.gif", GifFXLock);
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

    void createAnimations(Node target) {
        Duration firstDuration = Duration.millis(2000);
        ScaleTransition st = new ScaleTransition(firstDuration, target);
        st.setFromX(0.1);
        st.setToX(1);
        st.setFromY(0.1);
        st.setToY(1);
        st.setInterpolator(Interpolator.LINEAR);

        RotateTransition rt = new RotateTransition(firstDuration.divide(2), target);
        rt.setByAngle(360);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.setCycleCount(2);

        FadeTransition ft = new FadeTransition(Duration.millis(300), target);
        ft.setFromValue(1);
        ft.setToValue(0.1);
        ft.setCycleCount(2);
        ft.setAutoReverse(true);

        ParallelTransition pt = new ParallelTransition(st, rt);
        anime = new SequentialTransition(pt, ft);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
