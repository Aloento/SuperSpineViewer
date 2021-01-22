package org.lwjgl.util.stream;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

public interface StreamHandler {
    int getWidth();

    int getHeight();

    void process(final int width, final int height, ByteBuffer data, final int stride, Semaphore signal);
}