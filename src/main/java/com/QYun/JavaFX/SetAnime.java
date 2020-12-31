package com.QYun.JavaFX;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

public class SetAnime {

    public Transition EGAnime(Node target) {
        Duration firstDuration = Duration.millis(2000);
        ScaleTransition st = new ScaleTransition(firstDuration, target);
        st.setFromX(0.1);
        st.setToX(1);
        st.setFromY(0.1);
        st.setToY(1);
        st.setInterpolator(Interpolator.LINEAR);

        RotateTransition rt = new RotateTransition(firstDuration.divide(2), target);
        rt.setByAngle(360);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.setCycleCount(2);

        FadeTransition ft = new FadeTransition(Duration.millis(300), target);
        ft.setFromValue(1);
        ft.setToValue(0.5);
        ft.setCycleCount(2);
        ft.setAutoReverse(true);

        ParallelTransition pt = new ParallelTransition(st, rt);
        return new SequentialTransition(pt);
    }
}
