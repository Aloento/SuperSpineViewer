package to.aloen.ssv;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import javafx.application.Platform;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RecordFX extends Main {
    private String fileName = null;
    private short counter;
    private short items;

    public void recorderFX(WritableImage image) {
        if (spine.getPercent() < 1) {
            Thread.startVirtualThread(new savePNG(image, counter++));
            // System.out.println("捕获：" + counter + "\t" + spine.getPercent());
        } else {
            recording = false;
            encodeFX();
        }
    }

    public void Start(String fileName) {
        if (!recording) {
            while (spine.getPercent() == 2)
                Thread.onSpinWait();
            this.fileName = fileName;
            recording = true;
        }
    }

    public void Exit() {
        recording = false;
        spine.setSpeed(1);
        spine.setIsPlay(false);
        counter = 0;
        items = 0;
        System.out.println("强制停止");
        System.gc();
    }

    private void ffmpegFX() {
        try {
            System.out.println("FFmpeg处理开始");
            new File((outPath + fileName) + ".mov").delete();
            Platform.runLater(() -> progressBar.setProgress(-1));

            if (Runtime.getRuntime().exec(new String[]{
                    "ffmpeg", "-r", "60",
                    "-i", outPath + fileName + "_Sequence" + File.separator + fileName + "_%d.png",
                    "-c:v", "png", "-pix_fmt", "rgba",
                    "-filter:v", "\"setpts=" + quality + "*PTS\"",
                    "-nostdin", "-y", "-loglevel", "quiet",
                    outPath + fileName + ".mov"
            }, new String[]{System.getProperty("user.dir")}).waitFor() == 0) {
                File sequence = new File(outPath + fileName + "_Sequence" + File.separator);
                for (String file : Objects.requireNonNull(sequence.list()))
                    new File(sequence, file).delete();
                sequence.delete();

                System.out.println("视频导出成功");
            } else Platform.runLater(() -> {
                progressBar.setProgress(0);
                System.out.println("FFMPEG错误，序列已导出");
            });
        } catch (Exception ignored) {
        }
    }

    private void encodeFX() {
        new Thread("RecordFX_Encoding") {
            {
                setDaemon(true);
                start();
            }

            @Override
            public void run() {
                Platform.runLater(() -> progressBar.setProgress(-1));
                System.out.println("请求：停止录制");
                System.gc();

                if (sequence == 0)
                    ffmpegFX();

                Platform.runLater(() -> {
                    spine.setSpeed(1);
                    counter = 0;
                    items = 0;
                    progressBar.setProgress(1);
                    System.out.println("导出结束");
                });
            }
        };
    }

    private class savePNG implements Runnable {
        private final short index;
        private WritableImage image;

        private savePNG(WritableImage image, short index) {
            this.image = image;
            this.index = index;
        }

        @Override
        public void run() {
            PixmapIO.writePNG(Gdx.files.absolute(
                    (outPath + fileName + "_Sequence" + File.separator + fileName) + "_" + index + ".png"),
                    new Pixmap(width, height, Pixmap.Format.RGBA8888) {{
                        for (int h = 0; h < height; h++) {
                            for (int w = 0; w < width; w++) {
                                Color c = image.getPixelReader().getColor(w, h);
                                drawPixel(w, h,
                                        ((int) (c.getRed() * 255) << 24) | ((int) (c.getGreen() * 255) << 16) |
                                                ((int) (c.getBlue() * 255) << 8) | (int) (c.getOpacity() * 255));
                            }
                        }
                    }}, sequence, true);

            image = null;
            items++;
            Platform.runLater(() -> progressBar.setProgress((double) items / counter));
            // System.out.println("保存：" + index);
        }
    }
}
