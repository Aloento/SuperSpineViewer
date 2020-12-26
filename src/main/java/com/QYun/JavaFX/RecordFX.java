package com.QYun.JavaFX;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RecordFX {

    Node node;
    AtomicInteger duration = new AtomicInteger(0);
    public RecordFX (Node node, int duration) {
        this.node = node;
        this.duration.set(duration);
        initStorages();
    }

    List<ImageView> videoFrames;
    List<Image> recordFrames;
    List<byte[]> tmpFrames;
    private void initStorages () {
        videoFrames = new LinkedList<>();
        recordFrames = new LinkedList<>();
        tmpFrames = new LinkedList<>();
    }

    private void resetStorages () {
        if (videoFrames != null)
            videoFrames.clear();
        if (recordFrames != null)
            recordFrames.clear();
        if (tmpFrames != null)
            tmpFrames.clear();
    }

    private void addFrame (Image... frame) {
        if (frame.length > 1)
            recordFrames.addAll(Arrays.asList(frame));
        else
            recordFrames.add(frame[0]);

    }

    SnapshotParameters parameters = new SnapshotParameters();
    private synchronized Image createFrame () {
        parameters.setFill(Color.TRANSPARENT);
        WritableImage imgShot = new WritableImage((int)node.getBoundsInParent().getWidth(), (int)node.getBoundsInParent().getHeight());
        node.snapshot(parameters, imgShot);
        return imgShot;
    }

    AtomicInteger timer = new AtomicInteger(0);
    Float frameRate = 15.0f;
    private void recordingTimer () {
        timer.getAndIncrement();
        if (recordingLock.get() && timer.get() >= frameRate) {
            duration.getAndDecrement();
            timer.set(0);

            if (duration.get() <= 0)
                duration.set(0);
        }
    }

    AtomicLong FPS = new AtomicLong((long) (1000f/frameRate));
    private void recorderFX () {
        Task<Void> recorderTask = new Task<>() {
            @Override
            protected Void call() throws InterruptedException {
                while (true) {
                    Platform.runLater(() -> {
                        if (recordingLock.get() && duration.get() > 0)
                            addFrame(createFrame());
                        recordingTimer();
                    });
                    Thread.sleep(FPS.get());
                }
            }
        };
        Thread recodeThread = new Thread(recorderTask);
        recodeThread.setName("RecordFX_Capturing");
        recodeThread.setDaemon(true);
        recodeThread.start();
    }

    AtomicBoolean recordingLock = new AtomicBoolean(false);
    public void startRecording () {
        if (!recordingLock.get()) {
            resetStorages ();
            recorderFX ();
            recordingLock.set(true);
        }
    }

    private void processorFX () {
        Task<Void> processTask = new Task<>() {
            @Override
            protected Void call() {
                for (Image recordFrame : recordFrames)
                    videoFrames.add(new ImageView(recordFrame));
                return null;
            }
        };
        Thread processThread = new Thread(processTask);
        processThread.setName("RecordFX_Processing");
        processThread.setDaemon(true);
        processThread.start();
    }

    AtomicInteger counter = new AtomicInteger(0);
    AtomicBoolean saveSequence = new AtomicBoolean(false);
    private void saveToFile (Image image) {
        counter.getAndIncrement();
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        tmpFrames.add(ImageToByte(bufferedImage));

        if (saveSequence.get()) {
            File video = new File();
        }
    }

    String directoryName = "SuperSpineViewer" + File.separator;
    String rootPath = System.getProperty("user.home") + "/Desktop" + File.separator + directoryName;
    String videoName = "SuperSpineViewer_Record_1";
    String videoPath = rootPath + videoName;
    private void saveRecordFX (List<byte[]> saveList) {
        Task<Void> saveRecordTask = new Task<>() {
            @Override
            protected Void call() {
                File root = new File(rootPath);
                File video = new File(videoPath);
                video.delete();

                try {
                    root.mkdirs();
                    FileOutputStream fileStream = new FileOutputStream(videoPath);
                    BufferedOutputStream bufferedStream = new BufferedOutputStream(fileStream);
                    ObjectOutputStream objectStream = new ObjectOutputStream(bufferedStream);

                    objectStream.writeObject(saveList);
                    objectStream.close();
                    fileStream.close();

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

    private void saveVideoFX () {
        File root = new File(rootPath);
        Task<Void> saveVideoTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                root.mkdirs();
                for (Image recordFrame : recordFrames)
                    saveToFile(recordFrame);
                saveRecordFX(tmpFrames);
                return null;
            }
        };
        Thread saveVideoThread = new Thread(saveVideoTask);
        saveVideoThread.setName("RecordFX_VideoSaving");
        saveVideoThread.setDaemon(true);
        saveVideoThread.start();
    }

    public void stopRecording () {
        if (recordingLock.get()) {
            recordingLock.set(false);
            processorFX ();
            saveVideoFX ();
        }
    }

}
