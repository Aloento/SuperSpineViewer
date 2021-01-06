/*
 * Copyright (c) 2002-2012 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lwjgl.util.stream;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER;
import static org.lwjgl.opengl.GL30.*;

/**
 * Implements streaming PBO updates from a framebuffer object.
 */
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

        // Setup render FBO

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
            // Setup MSAA resolve FBO

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

        // Setup read-back buffers

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
            // Resolve MSAA
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

        // Back-pressure. Make sure we never buffer more than <transfersToBuffer> frames ahead.
        if (processingState.get(trgPBO))
            waitForProcessingToComplete(trgPBO);

        readBack(trgPBO);

        // This will be non-zero for the first (transfersToBuffer - 1) frames
        // after start-up or a resize.
        if (0 < synchronousFrames) {
            // The srcPBO is currently empty. Wait for trgPBO's ReadPixels to complete and copy the current frame to srcPBO.
            // We do this to avoid sending an empty buffer for processing, which would cause a visible flicker on resize.
            copyFrames(trgPBO, srcPBO);
            synchronousFrames--;
        }

        // Time to process the srcPBO

        pinBuffer(srcPBO);

        // Send the buffer for processing

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
        // Stride in pixels
        glPixelStorei(GL_PACK_ROW_LENGTH, stride >> 2);

        // Asynchronously transfer current frame
        if (readbackType == ReadbackType.READ_PIXELS) {
            fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, msaaResolveFBO == 0 ? renderFBO : msaaResolveFBO);
            glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, 0);
            fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        } else {
            glBindTexture(GL_TEXTURE_2D, msaaResolveFBO == 0 ? rgbaBuffer : msaaResolveBuffer);
            glGetTexImage(GL_TEXTURE_2D, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, 0);
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        // Restore PACK_ROW_LENGTH
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
        /**
         * RenderBuffers on FBO, ReadPixels to readback.
         */
        READ_PIXELS,
        /**
         * Textures on FBO, GetTexImage to readback.
         */
        GET_TEX_IMAGE
    }

}