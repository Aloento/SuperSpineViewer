package com.QYun.SuperSpineViewer.GUI;

import com.jfoenix.controls.*;
import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import javax.annotation.PostConstruct;

import static io.datafx.controller.flow.container.ContainerAnimations.SWIPE_LEFT;

@FXMLController(value = "/UI/Main.fxml", title = "SuperSpineViewer")
public final class MainController {

    @FXMLViewFlowContext
    private ViewFlowContext context;

    @FXML
    private AnchorPane AnchorPane;

    @FXML
    private StackPane titleBurgerContainer;

    @FXML
    private JFXHamburger titleBurger;

    @FXML
    private JFXRippler optionsRippler;

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

        context = new ViewFlowContext();
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

                StackPane aboutPane = new StackPane();
                aboutPane.setStyle("-fx-pref-height: 240; -fx-pref-width: 420; -fx-background-color: transparent; -fx-padding: 10px");

                Scene aboutScene = new Scene(aboutPane);


                System.out.println(
                        "                                  ...                                     `\n" +
                        "                        .;$#################@|`                           `\n" +
                        "                    .%###########################&:                       `\n" +
                        "                 .%#################################@:                    `\n" +
                        "               '&######################################!                  `\n" +
                        "             `$#############@|'         .;&##############;                `\n" +
                        "            ;############%.                  ;@###########%.              `\n" +
                        "           !###########;                       `$##########$`             `\n" +
                        "          ;##########%. `%%`               `|:   ;##########%.            `\n" +
                        "         `$#########%. '&##&'            .|###!   ;##########;            `\n" +
                        "         :@########@:                   |######!  .%#########|            `\n" +
                        "         ;#########&`                  ;########%. |#########%.           `\n" +
                        "         :#########@:            '`   `$##########%$#########%.           `\n" +
                        "         `$#########%           :$|`  !######################%.           `\n" +
                        "          ;##########|       `::;`'%%&#######################%.           `\n" +
                        "           |##########@:   |#################################%.           `\n" +
                        "            !############|$##################################%.           `\n" +
                        "             '&##############################################%.           `\n" +
                        "               ;#######################&''&##@!%########@!%##%.           `\n" +
                        "                 '&###################|:&&:|#@!|@@@@@###@!%##%.           `\n" +
                        "                    '&##############@;;####;;@######&!$#@!%##%.           `\n" +
                        "                        '%#########$:%######%:$#####&!$#@!%##%.           `\n" +
                        "                                `'::::::::::::::::::::::'.`::`            `");

            }

            if (toolbarPopupList.getSelectionModel().getSelectedIndex() == 1)
                Platform.exit();
        }
    }

}
