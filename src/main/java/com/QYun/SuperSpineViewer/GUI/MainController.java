package com.QYun.SuperSpineViewer.GUI;

import com.jfoenix.controls.*;
import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static io.datafx.controller.flow.container.ContainerAnimations.SWIPE_LEFT;

@FXMLController(value = "/UI/Main.fxml", title = "SuperSpineViewer")
public final class MainController {

    @FXML
    private AnchorPane AnchorPane;

    @FXML
    private StackPane titleBurgerContainer;

    @FXML
    private JFXHamburger titleBurger;

    @FXML
    private StackPane optionsBurger;

    @FXML
    private JFXDrawer mainDrawer;

    private JFXPopup toolbarPopup;

    @PostConstruct
    public void init() throws Exception {

        mainDrawer.setOnDrawerOpening(e -> {
            final Transition animation = titleBurger.getAnimation();
            animation.setRate(1);
            animation.play();
        });
        mainDrawer.setOnDrawerClosing(e -> {
            final Transition animation = titleBurger.getAnimation();
            animation.setRate(-1);
            animation.play();
        });
        titleBurgerContainer.setOnMouseClicked(e -> {
            if (mainDrawer.isClosed() || mainDrawer.isClosing()) {
                mainDrawer.open();
            } else {
                mainDrawer.close();
            }
        });

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/MainPopup.fxml"));
        loader.setController(new InputController());
        toolbarPopup = new JFXPopup(loader.load());

        optionsBurger.setOnMouseClicked(e ->
                toolbarPopup.show(optionsBurger,
                        JFXPopup.PopupVPosition.TOP,
                        JFXPopup.PopupHPosition.RIGHT,
                        -12,
                        15));

        ViewFlowContext context = new ViewFlowContext();
        Flow innerFlow = new Flow(SpineController.class);
        final FlowHandler flowHandler = innerFlow.createHandler(context);

        context.register("ContentFlowHandler", flowHandler);
        context.register("ContentFlow", innerFlow);

        final Duration containerAnimationDuration = Duration.millis(320);
        ExtendedAnimatedFlowContainer animatedFlowContainer = new ExtendedAnimatedFlowContainer(containerAnimationDuration, SWIPE_LEFT);

        mainDrawer.setContent(flowHandler.start(animatedFlowContainer));
        context.register("ContentPane", mainDrawer.getContent().get(0));

        Flow exporterFlow = new Flow(ExporterController.class);
        mainDrawer.setSidePane(exporterFlow.start());

    }

    public static final class InputController {

        @FXML
        private JFXListView<?> toolbarPopupList;

        @FXML
        private void mainSubmit() {
            if (toolbarPopupList.getSelectionModel().getSelectedIndex() == 0) {

                AtomicReference<Double> xOffset = new AtomicReference<>((double) 0);
                AtomicReference<Double> yOffset = new AtomicReference<>((double) 0);
                Stage aboutStage = new Stage(StageStyle.TRANSPARENT);
                Parent about = null;
                try {
                    about = FXMLLoader.load(getClass().getResource("/UI/About.fxml"));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Scene aboutScene = new Scene(Objects.requireNonNull(about));
                aboutScene.getRoot().setEffect(new DropShadow(10, Color.rgb(100, 100, 100)));
                aboutScene.setFill(Color.TRANSPARENT);

                aboutStage.setResizable(false);
                aboutStage.setAlwaysOnTop(true);
                aboutStage.setScene(aboutScene);
                aboutStage.show();

                about.setOnMousePressed(event -> {
                    xOffset.set(event.getSceneX());
                    yOffset.set(event.getSceneY());
                    event.consume();
                });
                about.setOnMouseDragged(event -> {
                    aboutStage.setX(event.getScreenX() - xOffset.get());
                    aboutStage.setY(event.getScreenY() - yOffset.get());
                    event.consume();
                });

                System.out.println(
                        """
                                                         ...                                     `
                                               .;$#################@|`                           `
                                           .%###########################&:                       `
                                        .%#################################@:                    `
                                      '&######################################!                  `
                                    `$#############@|'         .;&##############;                `
                                   ;############%.                  ;@###########%.              `
                                  !###########;                       `$##########$`             `
                                 ;##########%. `%%`               `|:   ;##########%.            `
                                `$#########%. '&##&'            .|###!   ;##########;            `
                                :@########@:                   |######!  .%#########|            `
                                ;#########&`                  ;########%. |#########%.           `
                                :#########@:            '`   `$##########%$#########%.           `
                                `$#########%           :$|`  !######################%.           `
                                 ;##########|       `::;`'%%&#######################%.           `
                                  |##########@:   |#################################%.           `
                                   !############|$##################################%.           `
                                    '&##############################################%.           `
                                      ;#######################&''&##@!%########@!%##%.           `
                                        '&###################|:&&:|#@!|@@@@@###@!%##%.           `
                                           '&##############@;;####;;@######&!$#@!%##%.           `
                                               '%#########$:%######%:$#####&!$#@!%##%.           `
                                                       `'::::::::::::::::::::::'.`::`            `""".indent(9));

            }

            if (toolbarPopupList.getSelectionModel().getSelectedIndex() == 1)
                Platform.exit();
        }
    }

}
