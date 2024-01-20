package to.aloen.spine;

import com.badlogic.gdx.files.FileHandle;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public abstract class Spine {

    public static final SimpleStringProperty spineVersion = new SimpleStringProperty(null);

    public static final SimpleStringProperty projectName = new SimpleStringProperty(null);

    public static final SimpleStringProperty skin = new SimpleStringProperty(null);

    public static final SimpleStringProperty animate = new SimpleStringProperty(null);

    public static final SimpleBooleanProperty isLoop = new SimpleBooleanProperty(false);

    public static final SimpleBooleanProperty isPlay = new SimpleBooleanProperty(false);

    public static final SimpleBooleanProperty renderA = new SimpleBooleanProperty(true);

    public static final SimpleBooleanProperty batchA = new SimpleBooleanProperty(true);

    public static final SimpleFloatProperty scale = new SimpleFloatProperty(1.0f);

    public static final SimpleFloatProperty X = new SimpleFloatProperty(0.0f);

    public static final SimpleFloatProperty Y = new SimpleFloatProperty(-200f);

    public static final SimpleFloatProperty speed = new SimpleFloatProperty(1);

    public static final ObservableList<String> skinsList = FXCollections.observableArrayList();

    public static final ObservableList<String> animatesList = FXCollections.observableArrayList();

    public static double percent = -1;

    public static FileHandle skelFile;

    public static FileHandle atlasFile;

    public static boolean isBinary = true;

    public static byte currentVersion = 0;

    public abstract void create();

    public abstract void render();

    public abstract void resize();

    public void reload() {
        skin.set(null);
        animate.set(null);
        skinsList.clear();
        animatesList.clear();
    }
}
