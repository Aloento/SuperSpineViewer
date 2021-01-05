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
import org.lwjgl.util.stream.StreamUtil.TextureStreamFactory;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.*;

/** Implements streaming PBO updates to an OpenGL texture. */
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
		glBufferData(GL_PIXEL_UNPACK_BUFFER, height * stride, GL_STREAM_DRAW); // Orphan previous buffer
		pinnedBuffers[index] = glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY, height * stride, pinnedBuffers[index]);
		glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
	}

	public void destroy() {
		destroyObjects();
	}

}