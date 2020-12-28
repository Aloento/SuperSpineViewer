package com.badlogic.gdx.backends.lwjgl;

import com.QYun.SuperSpineViewer.GUI;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class LwjglFXGraphics extends LwjglGraphics {
	ImageView target;
	LwjglToJavaFX toFX;
	GUI UITag;

	LwjglFXGraphics (LwjglApplicationConfiguration config, ImageView target, GUI UITag) {
		super(config);
		this.target = target;
		this.UITag = UITag;
	}
	
	@Override
	public int getHeight () {
		return (int) target.getLayoutBounds().getHeight();
	}
	
	@Override
	public int getWidth () {
		return (int) target.getLayoutBounds().getWidth();
	}

	@Override
	void setupDisplay () throws LWJGLException {
		if (canvas != null) {
			Display.setParent(canvas);
		} else {
			if (!displayMode(config.width, config.height, config.fullscreen))
				throw new GdxRuntimeException("Couldn't set display mode " + config.width + "x" + config.height + ", fullscreen: "
					+ config.fullscreen);

			if (config.iconPaths.size > 0) {
				ByteBuffer[] icons = new ByteBuffer[config.iconPaths.size];
				for (int i = 0, n = config.iconPaths.size; i < n; i++) {
					Pixmap pixmap = new Pixmap(Gdx.files.getFileHandle(config.iconPaths.get(i), config.iconFileTypes.get(i)));
					if (pixmap.getFormat() != Format.RGBA8888) {
						Pixmap rgba = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Format.RGBA8888);
						rgba.drawPixmap(pixmap, 0, 0);
						pixmap = rgba;
					}
					icons[i] = ByteBuffer.allocateDirect(pixmap.getPixels().limit());
					icons[i].put(pixmap.getPixels()).flip();
					pixmap.dispose();
				}
			}
		}
		Display.setInitialBackground(config.initialBackgroundColor.r, config.initialBackgroundColor.g,
			config.initialBackgroundColor.b);

		if (config.x != -1 && config.y != -1) Display.setLocation(config.x, config.y);
		createDisplayPixelFormat();
		config.x = Display.getX();
		config.y = Display.getY();
		this.initiateGL();
	}

	@Override
	void initiateGL() {
		extractVersion();
		extractExtensions();
		this.initiateGLInstances();
	}

	@Override
	public void initiateGLInstances() {
		if (this.usingGL30) {
			this.gl30 = new LwjglGL30();
			this.gl20 = this.gl30;
		} else {
			this.gl20 = new LwjglGL20();
		}

		Gdx.gl = this.gl20;
		Gdx.gl20 = this.gl20;
		Gdx.gl30 = this.gl30;
	}

	private boolean displayMode(int width, int height, boolean fullscreen) {
		if (this.getWidth() == width && this.getHeight() == height && Display.isFullscreen() == fullscreen) {
			return true;
		} else {
			try {
				org.lwjgl.opengl.DisplayMode targetDisplayMode = null;
				if (fullscreen) {
					org.lwjgl.opengl.DisplayMode[] modes = Display.getAvailableDisplayModes();
					int freq = 0;

					for (org.lwjgl.opengl.DisplayMode current : modes) {
						if (current.getWidth() == width && current.getHeight() == height) {
							if ((targetDisplayMode == null || current.getFrequency() >= freq) && (targetDisplayMode == null || current.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel())) {
								targetDisplayMode = current;
								freq = current.getFrequency();
							}

							if (current.getBitsPerPixel() == Display.getDesktopDisplayMode().getBitsPerPixel() && current.getFrequency() == Display.getDesktopDisplayMode().getFrequency()) {
								targetDisplayMode = current;
								break;
							}
						}
					}
				} else {
					targetDisplayMode = new org.lwjgl.opengl.DisplayMode(width, height);
				}

				if (targetDisplayMode == null) {
					return false;
				} else {
					boolean resizable = !fullscreen && this.config.resizable;
					Display.setDisplayMode(targetDisplayMode);
					Display.setFullscreen(fullscreen);
					if (resizable == Display.isResizable()) {
						Display.setResizable(!resizable);
					}

					Display.setResizable(resizable);
					float scaleFactor = Display.getPixelScaleFactor();
					this.config.width = (int) ((float) targetDisplayMode.getWidth() * scaleFactor);
					this.config.height = (int) ((float) targetDisplayMode.getHeight() * scaleFactor);
					if (Gdx.gl != null) {
						Gdx.gl.glViewport(0, 0, this.config.width, this.config.height);
					}

					this.resize = true;
					return true;
				}
			} catch (LWJGLException var9) {
				return false;
			}
		}
	}

	static Array<String> extensions;
	private static void extractVersion() {
		String versionString = GL11.glGetString(7938);
		String vendorString = GL11.glGetString(7936);
		String rendererString = GL11.glGetString(7937);
		glVersion = new GLVersion(Application.ApplicationType.Desktop, versionString, vendorString, rendererString);
	}

	private static void extractExtensions() {
		extensions = new Array();
		if (glVersion.isVersionEqualToOrHigher(3, 2)) {
			int numExtensions = GL11.glGetInteger(33309);

			for(int i = 0; i < numExtensions; ++i) {
				extensions.add(org.lwjgl.opengl.GL30.glGetStringi(7939, i));
			}
		} else {
			extensions.addAll(GL11.glGetString(7939).split(" "));
		}

	}

	@Override
	public boolean supportsExtension(String extensions) {
		if (LwjglFXGraphics.extensions == null) {
			LwjglFXGraphics.extensions.add(this.gl20.glGetString(7939));
		}

		return LwjglFXGraphics.extensions.contains(extensions, false);
	}

	private void createDisplayPixelFormat () {
		bufferFormat = new BufferFormat(config.r, config.g, config.b, config.a, config.depth, config.stencil, config.samples, false);
		this.toFX = new LwjglToJavaFX(target);
	}

	@Override
	public void setTitle(String FPS){
		Platform.runLater(() -> {
			UITag.RightInf.setText(FPS + "\t渲染正常");
		});
	}
}
