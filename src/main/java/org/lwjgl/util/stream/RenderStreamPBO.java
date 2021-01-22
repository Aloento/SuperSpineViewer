package org.lwjgl.util.stream;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER;
import static org.lwjgl.opengl.GL30.*;

abstract class RenderStreamPBO extends StreamBufferedPBO implements RenderStream {
    protected final StreamUtil.FBOUtil fboUtil;
    private final ReadbackType readbackType;
    private final int renderFBO;
    private final int samples;
    protected int synchronousFrames;
    private int rgbaBuffer;
    private int depthBuffer;
    private int msaaResolveFBO;
    private int msaaResolveBuffer;

    protected RenderStreamPBO(final StreamHandler handler, final int samples, final int transfersToBuffer, final ReadbackType readbackType) {
        super(handler, transfersToBuffer);
        this.readbackType = readbackType;
        final ContextCapabilities caps = GLContext.getCapabilities();
        fboUtil = StreamUtil.getFBOUtil(caps);
        renderFBO = fboUtil.genFramebuffers();
        this.samples = StreamUtil.checkSamples(samples, caps);
    }

    public StreamHandler getHandler() {
        return handler;
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
        bufferIndex = synchronousFrames = transfersToBuffer - 1;
        fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, renderFBO);
        if (samples <= 1 && readbackType == ReadbackType.GET_TEX_IMAGE)
            fboUtil.framebufferTexture2D(
                    GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
                    rgbaBuffer = StreamUtil.createRenderTexture(width, height), 0
            );
        else
            fboUtil.framebufferRenderbuffer(
                    GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER,
                    rgbaBuffer = StreamUtil.createRenderBuffer(fboUtil, width, height, samples, GL_RGBA8)
            );
        fboUtil.framebufferRenderbuffer(
                GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER,
                depthBuffer = StreamUtil.createRenderBuffer(fboUtil, width, height, samples, GL_DEPTH24_STENCIL8)
        );
        glViewport(0, 0, width, height);
        fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        if (1 < samples) {
            if (msaaResolveFBO == 0) msaaResolveFBO = fboUtil.genFramebuffers();
            fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, msaaResolveFBO);
            if (readbackType == ReadbackType.READ_PIXELS)
                fboUtil.framebufferRenderbuffer(
                        GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER,
                        msaaResolveBuffer = StreamUtil.createRenderBuffer(fboUtil, width, height)
                );
            else
                fboUtil.framebufferTexture2D(
                        GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
                        msaaResolveBuffer = StreamUtil.createRenderTexture(width, height), 0
                );
            fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        } else if (msaaResolveFBO != 0) {
            if (readbackType == ReadbackType.READ_PIXELS)
                fboUtil.deleteRenderbuffers(msaaResolveBuffer);
            else
                glDeleteTextures(msaaResolveBuffer);
            msaaResolveBuffer = 0;
            fboUtil.deleteFramebuffers(msaaResolveFBO);
            msaaResolveFBO = 0;
        }
        resizeBuffers(height, stride);
    }

    protected void resizeBuffers(final int height, final int stride) {
        super.resizeBuffers(height, stride, GL_PIXEL_PACK_BUFFER, GL_STREAM_READ);
    }

    public void bind() {
        if (this.width != handler.getWidth() || this.height != handler.getHeight())
            resize(handler.getWidth(), handler.getHeight());
        fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, renderFBO);
    }

    protected void prepareFramebuffer() {
        if (msaaResolveFBO == 0)
            fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        else {
            fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, renderFBO);
            fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, msaaResolveFBO);
            fboUtil.blitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
            fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
            fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        }
    }

    public void swapBuffers() {
        if (width == 0 || height == 0)
            return;
        prepareFramebuffer();
        final int trgPBO = (int) (bufferIndex % transfersToBuffer);
        final int srcPBO = (int) ((bufferIndex - 1) % transfersToBuffer);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[trgPBO]);
        if (processingState.get(trgPBO))
            waitForProcessingToComplete(trgPBO);
        readBack(trgPBO);
        if (0 < synchronousFrames) {
            copyFrames(trgPBO, srcPBO);
            synchronousFrames--;
        }
        pinBuffer(srcPBO);
        processingState.set(srcPBO, true);
        semaphores[srcPBO].acquireUninterruptibly();
        handler.process(
                width, height,
                pinnedBuffers[srcPBO],
                stride,
                semaphores[srcPBO]
        );
        bufferIndex++;
    }

    protected void readBack(final int index) {
        glPixelStorei(GL_PACK_ROW_LENGTH, stride >> 2);
        if (readbackType == ReadbackType.READ_PIXELS) {
            fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, msaaResolveFBO == 0 ? renderFBO : msaaResolveFBO);
            glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, 0);
            fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        } else {
            glBindTexture(GL_TEXTURE_2D, msaaResolveFBO == 0 ? rgbaBuffer : msaaResolveBuffer);
            glGetTexImage(GL_TEXTURE_2D, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, 0);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        glPixelStorei(GL_PACK_ROW_LENGTH, 0);
    }

    protected abstract void copyFrames(final int src, final int trg);

    protected abstract void pinBuffer(final int index);

    protected void destroyObjects() {
        for (int i = 0; i < semaphores.length; i++) {
            if (processingState.get(i)) {
                glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[i]);
                waitForProcessingToComplete(i);
            }
        }
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
        for (int pbo : pbos) {
            if (pbo != 0)
                glDeleteBuffers(pbo);
        }
        if (msaaResolveBuffer != 0) {
            if (readbackType == ReadbackType.READ_PIXELS)
                fboUtil.deleteRenderbuffers(msaaResolveBuffer);
            else
                glDeleteTextures(msaaResolveBuffer);
        }
        if (depthBuffer != 0) fboUtil.deleteRenderbuffers(depthBuffer);
        if (rgbaBuffer != 0) {
            if (samples <= 1 && readbackType == ReadbackType.GET_TEX_IMAGE)
                glDeleteTextures(rgbaBuffer);
            else
                fboUtil.deleteRenderbuffers(rgbaBuffer);
        }
    }

    public void destroy() {
        destroyObjects();
        if (msaaResolveFBO != 0)
            fboUtil.deleteFramebuffers(msaaResolveFBO);
        fboUtil.deleteFramebuffers(renderFBO);
    }

    public enum ReadbackType {
        READ_PIXELS,
        GET_TEX_IMAGE
    }
}