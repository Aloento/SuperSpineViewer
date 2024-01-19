package org.lwjgl.util.stream;

import static org.lwjgl.opengl.GL15.*;

abstract class StreamBufferedPBO extends StreamBuffered {
    protected final int[] pbos;

    protected StreamBufferedPBO(final StreamHandler handler, final int transfersToBuffer) {
        super(handler, transfersToBuffer);
        pbos = new int[transfersToBuffer];
    }

    protected void resizeBuffers(final int height, final int stride, final int pboTarget, final int pboUsage) {
        final int renderBytes = height * stride;
        for (int i = 0; i < pbos.length; i++) {
            pbos[i] = glGenBuffers();
            glBindBuffer(pboTarget, pbos[i]);
            glBufferData(pboTarget, renderBytes, pboUsage);
            pinnedBuffers[i] = null;
        }
        glBindBuffer(pboTarget, 0);
    }
}