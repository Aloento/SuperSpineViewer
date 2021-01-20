package com.QYun.Spine;

import com.badlogic.gdx.files.FileHandle;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SuperSpine {

    static final SimpleBooleanProperty isReload = new SimpleBooleanProperty(false);
    static final SimpleStringProperty spineVersion = new SimpleStringProperty(null);
    static final SimpleStringProperty projectName = new SimpleStringProperty(null);
    static final SimpleStringProperty skin = new SimpleStringProperty(null);
    static final SimpleStringProperty animate = new SimpleStringProperty(null);
    static final SimpleBooleanProperty isLoop = new SimpleBooleanProperty(false);
    static final SimpleBooleanProperty isPlay = new SimpleBooleanProperty(false);
    static final SimpleFloatProperty scale = new SimpleFloatProperty(1.0f);
    static final SimpleFloatProperty X = new SimpleFloatProperty(0.0f);
    static final SimpleFloatProperty Y = new SimpleFloatProperty(-200f);
    static final SimpleFloatProperty speed = new SimpleFloatProperty(1);
    static double percent = -1;
    static FileHandle skelFile;
    static FileHandle atlasFile;
    static boolean isBinary = true;
    static ObservableList<String> skinsList = FXCollections.observableArrayList();
    static ObservableList<String> animatesList = FXCollections.observableArrayList();

    public void setIsReload(boolean isReload) {
        SuperSpine.isReload.set(isReload);
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

    public String getProjectName() {
        return projectName.get();
    }

    public SimpleStringProperty projectNameProperty() {
        return projectName;
    }

    public String getSkin() {
        return skin.get();
    }

    public void setSkin(String skin) {
        SuperSpine.skin.set(skin);
    }

    public String getAnimate() {
        return animate.get();
    }

    public void setAnimate(String animate) {
        SuperSpine.animate.set(animate);
    }

    public void setIsLoop(boolean isLoop) {
        SuperSpine.isLoop.set(isLoop);
    }

    public boolean isIsPlay() {
        return isPlay.get();
    }

    public void setIsPlay(boolean isPlay) {
        SuperSpine.isPlay.set(isPlay);
    }

    public SimpleBooleanProperty isPlayProperty() {
        return isPlay;
    }

    public float getScale() {
        return scale.get();
    }

    public void setScale(float scale) {
        SuperSpine.scale.set(scale);
    }

    public float getX() {
        return X.get();
    }

    public void setX(float x) {
        X.set(x);
    }

    public float getY() {
        return Y.get();
    }

    public void setY(float y) {
        Y.set(y);
    }

    public void setSpeed(float speed) {
        SuperSpine.speed.set(speed);
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        SuperSpine.percent = percent;
    }

    public void setSkelFile(FileHandle skelFile) {
        SuperSpine.skelFile = skelFile;
    }

    public FileHandle getAtlasFile() {
        return atlasFile;
    }

    public void setAtlasFile(FileHandle atlasFile) {
        SuperSpine.atlasFile = atlasFile;
    }

    public void setIsBinary(boolean isBinary) {
        SuperSpine.isBinary = isBinary;
    }

    public boolean isIsBinary() {
        return isBinary;
    }

    public ObservableList<String> getSkinsList() {
        return skinsList;
    }

    public ObservableList<String> getAnimatesList() {
        return animatesList;
    }

    void create() {
    }

    void render() {
    }

    void resize() {
    }

}
