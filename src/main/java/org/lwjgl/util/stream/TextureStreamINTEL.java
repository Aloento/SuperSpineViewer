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
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.stream.StreamUtil.TextureStreamFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.INTELMapTexture.*;

/**
 * Optimized StreamPBOReader for Intel IGPs:
 * <p/>
 * - We render to a standard FBO.
 * - We asynchronously blit to another FBO with INTEL_map_texture attachments (linear layout).
 * - We synchronously map the linear textures.
 * <p/>
 */
final class TextureStreamINTEL extends StreamBuffered implements TextureStream {

	public static final TextureStreamFactory FACTORY = new TextureStreamFactory("INTEL_map_texture") {
		public boolean isSupported(final ContextCapabilities caps) {
			// TODO: We currently require BlitFramebuffer. Relax and implement manually?
			return caps.GL_INTEL_map_texture && (caps.OpenGL30 || caps.GL_ARB_framebuffer_object || caps.GL_EXT_framebuffer_blit);
		}

		public TextureStream create(final StreamHandler handler, final int transfersToBuffer) {
			return new TextureStreamINTEL(handler, transfersToBuffer);
		}
	};

	private final IntBuffer strideBuffer;
	private final IntBuffer layoutBuffer;

	private final StreamUtil.FBOUtil fboUtil;

	private final int texFBO;
	private final int bufferFBO;

	private int   texID;
	private int[] buffers;

	private long currentIndex;

	private boolean resetTexture;

	TextureStreamINTEL(final StreamHandler handler, final int transfersToBuffer) {
		super(handler, transfersToBuffer);

		this.strideBuffer = BufferUtils.createIntBuffer(1);
		this.layoutBuffer = BufferUtils.createIntBuffer(1);

		fboUtil = StreamUtil.getFBOUtil(GLContext.getCapabilities());

		texFBO = fboUtil.genFramebuffers();
		bufferFBO = fboUtil.genFramebuffers();

		buffers = new int[transfersToBuffer];
	}

	public StreamHandler getHandler() {
		return handler;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	private void resize(final int width, final int height) {
		if ( width < 0 || height < 0 )
			throw new IllegalArgumentException("Invalid dimensions: " + width + " x " + height);

		destroyObjects();

		this.width = width;
		this.height = height;

		this.stride = StreamUtil.getStride(width);

		if ( width == 0 || height == 0 )
			return;

		bufferIndex = 0;
		currentIndex = 0;

		resetTexture = true;

		texID = StreamUtil.createRenderTexture(width, height, GL_LINEAR);

		fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, texFBO);
		fboUtil.framebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texID, 0);
		fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

		for ( int i = 0; i < buffers.length; i++ )
			buffers[i] = genLayoutLinearTexture(width, height);

		glBindTexture(GL_TEXTURE_2D, 0);
	}

	private static int genLayoutLinearTexture(final int width, final int height) {
		final int texID = glGenTextures();

		glBindTexture(GL_TEXTURE_2D, texID);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MEMORY_LAYOUT_INTEL, GL_LAYOUT_LINEAR_CPU_CACHED_INTEL);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, (ByteBuffer)null);

		return texID;
	}

	public void snapshot() {
		if ( width != handler.getWidth() || height != handler.getHeight() )
			resize(handler.getWidth(), handler.getHeight());

		if ( width == 0 || height == 0 )
			return;

		final int trgPBO = (int)(bufferIndex % transfersToBuffer);

		// Back-pressure. Make sure we never buffer more than <transfersToBuffer> frames ahead.

		if ( processingState.get(trgPBO) )
			syncCopy(trgPBO);

		pinnedBuffers[trgPBO] = glMapTexture2DINTEL(buffers[trgPBO], 0, height * stride, GL_MAP_WRITE_BIT, strideBuffer, layoutBuffer, pinnedBuffers[trgPBO]);

		// Send the buffer for processing

		processingState.set(trgPBO, true);
		semaphores[trgPBO].acquireUninterruptibly();

		handler.process(
			width, height,
			pinnedBuffers[trgPBO],
			stride,
			semaphores[trgPBO]
		);

		bufferIndex++;

		if ( resetTexture ) {
			syncCopy(trgPBO);
			resetTexture = true;
		}
	}

	public void tick() {
		final int srcPBO = (int)(currentIndex % transfersToBuffer);
		if ( !processingState.get(srcPBO) )
			return;

		// Try again next frame
		if ( !semaphores[srcPBO].tryAcquire() )
			return;

		semaphores[srcPBO].release(); // Give it back

		postProcess(srcPBO);
		processingState.set(srcPBO, false);

		copyTexture(srcPBO);
	}

	private void syncCopy(final int index) {
		waitForProcessingToComplete(index);
		copyTexture(index);
	}

	private void copyTexture(final int index) {
		fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, bufferFBO);
		fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, texFBO);

		fboUtil.framebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, buffers[index], 0);
		fboUtil.blitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
		fboUtil.framebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);

		fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
		fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, 0);

		currentIndex++;
	}

	protected void postProcess(final int index) {
		glUnmapTexture2DINTEL(buffers[index], 0);
	}

	public void bind() {
		glBindTexture(GL_TEXTURE_2D, texID);
	}

	private void destroyObjects() {
		for ( int i = 0; i < semaphores.length; i++ ) {
			if ( processingState.get(i) )
				waitForProcessingToComplete(i);
		}

		for ( int i = 0; i < buffers.length; i++ ) {
			glDeleteTextures(buffers[i]);
			buffers[i] = 0;
		}

		glDeleteTextures(texID);
	}

	public void destroy() {
		destroyObjects();

		fboUtil.deleteFramebuffers(bufferFBO);
		fboUtil.deleteFramebuffers(texFBO);
	}

}