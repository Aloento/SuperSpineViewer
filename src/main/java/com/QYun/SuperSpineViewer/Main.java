package com.QYun.SuperSpineViewer;

import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglFXApplication;
import com.gluonhq.charm.glisten.visual.Swatch;
import com.gluonhq.charm.glisten.visual.GlistenStyleClasses;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class Main extends Application {
    private final CountDownLatch runningLatch = new CountDownLatch(1);
    LwjglFXApplication gdxApp;
    GUI controller = null;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SuperSpineViewer - QYun_SoarTeam");
        primaryStage.setResizable(false);

        VBox vBox = null;
        try {
            FXMLLoader fxml = new FXMLLoader(getClass().getClassLoader().getResource("SSVGUI.fxml"));
            vBox = fxml.load();
            controller = fxml.getController();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("错误：加载布局失败");
            Platform.exit();
        }

        Scene scene = new Scene(Objects.requireNonNull(vBox));
        Swatch.LIGHT_BLUE.assignTo(scene);
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(windowEvent -> {
            windowEvent.consume();
            runningLatch.countDown();
        });

        ImageView LibGDX = Objects.requireNonNull(controller).LibGDX;
        new Thread("LibGDX Render")
        {
            @Override
            public void run()
            {
                System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true");
                LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
                // config.samples = 4;
                gdxApp = new LwjglFXApplication(new Frostl38Test(), LibGDX, config, controller);
            }
        }.start();

    }

    public static void main(String[] args) {
        launch(args);
    }

}
