package to.aloen.spine;

import com.badlogic.gdx.files.FileHandle;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class BaseSpine {
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
    static final ObservableList<String> skinsList = FXCollections.observableArrayList();
    static final ObservableList<String> animatesList = FXCollections.observableArrayList();
    static double percent = -1;
    static FileHandle skelFile;
    static FileHandle atlasFile;
    static boolean isBinary = true;

    public SimpleStringProperty spineVersionProperty() {
        return spineVersion;
    }

    public String getProjectName() {
        return projectName.get();
    }

    public SimpleStringProperty projectNameProperty() {
        return projectName;
    }

    public void setSkin(String skin) {
        BaseSpine.skin.set(skin);
    }

    public String getAnimate() {
        return animate.get();
    }

    public void setAnimate(String animate) {
        BaseSpine.animate.set(animate);
    }

    public void setIsLoop(boolean isLoop) {
        BaseSpine.isLoop.set(isLoop);
    }

    public boolean isIsPlay() {
        return isPlay.get();
    }

    public void setIsPlay(boolean isPlay) {
        BaseSpine.isPlay.set(isPlay);
    }

    public SimpleBooleanProperty isPlayProperty() {
        return isPlay;
    }

    public void setScale(float scale) {
        BaseSpine.scale.set(scale);
    }

    public void setX(float x) {
        X.set(x);
    }

    public void setY(float y) {
        Y.set(y);
    }

    public void setSpeed(float speed) {
        BaseSpine.speed.set(speed);
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        BaseSpine.percent = percent;
    }

    public void setSkelFile(FileHandle skelFile) {
        BaseSpine.skelFile = skelFile;
    }

    public FileHandle getAtlasFile() {
        return atlasFile;
    }

    public void setAtlasFile(FileHandle atlasFile) {
        BaseSpine.atlasFile = atlasFile;
    }

    public boolean isIsBinary() {
        return isBinary;
    }

    public void setIsBinary(boolean isBinary) {
        BaseSpine.isBinary = isBinary;
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

    void reload() {
        skin.set(null);
        animate.set(null);
        skinsList.clear();
        animatesList.clear();
    }
}
