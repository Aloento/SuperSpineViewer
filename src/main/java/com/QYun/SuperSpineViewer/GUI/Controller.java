package com.QYun.SuperSpineViewer.GUI;

import com.jfoenix.controls.JFXProgressBar;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class Controller {

    public static final SimpleBooleanProperty isLoad = new SimpleBooleanProperty(false);
    public static SpineController spineController;
    public static ExporterController exporterController;
    public static ImageView spineRender;
    public static JFXProgressBar progressBar;
    public static Label FPS;
    public static Label Skel;
    public static Label Atlas;
    public static int width;
    public static int height;
    public static boolean sequence = false;
    public static boolean isFX = true;
    public static boolean requestReload = false;
    public static String path = null;
    public static String arg = null;

}
