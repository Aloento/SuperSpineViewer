package com.QYun.JavaFX;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class GiFX {

    public static void Capture (Parent target,
                                int durationMS,
                                int timeBetweenFramesMS,
                                boolean loopContinuously,
                                String savePath,
                                AtomicBoolean GifFXLock) throws IOException {

        GifFXLock.set(true);
        ImageOutputStream outputStream = new FileImageOutputStream(new File(savePath));
        GiFXWriter writer = new GiFXWriter(outputStream, 3, timeBetweenFramesMS, loopContinuously);

        Consumer<Event> run = event -> {
            int width = (int) target.getBoundsInParent().getWidth();
            int height = (int) target.getBoundsInParent().getHeight();

            WritableImage image = new WritableImage(width, height);
            target.snapshot(null, image);
            try {
                writer.writeToSequence(SwingFXUtils.fromFXImage(image, null));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        System.out.println("GiFX运行在线程：" + Thread.currentThread().getName());
        final KeyFrame oneFrame = new KeyFrame(Duration.millis(durationMS/(durationMS/timeBetweenFramesMS)), run::accept);
        Timeline timeline = new Timeline(durationMS, oneFrame);
        timeline.setCycleCount(durationMS / timeBetweenFramesMS);

        timeline.setOnFinished(event -> {
            try {
                writer.close();
                outputStream.close();
                GifFXLock.set(false);
                System.out.println("GIF导出成功，位于：" + savePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        timeline.play();

    }
}

