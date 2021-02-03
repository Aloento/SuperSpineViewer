package com.QYun.SuperSpineViewer;

import com.QYun.Spine.SuperSpine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RecordFX extends Main {
    private static volatile boolean recording = false;
    private final ThreadPoolExecutor savePool = new ThreadPoolExecutor(0, 1,
            1L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            (r -> {
                Thread save = new Thread(r, "SavePNG");
                save.setPriority(Thread.MIN_PRIORITY);
                save.setDaemon(true);
                return save;
            }));
    private final SnapshotParameters parameters = new SnapshotParameters();
    private final SuperSpine spine = new SuperSpine();
    private final byte FPS = 60;
    private short counter;
    private String fileName = null;

    public RecordFX() {
        savePool.allowCoreThreadTimeOut(true);
        parameters.setFill(Color.TRANSPARENT);
        System.out.println("SuperSpineViewer已启动");
    }

    private void recorderFX() {
        Thread recodeThread = new Thread("RecordFX_Capturing") {
            @Override
            public void run() {
                new File(outPath + fileName + "_Sequence").mkdirs();
                System.out.println("录制开始");
                do {
                    Platform.runLater(() -> {
                        if (spine.getPercent() < 1) {
                            savePool.submit(new savePNG(spineRender.snapshot(parameters, null), counter++));
                            System.out.println("捕获：" + counter + "\t" + spine.getPercent());
                        } else recording = false;
                    });
                    try {
                        Thread.sleep((1000 / FPS));
                    } catch (InterruptedException ignored) {
                    }
                } while (recording);

                encodeFX();
                savePool.setMaximumPoolSize(Integer.MAX_VALUE);
                savePool.setCorePoolSize(Integer.MAX_VALUE);
            }
        };
        recodeThread.setDaemon(true);
        recodeThread.start();
    }

    public void startRecording(String fileName) {
        if (!recording) {
            this.fileName = fileName;
            savePool.setMaximumPoolSize(perform);
            savePool.setCorePoolSize(perform);
            spine.setPercent(0);
            recording = true;
            recorderFX();
            System.out.println("请求：开始录制");
        }
    }

    private void ffmpegFX() {
        try {
            System.out.println("FFmpeg处理开始");
            new File((outPath + fileName) + ".mov").delete();

            if (Runtime.getRuntime().exec(new String[]{
                    "ffmpeg", "-r", String.valueOf(FPS),
                    "-i", outPath + fileName + "_Sequence" + File.separator + fileName + "_%d.png",
                    "-c:v", "png", "-pix_fmt", "rgba",
                    "-filter:v", "\"setpts=" + quality + "*PTS\"",
                    outPath + fileName + ".mov"
            }, new String[]{System.getProperty("user.dir")}).waitFor() == 0) {
                File sequence = new File(outPath + fileName + "_Sequence" + File.separator);
                for (String file : Objects.requireNonNull(sequence.list()))
                    new File(sequence, file).delete();
                sequence.delete();

                System.out.println("视频导出成功");
            } else Platform.runLater(() -> {
                Main.progressBar.setProgress(0);
                System.out.println("FFMPEG错误，序列已导出");
            });

        } catch (Exception ignored) {
        }
    }

    private void encodeFX() {
        Thread ffmpeg = new Thread("RecordFX_Encoding") {
            @Override
            public void run() {
                System.out.println("请求：停止录制");
                Platform.runLater(() -> Main.progressBar.setProgress(-1));
                while (savePool.getActiveCount() != 0)
                    Thread.onSpinWait();

                if (!sequence)
                    ffmpegFX();

                Platform.runLater(() -> {
                    spine.setSpeed(1);
                    counter = 0;
                    Main.progressBar.setProgress(1);
                    System.out.println("导出结束");
                });

                savePool.setCorePoolSize(0);
                System.gc();
            }
        };
        ffmpeg.setDaemon(true);
        ffmpeg.start();
    }

    private class savePNG implements Runnable {
        private final short index;
        private final Image image;

        private savePNG(Image image, short index) {
            this.image = image;
            this.index = index;
        }

        @Override
        public void run() {
            Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    Color c = image.getPixelReader().getColor(w, h);
                    pixmap.drawPixel(w, h,
                            ((int) (c.getRed() * 255) << 24) | ((int) (c.getGreen() * 255) << 16) |
                                    ((int) (c.getBlue() * 255) << 8) | (int) (c.getOpacity() * 255));
                }
            }

            PixmapIO.writePNG(Gdx.files.absolute(
                    (outPath + fileName + "_Sequence" + File.separator + fileName) + "_" + index + ".png"),
                    pixmap, 9, false);

            System.out.println("保存：" + index);
        }
    }
}
