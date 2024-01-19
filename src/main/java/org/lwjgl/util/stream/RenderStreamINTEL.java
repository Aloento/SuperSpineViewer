package org.lwjgl.util.stream;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.stream.StreamUtil.RenderStreamFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.INTELMapTexture.*;

final class RenderStreamINTEL extends StreamBuffered implements RenderStream {
    public static final RenderStreamFactory FACTORY = new RenderStreamFactory("INTEL_map_texture") {
        public boolean isSupported(final ContextCapabilities caps) {
            return caps.GL_INTEL_map_texture && (caps.OpenGL30 || caps.GL_ARB_framebuffer_object || caps.GL_EXT_framebuffer_blit);
        }

        public RenderStream create(final StreamHandler handler, final int samples, final int transfersToBuffer) {
            return new RenderStreamINTEL(handler, samples, transfersToBuffer);
        }
    };

    private final IntBuffer strideBuffer;
    private final IntBuffer layoutBuffer;
    private final StreamUtil.FBOUtil fboUtil;
    private final int renderFBO;
    private final int resolveFBO;
    private final int[] resolveBuffers;
    private final int samples;
    private int rgbaBuffer;
    private int depthBuffer;
    private int synchronousFrames;

    RenderStreamINTEL(final StreamHandler handler, final int samples, final int transfersToBuffer) {
        super(handler, transfersToBuffer);

        final ContextCapabilities caps = GLContext.getCapabilities();

        this.strideBuffer = BufferUtils.createIntBuffer(1);
        this.layoutBuffer = BufferUtils.createIntBuffer(1);

        fboUtil = StreamUtil.getFBOUtil(caps);
        renderFBO = fboUtil.genFramebuffers();
        resolveFBO = fboUtil.genFramebuffers();
        resolveBuffers = new int[transfersToBuffer];

        this.samples = StreamUtil.checkSamples(samples, caps);
    }

    private static int genLayoutLinearTexture(final int width, final int height) {
        final int texID = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MEMORY_LAYOUT_INTEL, GL_LAYOUT_LINEAR_CPU_CACHED_INTEL);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, (ByteBuffer) null);

        return texID;
    }

    public StreamHandler getHandler() {
        return handler;
    }

    private void resize(final int width, final int height) {
        if (width < 0 || height < 0)
            throw new IllegalArgumentException(STR."Invalid dimensions: \{width} x \{height}");

        destroyObjects();
        this.width = width;
        this.height = height;
        this.stride = StreamUtil.getStride(width);

        if (width == 0 || height == 0)
            return;

        bufferIndex = synchronousFrames = transfersToBuffer - 1;
        fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, renderFBO);

        rgbaBuffer = StreamUtil.createRenderBuffer(fboUtil, width, height, samples, GL_RGBA8);
        fboUtil.framebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, rgbaBuffer);

        depthBuffer = StreamUtil.createRenderBuffer(fboUtil, width, height, samples, GL_DEPTH24_STENCIL8);
        fboUtil.framebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer);

        glViewport(0, 0, width, height);
        fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

        for (int i = 0; i < resolveBuffers.length; i++)
            resolveBuffers[i] = genLayoutLinearTexture(width, height);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void bind() {
        if (this.width != handler.getWidth() || this.height != handler.getHeight())
            resize(handler.getWidth(), handler.getHeight());

        fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, renderFBO);
    }

    private void prepareFramebuffer(final int trgTEX) {
        if (processingState.get(trgTEX))
            waitForProcessingToComplete(trgTEX);

        fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, renderFBO);
        fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, resolveFBO);

        fboUtil.framebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, resolveBuffers[trgTEX], 0);

        fboUtil.blitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, 0);
    }

    public void swapBuffers() {
        if (width == 0 || height == 0)
            return;

        final int trgTEX = (int) (bufferIndex % transfersToBuffer);
        final int srcTEX = (int) ((bufferIndex - 1) % transfersToBuffer);

        prepareFramebuffer(trgTEX);

        if (0 < synchronousFrames) {
            copyFrames(trgTEX, srcTEX);
            synchronousFrames--;
        }

        pinBuffer(srcTEX);
        processingState.set(srcTEX, true);
        semaphores[srcTEX].acquireUninterruptibly();

        handler.process(
            width, height,
            pinnedBuffers[srcTEX],
            stride,
            semaphores[srcTEX]
        );

        bufferIndex++;
    }

    private void copyFrames(final int src, final int trg) {
        pinnedBuffers[src] = glMapTexture2DINTEL(resolveBuffers[src], 0, (long) height * stride, GL_MAP_READ_BIT, strideBuffer, layoutBuffer, pinnedBuffers[src]);
        pinnedBuffers[trg] = glMapTexture2DINTEL(resolveBuffers[trg], 0, (long) height * stride, GL_MAP_WRITE_BIT, strideBuffer, layoutBuffer, pinnedBuffers[trg]);

        pinnedBuffers[trg].put(pinnedBuffers[src]);
        pinnedBuffers[src].flip();
        pinnedBuffers[trg].flip();

        glUnmapTexture2DINTEL(resolveBuffers[trg], 0);
        glUnmapTexture2DINTEL(resolveBuffers[src], 0);
    }

    private void pinBuffer(final int index) {
        final int texID = resolveBuffers[index];
        pinnedBuffers[index] = glMapTexture2DINTEL(texID, 0, (long) height * stride, GL_MAP_READ_BIT, strideBuffer, layoutBuffer, pinnedBuffers[index]);
        checkStride(index, texID);
    }

    private void checkStride(final int index, final int texID) {
        if (strideBuffer.get(0) != stride) {
            System.err.println(STR."Wrong stride: \{stride}. Should be: \{strideBuffer.get(0)}");
            glUnmapTexture2DINTEL(texID, 0);

            stride = strideBuffer.get(0);
            pinnedBuffers[index] = glMapTexture2DINTEL(texID, 0, (long) height * stride, GL_MAP_READ_BIT, strideBuffer, layoutBuffer, pinnedBuffers[index]);
        }
    }

    protected void postProcess(int index) {
        glUnmapTexture2DINTEL(resolveBuffers[index], 0);
    }

    private void destroyObjects() {
        for (int i = 0; i < semaphores.length; i++) {
            if (processingState.get(i))
                waitForProcessingToComplete(i);
        }

        if (rgbaBuffer != 0) fboUtil.deleteRenderbuffers(rgbaBuffer);
        if (depthBuffer != 0) fboUtil.deleteRenderbuffers(depthBuffer);

        for (int i = 0; i < resolveBuffers.length; i++) {
            glDeleteTextures(resolveBuffers[i]);
            resolveBuffers[i] = 0;
        }
    }

    public void destroy() {
        destroyObjects();

        if (resolveFBO != 0)
            fboUtil.deleteFramebuffers(resolveFBO);

        fboUtil.deleteFramebuffers(renderFBO);
    }
}
