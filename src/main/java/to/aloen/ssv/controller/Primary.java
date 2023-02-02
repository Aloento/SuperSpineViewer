package to.aloen.ssv.controller;

import to.aloen.ssv.Main;
import com.jfoenix.controls.JFXDrawer;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXPopup;
import javafx.animation.Transition;
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
    private JFXDrawer mainDrawer;

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
            FXMLLoader spineLoader = new FXMLLoader(getClass().getResource("/UI/Spine.fxml"));
            mainDrawer.setContent(spineLoader.<Parent>load());
            mainDrawer.setSidePane(new FXMLLoader(getClass().getResource("/UI/Exporter.fxml")).<Parent>load());
            spineController = spineLoader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
