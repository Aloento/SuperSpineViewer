package org.lwjgl.util.stream;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.util.stream.StreamUtil.RenderStreamFactory;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER;
import static org.lwjgl.opengl.GL31.*;

final class RenderStreamPBOCopy extends RenderStreamPBO {
    public static final RenderStreamFactory FACTORY = new RenderStreamFactory("ARB_copy_buffer") {
        public boolean isSupported(final ContextCapabilities caps) {
            return RenderStreamPBODefault.FACTORY.isSupported(caps)
                    && caps.GL_ARB_copy_buffer
                    && caps.GL_NV_gpu_program5
                    && (caps.OpenGL40 || caps.GL_ARB_tessellation_shader)
                    ;
        }

        public RenderStream create(final StreamHandler handler, final int samples, final int transfersToBuffer) {
            return new RenderStreamPBOCopy(handler, samples, transfersToBuffer);
        }
    };
    private int devicePBO;

    RenderStreamPBOCopy(final StreamHandler handler, final int samples, final int transfersToBuffer) {
        super(handler, samples, transfersToBuffer, ReadbackType.GET_TEX_IMAGE);
    }

    protected void resizeBuffers(final int height, final int stride) {
        super.resizeBuffers(height, stride);
        devicePBO = glGenBuffers();
        glBindBuffer(GL_PIXEL_PACK_BUFFER, devicePBO);
        glBufferData(GL_PIXEL_PACK_BUFFER, height * stride, GL_STREAM_COPY);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
    }

    protected void readBack(final int index) {
        glBindBuffer(GL_PIXEL_PACK_BUFFER, devicePBO);
        super.readBack(index);
        glBindBuffer(GL_COPY_WRITE_BUFFER, pbos[index]);
        glCopyBufferSubData(GL_PIXEL_PACK_BUFFER, GL_COPY_WRITE_BUFFER, 0, 0, height * stride);
        glBindBuffer(GL_COPY_WRITE_BUFFER, 0);
        glBindBuffer(GL_COPY_READ_BUFFER, 0);
    }

    protected void pinBuffer(final int index) {
        glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[index]);
        pinnedBuffers[index] = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY, height * stride, pinnedBuffers[index]);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
    }

    protected void copyFrames(final int src, final int trg) {
        glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[src]);
        glBindBuffer(GL_COPY_WRITE_BUFFER, pbos[trg]);
        glCopyBufferSubData(GL_PIXEL_PACK_BUFFER, GL_COPY_WRITE_BUFFER, 0, 0, height * stride);
        glBindBuffer(GL_COPY_WRITE_BUFFER, 0);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
    }

    protected void postProcess(final int index) {
        glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
    }

    protected void destroyObjects() {
        glDeleteBuffers(devicePBO);
        super.destroyObjects();
    }
}