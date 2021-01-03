package com.QYun.Spine;

import com.badlogic.gdx.ApplicationAdapter;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SuperSpine extends ApplicationAdapter {

    static ObservableList<String> skinsList = FXCollections.observableArrayList();
    static ObservableList<String> animatesList = FXCollections.observableArrayList();
    static SimpleStringProperty spineVersion;
    static SimpleStringProperty skin;
    static SimpleStringProperty animate;
    static SimpleBooleanProperty isLoop = new SimpleBooleanProperty(false);
    static SimpleBooleanProperty isPlay = new SimpleBooleanProperty(false);
    static SimpleFloatProperty scale;
    static SimpleFloatProperty X;
    static SimpleFloatProperty Y;
    static float speed;

    public SimpleBooleanProperty isPlayProperty() {
        return isPlay;
    }

    public boolean isIsPlay() {
        return isPlay.get();
    }

    public void setIsPlay(boolean isPlay) {
        SuperSpine.isPlay.set(isPlay);
    }

    public void setSkin(String skin) {
        SuperSpine.skin.set(skin);
    }

    public void setAnimate(String animate) {
        SuperSpine.animate.set(animate);
    }

    public void setIsLoop(boolean isLoop) {
        SuperSpine.isLoop.set(isLoop);
    }

    public void setX(float x) {
        X.set(x);
    }

    public void setY(float y) {
        Y.set(y);
    }

    public void setSpeed(float speed) {
        SuperSpine.speed = speed;
    }

    public float getScale() {
        return scale.get();
    }

    public void setScale(float scale) {
        SuperSpine.scale.set(scale);
    }

    public SimpleFloatProperty scaleProperty() {
        return scale;
    }

    public String getSpineVersion() {
        return spineVersion.get();
    }

    public void setSpineVersion(String spineVersion) {
        SuperSpine.spineVersion.set(spineVersion);
    }

    public SimpleStringProperty spineVersionProperty() {
        return spineVersion;
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
