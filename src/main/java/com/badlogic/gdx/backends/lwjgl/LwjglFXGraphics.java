package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.GdxRuntimeException;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.lwjgl.opengl.*;
import org.lwjgl.util.stream.RenderStream;
import org.lwjgl.util.stream.StreamHandler;
import org.lwjgl.util.stream.StreamUtil;
import org.lwjgl.util.stream.StreamUtil.RenderStreamFactory;
import org.lwjgl.util.stream.StreamUtil.TextureStreamFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import static org.lwjgl.opengl.AMDDebugOutput.glDebugMessageCallbackAMD;
import static org.lwjgl.opengl.ARBDebugOutput.glDebugMessageCallbackARB;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL30.GL_MAX_SAMPLES;


/** An implementation of the {@link Graphics}  based on LwjglGraphics.
 * @author Natan Guilherme */
public class LwjglFXGraphics implements Graphics, StreamHandler
{
	static int major, minor;
	
	ImageView imageView;
	WritableImage renderImage;
	RenderStream renderStream;
	RenderStreamFactory renderStreamFactory;
	FPSListener fpsListener;
	
	int transfersToBuffer = 3;
	int samples           = 0;
	int     maxSamples;
	
	TextureStreamFactory textureStreamFactory;
	
	
	float deltaTime = 0;
	long frameStart = 0;
	int frames = 0;
	int fps;
	long lastTime = System.nanoTime();
	
	GL20 gl20;
	GL30 gl30;
	
	boolean vsync = true;
	
	private final ConcurrentLinkedQueue<Runnable> pendingRunnables;
	
	public LwjglFXGraphics(ImageView imageView)
	{
		this.imageView = imageView;
		this.pendingRunnables = new ConcurrentLinkedQueue<>();
		
		final ContextCapabilities caps = GLContext.getCapabilities();

		if ( caps.OpenGL30 || (caps.GL_EXT_framebuffer_multisample && caps.GL_EXT_framebuffer_blit) )
			maxSamples = glGetInteger(GL_MAX_SAMPLES);
		else
			maxSamples = 1;

		if ( caps.GL_ARB_debug_output )
			glDebugMessageCallbackARB(new ARBDebugOutputCallback());
		else if ( caps.GL_AMD_debug_output )
			glDebugMessageCallbackAMD(new AMDDebugOutputCallback());

		this.renderStreamFactory = StreamUtil.getRenderStreamImplementation();
		this.renderStream = renderStreamFactory.create(this, 0, transfersToBuffer);

		this.textureStreamFactory = StreamUtil.getTextureStreamImplementation();

		
		initiateGLInstances();
		
	}
	
	public void initiateGLInstances () {
		String version = org.lwjgl.opengl.GL11.glGetString(GL11.GL_VERSION);
		major = Integer.parseInt("" + version.charAt(0));
		minor = Integer.parseInt("" + version.charAt(2));

		gl20 = new LwjglGL20();

		if (major <= 1)
			throw new GdxRuntimeException("OpenGL 2.0 or higher with the FBO extension is required. OpenGL version: " + version);
		if (major == 2 || version.contains("2.1")) {
			if (!supportsExtension("GL_EXT_framebuffer_object") && !supportsExtension("GL_ARB_framebuffer_object")) {
				throw new GdxRuntimeException("OpenGL 2.0 or higher with the FBO extension is required. OpenGL version: " + version
					+ ", FBO extension: false");
			}
		}

		Gdx.gl = gl20;
		Gdx.gl20 = gl20;
	}
	
	@Override
	public boolean isGL30Available()
	{
		return false;
	}

	public GL20 getGL20 () {
		return gl20;
	}

	@Override
	public GL30 getGL30()
	{
		return gl30;
	}


	@Override
	public float getDeltaTime()
	{
		return deltaTime;
	}

	@Override
	public float getRawDeltaTime()
	{
		return deltaTime;
	}

	@Override
	public int getFramesPerSecond()
	{
		return fps;
	}

	@Override
	public GraphicsType getType()
	{
		return null;
	}

	@Override
	public float getPpiX()
	{
		return 0;
	}

	@Override
	public float getPpiY()
	{
		return 0;
	}

	@Override
	public float getPpcX()
	{
		return 0;
	}

	@Override
	public float getPpcY()
	{
		return 0;
	}

	@Override
	public float getDensity()
	{
		return 0;
	}

	@Override
	public boolean supportsDisplayModeChange()
	{
		return false;
	}

