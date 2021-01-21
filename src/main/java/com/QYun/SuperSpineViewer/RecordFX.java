package com.QYun.SuperSpineViewer;

import com.QYun.Spine.SuperSpine;
import com.QYun.SuperSpineViewer.GUI.Controller;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RecordFX extends Controller {
    private static volatile boolean recording = false;
    private final Node node;
    private final ThreadPoolExecutor savePool = new ThreadPoolExecutor(0, 1,
            0L, TimeUnit.MILLISECONDS,
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

    public RecordFX(Node node) {
        this.node = node;
        parameters.setFill(Color.TRANSPARENT);
        System.out.println("SuperSpineViewer已启动");
    }

    private void recorderFX() {
        Thread recodeThread = new Thread("RecordFX_Capturing") {
            @Override
            public void run() {
                new File(outPath + "Sequence").mkdirs();
                System.out.println("录制开始");
                do {
                    Platform.runLater(() -> {
                        if (spine.getPercent() < 1) {
                            savePool.submit(new savePNG(node.snapshot(parameters, null), counter++));
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
                System.out.println("请求：停止录制");
            }
        };
        recodeThread.setDaemon(true);
        recodeThread.start();
    }

    public void startRecording(String fileName) {
        this.fileName = fileName;

        if (!recording) {
            spine.setPercent(0);
            recording = true;
            recorderFX();
            System.out.println("请求：开始录制");
        }
    }

    private void ffmpegFX() {
        try {
            new File((outPath + fileName) + ".mov").delete();

            if (Runtime.getRuntime().exec(new String[]{
                    "ffmpeg", "-r", String.valueOf(FPS),
                    "-i", outPath + "Sequence" + File.separator + fileName + "_%d.png",
                    "-c:v", "png", "-pix_fmt", "rgba",
                    "-filter:v", "\"setpts=0.5*PTS\"",
                    outPath + fileName + ".mov"
            }, new String[]{System.getProperty("user.dir")}).waitFor() == 0) {
                File sequence = new File(outPath + "Sequence" + File.separator);
                for (String file : Objects.requireNonNull(sequence.list()))
                    new File(sequence, file).delete();
                sequence.delete();

                System.out.println("视频导出成功");
            } else Platform.runLater(() -> {
                Controller.progressBar.setProgress(0);
                System.out.println("FFMPEG错误，序列已导出");
            });

        } catch (Exception ignored) {
        }
    }

    private void encodeFX() {
        Thread ffmpeg = new Thread("RecordFX_Encoding") {
            @Override
            public void run() {
                Platform.runLater(() -> Controller.progressBar.setProgress(-1));
                while (savePool.getActiveCount() != 0)
                    Thread.onSpinWait();

                if (!sequence)
                    ffmpegFX();

                Platform.runLater(() -> {
                    spine.setSpeed(1);
                    counter = 0;
                    Controller.progressBar.setProgress(1);
                    System.out.println("导出结束");
                });

                savePool.setCorePoolSize(0);
                savePool.setMaximumPoolSize(1);
                System.gc();
            }
        };
        ffmpeg.setDaemon(true);
        ffmpeg.start();
    }

    private class savePNG implements Runnable {
        private final short index;
        private Image image;

        private savePNG(Image image, short index) {
            this.image = image;
            this.index = index;
        }

        @Override
        public void run() {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png",
                        new File((outPath + "Sequence" + File.separator + fileName) + "_" + index + ".png"));
                image = null;
                System.out.println("保存：" + index);
            } catch (IOException e) {
                System.out.println("保存PNG文件失败");
                e.printStackTrace();
            }
        }
    }
}
