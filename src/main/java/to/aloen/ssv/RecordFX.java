package to.aloen.ssv;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import javafx.application.Platform;
import javafx.scene.image.WritableImage;
import to.aloen.spine.Spine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

public class RecordFX {
    private static final LinkedBlockingQueue<Runnable> savePool = new LinkedBlockingQueue<>();

    private static String fileName = null;

    private static short counter;

    private static short items;

    public void recorderFX(WritableImage image) {
        if (Spine.percent < 1) {
            savePool.add(new savePNG(image, counter++));
            // System.out.println("捕获：" + counter + "\t" + spine.getPercent());
        } else {
            Main.recording = false;
            new Thread(this::encodeFX).start();
        }
    }

    public void Start(String fileName) {
        if (!Main.recording) {
            while (Spine.percent == 2)
                Thread.onSpinWait();

            RecordFX.fileName = fileName;
            Main.recording = true;
        }
    }

    public void Exit() {
        Main.recording = false;
        Spine.speed.set(1);
        Spine.isPlay.set(false);
        counter = 0;
        items = 0;
        System.out.println("强制停止");
        System.gc();
    }

    private void ffmpegFX() {
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

    private void encodeFX() {
        Platform.runLater(() -> Main.progressBar.setProgress(-1));
        System.out.println("请求：停止录制");

        ArrayList<Thread> threads = new ArrayList<>();

        for (Runnable runnable : savePool)
            threads.add(Thread.startVirtualThread(runnable));

        for (Thread thread : threads)
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }

        System.gc();

        if (Main.sequence == Byte.MIN_VALUE)
            ffmpegFX();

        Platform.runLater(() -> {
            Spine.speed.set(1);
            counter = 0;
            items = 0;
            Main.progressBar.setProgress(1);
            System.out.println("导出结束");
        });
    }

    private class savePNG implements Runnable {
        private final short index;
        private final WritableImage image;

        private savePNG(WritableImage image, short index) {
            this.image = image;
            this.index = index;
        }

        @Override
        public void run() {
            PixmapIO.writePNG(Gdx.files.absolute(STR."\{Main.outPath}\{fileName}_Sequence\{File.separator}\{fileName}_\{index}.png"),
                new Pixmap(Main.width, Main.height, Pixmap.Format.RGBA8888) {{
                    for (int h = 0; h < Main.height; h++) {
                        for (int w = 0; w < Main.width; w++) {
                            int argb = image.getPixelReader().getArgb(w, h);
                            drawPixel(w, h, (argb << 8) | (argb >>> 24));
                        }
                    }
                }}, Main.sequence, true);

            var percent = (double) items++ / counter;
            Platform.runLater(() -> Main.progressBar.setProgress(percent));
            // System.out.println("保存：" + index);
        }
    }
}
