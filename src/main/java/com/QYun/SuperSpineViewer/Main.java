package com.QYun.SuperSpineViewer;

import com.badlogic.gdx.backends.lwjgl.LwjglFXGraphics;
import com.badlogic.gdx.backends.lwjgl.LwjglFXNode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class Main extends Application {
    private final CountDownLatch runningLatch = new CountDownLatch(1);
    LwjglFXNode node;
    GUI controller = null;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SuperSpineViewer - QYun_SoarTeam");

        final Rectangle2D screenRange = Screen.getPrimary().getVisualBounds();
        if (screenRange.getWidth() < primaryStage.getWidth() ||
                screenRange.getHeight() < primaryStage.getHeight())
        {
            primaryStage.setX(screenRange.getMinX());
            primaryStage.setY(screenRange.getMinY());

            primaryStage.setWidth(screenRange.getWidth());
            primaryStage.setHeight(screenRange.getHeight());
        }

        VBox vBox = null;
        try {
            FXMLLoader fxml = new FXMLLoader(getClass().getClassLoader().getResource("GUI.fxml"));
            vBox = fxml.load();
            controller = fxml.getController();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("错误：加载布局失败");
            Platform.exit();
        }

        Scene scene = new Scene(Objects.requireNonNull(vBox));
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(windowEvent -> {
            windowEvent.consume();
            runningLatch.countDown();
        });

        ImageView imgView1 = Objects.requireNonNull(controller).imgView1;

        new Thread("libGDX Render")
        {
            @Override
            public void run()
            {
                node = new LwjglFXNode(new SimpleTest(), imgView1);
                controller.graphics1 = (LwjglFXGraphics) node.getGraphics();
                updateFPS();
                node.runSingleThread(runningLatch);

                Platform.runLater(primaryStage::close);
            }
        }.start();
    }

    void updateFPS()
    {
        final Runnable runnable1 = () -> controller.fpsLabel.setText("FPS: " + controller.graphics1.getFramesPerSecond());
        controller.graphics1.setFPSListener(fps -> Platform.runLater(runnable1));
        node.postRunnable(() -> controller.runGears());
    }

    public static void main(String[] args) {
        launch(args);
    }

}
