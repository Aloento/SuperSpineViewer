package com.QYun.JavaFX;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.jcodec.api.awt.AWTSequenceEncoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RecordFX {

    private final Node node;
    private final AtomicInteger durationS = new AtomicInteger(0);
    public RecordFX (Node node, int durationS) {
        this.node = node;
        this.durationS.set(durationS);
        this.initStorages();
        System.out.println("录制实例已创建");
    }

    private List<Image> recordFrames;
    private ArrayList<BufferedImage> imageFrames;
    private void initStorages () {
        recordFrames = new LinkedList<>();
        imageFrames = new ArrayList<>();
    }

    private void resetStorages () {
        if (recordFrames != null)
            recordFrames.clear();
        if (imageFrames != null)
            imageFrames.clear();
    }

    private void addFrame (Image... frames) {
        if (frames.length > 1) {
            recordFrames.addAll(Arrays.asList(frames));
            System.out.println("添加帧：" + frames.length);
        }
        else {
            recordFrames.add(frames[0]);
            System.out.println("捕获帧");
        }
    }

    private final SnapshotParameters parameters = new SnapshotParameters();
    private synchronized Image createFrame () {
        WritableImage imgShot = new WritableImage((int)node.getBoundsInParent().getWidth(), (int)node.getBoundsInParent().getHeight());
        node.snapshot(parameters, imgShot);
        return imgShot;
    }

    private final AtomicBoolean allowRecording = new AtomicBoolean(false);
    private final AtomicInteger timer = new AtomicInteger(0);
    private final AtomicReference<Float> FPS = new AtomicReference<>(60.0f);
    private void recordingTimer () {
        timer.getAndIncrement();
        if (allowRecording.get() && timer.get() >= FPS.get()) {
            durationS.getAndDecrement();
            timer.set(0);

            if (durationS.get() <= 0) {
                durationS.set(0);
                stopRecording();
            }
        }
        System.out.println("计时器：" + timer.get() + "\t" + durationS.get());
    }

    private void recorderFX () {
        Task<Void> recorderTask = new Task<>() {
            @Override
            protected Void call() throws InterruptedException {
                System.out.println("录制开始");
                parameters.setFill(Color.TRANSPARENT);
                while (durationS.get() > 0) {
                    Platform.runLater(() -> {
                        if (allowRecording.get() && durationS.get() > 0) {
                            addFrame(createFrame());
                        }
                        recordingTimer();
                    });
                    Thread.sleep((long) (1000 / FPS.get()));
                }
                return null;
            }
        };
        Thread recodeThread = new Thread(recorderTask);
        recodeThread.setName("RecordFX_Capturing");
        recodeThread.setDaemon(true);
        recodeThread.start();
    }

    public void startRecording () {
        if (!allowRecording.get()) {
            System.out.println("开始录制请求");
            allowRecording.set(true);
            resetStorages ();
            recorderFX ();
        }
    }

    private final String rootPath = "C:/CaChe/CaChe/";
    private final String fileName = "Record1";
    private final String videoPath = rootPath + fileName;
    private void saveRecordFX () {
        Task<Void> saveRecordTask = new Task<>() {
            @Override
            protected Void call() {
                File root = new File(rootPath);
                File video = new File(videoPath + ".mp4");
                video.delete();

                try {
                    root.mkdirs();
                    AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(video, FPS.get().intValue());
                    imageFrames.forEach((image -> {
                        try {
                            encoder.encodeImage(image);
                            System.out.println("编码帧：" + counter.getAndDecrement());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }));
                    encoder.finish();
                    System.out.println("保存录像成功");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        Thread saveRecordThread = new Thread(saveRecordTask);
        saveRecordThread.setName("RecordFX_RecordSaving");
        saveRecordThread.setDaemon(true);
        saveRecordThread.start();
    }

    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicBoolean saveSequence = new AtomicBoolean(true);
    private void saveToArray (Image image) {
        counter.getAndIncrement();
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        imageFrames.add(bufferedImage);

        if (saveSequence.get()) {
            File video = new File((rootPath + fileName) + "_" + counter + ".png");
            try {
                ImageIO.write(bufferedImage, "png", video);
                System.out.println("保存序列：" + counter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else System.out.println("缓存：" + counter.get());
    }

    private void encodeFX () {
        Task<Void> saveVideoTask = new Task<>() {
            @Override
            protected Void call() {
                for (Image recordFrame : recordFrames)
                    saveToArray(recordFrame);
                if (!saveSequence.get())
                    saveRecordFX();
                System.out.println("开始编码");
                return null;
            }
        };
        Thread saveVideoThread = new Thread(saveVideoTask);
        saveVideoThread.setName("RecordFX_Encoding");
        saveVideoThread.setDaemon(true);
        saveVideoThread.start();
    }

    public void stopRecording () {
        if (allowRecording.get()) {
            System.out.println("停止录制请求");
            allowRecording.set(false);
            encodeFX ();
        }
    }

}
