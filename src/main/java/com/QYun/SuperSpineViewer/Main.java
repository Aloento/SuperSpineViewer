package com.QYun.SuperSpineViewer;

import com.QYun.SuperSpineViewer.GUI.MainController;
import com.jfoenix.assets.JFoenixResources;
import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.svg.SVGGlyph;
import com.jfoenix.svg.SVGGlyphLoader;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.container.DefaultFlowContainer;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        new Thread(() -> {
            try {
                SVGGlyphLoader.loadGlyphsFont(Main.class.getResourceAsStream("/UI/icomoon.svg"),
                        "icomoon.svg");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        Flow flow = new Flow(MainController.class);
        DefaultFlowContainer flowContainer = new DefaultFlowContainer();
        ViewFlowContext flowContext = new ViewFlowContext();
        flowContext.register("Stage", primaryStage);
        try {
            flow.createHandler(flowContext).start(flowContainer);
        } catch (FlowException e) {
            e.printStackTrace();
        }

        JFXDecorator decorator = new JFXDecorator(primaryStage, flowContainer.getView());
        decorator.setCustomMaximize(true);
        decorator.setGraphic(new SVGGlyph(""));

        double width = 1280;
        double height = 720;
        Rectangle2D screenBounds = Screen.getScreens().get(0).getBounds();
        width = screenBounds.getWidth() / 2.5;
        height = screenBounds.getHeight() / 1.35;

        Scene scene = new Scene(decorator, width, height);
        ObservableList<String> styleSheets = scene.getStylesheets();
        styleSheets.addAll(JFoenixResources.load("css/jfoenix-fonts.css").toExternalForm(),
                JFoenixResources.load("css/jfoenix-design.css").toExternalForm(),
                Main.class.getResource("/UI/Main.css").toExternalForm());

        primaryStage.setTitle("SuperSpineViewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

}
