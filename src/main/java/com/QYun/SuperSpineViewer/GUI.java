package com.QYun.SuperSpineViewer;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.ResourceBundle;

public class GUI implements Initializable {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private AnchorPane SpinePane1;

    @FXML
    private AnchorPane SpinePane;

    @FXML
    public ImageView LibGDX;

    @FXML
    private Label JavaInf;

    @FXML
    private Label SystemInf;

    @FXML
    private Font x41;

    @FXML
    private Font x5;

    @FXML
    private Label LeftInf;

    @FXML
    private Font x3;

    @FXML
    private Color x4;

    @FXML
    public Label RightInf;

    public GUI()
    {
    }

    public void initialize(final URL url, final ResourceBundle resourceBundle)
    {
        final StringBuilder info = new StringBuilder(128);
        info.append(System.getProperty("java.vm.name")).append(' ').append(System.getProperty("java.version")).append(' ').append(System.getProperty("java.vm.version"));
        JavaInf.setText(info.toString());

        info.setLength(0);
        info.append(System.getProperty("os.name")).append(" - JavaFX ").append(System.getProperty("javafx.runtime.version"));
        SystemInf.setText(info.toString());
    }

}
