package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxRuntimeException;
import javafx.scene.image.ImageView;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * An OpenGL surface for JavaFX, allowing OpenGL to be embedded in a JavaFX application.
 * 
 * @author Natan Guilherme
 */
public class LwjglFXNode implements Application
{
	Pbuffer pbuffer;
	final LwjglFXGraphics graphics;
	final OpenALAudio audio;
	final LwjglFiles files;
	final XpeFXInput input;
	final LwjglNet net;

	final ApplicationListener listener;
	final List<Runnable> runnables = new ArrayList<>();
	final List<Runnable> executedRunnables = new ArrayList<>();
	final Array<LifecycleListener> lifecycleListeners = new Array<>();
	boolean running = true;
	int lastWidth;
	int lastHeight;
	int logLevel = LOG_INFO;

	LwjglFXNode parent;
	Array<LwjglFXNode> childContexts;

	public LwjglFXNode(ApplicationListener listener, ImageView imageView)
	{
		this(listener, imageView, null);
		childContexts = new Array<>(false, 2);
	}

	PixelFormat pixelFormat;

	public LwjglFXNode(ApplicationListener listener, ImageView imageView, LwjglFXNode parent)
	{
		LwjglNativesLoader.load();

		this.parent = parent;

		Pbuffer sharedDrawable = null;

		pixelFormat = new PixelFormat(8 + 8 + 8, 8, 16, 0, 0);

		if (parent != null)
		{ // add this child to parent context array
			sharedDrawable = parent.pbuffer;
			parent.childContexts.add(this);
		}

		try
		{
			pbuffer = new Pbuffer(1, 1, pixelFormat, null, sharedDrawable, new ContextAttribs().withDebug(false));
			pbuffer.makeCurrent();
			// pbuffer.makeCurrent();
		}
		catch (LWJGLException e)
		{
			throw new RuntimeException(e);
		}

		graphics = new LwjglFXGraphics(imageView);

		if (!LwjglApplicationConfiguration.disableAudio && Gdx.audio == null)
		{
			audio = new OpenALAudio();
			Gdx.audio = audio;
		}
		else
		{
			audio = null;
		}
		if (Gdx.files == null)
		{
			files = new LwjglFiles();
			Gdx.files = files;
		}
		else
		{
			files = null;
		}
		if (Gdx.net == null)
		{
			net = new LwjglNet();
			Gdx.net = net;
		}
		else
		{
			net = null;
		}

		input = new XpeFXInput(imageView);

		this.listener = listener;
		setGlobals();
	}

	@Override
	public ApplicationListener getApplicationListener()
	{
		return listener;
	}

	@Override
	public Audio getAudio()
	{
		return Gdx.audio;
	}

	@Override
	public Files getFiles()
	{
		return files;
	}

	@Override
	public Graphics getGraphics()
	{
		return graphics;
	}

	@Override
	public Input getInput()
	{
		return input;
	}

	@Override
	public Net getNet()
	{
		return net;
	}

	@Override
	public ApplicationType getType()
	{
		return ApplicationType.Desktop;
	}

	@Override
	public int getVersion()
	{
		return 0;
	}

	void setGlobals()
	{
		Gdx.app = this;
		if (audio != null)
			Gdx.audio = audio;
		if (files != null)
			Gdx.files = files;
		if (net != null)
			Gdx.net = net;
		Gdx.graphics = graphics;
		Gdx.input = input;
	}

	void create()
	{
		try
		{
			setGlobals();
			graphics.initiateGLInstances();
			listener.create();
			lastWidth = Math.max(1, graphics.getWidth());
			lastHeight = Math.max(1, graphics.getHeight());
			listener.resize(lastWidth, lastHeight);
		}
		catch (Exception ex)
		{
			throw new GdxRuntimeException(ex);
		}
	}

	public void runSingleThread(final CountDownLatch running)
	{
		create();
		for (int i = 0; i < childContexts.size; i++)
			childContexts.get(i).create();

		while (0 < running.getCount())
		{
			render();
			for (int i = 0; i < childContexts.size; i++)
				childContexts.get(i).render();

			if (graphics.isVsync())
				Display.sync(60);
		}

		exit();

		for (int i = 0; i < childContexts.size; i++)
			childContexts.get(i).exit();

	}

	public void run(final CountDownLatch running)
	{
		create();

		while (0 < running.getCount())
		{
			render();
		}

		exit();

	}

