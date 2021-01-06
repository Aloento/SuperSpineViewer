package com.QYun.SuperSpineViewer.GUI;

import com.QYun.Spine.SuperSpine;
import com.QYun.SuperSpineViewer.RuntimesLoader;
import com.jfoenix.controls.*;
import com.jfoenix.controls.JFXButton.ButtonType;
import com.jfoenix.effects.JFXDepthManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.embed.swing.SwingFXUtils;
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
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import static javafx.animation.Interpolator.EASE_BOTH;

public class SpineController extends Controller implements Initializable {

    @FXML
    private StackPane spinePane;

    @FXML
    private StackPane Spine;

    @FXML
    private StackPane Viewer;

    @FXML
    private ImageView SpineRender;

    @FXML
    private StackPane loadPane;

    @FXML
    private JFXSpinner purpleSpinner;

    @FXML
    private JFXSpinner blueSpinner;

    @FXML
    private JFXSpinner cyanSpinner;

    @FXML
    private JFXSpinner greenSpinner;

    @FXML
    private JFXSpinner yellowSpinner;

    @FXML
    private JFXSpinner orangeSpinner;

    @FXML
    private JFXSpinner redSpinner;

    public SpineController() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ImageView spineLogo = new ImageView();
        BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
        try (InputStream file = getClass().getResourceAsStream("/UI/SpineLogo.svg")) {
            TranscoderInput transIn = new TranscoderInput(file);
            try {
                transcoder.transcode(transIn, null);
                Image svg = SwingFXUtils.toFXImage(transcoder.getBufferedImage(), null);
                spineLogo.setImage(svg);
            } catch (TranscoderException ex) {
                ex.printStackTrace();
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
        spineLogo.setFitHeight(138);
        spineLogo.setPreserveRatio(true);
        spineLogo.setSmooth(true);

        StackPane header = new StackPane();
        AtomicReference<String> headerColor = new AtomicReference<>(getDefaultColor((int) ((Math.random() * 12) % 22)));
        header.setStyle("-fx-background-radius: 0 5 0 0; -fx-background-color: " + headerColor);

        HBox hBox = new HBox(8);
        hBox.setPadding(new Insets(0, 0, 0, 105));
        hBox.getChildren().addAll(spineLogo);
        Label project = new Label("Waiting Loading...");
        project.setStyle("-fx-text-fill: #f1f1f2;");
        project.getStyleClass().add("normal-label");
        header.setAlignment(Pos.BOTTOM_LEFT);
        header.getChildren().addAll(hBox, project);
        SuperSpine.projectName.addListener((observable, oldValue, newValue) -> Platform.runLater(() -> project.setText(newValue)));

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
        T_Scale.setPromptText("2.0");
        JFXTextField T_Width = new JFXTextField();
        JFXTextField T_Height = new JFXTextField();
        JFXTextField T_X = new JFXTextField();
        T_X.setPromptText("0.0");
        JFXTextField T_Y = new JFXTextField();
        T_Y.setPromptText("-200");

        JFXSlider S_Speed = new JFXSlider();
        S_Speed.setSnapToTicks(true);
        S_Speed.setShowTickLabels(true);
        S_Speed.setMin(0.5);
        S_Speed.setMax(2.5);
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
                L_Width, T_Width,
                L_Height, T_Height,
                L_X, T_X,
                L_Y, T_Y,
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
        playButton.setStyle("-fx-background-radius: 40;-fx-background-color: " + getDefaultColor((int) ((Math.random() * 20) % 22)));
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

        spineRender = SpineRender;
        Platform.runLater(() -> {
            SpineRender.fitHeightProperty().bind(SpineRender.getScene().heightProperty().add(-103));
            SpineRender.fitWidthProperty().bind(SpineRender.getScene().widthProperty().add(-368));
        });
        SpineRender.fitWidthProperty().addListener((observable, oldValue, newValue) -> T_Width.setPromptText(String.valueOf(newValue.intValue())));
        SpineRender.fitHeightProperty().addListener((observable, oldValue, newValue) -> T_Height.setPromptText(String.valueOf(newValue.intValue())));

        SuperSpine spine = new SuperSpine();
        C_Skins.setItems(spine.getSkinsList());
        C_Animate.setItems(spine.getAnimatesList());

        playButton.setOnAction(event -> {
            if (isLoad.get()) {
                if (spine.isIsPlay()) {
                    spine.setIsPlay(false);
                    playButton.setGraphic(playIcon);
                } else {
                    spine.setIsPlay(true);
                    playButton.setGraphic(pauseIcon);
                    headerColor.set(getDefaultColor((int) ((Math.random() * 12) % 22)));
                    header.setStyle("-fx-background-radius: 0 5 0 0; -fx-background-color: " + headerColor);
                    playButton.setStyle("-fx-background-radius: 40;-fx-background-color: " + getDefaultColor((int) ((Math.random() * 20) % 22)));
                    playButton.setRipplerFill(Color.valueOf(headerColor.get()));
                }
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

        T_Width.setTextFormatter(new TextFormatter<String>(change -> {
            if (change.getText().matches("[0-9]*"))
                return change;
            return null;
        }));
        T_Width.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER))
                if (T_Width.getText().matches("^[1-9]\\d*$"))
                    width = Integer.parseInt(T_Width.getText());
        });

        T_Height.setTextFormatter(new TextFormatter<String>(change -> {
            if (change.getText().matches("[0-9]*"))
                return change;
            return null;
        }));
        T_Height.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER))
                if (T_Height.getText().matches("^[1-9]\\d*$"))
                    height = Integer.parseInt(T_Height.getText());
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

        B_Reload.setOnAction(event -> RuntimesLoader.spineVersion.set(-1));

        B_Reset.setOnAction(event -> {

        });

        C_Skins.setOnAction(event -> spine.setSkin(C_Skins.getValue()));

        C_Animate.setOnAction(event -> spine.setAnimate(C_Animate.getValue()));

        isLoad.addListener((observable, oldValue, newValue) -> new Thread(() -> {
            if (newValue) {
                try {
                    setProgressAnimate(purpleSpinner);
                    Thread.sleep(100);
                    setProgressAnimate(blueSpinner);
                    Thread.sleep(100);
                    setProgressAnimate(cyanSpinner);
                    Thread.sleep(100);
                    setProgressAnimate(greenSpinner);
                    Thread.sleep(100);
                    setProgressAnimate(yellowSpinner);
                    Thread.sleep(100);
                    setProgressAnimate(orangeSpinner);
                    Thread.sleep(100);
                    setProgressAnimate(redSpinner);
                    Thread.sleep(1000);
                    Timeline paneLine = new Timeline(
                            new KeyFrame(
                                    Duration.seconds(1),
                                    new KeyValue(loadPane.opacityProperty(), 0)
                            )
                    );
                    paneLine.play();
                    Thread.sleep(500);
                    Timeline viewerLine = new Timeline(
                            new KeyFrame(
                                    Duration.seconds(1),
                                    new KeyValue(SpineRender.opacityProperty(), 1)
                            )
                    );
                    viewerLine.play();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start());

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

    private String getDefaultColor(int i) {
        String color = "#FFFFFF";
        switch (i) {
            case 0:
                color = "#455A64";
                break;
            case 1:
                color = "#616161";
                break;
            case 2:
                color = "#512DA8";
                break;
            case 3:
                color = "#5D4037";
                break;
            case 4:
                color = "#9C27B0";
                break;
            case 5:
                color = "#7B1FA2";
                break;
            case 6:
                color = "#673AB7";
                break;
            case 7:
                color = "#7C4DFF";
                break;
            case 8:
                color = "#3F51B5";
                break;
            case 9:
                color = "#536DFE";
                break;
            case 10:
                color = "#2196F3";
                break;
            case 11:
                color = "#448AFF";
                break;
            case 12:
                color = "#0288D1";
                break;
            case 13:
                color = "#00BCD4";
                break;
            case 14:
                color = "#009688";
                break;
            case 15:
                color = "#4CAF50";
                break;
            case 16:
                color = "#689F38";
                break;
            case 17:
                color = "#607D8B";
                break;
            case 18:
                color = "#FFC107";
                break;
            case 19:
                color = "#FF9800";
                break;
            case 20:
                color = "#FF5722";
                break;
            case 21:
                color = "#795548";
                break;
            case 22:
                color = "#9E9E9E";
                break;
            default:
                break;
        }
        return color;
    }

    private static class BufferedImageTranscoder extends ImageTranscoder {
        BufferedImage svg = null;

        @Override
        public BufferedImage createImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void writeImage(BufferedImage svg, TranscoderOutput to) {
            this.svg = svg;
        }

        public BufferedImage getBufferedImage() {
            return svg;
        }
    }

}
