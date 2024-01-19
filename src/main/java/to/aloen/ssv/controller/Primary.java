package to.aloen.ssv.controller;

import com.jfoenix.controls.JFXDrawer;
import javafx.animation.Transition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import to.aloen.ssv.Main;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static to.aloen.ssv.controller.Launcher.Hamburger;

public class Primary extends Main implements Initializable {
    @FXML
    private JFXDrawer mainDrawer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainDrawer.setOnDrawerOpening(e -> {
            final Transition animation = Hamburger.getAnimation();
            animation.setRate(1);
            animation.play();
        });

        mainDrawer.setOnDrawerClosing(e -> {
            final Transition animation = Hamburger.getAnimation();
            animation.setRate(-1);
            animation.play();
        });

        Hamburger.setOnMouseClicked(e -> {
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
