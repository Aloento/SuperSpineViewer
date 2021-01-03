package com.QYun.Spine;

import com.badlogic.gdx.ApplicationAdapter;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SuperSpine extends ApplicationAdapter {

    static SimpleFloatProperty scale;
    static SimpleStringProperty spineVersion;
    static ObservableList<String> skinsList = FXCollections.observableArrayList();
    static ObservableList<String> animatesList = FXCollections.observableArrayList();

    public float getScale() {
        return scale.get();
    }

    public SimpleFloatProperty scaleProperty() {
        return scale;
    }

    public void setScale(float scale) {
        SuperSpine.scale.set(scale);
    }

    public String getSpineVersion() {
        return spineVersion.get();
    }

    public SimpleStringProperty spineVersionProperty() {
        return spineVersion;
    }

    public void setSpineVersion(String spineVersion) {
        SuperSpine.spineVersion.set(spineVersion);
    }

    public ObservableList<String> getSkinsList() {
        return skinsList;
    }

    public void setSkinsList(ObservableList<String> skinsList) {
        SuperSpine.skinsList = skinsList;
    }

    public ObservableList<String> getAnimatesList() {
        return animatesList;
    }

    public void setAnimatesList(ObservableList<String> animatesList) {
        SuperSpine.animatesList = animatesList;
    }

}
