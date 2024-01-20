package to.aloen.spine;

import com.badlogic.gdx.ApplicationAdapter;
import to.aloen.ssv.Loader;

public class SpineAdapter extends ApplicationAdapter {
    @Deprecated
    public static byte Range;

    private static Spine Runtimes;

    @Override
    public void create() {
        Loader.spineVersion = 38;
        Runtimes = new Spine38();
        Runtimes.create();
    }

    @Override
    public void render() {
        Runtimes.render();
    }

    @Override
    public void resize(int width, int height) {
        Runtimes.resize();
    }

    @Override
    public void dispose() {
        if (Runtimes != null)
            Runtimes.reload();
    }
}
