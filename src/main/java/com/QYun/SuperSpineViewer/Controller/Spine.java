package com.QYun.SuperSpineViewer.Controller;

import com.QYun.Spine.SuperSpine;
import com.QYun.Spine.Universal;
import com.QYun.SuperSpineViewer.Loader;
import com.QYun.SuperSpineViewer.Main;
import com.jfoenix.controls.*;
import com.jfoenix.controls.JFXButton.ButtonType;
import com.jfoenix.effects.JFXDepthManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import static javafx.animation.Interpolator.EASE_BOTH;

public class Spine extends Main implements Initializable {
    @FXML
    private BorderPane Viewer;

    @FXML
    private StackPane spinePane;

    @FXML
    private StackPane loadPane;

    @FXML
    private JFXSpinner purple;

    @FXML
    private JFXSpinner blue;

    @FXML
    private JFXSpinner cyan;

    @FXML
    private JFXSpinner green;

    @FXML
    private JFXSpinner yellow;

    @FXML
    private JFXSpinner orange;

    @FXML
    private JFXSpinner red;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SuperSpine spine = new SuperSpine();
        ImageView spineLogo = new ImageView();
        spineLogo.setImage(new Image("/UI/SpineLogo.png", 138, 0, true, true, false));

        StackPane header = new StackPane();
        AtomicReference<String> headerColor = new AtomicReference<>(getDefaultColor((short) ((Math.random() * 12) % 22)));
        header.setStyle("-fx-background-radius: 0 5 0 0; -fx-min-height: 138; -fx-background-color: " + headerColor);

