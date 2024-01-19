package org.lwjgl.util.stream;

import org.lwjgl.BufferUtils;
import org.lwjgl.MemoryUtil;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLSync;
import org.lwjgl.util.stream.StreamUtil.PageSizeProvider;
import org.lwjgl.util.stream.StreamUtil.RenderStreamFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.opengl.AMDPinnedMemory.GL_EXTERNAL_VIRTUAL_MEMORY_BUFFER_AMD;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL32.GL_SYNC_GPU_COMMANDS_COMPLETE;
import static org.lwjgl.opengl.GL32.glFenceSync;

final class RenderStreamPBOAMD extends RenderStreamPBO {
    public static final RenderStreamFactory FACTORY = new RenderStreamFactory("AMD_pinned_memory") {
        public boolean isSupported(final ContextCapabilities caps) {
            return TextureStreamPBODefault.FACTORY.isSupported(caps) && caps.GL_AMD_pinned_memory && (caps.OpenGL32 || caps.GL_ARB_sync);
        }

        public RenderStream create(final StreamHandler handler, final int samples, final int transfersToBuffer) {
            return new RenderStreamPBOAMD(handler, samples, transfersToBuffer);
        }
    };
    private final GLSync[] fences;

    RenderStreamPBOAMD(final StreamHandler handler, final int samples, final int transfersToBuffer) {
        super(handler, samples, transfersToBuffer, ReadbackType.READ_PIXELS);
        fences = new GLSync[this.transfersToBuffer];
    }

    protected void resizeBuffers(final int height, final int stride) {
        final int renderBytes = height * stride;
        for (int i = 0; i < pbos.length; i++) {
            pbos[i] = glGenBuffers();
            glBindBuffer(GL_EXTERNAL_VIRTUAL_MEMORY_BUFFER_AMD, pbos[i]);
            final int PAGE_SIZE = PageSizeProvider.PAGE_SIZE;
            final ByteBuffer buffer = BufferUtils.createByteBuffer(renderBytes + PAGE_SIZE);
            final int pageOffset = (int) (MemoryUtil.getAddress(buffer) % PAGE_SIZE);
            buffer.position(PAGE_SIZE - pageOffset);
            buffer.limit(buffer.capacity() - pageOffset);
            pinnedBuffers[i] = buffer.slice().order(ByteOrder.nativeOrder());
            glBufferData(GL_EXTERNAL_VIRTUAL_MEMORY_BUFFER_AMD, pinnedBuffers[i], GL_STREAM_READ);
        }
        glBindBuffer(GL_EXTERNAL_VIRTUAL_MEMORY_BUFFER_AMD, 0);
    }

    protected void readBack(final int index) {
        super.readBack(index);
        fences[index] = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    protected void pinBuffer(final int index) {
        if (fences[index] != null)
            StreamUtil.waitOnFence(fences, index);
    }

    protected void copyFrames(final int src, final int trg) {
        StreamUtil.waitOnFence(fences, src);
        final ByteBuffer srcBuffer = pinnedBuffers[src];
        final ByteBuffer trgBuffer = pinnedBuffers[trg];
        trgBuffer.put(srcBuffer);
        trgBuffer.flip();
        srcBuffer.flip();
    }

    protected void postProcess(final int index) {
    }

    protected void destroyObjects() {
        for (int i = 0; i < fences.length; i++) {
            if (fences[i] != null)
                StreamUtil.waitOnFence(fences, i);
        }
        super.destroyObjects();
    }
}