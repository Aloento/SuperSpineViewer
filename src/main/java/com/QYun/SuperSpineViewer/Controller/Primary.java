package com.QYun.SuperSpineViewer.Controller;

import com.QYun.SuperSpineViewer.Main;
import com.jfoenix.controls.JFXDrawer;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXPopup;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class Primary extends Main implements Initializable {
    @FXML
    private StackPane titleBurgerContainer;

    @FXML
    private JFXHamburger titleBurger;

    @FXML
    private StackPane optionsBurger;

    @FXML
    private JFXDrawer mainDrawer;

    private JFXPopup toolbarPopup;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

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
        try {
            toolbarPopup = new JFXPopup(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }

        optionsBurger.setOnMouseClicked(e ->
                toolbarPopup.show(optionsBurger,
                        JFXPopup.PopupVPosition.TOP,
                        JFXPopup.PopupHPosition.RIGHT,
                        -12,
                        15));

        Parent Spine = null;
        try {
            FXMLLoader spineLoader = new FXMLLoader(getClass().getResource("/UI/Spine.fxml"));
            Spine = spineLoader.load();
            spineController = spineLoader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mainDrawer.setContent(Spine);

        Parent Exporter = null;
        try {
            FXMLLoader exporterLoader = new FXMLLoader(getClass().getResource("/UI/Exporter.fxml"));
            Exporter = exporterLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mainDrawer.setSidePane(Exporter);
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
                aboutStage.getIcons().add(new Image("/UI/Q-Audio.png"));
                aboutStage.setTitle("SuperSpineViewer - Aloento_QYun_SoarTeam");
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
                Platform.runLater(() -> System.exit(0));
        }
    }
}
