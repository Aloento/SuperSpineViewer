package org.lwjgl.util.stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;

abstract class TextureStreamPBO extends StreamBufferedPBO implements TextureStream {
    private final int texID;
    private long currentIndex;
    private boolean resetTexture;

    protected TextureStreamPBO(final StreamHandler handler, final int transfersToBuffer) {
        super(handler, transfersToBuffer);
        texID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public StreamHandler getHandler() {
        return handler;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void resize(final int width, final int height) {
        if (width < 0 || height < 0)
            throw new IllegalArgumentException("Invalid dimensions: " + width + " x " + height);

        destroyObjects();
        this.width = width;
        this.height = height;
        this.stride = StreamUtil.getStride(width);

        if (width == 0 || height == 0)
            return;

        bufferIndex = 0;
        currentIndex = 0;
        resetTexture = true;
        resizeBuffers(height, stride);
    }

    protected void resizeBuffers(final int height, final int stride) {
        super.resizeBuffers(height, stride, GL_PIXEL_UNPACK_BUFFER, GL_STREAM_DRAW);
    }

    public void snapshot() {
        if (width != handler.getWidth() || height != handler.getHeight())
            resize(handler.getWidth(), handler.getHeight());

        if (width == 0 || height == 0)
            return;

        final int trgPBO = (int) (bufferIndex % transfersToBuffer);

        if (processingState.get(trgPBO))
            syncUpload(trgPBO);

        pinBuffer(trgPBO);
        processingState.set(trgPBO, true);
        semaphores[trgPBO].acquireUninterruptibly();

        handler.process(
            width, height,
            pinnedBuffers[trgPBO],
            stride,
            semaphores[trgPBO]
        );

        bufferIndex++;

        if (resetTexture)
            syncUpload(trgPBO);
    }

    protected abstract void pinBuffer(final int index);

    public void tick() {
        final int srcPBO = (int) (currentIndex % transfersToBuffer);

        if (!processingState.get(srcPBO))
            return;

        syncUpload(srcPBO);
    }

    private void syncUpload(final int index) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbos[index]);
        waitForProcessingToComplete(index);
        upload(index);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
    }

    private void upload(final int srcPBO) {
        glBindTexture(GL_TEXTURE_2D, texID);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, stride >> 2);

        if (resetTexture) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, 0);
            resetTexture = false;
        } else
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, 0);

        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        postUpload(srcPBO);
        currentIndex++;
    }

    protected abstract void postUpload(int index);

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, texID);
    }

    protected void destroyObjects() {
        for (int i = 0; i < semaphores.length; i++) {
            if (processingState.get(i)) {
                glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbos[i]);
                waitForProcessingToComplete(i);
            }
        }

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

        for (int pbo : pbos) {
            if (pbo != 0)
                glDeleteBuffers(pbo);
        }
    }

    public void destroy() {
        destroyObjects();
    }
}
