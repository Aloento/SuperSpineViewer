package org.lwjgl.util.stream;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.util.stream.StreamUtil.TextureStreamFactory;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;

public class TextureStreamPBODefault extends TextureStreamPBO {
    public static final TextureStreamFactory FACTORY = new TextureStreamFactory("Asynchronous PBO") {
        public boolean isSupported(final ContextCapabilities caps) {
            return caps.OpenGL21 || caps.GL_ARB_pixel_buffer_object || caps.GL_EXT_pixel_buffer_object;
        }

        public TextureStream create(final StreamHandler handler, final int transfersToBuffer) {
            return new TextureStreamPBODefault(handler, transfersToBuffer);
        }
    };

    public TextureStreamPBODefault(final StreamHandler handler, final int transfersToBuffer) {
        super(handler, transfersToBuffer);
    }

    protected void postProcess(final int index) {
        glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
    }

    protected void postUpload(final int index) {
    }

    public void pinBuffer(final int index) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbos[index]);
        glBufferData(GL_PIXEL_UNPACK_BUFFER, height * stride, GL_STREAM_DRAW);
        pinnedBuffers[index] = glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY, height * stride, pinnedBuffers[index]);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
    }

    public void destroy() {
        destroyObjects();
    }
}