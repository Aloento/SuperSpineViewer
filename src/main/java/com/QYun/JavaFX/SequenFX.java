package com.QYun.JavaFX;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SequenFX {

    public static void Capture (Parent target,
                                int durationMS,
                                int timeBetweenFramesMS,
                                SnapshotParameters parameters,
                                AtomicBoolean SequenFXLock,
                                String savePath) throws IOException {

        SequenFXLock.set(true);
        AtomicInteger index = new AtomicInteger();

        Consumer<Event> run = event -> {
            int width = (int) target.getBoundsInParent().getWidth();
            int height = (int) target.getBoundsInParent().getHeight();

            WritableImage image = new WritableImage(width, height);
            target.snapshot(parameters, image);
            BufferedImage buffer = SwingFXUtils.fromFXImage(image, null);
            try {
                ImageIO.write(buffer, "png", new File(savePath + "SequenFX-" + index.getAndIncrement() + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        System.out.println("SequenFX运行在线程：" + Thread.currentThread().getName());
        final KeyFrame oneFrame = new KeyFrame(Duration.millis(durationMS/(durationMS/timeBetweenFramesMS)), run::accept);
        Timeline timeline = new Timeline(durationMS, oneFrame);
        timeline.setCycleCount(durationMS / timeBetweenFramesMS);

        timeline.setOnFinished(event -> {
            SequenFXLock.set(false);
            System.out.println("序列导出成功，位于：" + savePath);
        });
        timeline.play();

    }
}

