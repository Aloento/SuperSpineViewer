package com.jfoenix.skins;

import com.jfoenix.controls.JFXSpinner;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.TreeShowingProperty;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class JFXSpinnerSkin extends SkinBase<JFXSpinner> {

    private final StackPane arcPane;
    private final Rectangle fillRect;
    private final TreeShowingProperty TreeShowingProperty;
    private final Color greenColor;
    private final Color redColor;
    private final Color yellowColor;
    private final Color blueColor;
    private final Text text;
    boolean wasIndeterminate = false;
    private JFXSpinner control;
    private boolean isValid = false;
    private Timeline timeline;
    private Arc arc;
    private Arc track;
    private double arcLength = -1;

    public JFXSpinnerSkin(JFXSpinner control) {
        super(control);

        this.control = control;
        this.TreeShowingProperty = new TreeShowingProperty(control);

        blueColor = Color.valueOf("#4285f4");
        redColor = Color.valueOf("#db4437");
        yellowColor = Color.valueOf("#f4b400");
        greenColor = Color.valueOf("#0F9D58");

        arc = new Arc();
        arc.setManaged(false);
        arc.setStartAngle(0);
        arc.setLength(180);
        arc.getStyleClass().setAll("arc");
        arc.setFill(Color.TRANSPARENT);
        arc.setStrokeWidth(3);

        track = new Arc();
        track.setManaged(false);
        track.setStartAngle(0);
        track.setLength(360);
        track.setStrokeWidth(3);
        track.getStyleClass().setAll("track");
        track.setFill(Color.TRANSPARENT);

        fillRect = new Rectangle();
        fillRect.setFill(Color.TRANSPARENT);
        text = new Text();
        text.setStyle("-fx-font-size:null");
        text.getStyleClass().setAll("text", "percentage");
        final Group group = new Group(fillRect, track, arc, text);
        group.setManaged(false);
        arcPane = new StackPane(group);
        arcPane.setPrefSize(50, 50);
        getChildren().setAll(arcPane);

        // register listeners
        registerChangeListener(control.indeterminateProperty(), obs -> initialize());
        registerChangeListener(control.progressProperty(), obs -> updateProgress());
        registerChangeListener(TreeShowingProperty, obs -> updateAnimation());
        registerChangeListener(control.sceneProperty(), obs -> updateAnimation());
    }

    private void initialize() {
        if (getSkinnable().isIndeterminate()) {
            if (timeline == null) {
                createTransition();
                if (NodeHelper.isTreeShowing(getSkinnable())) {
                    timeline.play();
                }
            }
        } else {
            clearAnimation();
            arc.setStartAngle(90);
            updateProgress();
        }
    }

    private KeyFrame[] getKeyFrames(double angle, double duration, Paint color) {
        KeyFrame[] frames = new KeyFrame[4];
        frames[0] = new KeyFrame(Duration.seconds(duration),
            new KeyValue(arc.lengthProperty(), 5, Interpolator.LINEAR),
            new KeyValue(arc.startAngleProperty(),
                angle + 45 + control.getStartingAngle(),
                Interpolator.LINEAR));
        frames[1] = new KeyFrame(Duration.seconds(duration + 0.4),
            new KeyValue(arc.lengthProperty(), 250, Interpolator.LINEAR),
            new KeyValue(arc.startAngleProperty(),
                angle + 90 + control.getStartingAngle(),
                Interpolator.LINEAR));
        frames[2] = new KeyFrame(Duration.seconds(duration + 0.7),
            new KeyValue(arc.lengthProperty(), 250, Interpolator.LINEAR),
            new KeyValue(arc.startAngleProperty(),
                angle + 135 + control.getStartingAngle(),
                Interpolator.LINEAR));
        frames[3] = new KeyFrame(Duration.seconds(duration + 1.1),
            new KeyValue(arc.lengthProperty(), 5, Interpolator.LINEAR),
            new KeyValue(arc.startAngleProperty(),
                angle + 435 + control.getStartingAngle(),
                Interpolator.LINEAR),
            new KeyValue(arc.strokeProperty(), color, Interpolator.EASE_BOTH));
        return frames;
    }

    private void pauseTimeline(boolean pause) {
        if (getSkinnable().isIndeterminate()) {
            if (timeline == null) {
                createTransition();
            }
            if (pause) {
                timeline.pause();
            } else {
                timeline.play();
            }
        }
    }

    private void updateAnimation() {
        ProgressIndicator control = getSkinnable();
        final boolean isTreeShowing = NodeHelper.isTreeShowing(control) && control.getScene() != null;
        if (timeline != null) {
            pauseTimeline(!isTreeShowing);
        } else if (isTreeShowing) {
            createTransition();
        }
    }

    private double computeSize() {
        return control.getRadius() * 2 + arc.getStrokeWidth() * 2;
    }

    @Override
    protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (Region.USE_COMPUTED_SIZE == control.getRadius()) {
            return super.computeMaxHeight(width, topInset, rightInset, bottomInset, leftInset);
        } else {
            return computeSize();
        }
    }

    @Override
    protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (Region.USE_COMPUTED_SIZE == control.getRadius()) {
            return super.computeMaxHeight(height, topInset, rightInset, bottomInset, leftInset);
        } else {
            return computeSize();
        }
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (Region.USE_COMPUTED_SIZE == control.getRadius()) {
            return arcPane.prefWidth(-1);
        } else {
            return computeSize();
        }
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (Region.USE_COMPUTED_SIZE == control.getRadius()) {
            return arcPane.prefHeight(-1);
        } else {
            return computeSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
        final double strokeWidth = arc.getStrokeWidth();
        final double radius = Math.min(contentWidth, contentHeight) / 2 - strokeWidth / 2;
        final double arcSize = snapSize(radius * 2 + strokeWidth);

        arcPane.resizeRelocate((contentWidth - arcSize) / 2 + 1, (contentHeight - arcSize) / 2 + 1, arcSize, arcSize);
        updateArcLayout(radius, arcSize);

        fillRect.setWidth(arcSize);
        fillRect.setHeight(arcSize);

        if (!isValid) {
            initialize();
            isValid = true;
        }

        if (!getSkinnable().isIndeterminate()) {
            arc.setLength(arcLength);
            if (text.isVisible()) {
                final double progress = control.getProgress();
                int intProgress = (int) Math.round(progress * 100.0);
                Font font = text.getFont();
                text.setFont(Font.font(font.getFamily(), radius / 1.7));
                text.setText((progress > 1 ? 100 : intProgress) + "%");
                text.relocate((arcSize - text.getLayoutBounds().getWidth()) / 2, (arcSize - text.getLayoutBounds().getHeight()) / 2);
            }
        }
    }

    private void updateArcLayout(double radius, double arcSize) {
        arc.setRadiusX(radius);
        arc.setRadiusY(radius);
        arc.setCenterX(arcSize / 2);
        arc.setCenterY(arcSize / 2);

        track.setRadiusX(radius);
        track.setRadiusY(radius);
        track.setCenterX(arcSize / 2);
        track.setCenterY(arcSize / 2);
        track.setStrokeWidth(arc.getStrokeWidth());
    }

    protected void updateProgress() {
        final ProgressIndicator control = getSkinnable();
        final boolean isIndeterminate = control.isIndeterminate();
        if (!(isIndeterminate && wasIndeterminate)) {
            arcLength = -360 * control.getProgress();
            control.requestLayout();
        }
        wasIndeterminate = isIndeterminate;
    }

    private void createTransition() {
        if (!getSkinnable().isIndeterminate()) return;
        final Paint initialColor = arc.getStroke();
        if (initialColor == null) {
            arc.setStroke(blueColor);
        }

        KeyFrame[] blueFrame = getKeyFrames(0, 0, initialColor == null ? blueColor : initialColor);
        KeyFrame[] redFrame = getKeyFrames(450, 1.4, initialColor == null ? redColor : initialColor);
        KeyFrame[] yellowFrame = getKeyFrames(900, 2.8, initialColor == null ? yellowColor : initialColor);
        KeyFrame[] greenFrame = getKeyFrames(1350, 4.2, initialColor == null ? greenColor : initialColor);

        KeyFrame endingFrame = new KeyFrame(Duration.seconds(5.6),
            new KeyValue(arc.lengthProperty(), 5, Interpolator.LINEAR),
            new KeyValue(arc.startAngleProperty(),
                1845 + control.getStartingAngle(),
                Interpolator.LINEAR));

        if (timeline != null) {
            timeline.stop();
            timeline.getKeyFrames().clear();
        }
        timeline = new Timeline(blueFrame[0],
            blueFrame[1],
            blueFrame[2],
            blueFrame[3],
            redFrame[0],
            redFrame[1],
            redFrame[2],
            redFrame[3],
            yellowFrame[0],
            yellowFrame[1],
            yellowFrame[2],
            yellowFrame[3],
            greenFrame[0],
            greenFrame[1],
            greenFrame[2],
            greenFrame[3],
            endingFrame);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setDelay(Duration.ZERO);
        timeline.playFromStart();
    }

    private void clearAnimation() {
        if (timeline != null) {
            timeline.stop();
            timeline.getKeyFrames().clear();
            timeline = null;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        TreeShowingProperty.dispose();
        clearAnimation();
        arc = null;
        track = null;
        control = null;
    }
}
