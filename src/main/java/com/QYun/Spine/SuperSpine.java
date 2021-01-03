package com.QYun.Spine;

import com.badlogic.gdx.ApplicationAdapter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SuperSpine extends ApplicationAdapter {

    static float scale;
    static String spineVersion;
    static ObservableList<String> skinsList = FXCollections.observableArrayList();
    static ObservableList<String> animatesList = FXCollections.observableArrayList();

    public float getScale() {
        return scale;
    }

    public String getSpineVersion() {
        return spineVersion;
    }

    public ObservableList<String> getSkinsList() {
        return skinsList;
    }

    public ObservableList<String> getAnimatesList() {
        return animatesList;
    }

}
