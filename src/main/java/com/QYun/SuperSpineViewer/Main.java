package com.QYun.SuperSpineViewer;

import com.QYun.SuperSpineViewer.GUI.MainController;
import com.jfoenix.assets.JFoenixResources;
import com.jfoenix.controls.JFXDecorator;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.container.DefaultFlowContainer;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.paint.Paint;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

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
        Label icon = new Label();
        FontIcon fontIcon = new FontIcon();
        fontIcon.setIconLiteral("fas-draw-polygon");
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(Paint.valueOf("WHITE"));
        icon.setGraphic(fontIcon);
        decorator.setGraphic(icon);

        double width;
        double height;
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
        icon.requestFocus();

    }

}
