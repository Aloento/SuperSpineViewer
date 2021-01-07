package com.QYun.SuperSpineViewer;

import com.QYun.SuperSpineViewer.GUI.Controller;
import javafx.application.Application;

public class AppLauncher extends Controller {
    public static void main(String[] args) {
        if (args.length != 0) {
            arg = args[0];
            System.out.println(arg);
        }

    }
}
