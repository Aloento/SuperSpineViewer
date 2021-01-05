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
import org.lwjgl.util.stream.StreamUtil.RenderStreamFactory;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL31.*;

/** Default StreamPBOReader implementation: Asynchronous ReadPixels to PBOs */
final class RenderStreamPBODefault extends RenderStreamPBO {

	public static final RenderStreamFactory FACTORY = new RenderStreamFactory("Asynchronous PBO") {
		public boolean isSupported(final ContextCapabilities caps) {
			return caps.OpenGL21 || caps.GL_ARB_pixel_buffer_object || caps.GL_EXT_pixel_buffer_object;
		}

		public RenderStream create(final StreamHandler handler, final int samples, final int transfersToBuffer) {
			final ContextCapabilities caps = GLContext.getCapabilities();

			return new RenderStreamPBODefault(
				handler, samples, transfersToBuffer,
				// Detect NVIDIA and use GetTexImage instead of ReadPixels
				StreamUtil.isNVIDIA(caps) ? ReadbackType.GET_TEX_IMAGE : ReadbackType.READ_PIXELS
			);
		}
	};

	private final boolean USE_COPY_BUFFER_SUB_DATA;

	RenderStreamPBODefault(final StreamHandler handler, final int samples, final int transfersToBuffer, final ReadbackType readbackType) {
		super(handler, samples, transfersToBuffer, readbackType);

		final ContextCapabilities caps = GLContext.getCapabilities();

		USE_COPY_BUFFER_SUB_DATA = (caps.OpenGL31 || caps.GL_ARB_copy_buffer) &&
		                           // Disable on ATI/AMD GPUs: ARB_copy_buffer is unoptimized on current
		                           // drivers and kills performance. TODO: Fix?
		                           !StreamUtil.isAMD(caps);
	}

	protected void pinBuffer(final int index) {
		glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[index]);

		// We don't need to manually synchronized here, MapBuffer will block until ReadPixels above has finished.
		// The buffer will be unmapped in waitForProcessingToComplete
		pinnedBuffers[index] = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY, height * stride, pinnedBuffers[index]);

		glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
	}

	protected void copyFrames(final int src, final int trg) {
		if ( USE_COPY_BUFFER_SUB_DATA ) {
			glBindBuffer(GL_COPY_WRITE_BUFFER, pbos[trg]);
			glCopyBufferSubData(GL_PIXEL_PACK_BUFFER, GL_COPY_WRITE_BUFFER, 0, 0, height * stride);
			glBindBuffer(GL_COPY_WRITE_BUFFER, 0);
		} else {
			pinnedBuffers[src] = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY, height * stride, pinnedBuffers[src]);

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