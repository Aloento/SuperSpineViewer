package to.aloen.ssv;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import to.aloen.spine.Spine;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class RecordFX {
    private static final LinkedBlockingQueue<Runnable> savePool = new LinkedBlockingQueue<>() {{
        Thread saving = new Thread(() -> {
            while (true) {
                try {
                    take().run();
                } catch (InterruptedException ignored) {
                }
            }
        }, "Long Saving");
        saving.setDaemon(true);
        saving.start();
    }};

    private static String fileName;

    private static short counter;

    private static short items;

    public static void Record(final PixelReader image) {
        if (Spine.percent < 1) {
            savePool.add(new Task(image, counter++));
            // System.out.println(STR."捕获：\{counter}\t\{Spine.percent}");
        } else {
            Main.recording = false;
            Thread.startVirtualThread(RecordFX::encodeFX);
        }
    }

    public static void Start(String fileName) {
        if (!Main.recording) {
            while (Spine.percent == 2)
                Thread.onSpinWait();

            RecordFX.fileName = fileName;
            Platform.runLater(() -> Main.recording = true);
        }
    }

    public static void Exit() {
        Main.recording = false;
        Spine.speed.set(1);
        Spine.isPlay.set(false);
        savePool.clear();

        counter = 0;
        items = 0;

        System.out.println("强制停止");
        System.gc();
    }

    private static void ffmpegFX() {
        try {
            System.out.println("FFmpeg处理开始");
            Files.deleteIfExists(Path.of(STR."\{Main.outPath}\{fileName}.mov"));
            Platform.runLater(() -> Main.progressBar.setProgress(-1));

            if (Runtime.getRuntime().exec(new String[]{
                "ffmpeg", "-r", "60",
                "-i", STR."\{Main.outPath}\{fileName}_Sequence\{File.separator}\{fileName}_%d.png",
                "-c:v", "png", "-pix_fmt", "rgba",
                "-filter:v", STR."\"setpts=\{Main.quality}*PTS\"",
                "-nostdin", "-y", "-loglevel", "quiet",
                STR."\{Main.outPath}\{fileName}.mov"
            }, new String[]{System.getProperty("user.dir")}).waitFor() == 0) {
                File sequence = new File(STR."\{Main.outPath}\{fileName}_Sequence\{File.separator}");

                for (File file : Objects.requireNonNull(sequence.listFiles())) {
                    file.deleteOnExit();
                }

                sequence.deleteOnExit();

                System.out.println("视频导出成功");
            } else
                Platform.runLater(() -> {
                    Main.progressBar.setProgress(0);
                    System.out.println("FFMPEG错误，序列已导出");
                });
        } catch (Exception ignored) {
        }
    }

    private static void encodeFX() {
        Platform.runLater(() -> Main.progressBar.setProgress(-1));
        System.out.println("请求：停止录制");

        ArrayBlockingQueue<Thread> threads = new ArrayBlockingQueue<>(savePool.size());

        while (!savePool.isEmpty())
            threads.add(Thread.startVirtualThread(savePool.poll()));

        while (!threads.isEmpty())
            try {
                threads.poll().join();
            } catch (InterruptedException ignored) {
            }

        if (Main.sequence == Byte.MIN_VALUE)
            ffmpegFX();

        Spine.speed.set(1);
        counter = 0;
        items = 0;

        Platform.runLater(() -> {
            Main.progressBar.setProgress(-1);
            System.out.println("导出结束");
            System.gc();
        });
    }

    private static WritableImage flipY(final PixelReader image) {
        final WritableImage flippedImage = new WritableImage(Main.width, Main.height);
        final PixelWriter pixelWriter = flippedImage.getPixelWriter();

        for (int y = 0; y < Main.height; y++) {
            for (int x = 0; x < Main.width; x++) {
                Color color = image.getColor(x, y);
                pixelWriter.setColor(x, Main.height - 1 - y, color);
            }
        }

        return flippedImage;
    }

    private static void writePNG(final WritableImage image, final short index) {
        try {
            File outputFile = new File(STR."\{Main.outPath}\{fileName}_Sequence\{File.separator}\{fileName}_\{index}.png");
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();

            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (!Main.recording) {
                var percent = (double) items++ / counter;
                Platform.runLater(() -> Main.progressBar.setProgress(percent));
            }

            // System.out.println(STR."保存：\{index}");
        }
    }

    private static class Task implements Runnable {
        private final short counter;

        private PixelReader image;

        private Task(PixelReader image, short counter) {
            this.image = image;
            this.counter = counter;
        }

        @Override
        public void run() {
            final WritableImage flipY = flipY(image);
            image = null;
            writePNG(flipY, counter);
        }
    }
}