	void render()
	{

		if (!running)
			return;

		graphics.drainPendingActionsQueue();

		graphics.renderStream.bind();

		setGlobals();
		graphics.updateTime();

		int width = Math.max(1, graphics.getWidth());
		int height = Math.max(1, graphics.getHeight());
		if (lastWidth != width || lastHeight != height)
		{
			lastWidth = width;
			lastHeight = height;
			Gdx.gl.glViewport(0, 0, lastWidth, lastHeight);
			listener.resize(width, height);
		}

		synchronized (runnables)
		{
			executedRunnables.clear();
			executedRunnables.addAll(runnables);
			runnables.clear();

			for (Runnable executedRunnable : executedRunnables) {
				try {
					executedRunnable.run();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}

		input.processEvents();
		if (running)
		{
			listener.render();
			if (audio != null)
			{
				audio.update();
			}
		}

		graphics.renderStream.swapBuffers();
	}

	public void stop()
	{
		if (!running)
			return;
		running = false;
		setGlobals();
		Array<LifecycleListener> listeners = lifecycleListeners;
		synchronized (listeners)
		{
			for (LifecycleListener listener : listeners)
			{
				listener.pause();
				listener.dispose();
			}
		}
		listener.pause();
		listener.dispose();

		Gdx.app = null;

		Gdx.graphics = null;

		if (audio != null)
		{
			audio.dispose();
			Gdx.audio = null;
		}

		if (files != null)
			Gdx.files = null;

		if (net != null)
			Gdx.net = null;

		graphics.dispose();
		pbuffer.destroy();

	}

	@Override
	public long getJavaHeap()
	{
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	@Override
	public long getNativeHeap()
	{
		return getJavaHeap();
	}

	Map<String, Preferences> preferences = new HashMap<>();

	@Override
	public Preferences getPreferences(String name)
	{
		if (preferences.containsKey(name))
		{
			return preferences.get(name);
		}
		else
		{
			Preferences prefs = new LwjglPreferences(name, ".prefs/");
			preferences.put(name, prefs);
			return prefs;
		}
	}

	@Override
	public Clipboard getClipboard()
	{
		// TODO clipboard in JavaFX ??
		return null;
	}

	@Override
	public void postRunnable(Runnable runnable)
	{
		synchronized (runnables)
		{
			runnables.add(runnable);
		}
	}

	@Override
	public void debug(String tag, String message)
	{
		if (logLevel >= LOG_DEBUG)
		{
			System.out.println(tag + ": " + message);
		}
	}

	@Override
	public void debug(String tag, String message, Throwable exception)
	{
		if (logLevel >= LOG_DEBUG)
		{
			System.out.println(tag + ": " + message);
			exception.printStackTrace(System.out);
		}
	}

	public void log(String tag, String message)
	{
		if (logLevel >= LOG_INFO)
		{
			System.out.println(tag + ": " + message);
		}
	}

	@Override
	public void log(String tag, String message, Throwable exception)
	{
		if (logLevel >= LOG_INFO)
		{
			System.out.println(tag + ": " + message);
			exception.printStackTrace(System.out);
		}
	}

	@Override
	public void error(String tag, String message)
	{
		if (logLevel >= LOG_ERROR)
		{
			System.err.println(tag + ": " + message);
		}
	}

	@Override
	public void error(String tag, String message, Throwable exception)
	{
		if (logLevel >= LOG_ERROR)
		{
			System.err.println(tag + ": " + message);
			exception.printStackTrace(System.err);
		}
	}

	@Override
	public void setLogLevel(int logLevel)
	{
		this.logLevel = logLevel;
	}

	@Override
	public int getLogLevel()
	{
		return logLevel;
	}

	@Override
	public void exit()
	{
		stop();

	}

	/**
	 * Make the canvas' context current. It is highly recommended that the context is only made current inside the AWT thread (for example in an overridden paintGL()).
	 */
	public void makeCurrent()
	{
		try
		{
			pbuffer.makeCurrent();
			setGlobals();
		}
		catch (LWJGLException ex)
		{
			throw new GdxRuntimeException(ex);
		}
	}

	/** Test whether the canvas' context is current. */
	public boolean isCurrent()
	{
		try
		{
			return pbuffer.isCurrent();
		}
		catch (LWJGLException ex)
		{
			throw new GdxRuntimeException(ex);
		}
	}

	/**
	 * @param cursor
	 *            May be null.
	 */
	public void setCursor(Cursor cursor)
	{
	}

	@Override
	public void addLifecycleListener(LifecycleListener listener)
	{
		synchronized (lifecycleListeners)
		{
			lifecycleListeners.add(listener);
		}
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener)
	{
		synchronized (lifecycleListeners)
		{
			lifecycleListeners.removeValue(listener, true);
		}
	}
}
