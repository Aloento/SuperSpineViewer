package org.lwjgl.util.stream;

public interface RenderStream {
    StreamHandler getHandler();

    void bind();

    void swapBuffers();

    void destroy();
}
