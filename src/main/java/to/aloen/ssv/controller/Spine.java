package to.aloen.ssv.controller;

import com.jfoenix.controls.*;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import to.aloen.spine.Universal;
import to.aloen.ssv.Loader;
import to.aloen.ssv.Main;

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
        AtomicReference<String> headerColor = new AtomicReference<>(getColor((short) ((Math.random() * 12) % 22)));

        JFXTextField T_Scale = new JFXTextField() {{
            setPromptText("1.0");
            setTextFormatter(new TextFormatter<String>(change -> {
                if (change.getText().matches("[0-9]*|\\."))
                    return change;
                return null;
            }));

            setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode().equals(KeyCode.ENTER))
                    if (getText().matches("^[1-9]\\d*$|^[1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*$"))
                        spine.setScale(Float.parseFloat(getText()));
            });
        }};

        JFXTextField T_Width = new JFXTextField() {{
            setEditable(false);
        }};

        JFXTextField T_Height = new JFXTextField() {{
            setEditable(false);
        }};

        JFXTextField T_X = new JFXTextField() {{
            setPromptText("0.0");
            setTextFormatter(new TextFormatter<String>(change -> {
                if (change.getText().matches("[0-9]*|\\.|-"))
                    return change;
                return null;
            }));

            setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode().equals(KeyCode.ENTER))
                    if (getText().matches("-?[0-9]\\d*|-?([1-9]\\d*.\\d*|0\\.\\d*[1-9]\\d*)"))
                        spine.setX(Float.parseFloat(getText()));
            });
        }};

        JFXTextField T_Y = new JFXTextField() {{
            setPromptText("-200");
            setTextFormatter(new TextFormatter<String>(change -> {
                if (change.getText().matches("[0-9]*|\\.|-"))
                    return change;
                return null;
            }));

            setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode().equals(KeyCode.ENTER))
                    if (getText().matches("-?[0-9]\\d*|-?([1-9]\\d*.\\d*|0\\.\\d*[1-9]\\d*)"))
                        spine.setY(Float.parseFloat(getText()));
            });
        }};

        JFXSlider S_Speed = new JFXSlider() {{
            setSnapToTicks(true);
            setShowTickLabels(true);
            setMin(0.25);
            setMax(2.0);
            setMajorTickUnit(0.25);
            setBlockIncrement(0.25);
            setValue(1);

            setValueFactory(slider ->
                    Bindings.createStringBinding(() -> ((int) (getValue() * 100) / 100f) + "x", slider.valueProperty())
            );
            valueProperty().addListener(
                    (observable, oldValue, newValue) -> spine.setSpeed((Float.parseFloat(String.valueOf(newValue)))));
        }};

        JFXComboBox<String> C_Skins = new JFXComboBox<>() {{
            setItems(spine.getSkinsList());
            setOnAction(event -> spine.setSkin(getValue()));
        }};

        JFXComboBox<String> C_Animate = new JFXComboBox<>() {{
            setItems(spine.getAnimatesList());
            setOnAction(event -> spine.setAnimate(getValue()));
        }};

        FontIcon playIcon = new FontIcon() {{
            setIconSize(20);
            setIconLiteral("fas-play");
            setIconColor(Paint.valueOf("WHITE"));
        }};

        FontIcon pauseIcon = new FontIcon() {{
            setIconSize(20);
            setIconLiteral("fas-pause");
            setIconColor(Paint.valueOf("WHITE"));
        }};

        JFXButton playButton = new JFXButton("") {{
            setStyle("-fx-background-radius: 40;-fx-background-color: " + getColor((short) ((Math.random() * 20) % 22)));
            setRipplerFill(Color.valueOf(headerColor.get()));
            setButtonType(ButtonType.RAISED);
            setPrefSize(56, 56);
            setScaleX(0);
            setScaleY(0);
            setGraphic(playIcon);

            setOnAction(event -> {
                if (isLoad) {
                    if (spine.isIsPlay()) {
                        spine.setIsPlay(false);
                        setGraphic(playIcon);
                    } else {
                        spine.setIsPlay(true);
                        setGraphic(pauseIcon);
                        headerColor.set(getColor((short) ((Math.random() * 12) % 22)));
                        setStyle("-fx-background-radius: 40;-fx-background-color: " + getColor((short) ((Math.random() * 20) % 22)));
                        setRipplerFill(Color.valueOf(headerColor.get()));
                    }
                }
            });
        }};

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

        spinePane.getChildren().addAll(new VBox() {{
            getChildren().addAll(new StackPane() {{
                setStyle("-fx-background-radius: 0 0 5 5; -fx-background-color: rgb(255,255,255,0.87);");
                setMargin(playButton, new Insets(0, 14, 0, 0));
                setAlignment(playButton, Pos.TOP_RIGHT);

                getChildren().add(new ScrollPane(new VBox(20) {{
                    setPadding(new Insets(14, 16, 20, 16));
                    getChildren().addAll(
                            new Label("Load Scale") {{
                                getStyleClass().add("normal-label");
                            }}, T_Scale,
                            new Label("Position X") {{
                                getStyleClass().add("normal-label");
                            }}, T_X,
                            new Label("Position Y") {{
                                getStyleClass().add("normal-label");
                            }}, T_Y,
                            new Label("Camera Width") {{
                                getStyleClass().add("normal-label");
                            }}, T_Width,
                            new Label("Camera Height") {{
                                getStyleClass().add("normal-label");
                            }}, T_Height,
                            new Label("Play Speed") {{
                                getStyleClass().add("normal-label");
                            }}, S_Speed,

                            new FlowPane(
                                    new Label("Loop") {{
                                        getStyleClass().add("normal-label");
                                    }},
                                    new JFXToggleButton() {{
                                        setOnAction(event1 -> spine.setIsLoop(isSelected()));
                                    }},
                                    new JFXButton("Reload") {{
                                        setStyle("-fx-text-fill:#5264AE;-fx-font-size:14px;");
                                        setButtonType(ButtonType.FLAT);

                                        setOnAction(event -> {
                                            Universal.Range = -1;
                                            new Loader().init();
                                        });
                                    }},
                                    new JFXButton("Reset") {{
                                        setStyle("-fx-text-fill:#5264AE;-fx-font-size:14px;");
                                        setButtonType(ButtonType.FLAT);

                                        setOnAction(event -> {
                                            spine.setScale(1);
                                            spine.setX(0);
                                            spine.setY(-200f);
                                            spine.setIsPlay(false);

                                            T_Scale.clear();
                                            T_X.clear();
                                            T_Y.clear();
                                            C_Skins.setValue(null);
                                            C_Animate.setValue(null);
                                            S_Speed.setValue(1);
                                            System.gc();
                                        });
                                    }}) {{
                                setMaxWidth(300);
                                setStyle("-fx-padding: 0 0 0 18;");
                            }},

                            new Label("Skins") {{
                                getStyleClass().add("normal-label");
                            }}, C_Skins,
                            new Label("Animations") {{
                                getStyleClass().add("normal-label");
                            }}, C_Animate);
                }}) {{
                    setHbarPolicy(ScrollBarPolicy.NEVER);
                }});
            }});
        }}, playButton);

        spineRender = new ImageView() {{
            setScaleY(-1);
            fitWidthProperty().addListener((observable, oldValue, newValue) -> {
                T_Width.setPromptText(String.valueOf(newValue.intValue()));
                width = newValue.intValue();
                Pref.putDouble("stageWidth", newValue.doubleValue() + 368);
            });

            fitHeightProperty().addListener((observable, oldValue, newValue) -> {
                T_Height.setPromptText(String.valueOf(newValue.intValue()));
                height = newValue.intValue();
                Pref.putDouble("stageHeight", newValue.doubleValue() + 103);
            });
        }};

        spine.isPlayProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                if (newValue) Platform.runLater(() -> playButton.setGraphic(pauseIcon));
                else Platform.runLater(() -> playButton.setGraphic(playIcon));
            }
        });
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
            new Timeline(
                    new KeyFrame(
                            Duration.seconds(1),
                            new KeyValue(loadPane.opacityProperty(), 0)
                    )
            ).play();
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
        new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        new KeyValue(spinner.progressProperty(), 1)
                )
        ).play();
    }

    private String getColor(short i) {
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
