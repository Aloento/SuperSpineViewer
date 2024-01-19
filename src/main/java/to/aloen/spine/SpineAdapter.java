package to.aloen.spine;

import com.badlogic.gdx.ApplicationAdapter;

public class SpineAdapter extends ApplicationAdapter {
    public static byte Range;

    private static Spine Runtimes;

    public void reload() {
        if (Runtimes != null)
            Runtimes.reload();
    }

    @Override
    public void create() {
        if (SpineAdapter.Range == 2)
            Runtimes = new Spine40();
        else if (SpineAdapter.Range == 0)
            Runtimes = new Legacy();
        else Runtimes = new Standard();

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
}
