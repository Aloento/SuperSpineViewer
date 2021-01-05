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

import org.lwjgl.BufferUtils;
import org.lwjgl.MemoryUtil;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLSync;
import org.lwjgl.util.stream.StreamUtil.PageSizeProvider;
import org.lwjgl.util.stream.StreamUtil.RenderStreamFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.opengl.AMDPinnedMemory.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL32.*;

/** Optimized StreamPBOReader for AMD GPUs: Asynchronous ReadPixels to AMD_pinned_memory buffers. */
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

		for ( int i = 0; i < pbos.length; i++ ) {
			pbos[i] = glGenBuffers();

			glBindBuffer(GL_EXTERNAL_VIRTUAL_MEMORY_BUFFER_AMD, pbos[i]);

			// Pre-allocate page-aligned pinned buffers
			final int PAGE_SIZE = PageSizeProvider.PAGE_SIZE;

			final ByteBuffer buffer = BufferUtils.createByteBuffer(renderBytes + PAGE_SIZE);
			final int pageOffset = (int)(MemoryUtil.getAddress(buffer) % PAGE_SIZE);
			buffer.position(PAGE_SIZE - pageOffset); // Aligns to page
			buffer.limit(buffer.capacity() - pageOffset); // Caps remaining() to renderBytes

			pinnedBuffers[i] = buffer.slice().order(ByteOrder.nativeOrder());
			glBufferData(GL_EXTERNAL_VIRTUAL_MEMORY_BUFFER_AMD, pinnedBuffers[i], GL_STREAM_READ);
		}

		glBindBuffer(GL_EXTERNAL_VIRTUAL_MEMORY_BUFFER_AMD, 0);
	}

	protected void readBack(final int index) {
		super.readBack(index);

		// Insert a fence after ReadPixels
		fences[index] = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
	}

	protected void pinBuffer(final int index) {
		if ( fences[index] != null ) // Wait for ReadPixels on the PBO to complete
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
		for ( int i = 0; i < fences.length; i++ ) {
			if ( fences[i] != null )
				StreamUtil.waitOnFence(fences, i);
		}

		super.destroyObjects();
	}

}