package com.QYun.JavaFX;

import javafx.animation.Transition;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecordFXTest extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Label test = new Label("Aloento");
        AnchorPane pane = new AnchorPane(test);
        Button start = new Button("Start");
        Button stop = new Button("Stop");
        Button snap = new Button("PNG截图");
        CheckBox export = new CheckBox("isRecordFX");

        test.setStyle("-fx-font-size: 80; -fx-text-fill: #ff0000; -fx-font-weight: BOLD");
        test.setLayoutX(150.0);
        test.setLayoutY(120.0);
        SetAnime setAnime = new SetAnime();
        Transition anime = setAnime.EGAnime(test);
        pane.setStyle("-fx-pref-width: 800px; -fx-pref-height: 400px");
        VBox vBox = new VBox(10, pane, new HBox(30, start, stop, snap, export));

        primaryStage.setScene(new Scene(vBox));
        primaryStage.setTitle("RecordFX Test");
        primaryStage.setHeight(600);
        primaryStage.setWidth(600);
        primaryStage.show();

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);

        AtomicBoolean RecordFXLock = new AtomicBoolean(false);
        RecordFX recordFX = new RecordFX(pane);

        start.setOnAction(actionEvent -> {
            if (RecordFXLock.get()) {
                System.out.println("上一次导出未完成");
                return;
            }

            if (export.isSelected()) {
                recordFX.startRecording("C:/CaChe/CaChe/", "Record", 3, 30f, false);
                anime.playFromStart();
            } else anime.playFromStart();
        });

        stop.setOnAction(actionEvent -> {
            anime.stop();
            if (export.isSelected())
                recordFX.stopRecording();
        });

        snap.setOnAction(actionEvent -> {
            WritableImage png = pane.snapshot(parameters, null);
            BufferedImage buffer = SwingFXUtils.fromFXImage(png, null);
            try {
                ImageIO.write(buffer, "png", new File("C:/CaChe/RecordFX.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }
}
