package com.QYun.JavaFX;

import javafx.application.Platform;
import javafx.concurrent.Task;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class RecordFX {

    private final Node node;
    private final SnapshotParameters parameters = new SnapshotParameters();
    private final List<Image> recordFrames;
    private Boolean allowRecording = false;
    private Boolean saveSequence = true;
    private int timer = 0;
    private int counter = 0;
    private float durationS = 0;
    private float FPS = 60f;
    private String rootPath = null;
    private String fileName = null;

    public RecordFX(Node node) {
        this.node = node;
        recordFrames = new LinkedList<>();
        System.out.println("录制实例已创建");
    }

    private void addFrame(Image... frames) {
        if (frames.length > 1) {
            recordFrames.addAll(Arrays.asList(frames));
            System.out.println("添加帧：" + frames.length);
        } else {
            recordFrames.add(frames[0]);
            System.out.println("捕获帧");
        }
    }

    private synchronized Image createFrame() {
        WritableImage imgShot = new WritableImage((int) node.getBoundsInParent().getWidth(), (int) node.getBoundsInParent().getHeight());
        node.snapshot(parameters, imgShot);
        return imgShot;
    }

    private void recordingTimer() {
        timer++;
        if (allowRecording && timer >= FPS) {
            durationS--;
            timer = 0;

            if (durationS <= 0) {
                durationS = 0;
                stopRecording();
            }
        }
        System.out.println("计时器：" + timer + "\t" + durationS);
    }

    private void recorderFX() {
        Task<Void> recorderTask = new Task<>() {
            @Override
            protected Void call() throws InterruptedException {
                System.out.println("录制开始");
                while (durationS > 0) {
                    Platform.runLater(() -> {
                        if (allowRecording && durationS > 0) {
                            addFrame(createFrame());
                        }
                        recordingTimer();
                    });
                    Thread.sleep((long) (1000 / FPS));
                }
                return null;
            }
        };
        Thread recodeThread = new Thread(recorderTask);
        recodeThread.setName("RecordFX_Capturing");
        recodeThread.setDaemon(true);
        recodeThread.start();
    }

    public void startRecording(String rootPath, String fileName, float durationS, Float FPS, boolean saveSequence) {
        this.rootPath = rootPath;
        this.fileName = fileName;
        this.durationS = durationS;
        this.FPS = FPS;
        this.saveSequence = saveSequence;

        parameters.setFill(Color.TRANSPARENT);
        if (!allowRecording) {
            System.out.println("请求：开始录制");
            allowRecording = true;
            recordFrames.clear();
            recorderFX();
        }
    }

    private void saveToArray(Image image) {
        counter++;
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        File video = new File((rootPath + "Sequence/" + fileName) + "_" + counter + ".png");
        try {
            ImageIO.write(bufferedImage, "png", video);
            System.out.println("保存序列：" + counter);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void ffmpegFX() {
        try {
            // new File((rootPath + fileName) + ".webm").delete();
            // Process ffmpeg = Runtime.getRuntime().exec(
            //         "ffmpeg -r " + FPS.get() +
            //                 " -i " + rootPath + fileName + "_%d.png" +
            //                 " -c:v libvpx-vp9 -lossless 1" +
            //                 " -pix_fmt yuva420p -row-mt 1" +
            //                 " " + rootPath + fileName + ".webm");

            System.out.println("FFMPEG处理开始，请确保已安装");
            new File((rootPath + fileName) + ".mov").delete();

            Process ffmpeg = Runtime.getRuntime().exec(
                    "ffmpeg -r " + FPS +
                            " -i " + rootPath + "Sequence/" + fileName + "_%d.png" +
                            " -c:v png" +
                            " -pix_fmt rgba" +
                            " " + rootPath + fileName + ".mov");

            int status = ffmpeg.waitFor();
            if (status == 0) {
                File sequence = new File(rootPath + "Sequence/");
                String[] files = sequence.list();
                for (String file : Objects.requireNonNull(files))
                    new File(sequence, file).delete();
                sequence.delete();
                System.out.println("视频导出成功");
            } else System.out.println("FFMPEG错误，请确保已安装，序列已导出");
            recordFrames.clear();
            System.gc();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void encodeFX() {
        Task<Void> saveVideoTask = new Task<>() {
            @Override
            protected Void call() {
                new File(rootPath + "Sequence/").mkdirs();
                if (!saveSequence) {
                    System.out.println("用ffmpeg编码");
                    for (Image recordFrame : recordFrames)
                        saveToArray(recordFrame);
                    ffmpegFX();
                } else {
                    System.out.println("导出序列");
                    for (Image recordFrame : recordFrames)
                        saveToArray(recordFrame);
                }
                return null;
            }
        };
        Thread saveVideoThread = new Thread(saveVideoTask);
        saveVideoThread.setName("RecordFX_Encoding");
        saveVideoThread.setDaemon(true);
        saveVideoThread.start();
    }

    public void stopRecording() {
        if (allowRecording) {
            System.out.println("请求：停止录制");
            allowRecording = false;
            encodeFX();
        }
    }

}
