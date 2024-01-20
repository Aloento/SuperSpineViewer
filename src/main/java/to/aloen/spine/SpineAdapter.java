package to.aloen.spine;

import com.badlogic.gdx.ApplicationAdapter;
import to.aloen.ssv.Loader;

public class SpineAdapter extends ApplicationAdapter {
    private static Spine Runtimes;

    @Override
    public void create() {
        switch (Loader.spineVersion) {
            case 31:
            case 32:
                Runtimes = new Spine31();
                break;
            case 33:
            case 34:
                Runtimes = new Spine34();
                break;
            case 35:
                Runtimes = new Spine35();
                break;
            case 36:
                Runtimes = new Spine36();
                break;
            case 37:
                Runtimes = new Spine37();
                break;
            case 38:
                Runtimes = new Spine38();
                break;
            case 40:
                Runtimes = new Spine40();
                break;
            case 41:
                Runtimes = new Spine41();
                break;
            case 42:
                Runtimes = new Spine42();
                break;
            default:
                Runtimes = new Spine21();
        }

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
