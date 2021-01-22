package org.lwjgl.util.stream;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.Semaphore;

abstract class StreamBuffered {
    protected final StreamHandler handler;
    protected final int transfersToBuffer;
    protected final ByteBuffer[] pinnedBuffers;
    protected final Semaphore[] semaphores;
    protected final BitSet processingState;
    protected int width;
    protected int height;
    protected int stride;
    protected long bufferIndex;

    protected StreamBuffered(final StreamHandler handler, final int transfersToBuffer) {
        this.handler = handler;
        this.transfersToBuffer = transfersToBuffer;
        pinnedBuffers = new ByteBuffer[transfersToBuffer];
        semaphores = new Semaphore[transfersToBuffer];
        for (int i = 0; i < semaphores.length; i++)
            semaphores[i] = new Semaphore(1, false);
        processingState = new BitSet(transfersToBuffer);
    }

    protected void waitForProcessingToComplete(final int index) {
        final Semaphore s = semaphores[index];
        if (s.availablePermits() == 0) {
            s.acquireUninterruptibly();
            s.release();
        }
        postProcess(index);
        processingState.set(index, false);
    }

    protected abstract void postProcess(int index);
}