        Label project = new Label("Waiting Loading...");
        project.setStyle("-fx-text-fill: #f1f1f2;");
        project.getStyleClass().add("normal-label");
        header.setAlignment(Pos.BOTTOM_LEFT);
        header.getChildren().addAll(spineLogo, project);
        StackPane.setAlignment(spineLogo, Pos.CENTER);
        spine.projectNameProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> project.setText(newValue)));

        VBox.setVgrow(header, Priority.NEVER);
        StackPane body = new StackPane();
        body.setStyle("-fx-background-radius: 0 0 5 5; -fx-background-color: rgb(255,255,255,0.87);");

        Label L_Scale = new Label("Load Scale");
        L_Scale.getStyleClass().add("normal-label");
        Label L_Width = new Label("Camera Width");
        L_Width.getStyleClass().add("normal-label");
        Label L_Height = new Label("Camera Height");
        L_Height.getStyleClass().add("normal-label");
        Label L_X = new Label("Position X");
        L_X.getStyleClass().add("normal-label");
        Label L_Y = new Label("Position Y");
        L_Y.getStyleClass().add("normal-label");
        Label L_Speed = new Label("Play Speed");
        L_Speed.getStyleClass().add("normal-label");
        Label L_Skins = new Label("Skins");
        L_Skins.getStyleClass().add("normal-label");
        Label L_Animate = new Label("Animations");
        L_Animate.getStyleClass().add("normal-label");
        Label L_Loop = new Label("Loop");
        L_Loop.getStyleClass().add("normal-label");

        JFXTextField T_Scale = new JFXTextField();
        T_Scale.setPromptText("1.0");
        JFXTextField T_Width = new JFXTextField();
        T_Width.setEditable(false);
        JFXTextField T_Height = new JFXTextField();
        T_Height.setEditable(false);
        JFXTextField T_X = new JFXTextField();
        T_X.setPromptText("0.0");
        JFXTextField T_Y = new JFXTextField();
        T_Y.setPromptText("-200");

        JFXSlider S_Speed = new JFXSlider();
        S_Speed.setSnapToTicks(true);
        S_Speed.setShowTickLabels(true);
        S_Speed.setMin(0.25);
        S_Speed.setMax(2.0);
        S_Speed.setMajorTickUnit(0.25);
        S_Speed.setBlockIncrement(0.25);
        S_Speed.setValue(1);

        JFXToggleButton T_Loop = new JFXToggleButton();
        JFXButton B_Reload = new JFXButton("Reload");
        B_Reload.setButtonType(ButtonType.FLAT);
        B_Reload.setStyle("-fx-text-fill:#5264AE;-fx-font-size:14px;");
        JFXButton B_Reset = new JFXButton("Reset");
        B_Reset.setButtonType(ButtonType.FLAT);
        B_Reset.setStyle("-fx-text-fill:#5264AE;-fx-font-size:14px;");

        FlowPane set = new FlowPane(L_Loop, T_Loop, B_Reload, B_Reset);
        set.setMaxWidth(300);
        set.setStyle("-fx-padding: 0 0 0 18;");
        JFXComboBox<String> C_Skins = new JFXComboBox<>();
        JFXComboBox<String> C_Animate = new JFXComboBox<>();

        VBox controller = new VBox(20);
        controller.setPadding(new Insets(14, 16, 20, 16));
        controller.getChildren().addAll(L_Scale, T_Scale,
                L_X, T_X,
                L_Y, T_Y,
                L_Width, T_Width,
                L_Height, T_Height,
                L_Speed, S_Speed, set,
                L_Skins, C_Skins,
                L_Animate, C_Animate);

        ScrollPane scrollPane = new ScrollPane(controller);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        body.getChildren().add(scrollPane);
        VBox content = new VBox();
        content.getChildren().addAll(header, body);

        JFXButton playButton = new JFXButton("");
        playButton.setButtonType(ButtonType.RAISED);
        playButton.setStyle("-fx-background-radius: 40;-fx-background-color: " + getDefaultColor((short) ((Math.random() * 20) % 22)));
        playButton.setPrefSize(56, 56);
        playButton.setRipplerFill(Color.valueOf(headerColor.get()));
        playButton.setScaleX(0);
        playButton.setScaleY(0);

        FontIcon playIcon = new FontIcon();
        playIcon.setIconLiteral("fas-play");
        playIcon.setIconSize(20);
        playIcon.setIconColor(Paint.valueOf("WHITE"));
        FontIcon pauseIcon = new FontIcon();
        pauseIcon.setIconLiteral("fas-pause");
        pauseIcon.setIconSize(20);
        pauseIcon.setIconColor(Paint.valueOf("WHITE"));
        playButton.setGraphic(playIcon);

        playButton.translateYProperty().bind(Bindings.createDoubleBinding(() ->
                header.getBoundsInParent().getHeight() - playButton.getHeight() / 2, header.boundsInParentProperty(), playButton.heightProperty()));
        StackPane.setMargin(playButton, new Insets(0, 26, 0, 0));
        StackPane.setAlignment(playButton, Pos.TOP_RIGHT);

        Timeline animation = new Timeline(new KeyFrame(Duration.millis(240),
                new KeyValue(playButton.scaleXProperty(),
                        1,
                        EASE_BOTH),
                new KeyValue(playButton.scaleYProperty(),
                        1,
                        EASE_BOTH)));
        animation.setDelay(Duration.millis((2000 * Math.random()) + 1000));
        animation.play();

        JFXDepthManager.setDepth(spinePane, 1);
        spinePane.getChildren().addAll(content, playButton);

        spineRender = new ImageView();
        spineRender.setScaleY(-1);

        spineRender.fitWidthProperty().addListener((observable, oldValue, newValue) -> {
            T_Width.setPromptText(String.valueOf(newValue.intValue()));
            width = newValue.intValue();
        });
        spineRender.fitHeightProperty().addListener((observable, oldValue, newValue) -> {
            T_Height.setPromptText(String.valueOf(newValue.intValue()));
            height = newValue.intValue();
        });

        C_Skins.setItems(spine.getSkinsList());
        C_Animate.setItems(spine.getAnimatesList());

        playButton.setOnAction(event -> {
            if (isLoad) {
                if (spine.isIsPlay()) {
                    spine.setIsPlay(false);
                    playButton.setGraphic(playIcon);
                } else {
                    spine.setIsPlay(true);
                    playButton.setGraphic(pauseIcon);
                    headerColor.set(getDefaultColor((short) ((Math.random() * 12) % 22)));
                    header.setStyle("-fx-background-radius: 0 5 0 0; -fx-min-height: 138; -fx-background-color: " + headerColor);
                    playButton.setStyle("-fx-background-radius: 40;-fx-background-color: " + getDefaultColor((short) ((Math.random() * 20) % 22)));
                    playButton.setRipplerFill(Color.valueOf(headerColor.get()));
                }
            }
        });

        spine.isPlayProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                if (newValue) Platform.runLater(() -> playButton.setGraphic(pauseIcon));
                else Platform.runLater(() -> playButton.setGraphic(playIcon));
            }
        });

        T_Scale.setTextFormatter(new TextFormatter<String>(change -> {
            if (change.getText().matches("[0-9]*|\\."))
                return change;
            return null;
        }));
        T_Scale.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER))
                if (T_Scale.getText().matches("^[1-9]\\d*$|^[1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*$"))
                    spine.setScale(Float.parseFloat(T_Scale.getText()));
        });

        T_X.setTextFormatter(new TextFormatter<String>(change -> {
            if (change.getText().matches("[0-9]*|\\.|-"))
                return change;
            return null;
        }));
        T_X.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER))
                if (T_X.getText().matches("-?[0-9]\\d*|-?([1-9]\\d*.\\d*|0\\.\\d*[1-9]\\d*)"))
                    spine.setX(Float.parseFloat(T_X.getText()));
        });

        T_Y.setTextFormatter(new TextFormatter<String>(change -> {
            if (change.getText().matches("[0-9]*|\\.|-"))
                return change;
            return null;
        }));
        T_Y.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER))
                if (T_Y.getText().matches("-?[0-9]\\d*|-?([1-9]\\d*.\\d*|0\\.\\d*[1-9]\\d*)"))
                    spine.setY(Float.parseFloat(T_Y.getText()));
        });

        S_Speed.setValueFactory(slider ->
                Bindings.createStringBinding(
                        () -> ((int) (S_Speed.getValue() * 100) / 100f) + "x",
                        slider.valueProperty()
                )
        );
        S_Speed.valueProperty().addListener((observable, oldValue, newValue) -> spine.setSpeed((Float.parseFloat(String.valueOf(newValue)))));

        T_Loop.setOnAction(event -> spine.setIsLoop(T_Loop.isSelected()));

        B_Reload.setOnAction(event -> {
            Universal.Range = -1;
            new Loader().init();
        });

        B_Reset.setOnAction(event -> {
            spine.setScale(1);
            spine.setX(0);
            spine.setY(-200f);
            spine.setSkin(null);
            spine.setAnimate(null);
            spine.setSpeed(1);
            spine.setIsPlay(false);
            System.gc();
        });

        C_Skins.setOnAction(event -> spine.setSkin(C_Skins.getValue()));

        C_Animate.setOnAction(event -> spine.setAnimate(C_Animate.getValue()));
    }

    public boolean isLoaded() {
        try {
            setProgressAnimate(purple);
            Thread.sleep(100);
            setProgressAnimate(blue);
            Thread.sleep(100);
            setProgressAnimate(cyan);
            Thread.sleep(100);
            setProgressAnimate(green);
            Thread.sleep(100);
            setProgressAnimate(yellow);
            Thread.sleep(100);
            setProgressAnimate(orange);
            Thread.sleep(100);
            setProgressAnimate(red);
            Thread.sleep(1000);
            Timeline paneLine = new Timeline(
                    new KeyFrame(
                            Duration.seconds(1),
                            new KeyValue(loadPane.opacityProperty(), 0)
                    )
            );
            paneLine.play();
            Thread.sleep(800);
            Platform.runLater(() -> {
                loadPane.getChildren().removeAll(purple, blue, cyan, green, yellow, orange, red);
                Viewer.getChildren().remove(loadPane);
                Viewer.setCenter(spineRender);
                spineRender.fitHeightProperty().bind(spineRender.getScene().heightProperty().add(-103));
                spineRender.fitWidthProperty().bind(spineRender.getScene().widthProperty().add(-368));
                Viewer = null;
                loadPane = null;
                purple = null;
                blue = null;
                cyan = null;
                green = null;
                yellow = null;
                orange = null;
                red = null;
            });
            return isLoad = true;
        } catch (InterruptedException ignored) {
            return false;
        }
    }

    private void setProgressAnimate(JFXSpinner spinner) {
        Timeline timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        new KeyValue(spinner.progressProperty(), 1)
                )
        );
        timeline.play();
    }

    private String getDefaultColor(short i) {
        return switch (i) {
            case 0 -> "#455A64";
            case 1 -> "#616161";
            case 2 -> "#512DA8";
            case 3 -> "#5D4037";
            case 4 -> "#9C27B0";
            case 5 -> "#7B1FA2";
            case 6 -> "#673AB7";
            case 7 -> "#7C4DFF";
            case 8 -> "#3F51B5";
            case 9 -> "#536DFE";
            case 10 -> "#2196F3";
            case 11 -> "#448AFF";
            case 12 -> "#0288D1";
            case 13 -> "#00BCD4";
            case 14 -> "#009688";
            case 15 -> "#4CAF50";
            case 16 -> "#689F38";
            case 17 -> "#607D8B";
            case 18 -> "#FFC107";
            case 19 -> "#FF9800";
            case 20 -> "#FF5722";
            case 21 -> "#795548";
            case 22 -> "#9E9E9E";
            default -> "#FFFFFF";
        };
    }
}
