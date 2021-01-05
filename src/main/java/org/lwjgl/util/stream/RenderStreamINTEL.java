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
import org.lwjgl.util.stream.StreamUtil.RenderStreamFactory;

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
final class RenderStreamINTEL extends StreamBuffered implements RenderStream {

	public static final RenderStreamFactory FACTORY = new RenderStreamFactory("INTEL_map_texture") {
		public boolean isSupported(final ContextCapabilities caps) {
			// TODO: We currently require BlitFramebuffer. Relax and implement manually?
			return caps.GL_INTEL_map_texture && (caps.OpenGL30 || caps.GL_ARB_framebuffer_object || caps.GL_EXT_framebuffer_blit);
		}

		public RenderStream create(final StreamHandler handler, final int samples, final int transfersToBuffer) {
			return new RenderStreamINTEL(handler, samples, transfersToBuffer);
		}
	};

	private final IntBuffer strideBuffer;
	private final IntBuffer layoutBuffer;

	private final StreamUtil.FBOUtil fboUtil;
	private final int                renderFBO;

	private int rgbaBuffer;
	private int depthBuffer;

	private final int   resolveFBO;
	private final int[] resolveBuffers;

	private int samples;

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

	public StreamHandler getHandler() {
		return handler;
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

		bufferIndex = synchronousFrames = transfersToBuffer - 1;

		// Setup render FBO

		fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, renderFBO);

		rgbaBuffer = StreamUtil.createRenderBuffer(fboUtil, width, height, samples, GL_RGBA8);
		fboUtil.framebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, rgbaBuffer);

		depthBuffer = StreamUtil.createRenderBuffer(fboUtil, width, height, samples, GL_DEPTH24_STENCIL8);
		fboUtil.framebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer);

		glViewport(0, 0, width, height);

		fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

		for ( int i = 0; i < resolveBuffers.length; i++ )
			resolveBuffers[i] = genLayoutLinearTexture(width, height);

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

	public void bind() {
		if ( this.width != handler.getWidth() || this.height != handler.getHeight() )
			resize(handler.getWidth(), handler.getHeight());

		fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, renderFBO);
	}

	private void prepareFramebuffer(final int trgTEX) {
		// Back-pressure. Make sure we never buffer more than <transfersToBuffer> frames ahead.
		if ( processingState.get(trgTEX) )
			waitForProcessingToComplete(trgTEX);

		fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, renderFBO);
		fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, resolveFBO);

		// Blit current texture
		fboUtil.framebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, resolveBuffers[trgTEX], 0);
		fboUtil.blitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);

		fboUtil.bindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
		fboUtil.bindFramebuffer(GL_READ_FRAMEBUFFER, 0);
	}

	public void swapBuffers() {
		if ( width == 0 || height == 0 )
			return;

		final int trgTEX = (int)(bufferIndex % transfersToBuffer);
		final int srcTEX = (int)((bufferIndex - 1) % transfersToBuffer);

		prepareFramebuffer(trgTEX);

		// This will be non-zero for the first (transfersToBuffer - 1) frames
		// after start-up or a resize.
		if ( 0 < synchronousFrames ) {
			// The srcTEX is currently empty. Wait for trgPBO's ReadPixels to complete and copy the current frame to srcTEX.
			// We do this to avoid sending an empty buffer for processing, which would cause a visible flicker on resize.
			copyFrames(trgTEX, srcTEX);
			synchronousFrames--;
		}

		// Time to process the srcTEX

		pinBuffer(srcTEX);

		// Send the buffer for processing

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
		pinnedBuffers[src] = glMapTexture2DINTEL(resolveBuffers[src], 0, height * stride, GL_MAP_READ_BIT, strideBuffer, layoutBuffer, pinnedBuffers[src]);
		pinnedBuffers[trg] = glMapTexture2DINTEL(resolveBuffers[trg], 0, height * stride, GL_MAP_WRITE_BIT, strideBuffer, layoutBuffer, pinnedBuffers[trg]);

		pinnedBuffers[trg].put(pinnedBuffers[src]);

		pinnedBuffers[src].flip();
		pinnedBuffers[trg].flip();

		glUnmapTexture2DINTEL(resolveBuffers[trg], 0);
		glUnmapTexture2DINTEL(resolveBuffers[src], 0);
	}

	private void pinBuffer(final int index) {
		final int texID = resolveBuffers[index];

		pinnedBuffers[index] = glMapTexture2DINTEL(texID, 0, height * stride, GL_MAP_READ_BIT, strideBuffer, layoutBuffer, pinnedBuffers[index]);
		// TODO: Row alignment is currently hardcoded to 16 pixels
		// We wouldn't need to do that if we could create a ByteBuffer
		// from an arbitrary address + length. Consider for LWJGL 3.0?
		checkStride(index, texID);
	}

	private void checkStride(final int index, final int texID) {
		if ( strideBuffer.get(0) != stride ) {
			System.err.println("Wrong stride: " + stride + ". Should be: " + strideBuffer.get(0));
			glUnmapTexture2DINTEL(texID, 0);
			stride = strideBuffer.get(0);
			pinnedBuffers[index] = glMapTexture2DINTEL(texID, 0, height * stride, GL_MAP_READ_BIT, strideBuffer, layoutBuffer, pinnedBuffers[index]);
		}
	}

	protected void postProcess(int index) {
		glUnmapTexture2DINTEL(resolveBuffers[index], 0);
	}

	private void destroyObjects() {
		for ( int i = 0; i < semaphores.length; i++ ) {
			if ( processingState.get(i) )
				waitForProcessingToComplete(i);
		}

		if ( rgbaBuffer != 0 ) fboUtil.deleteRenderbuffers(rgbaBuffer);
		if ( depthBuffer != 0 ) fboUtil.deleteRenderbuffers(depthBuffer);

		for ( int i = 0; i < resolveBuffers.length; i++ ) {
			glDeleteTextures(resolveBuffers[i]);
			resolveBuffers[i] = 0;
		}
	}

	public void destroy() {
		destroyObjects();

		if ( resolveFBO != 0 )
			fboUtil.deleteFramebuffers(resolveFBO);
		fboUtil.deleteFramebuffers(renderFBO);
	}

}