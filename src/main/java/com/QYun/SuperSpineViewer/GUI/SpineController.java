package com.QYun.SuperSpineViewer.GUI;

import com.jfoenix.controls.*;
import com.jfoenix.controls.JFXButton.ButtonType;
import com.jfoenix.effects.JFXDepthManager;
import io.datafx.controller.FXMLController;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static javafx.animation.Interpolator.EASE_BOTH;

@FXMLController(value = "/UI/Spine.fxml", title = "SpineController")
public class SpineController {

    @FXML
    private StackPane Spine;

    @FXML
    private BorderPane spineBorder;

    @FXML
    private StackPane spinePane;

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

    @PostConstruct
    public void init() {
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

        StackPane header = new StackPane();
        String headerColor = getDefaultColor((int) ((Math.random() * 12) % 22));
        header.setStyle("-fx-background-radius: 0 5 0 0; -fx-background-color: " + headerColor);
        Label controlText = new Label("Controller");
        controlText.getStyleClass().add("title-label");
        controlText.setStyle("-fx-pref-height: 135; -fx-text-fill: #f1f1f2; -fx-font-size: 28;");

        HBox hBox = new HBox(8);
        hBox.setPadding(new Insets(0, 0, 0, 70));
        hBox.getChildren().addAll(spineLogo, controlText);
        Label project = new Label("Waiting Loading...");
        project.setStyle("-fx-text-fill: #f1f1f2;");
        project.getStyleClass().add("title-label");
        header.setAlignment(Pos.BOTTOM_LEFT);
        header.getChildren().addAll(hBox, project);

        VBox.setVgrow(header, Priority.NEVER);
        StackPane body = new StackPane();
        body.setStyle("-fx-background-radius: 0 0 5 5; -fx-background-color: rgb(255,255,255,0.87);");

        VBox controller = new VBox(40);

        {
            Label L_Scale = new Label("Load Scale");
            L_Scale.getStyleClass().add("title-label");
            Label L_Width = new Label("Camera Width");
            L_Width.getStyleClass().add("title-label");
            Label L_Height = new Label("Camera Height");
            L_Height.getStyleClass().add("title-label");
            Label L_X = new Label("Position X");
            L_X.getStyleClass().add("title-label");
            Label L_Y = new Label("Position Y");
            L_Y.getStyleClass().add("title-label");
            Label L_Speed = new Label("Play Speed");
            L_Speed.getStyleClass().add("title-label");
            Label L_Set = new Label("Set");
            L_Set.getStyleClass().add("title-label");
            Label L_Skins = new Label("Skins");
            L_Skins.getStyleClass().add("title-label");
            Label L_Animate = new Label("Animations");
            L_Animate.getStyleClass().add("title-label");
        }
        {
            JFXTextField T_Scale = new JFXTextField("骨骼缩放");
            JFXTextField T_Width = new JFXTextField("渲染时的宽");
            JFXTextField T_Height = new JFXTextField("渲染时的高");
            JFXTextField T_X = new JFXTextField("骨骼X轴位置");
            JFXTextField T_Y = new JFXTextField("骨骼Y轴位置");
        }
        {
            JFXSlider S_Speed = new JFXSlider();
            S_Speed.getStyleClass().add("jfx-slider-style");
            JFXToggleButton T_Loop = new JFXToggleButton();
            JFXButton B_Reload = new JFXButton("Reload");
            B_Reload.setButtonType(ButtonType.FLAT);
            B_Reload.setStyle("-fx-text-fill:#5264AE;-fx-font-size:14px;");
            JFXButton B_Reset = new JFXButton("Reset");
            B_Reset.setButtonType(ButtonType.FLAT);
            B_Reset.setStyle("-fx-text-fill:#5264AE;-fx-font-size:14px;");
            HBox set = new HBox(20);
            set.setStyle("-fx-padding: 0 0 0 50;");
            set.getChildren().addAll(T_Loop, B_Reload, B_Reset);
            JFXComboBox<String> C_Skins = new JFXComboBox<>();
            JFXComboBox<String> C_Animate = new JFXComboBox<>();
        }
        {

        }

        VBox content = new VBox();
        content.getChildren().addAll(header, body);

        JFXButton playButton = new JFXButton("");
        playButton.setButtonType(ButtonType.RAISED);
        playButton.setStyle("-fx-background-radius: 40;-fx-background-color: " + getDefaultColor((int) ((Math.random() * 12) % 22)));
        playButton.setPrefSize(56, 56);
        playButton.setRipplerFill(Color.valueOf(headerColor));
        playButton.setScaleX(0);
        playButton.setScaleY(0);

        FontIcon playIcon = new FontIcon();
        playIcon.setIconLiteral("fas-play");
        playIcon.setIconSize(20);
        playIcon.setIconColor(Paint.valueOf("WHITE"));
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

    }

    private String getDefaultColor(int i) {
        String color = "#FFFFFF";
        switch (i) {
            case 0:
                color = "#F44336";
                break;
            case 1:
                color = "#FF5252";
                break;
            case 2:
                color = "#E91E63";
                break;
            case 3:
                color = "#FF4081";
                break;
            case 4:
                color = "#9C27B0";
                break;
            case 5:
                color = "#E040FB";
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
                color = "#03A9F4";
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
                color = "#8BC34A";
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