	@Override
	public DisplayMode[] getDisplayModes()
	{
		return null;
	}

	@Override
	public DisplayMode getDesktopDisplayMode()
	{
		return null;
	}

	@Override
	public boolean setDisplayMode(DisplayMode displayMode)
	{
		return false;
	}

	@Override
	public boolean setDisplayMode(int width, int height, boolean fullscreen)
	{
		return false;
	}

	@Override
	public void setTitle(String title)
	{
	}

	@Override
	public void setVSync(boolean vsync)
	{
		this.vsync = vsync;
	}

	public boolean isVsync()
	{
		return vsync;
	}

	@Override
	public BufferFormat getBufferFormat()
	{
		return null;
	}

	@Override
	public boolean supportsExtension(String extension)
	{
		return false;
	}

	@Override
	public void setContinuousRendering(boolean isContinuous)
	{
		
	}

	@Override
	public boolean isContinuousRendering()
	{
		return false;
	}

	@Override
	public void requestRendering()
	{
		
	}

	@Override
	public boolean isFullscreen()
	{
		return false;
	}

	
	void updateTime () {
		long time = System.nanoTime();
		deltaTime = (time - lastTime) / 1000000000.0f;
		lastTime = time;

		if (time - frameStart >= 1000000000) {
			fps = frames;
			if(fpsListener != null)
				fpsListener.fpsChanged(fps);
			frames = 0;
			frameStart = time;
		}
		frames++;
	}
	
	public int getWidth() {
		return (int)imageView.getFitWidth();
	}

	public int getHeight() {
		return (int)imageView.getFitHeight();
	}


	@Override
	public void process(final int width, final int height, final ByteBuffer data, final int stride, final Semaphore signal) {
		// This method runs in the background rendering thread
		// TODO: Run setPixels on the PlatformImage in this thread, run pixelsDirty on JFX application thread with runLater.
		Platform.runLater(() -> {
			try {
				// If we're quitting, discard update
				if ( !imageView.isVisible() )
					return;

				// Detect resize and recreate the image
				if ( renderImage == null || (int)renderImage.getWidth() != width || (int)renderImage.getHeight() != height ) {
					renderImage = new WritableImage(width, height);
					imageView.setImage(renderImage);

				}


				// Upload the image to JavaFX
				renderImage.getPixelWriter().setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getByteBgraPreInstance(), data, stride);
			} finally {
				// Notify the render thread that we're done processing
				signal.release();
			}
		});
	}
	
	public void dispose()
	{
		renderStream.destroy();
	}
	
	
	public void setTransfersToBuffer(final int transfersToBuffer) {
		if ( this.transfersToBuffer == transfersToBuffer )
			return;

		this.transfersToBuffer = transfersToBuffer;
		resetStreams();
	}

	public void setSamples(final int samples) {
		if ( this.samples == samples )
			return;

		this.samples = samples;
		resetStreams();
	}

	private void resetStreams() {
		pendingRunnables.offer(() -> {
			renderStream.destroy();

			renderStream = renderStreamFactory.create(renderStream.getHandler(), samples, transfersToBuffer);

			//updateSnapshot();
		});
	}
	
	void drainPendingActionsQueue() {
		Runnable runnable;

		while ( (runnable = pendingRunnables.poll()) != null )
			runnable.run();
	}


	public int getTransfersToBuffer() {
		return transfersToBuffer;
	}
	
	public int getMaxSamples() {
		return maxSamples;
	}

	public RenderStreamFactory getRenderStreamFactory() {
		return renderStreamFactory;
	}
	
	public void setRenderStreamFactory(final RenderStreamFactory renderStreamFactory) {
		pendingRunnables.offer(() -> {
			if ( renderStream != null )
				renderStream.destroy();
			LwjglFXGraphics.this.textureStreamFactory = textureStreamFactory;
			renderStream = renderStreamFactory.create(renderStream.getHandler(), samples, transfersToBuffer);
		});
	}
	
	public TextureStreamFactory getTextureStreamFactory() {
		return textureStreamFactory;
	}

	public void setTextureStreamFactory(final TextureStreamFactory textureStreamFactory) {
		pendingRunnables.offer(() -> LwjglFXGraphics.this.textureStreamFactory = textureStreamFactory);

	}
	
	public void setFPSListener(FPSListener listener)
	{
		fpsListener = listener;
	}
	
	public static interface FPSListener
	{
		public void fpsChanged(int fps);
	}

	@Override
	public long getFrameId()
	{
		return 0;
	}
	
}
