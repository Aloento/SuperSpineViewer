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
            if (mainDrawer.isClosed() || mainDrawer.isClosing())
                mainDrawer.open();
            else mainDrawer.close();
        });

        try {
            toolbarPopup = new JFXPopup(new FXMLLoader(Primary.this.getClass().getResource("/UI/MainPopup.fxml")) {{
                setController(new InputController());
            }}.load());
            FXMLLoader spineLoader = new FXMLLoader(getClass().getResource("/UI/Spine.fxml"));
            spineController = spineLoader.getController();
            mainDrawer.setContent(spineLoader.<Parent>load());
            mainDrawer.setSidePane(new FXMLLoader(getClass().getResource("/UI/Exporter.fxml")).<Parent>load());
        } catch (IOException e) {
            e.printStackTrace();
        }

        optionsBurger.setOnMouseClicked(e ->
                toolbarPopup.show(optionsBurger,
                        JFXPopup.PopupVPosition.TOP,
                        JFXPopup.PopupHPosition.RIGHT,
                        -12,
                        15));
    }

    public static final class InputController {
        @FXML
        private JFXListView<?> toolbarPopupList;

        @FXML
        private void mainSubmit() {
            if (toolbarPopupList.getSelectionModel().getSelectedIndex() == 0) {
                AtomicReference<Double> xOffset = new AtomicReference<>((double) 0);
                AtomicReference<Double> yOffset = new AtomicReference<>((double) 0);

                try {
                    Parent about = FXMLLoader.load(getClass().getResource("/UI/About.fxml"));
                    Stage aboutStage = new Stage(StageStyle.TRANSPARENT) {{
                        setResizable(false);
                        setAlwaysOnTop(true);
                        setScene(new Scene(about) {{
                            getRoot().setEffect(new DropShadow(10, Color.rgb(100, 100, 100)));
                            setFill(Color.TRANSPARENT);
                        }});
                        getIcons().add(new Image("/UI/Q-Audio.png"));
                        setTitle("SuperSpineViewer - Aloento_QYun_SoarTeam");
                        show();
                    }};
                    about.setOnMousePressed(event -> {
                        xOffset.set(event.getSceneX());
                        yOffset.set(event.getSceneY());
                    });
                    about.setOnMouseDragged(event -> {
                        aboutStage.setX(event.getScreenX() - xOffset.get());
                        aboutStage.setY(event.getScreenY() - yOffset.get());
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

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

            } else System.exit(0);
        }
    }
}
