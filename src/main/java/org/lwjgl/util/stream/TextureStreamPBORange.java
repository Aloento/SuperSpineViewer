package org.lwjgl.util.stream;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLSync;
import org.lwjgl.util.stream.StreamUtil.TextureStreamFactory;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.GL_SYNC_GPU_COMMANDS_COMPLETE;
import static org.lwjgl.opengl.GL32.glFenceSync;

public class TextureStreamPBORange extends TextureStreamPBO {
    public static final TextureStreamFactory FACTORY = new TextureStreamFactory("ARB_map_buffer_range") {
        public boolean isSupported(final ContextCapabilities caps) {
            return TextureStreamPBODefault.FACTORY.isSupported(caps) && (caps.OpenGL30 || caps.GL_ARB_map_buffer_range);
        }

        public TextureStream create(final StreamHandler handler, final int transfersToBuffer) {
            return new TextureStreamPBORange(handler, transfersToBuffer);
        }
    };

    private final GLSync[] fences;

    public TextureStreamPBORange(final StreamHandler handler, final int transfersToBuffer) {
        super(handler, transfersToBuffer);
        fences = new GLSync[this.transfersToBuffer];
    }

    protected void postUpload(final int index) {
        fences[index] = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    protected void postProcess(final int index) {
        glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
    }

    public void pinBuffer(final int index) {
        if (fences[index] != null)
            StreamUtil.waitOnFence(fences, index);

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbos[index]);
        glBufferData(GL_PIXEL_UNPACK_BUFFER, (long) height * stride, GL_STREAM_DRAW);
        pinnedBuffers[index] = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, (long) height * stride, GL_MAP_WRITE_BIT | GL_MAP_UNSYNCHRONIZED_BIT, pinnedBuffers[index]);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
    }

    public void destroy() {
        destroyObjects();
    }
}
