package com.QYun.SuperSpineViewer.GUI;

import com.jfoenix.controls.*;
import com.jfoenix.controls.JFXPopup.PopupHPosition;
import com.jfoenix.controls.JFXPopup.PopupVPosition;
import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
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
    private JFXDrawer drawer;

    private JFXPopup toolbarPopup;

    @PostConstruct
    public void init() throws Exception {
        // init the title hamburger icon
        final JFXTooltip burgerTooltip = new JFXTooltip("Open Drawer");

        drawer.setOnDrawerOpening(e -> {
            final Transition animation = titleBurger.getAnimation();
            burgerTooltip.setText("Close Drawer");
            animation.setRate(1);
            animation.play();
        });
        drawer.setOnDrawerClosing(e -> {
            final Transition animation = titleBurger.getAnimation();
            burgerTooltip.setText("Open Drawer");
            animation.setRate(-1);
            animation.play();
        });
        titleBurgerContainer.setOnMouseClicked(e -> {
            if (drawer.isClosed() || drawer.isClosing()) {
                drawer.open();
            } else {
                drawer.close();
            }
        });

        FXMLLoader loader = new FXMLLoader(getClass().getResource("GUI/MainPopup.fxml"));
        loader.setController(new InputController());
        toolbarPopup = new JFXPopup(loader.load());

        optionsBurger.setOnMouseClicked(e ->
                toolbarPopup.show(optionsBurger,
                        PopupVPosition.TOP,
                        PopupHPosition.RIGHT,
                        -12,
                        15));
        JFXTooltip.setVisibleDuration(Duration.millis(3000));
        JFXTooltip.install(titleBurgerContainer, burgerTooltip, Pos.BOTTOM_CENTER);

        // create the inner flow and content
        context = new ViewFlowContext();
        // set the default controller
        Flow innerFlow = new Flow(ButtonController.class);

        final FlowHandler flowHandler = innerFlow.createHandler(context);
        context.register("ContentFlowHandler", flowHandler);
        context.register("ContentFlow", innerFlow);
        final Duration containerAnimationDuration = Duration.millis(320);
        drawer.setContent(flowHandler.start(new ExtendedAnimatedFlowContainer(containerAnimationDuration, SWIPE_LEFT)));
        context.register("ContentPane", drawer.getContent().get(0));

        // side controller will add links to the content flow
        Flow sideMenuFlow = new Flow(SideMenuController.class);
        final FlowHandler sideMenuFlowHandler = sideMenuFlow.createHandler(context);
        drawer.setSidePane(sideMenuFlowHandler.start(new ExtendedAnimatedFlowContainer(containerAnimationDuration,
                SWIPE_LEFT)));
    }

    public static final class InputController {

        @FXML
        private JFXListView<?> toolbarPopupList;

        @FXML
        private void submit() {
            if (toolbarPopupList.getSelectionModel().getSelectedIndex() == 1)
                Platform.exit();
        }
    }

}
