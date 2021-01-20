package com.QYun.SuperSpineViewer;

import com.QYun.Spine.SuperSpine;
import com.QYun.SuperSpineViewer.GUI.Controller;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class RecordFX {
    private static volatile boolean exporting = false;
    private final Node node;
    private final SnapshotParameters parameters = new SnapshotParameters();
    private final SimpleListProperty<Image> recordFrames = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final SuperSpine spine = new SuperSpine();
    private final byte FPS = 60;
    private boolean recording = false;
    private boolean saveSequence = true;
    private short timer;
    private short counter;
    private String rootPath = null;
    private String fileName = null;

    public RecordFX(Node node) {
        this.node = node;
        parameters.setFill(Color.TRANSPARENT);
        System.out.println("SuperSpineViewer已启动");
        recordFrames.addListener((InvalidationListener) observable -> {
            if (!exporting) {
                exporting = true;
                new Thread("RecordFX_Saving") {
                    @Override
                    public void run() {
                        new File(rootPath + "Sequence" + File.separator).mkdirs();
                        while (recordFrames.size() != 0) {
                            saveToArray(recordFrames.get(0));
                            recordFrames.remove(0);
                        }
                        exporting = false;
                    }
                }.start();
            }
        });
    }

    private void recorderFX() {
        Thread recodeThread = new Thread("RecordFX_Capturing") {
            @Override
            public void run() {
                System.out.println("录制开始");
                do {
                    Platform.runLater(() -> {
                        if (spine.getPercent() <= 1) {
                            recordFrames.add(node.snapshot(parameters, null));
                            System.out.println("捕获的帧：" + timer++ + "\t" + spine.getPercent());
                        }
                    });
                    try {
                        Thread.sleep((1000 / FPS));
                    } catch (InterruptedException ignored) {
                    }
                } while (spine.getPercent() < 1);

                if (recording) {
                    encodeFX();
                    recording = false;
                    System.out.println("请求：停止录制");
                }
            }
        };
        recodeThread.setDaemon(true);
        recodeThread.start();
    }

    public void startRecording(String rootPath, String fileName, boolean saveSequence) {
        this.rootPath = rootPath;
        this.fileName = fileName;
        this.saveSequence = saveSequence;

        if (!recording) {
            recordFrames.clear();
            recorderFX();
            recording = true;
            System.out.println("请求：开始录制");
        }
    }

    private void saveToArray(Image image) {
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png",
                    new File((rootPath + "Sequence" + File.separator + fileName) + "_" + counter++ + ".png"));
        } catch (IOException e) {
            System.out.println("保存PNG文件失败");
            e.printStackTrace();
        }
        Platform.runLater(() -> {
            System.out.println("保存序列：" + counter);
            if (!spine.isIsPlay())
                Controller.progressBar.setProgress(((double) counter / (double) timer));
        });
    }

    private void ffmpegFX() {
        try {
            Platform.runLater(() -> {
                System.out.println("FFMPEG处理开始，请确保已安装");
                Controller.progressBar.setProgress(-1);
            });
            new File((rootPath + fileName) + ".mov").delete();

            if (Runtime.getRuntime().exec(new String[]{
                    "ffmpeg", "-r", String.valueOf(FPS),
                    "-i", rootPath + "Sequence" + File.separator + fileName + "_%d.png",
                    "-c:v", "png", "-pix_fmt", "rgba",
                    "-filter:v", "\"setpts=0.5*PTS\"",
                    rootPath + fileName + ".mov"
            }, new String[]{System.getProperty("user.dir")}).waitFor() == 0) {
                File sequence = new File(rootPath + "Sequence" + File.separator);
                for (String file : Objects.requireNonNull(sequence.list()))
                    new File(sequence, file).delete();
                sequence.delete();
                Platform.runLater(() -> {
                    Controller.progressBar.setProgress(1);
                    System.out.println("视频导出成功");
                });
            } else Platform.runLater(() -> {
                Controller.progressBar.setProgress(0);
                System.out.println("FFMPEG错误，序列已导出");
            });

        } catch (Exception ignored) {
        }
    }

    private void encodeFX() {
        Thread saveVideoThread = new Thread("RecordFX_Encoding") {
            @Override
            public void run() {
                new File(rootPath + "Sequence" + File.separator).mkdirs();
                while (exporting)
                    Thread.onSpinWait();

                while (recordFrames.size() != 0) {
                    saveToArray(recordFrames.get(0));
                    recordFrames.remove(0);
                }
                if (!saveSequence)
                    ffmpegFX();

                Platform.runLater(() -> {
                    spine.setSpeed(1);
                    timer = 0;
                    counter = 0;
                    System.out.println("导出结束");
                });
                System.gc();
            }
        };
        saveVideoThread.setDaemon(true);
        saveVideoThread.start();
    }
}
