package org.lwjgl.util.stream;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.stream.StreamUtil.RenderStreamFactory;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER;
import static org.lwjgl.opengl.GL31.GL_COPY_WRITE_BUFFER;
import static org.lwjgl.opengl.GL31.glCopyBufferSubData;

final class RenderStreamPBODefault extends RenderStreamPBO {
    public static final RenderStreamFactory FACTORY = new RenderStreamFactory("Asynchronous PBO") {
        public boolean isSupported(final ContextCapabilities caps) {
            return caps.OpenGL21 || caps.GL_ARB_pixel_buffer_object || caps.GL_EXT_pixel_buffer_object;
        }

        public RenderStream create(final StreamHandler handler, final int samples, final int transfersToBuffer) {
            final ContextCapabilities caps = GLContext.getCapabilities();
            return new RenderStreamPBODefault(
                handler, samples, transfersToBuffer,
                StreamUtil.isNVIDIA(caps) ? ReadbackType.GET_TEX_IMAGE : ReadbackType.READ_PIXELS
            );
        }
    };

    private final boolean USE_COPY_BUFFER_SUB_DATA;

    RenderStreamPBODefault(final StreamHandler handler, final int samples, final int transfersToBuffer, final ReadbackType readbackType) {
        super(handler, samples, transfersToBuffer, readbackType);
        final ContextCapabilities caps = GLContext.getCapabilities();

        USE_COPY_BUFFER_SUB_DATA = (caps.OpenGL31 || caps.GL_ARB_copy_buffer) &&
            !StreamUtil.isAMD(caps);
    }

    protected void pinBuffer(final int index) {
        glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[index]);
        pinnedBuffers[index] = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY, (long) height * stride, pinnedBuffers[index]);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
    }

    protected void copyFrames(final int src, final int trg) {
        if (USE_COPY_BUFFER_SUB_DATA) {
            glBindBuffer(GL_COPY_WRITE_BUFFER, pbos[trg]);
            glCopyBufferSubData(GL_PIXEL_PACK_BUFFER, GL_COPY_WRITE_BUFFER, 0, 0, (long) height * stride);
            glBindBuffer(GL_COPY_WRITE_BUFFER, 0);
        } else {
            pinnedBuffers[src] = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY, (long) height * stride, pinnedBuffers[src]);
            glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[trg]);
            glBufferSubData(GL_PIXEL_PACK_BUFFER, 0, pinnedBuffers[src]);
            glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[src]);
            glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
        }
    }

    protected void postProcess(final int index) {
        glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
    }
}
