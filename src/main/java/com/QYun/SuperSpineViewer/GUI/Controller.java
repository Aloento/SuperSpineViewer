package com.QYun.SuperSpineViewer.GUI;

import com.QYun.SuperSpineViewer.Main;
import com.jfoenix.controls.JFXProgressBar;
import javafx.application.Application;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class Controller {
    public static boolean isLoad = false;
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
    public static boolean preA = true;
    public static String outPath = null;
    public static String openPath = null;

    public static void main(String[] args) {
        if (args.length > 0) {
            openPath = args[0];
        }
        Application.launch(Main.class, args);
    }

}
