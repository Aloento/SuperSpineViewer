package to.aloen.ssv.controller;

import com.jfoenix.assets.JFoenixResources;
import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.transitions.hamburger.HamburgerBackArrowBasicTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import to.aloen.spine.Spine;

import java.io.IOException;
import java.util.Objects;

import static to.aloen.ssv.Main.Pref;

public class LauncherController extends Application {
    public static final JFXHamburger Hamburger = new JFXHamburger() {{
        setAnimation(new HamburgerBackArrowBasicTransition(this));
    }};

    private final String title = "SuperSpineViewer - Aloento : ";

    @Override
    public void start(Stage primaryStage) throws IOException {
        System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true");

        primaryStage.setScene(getScene(primaryStage));

        primaryStage.getIcons().add(new Image("UI/SuperSpineViewer.png"));

        primaryStage.setWidth(Pref.getDouble("stageWidth", 1280));
        primaryStage.setHeight(Pref.getDouble("stageHeight", 800));
        primaryStage.setTitle(STR."\{title}Waiting Loading...");

        primaryStage.show();

        Hamburger.requestFocus();

        Spine.projectName.addListener(
            (_, _, newValue) -> Platform.runLater(
                () -> primaryStage.setTitle(STR."\{title}\{newValue}")
            ));
    }

    private Scene getScene(Stage primaryStage) throws IOException {
        JFXDecorator decorator = new JFXDecorator(
            primaryStage,
            FXMLLoader.load(Objects.requireNonNull(
                LauncherController.this.getClass().getResource("/UI/Primary.fxml")
            ))
        ) {{
            setCustomMaximize(true);
            setGraphic(Hamburger);
        }};

        return new Scene(
            decorator,
            Screen.getScreens().getFirst().getBounds().getWidth() / 2.5,
            Screen.getScreens().getFirst().getBounds().getHeight() / 1.35
        ) {{
            getStylesheets().addAll(
                JFoenixResources.load("css/jfoenix-fonts.css").toExternalForm(),
                JFoenixResources.load("css/jfoenix-design.css").toExternalForm(),
                Objects.requireNonNull(
                    LauncherController.class.getResource("/UI/Main.css")
                ).toExternalForm());
        }};
    }
}
