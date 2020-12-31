package com.QYun.SuperSpineViewer.GUI;

import com.gluonhq.charm.glisten.visual.GlistenStyleClasses;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.ResourceBundle;

public class GUI implements Initializable {

    @FXML
    public ImageView LibGDX;
    @FXML
    public Label RightInf;
    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;
    @FXML
    private AnchorPane SpinePane1;
    @FXML
    private VBox SpineVBox;
    @FXML
    private VBox SpineVBox1;
    @FXML
    private Label T_Project;
    @FXML
    private TextField T_Scale;
    @FXML
    private Button B_Scale;
    @FXML
    private TextField T_Width;
    @FXML
    private Button B_Width;
    @FXML
    private TextField T_Height;
    @FXML
    private Button B_Height;
    @FXML
    private TextField T_X;
    @FXML
    private Button B_X;
    @FXML
    private TextField T_Y;
    @FXML
    private Button B_Y;
    @FXML
    private TextField T_Speed;
    @FXML
    private Button B_Speed;
    @FXML
    private CheckBox Check_Loop;
    @FXML
    private Button B_Reload;
    @FXML
    private Button B_Reset;
    @FXML
    private Button B_Start;
    @FXML
    private Button B_Stop;
    @FXML
    private ChoiceBox<?> C_Skins;
    @FXML
    private ChoiceBox<?> C_Animations;
    @FXML
    private AnchorPane BackImg;
    @FXML
    private Label JavaInf;
    @FXML
    private Label SystemInf;
    @FXML
    private Font x41;
    @FXML
    private Label LeftInf;
    @FXML
    private Font x3;

    public GUI() {
    }

    public void initialize(final URL url, final ResourceBundle resourceBundle) {
        final StringBuilder info = new StringBuilder(128);
        info.append(System.getProperty("java.vm.name")).append(' ').append(System.getProperty("java.version")).append(' ').append(System.getProperty("java.vm.version"));
        JavaInf.setText(info.toString());

        info.setLength(0);
        info.append(System.getProperty("os.name")).append(" - JavaFX ").append(System.getProperty("javafx.runtime.version"));
        SystemInf.setText(info.toString());

        GlistenStyleClasses.applyStyleClass(B_Height, "BUTTON_FLAT");
        B_Height.getStyleClass().forEach(System.out::println);
    }

}
