package com.QYun.JavaFX;

import com.QYun.Spine.SuperSpine;
import com.QYun.SuperSpineViewer.GUI.Controller;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class RecordFX {

    private static volatile boolean exporting = false;
    private final Node node;
    private final SnapshotParameters parameters = new SnapshotParameters();
    private final ObservableList<Image> framesList = FXCollections.observableArrayList();
    private final SimpleListProperty<Image> recordFrames = new SimpleListProperty<>(framesList);
    private final SuperSpine spine = new SuperSpine();
    private boolean recording = false;
    private boolean saveSequence = true;
    private int timer = 0;
    private int counter = 0;
    private float FPS = 60f;
    private String rootPath = null;
    private String fileName = null;

    public RecordFX(Node node) {
        this.node = node;
        parameters.setFill(Color.TRANSPARENT);
        System.out.println("录制实例已创建");
        recordFrames.addListener((InvalidationListener) observable -> {
            if (!exporting) {
                exporting = true;
                new Thread("RecordFX_Saving") {
                    @Override
                    public void run() {
                        new File(rootPath + "Sequence/").mkdirs();
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

    private void addFrame() {
        WritableImage imgShot = new WritableImage((int) node.getBoundsInParent().getWidth(), (int) node.getBoundsInParent().getHeight());
        node.snapshot(parameters, imgShot);
        recordFrames.add(imgShot);
    }

    private void recorderFX() {
        Thread recodeThread = new Thread("RecordFX_Capturing") {
            @Override
            public void run() {
                System.out.println("录制开始");
                do {
                    Platform.runLater(() -> {
                        if (spine.getPercent() <= 1)
                            addFrame();
                        System.out.println("捕获的帧：" + timer++ + "\t" + spine.getPercent());
                    });
                    try {
                        Thread.sleep((long) (1000 / FPS));
                    } catch (InterruptedException ignored) {
                    }
                } while (spine.getPercent() < 1);
                System.out.println("停止录制");
                stopRecording();
            }
        };
        recodeThread.setDaemon(true);
        recodeThread.start();
    }

    public void startRecording(String rootPath, String fileName, Float FPS, boolean saveSequence) {
        this.rootPath = rootPath;
        this.fileName = fileName;
        this.FPS = FPS;
        this.saveSequence = saveSequence;

        if (!recording) {
            recordFrames.clear();
            recorderFX();
            recording = true;
            System.out.println("请求：开始录制");
        }
    }

    private void saveToArray(Image image) {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        File video = new File((rootPath + "Sequence/" + fileName) + "_" + counter++ + ".png");
        try {
            ImageIO.write(bufferedImage, "png", video);
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

            Process ffmpeg = Runtime.getRuntime().exec(
                    "ffmpeg -r " + FPS +
                            " -i " + rootPath + "Sequence/" + fileName + "_%d.png" +
                            " -c:v png" +
                            " -pix_fmt rgba" +
                            " -filter:v \"setpts=0.5*PTS\"" +
                            " " + rootPath + fileName + ".mov");

            int status = ffmpeg.waitFor();
            if (status == 0) {
                File sequence = new File(rootPath + "Sequence/");
                String[] files = sequence.list();
                for (String file : Objects.requireNonNull(files))
                    new File(sequence, file).delete();
                sequence.delete();
                System.out.println("视频导出成功");
            } else System.out.println("FFMPEG错误，序列已导出");

        } catch (Exception ignored) {
        }
    }

    private void encodeFX() {
        Thread saveVideoThread = new Thread("RecordFX_Encoding") {
            @Override
            public void run() {
                new File(rootPath + "Sequence/").mkdirs();
                while (exporting)
                    Thread.onSpinWait();
                while (recordFrames.size() != 0) {
                    saveToArray(recordFrames.get(0));
                    recordFrames.remove(0);
                }
                if (!saveSequence)
                    ffmpegFX();
                spine.setSpeed(1);
                System.gc();
                System.out.println("导出结束");
            }
        };
        saveVideoThread.setDaemon(true);
        saveVideoThread.start();
    }

    public void stopRecording() {
        if (recording) {
            encodeFX();
            recording = false;
            System.out.println("请求：停止录制");
        }
    }

}
