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

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.Semaphore;

/** Base functionality for buffered transfers. */
abstract class StreamBuffered {

	protected final StreamHandler handler;

	// Low: Less memory usage, less concurrency, less transfers behind
	// High: More memory usages, more concurrency, more transfers behind
	protected final int transfersToBuffer; // 3 provides optimal concurrency in most cases

	protected final ByteBuffer[] pinnedBuffers;
	protected final Semaphore[]  semaphores; // Required for synchronization with the processing thread

	/**
	 * A flag per pinned buffer that indicates if it's currently being
	 * processed by the handler.
	 */
	protected final BitSet processingState;

	protected int width;
	protected int height;
	protected int stride;

	protected long bufferIndex;

	protected StreamBuffered(final StreamHandler handler, final int transfersToBuffer) {
		this.handler = handler;
		this.transfersToBuffer = transfersToBuffer;

		pinnedBuffers = new ByteBuffer[transfersToBuffer];
		semaphores = new Semaphore[transfersToBuffer];
		for ( int i = 0; i < semaphores.length; i++ )
			semaphores[i] = new Semaphore(1, false);

		processingState = new BitSet(transfersToBuffer);
	}

	protected void waitForProcessingToComplete(final int index) {
		final Semaphore s = semaphores[index];
		// Early-out: start-up or handler has finished processing
		if ( s.availablePermits() == 0 ) {
			// This will block until handler has finished processing
			s.acquireUninterruptibly();
			// Give the permit back
			s.release();
		}

		postProcess(index);
		processingState.set(index, false);
	}

	protected abstract void postProcess(int index);

}