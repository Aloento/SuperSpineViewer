package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Pool;
import javafx.event.EventHandler;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class XpeFXInput implements Input
{
	static class KeyEvent
	{
		static final int KEY_DOWN = 0;
		static final int KEY_UP = 1;
		static final int KEY_TYPED = 2;

		long timeStamp;
		int type;
		int keyCode;
		char keyChar;
	}

	static class TouchEvent
	{
		static final int TOUCH_DOWN = 0;
		static final int TOUCH_UP = 1;
		static final int TOUCH_DRAGGED = 2;
		static final int TOUCH_MOVED = 3;
		static final int TOUCH_SCROLLED = 4;

		long timeStamp;
		int type;
		int x;
		int y;
		int pointer;
		int button;
		int scrollAmount;
	}

	Pool<KeyEvent> usedKeyEvents = new Pool<>(16, 1000) {
		protected KeyEvent newObject() {
			return new KeyEvent();
		}
	};

	Pool<TouchEvent> usedTouchEvents = new Pool<>(16, 1000) {
		protected TouchEvent newObject() {
			return new TouchEvent();
		}
	};

	List<KeyEvent> keyEvents = new ArrayList<>();
	List<TouchEvent> touchEvents = new ArrayList<>();
	int touchX = 0;
	int touchY = 0;
	int deltaX = 0;
	int deltaY = 0;
	boolean touchDown = false;
	boolean justTouched = false;
	Set<Integer> keys = new HashSet<>();
	Set<Integer> pressedButtons = new HashSet<>();
	InputProcessor processor;
	ImageView canvas;
	boolean catched = false;
	Robot robot = null;
	long currentEventTimeStamp;

	public XpeFXInput(ImageView canvas)
	{
		setListeners(canvas);
		try
		{
			robot = new Robot(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
		}
		catch (HeadlessException | AWTException e) {
			e.printStackTrace();
		}
	}

	public void setListeners(ImageView canvas)
	{
		// if (this.canvas != null) {
		// canvas.removeMouseListener(this);
		// canvas.removeMouseMotionListener(this);
		// canvas.removeMouseWheelListener(this);
		// canvas.removeKeyListener(this);
		// }
		// canvas.addMouseListener(this);
		// canvas.addMouseMotionListener(this);
		// canvas.addMouseWheelListener(this);
		// canvas.addKeyListener(this);
		// canvas.setFocusTraversalKeysEnabled(false);
		this.canvas = canvas;

		canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<>() {

			@Override
			public void handle(final MouseEvent e) {
				synchronized (this) {
					TouchEvent event = usedTouchEvents.obtain();
					event.pointer = 0;
					event.x = (int) e.getX();
					event.y = (int) e.getY();
					event.type = TouchEvent.TOUCH_DRAGGED;
					event.timeStamp = System.nanoTime();
					touchEvents.add(event);

					deltaX = event.x - touchX;
					deltaY = event.y - touchY;
					touchX = event.x;
					touchY = event.y;
					checkCatched(e);
					if (Gdx.graphics != null)
						Gdx.graphics.requestRendering();
				}
			}

		});

		canvas.addEventHandler(MouseEvent.MOUSE_MOVED, new EventHandler<>() {

			@Override
			public void handle(final MouseEvent e) {
				synchronized (this) {
					TouchEvent event = usedTouchEvents.obtain();
					event.pointer = 0;
					event.x = (int) e.getX();
					event.y = (int) e.getY();
					event.type = TouchEvent.TOUCH_MOVED;
					event.timeStamp = System.nanoTime();
					touchEvents.add(event);

					deltaX = event.x - touchX;
					deltaY = event.y - touchY;
					touchX = event.x;
					touchY = event.y;
					checkCatched(e);

					if (Gdx.graphics != null)
						Gdx.graphics.requestRendering();
				}
			}

		});

		canvas.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
			touchX = (int) e.getX();
			touchY = (int) e.getY();
			checkCatched(e);
			if (Gdx.graphics != null)
				Gdx.graphics.requestRendering();
		});

		canvas.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
			touchX = (int) e.getX();
			touchY = (int) e.getY();
			checkCatched(e);
			if (Gdx.graphics != null)
				Gdx.graphics.requestRendering();
		});

		canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<>() {

			@Override
			public void handle(final MouseEvent e) {
				synchronized (this) {
					TouchEvent event = usedTouchEvents.obtain();
					event.pointer = 0;
					event.x = (int) e.getX();
					event.y = (int) e.getY();
					event.type = TouchEvent.TOUCH_DOWN;
					event.button = toGdxButton(e.getButton());
					event.timeStamp = System.nanoTime();
					touchEvents.add(event);

					deltaX = event.x - touchX;
					deltaY = event.y - touchY;
					touchX = event.x;
					touchY = event.y;
					touchDown = true;
					pressedButtons.add(event.button);
					if (Gdx.graphics != null)
						Gdx.graphics.requestRendering();
				}
			}

		});

		canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<>() {

			@Override
			public void handle(final MouseEvent e) {
				synchronized (this) {
					TouchEvent event = usedTouchEvents.obtain();
					event.pointer = 0;
					event.x = (int) e.getX();
					event.y = (int) e.getY();
					event.button = toGdxButton(e.getButton());
					event.type = TouchEvent.TOUCH_UP;
					event.timeStamp = System.nanoTime();
					touchEvents.add(event);

					deltaX = event.x - touchX;
					deltaY = event.y - touchY;
					touchX = event.x;
					touchY = event.y;
					pressedButtons.remove(event.button);
					if (pressedButtons.size() == 0)
						touchDown = false;
					if (Gdx.graphics != null)
						Gdx.graphics.requestRendering();
				}
			}

		});
		canvas.setOnScroll(new EventHandler<>() {

			@Override
			public void handle(ScrollEvent e) {
				synchronized (this) {
					TouchEvent event = usedTouchEvents.obtain();
					event.pointer = 0;
					event.type = TouchEvent.TOUCH_SCROLLED;
					event.scrollAmount = (int) e.getDeltaY();
					event.timeStamp = System.nanoTime();
					touchEvents.add(event);
					if (Gdx.graphics != null)
						Gdx.graphics.requestRendering();
				}
			}

		});


	}

	private int toGdxButton(MouseButton swingButton)
	{
		if (swingButton == MouseButton.PRIMARY)
			return Buttons.LEFT;
		if (swingButton == MouseButton.MIDDLE)
			return Buttons.MIDDLE;
		if (swingButton == MouseButton.SECONDARY)
			return Buttons.RIGHT;
		return Buttons.LEFT;
	}

	private void checkCatched(MouseEvent e)
	{
		if (catched && robot != null && canvas.isVisible())
		{
			int x = (int) (Math.max(0, Math.min(e.getX(), canvas.getFitWidth()) - 1) + canvas.getX());
			int y = (int) (Math.max(0, Math.min(e.getY(), canvas.getFitHeight()) - 1) + canvas.getY());
			if (e.getX() < 0 || e.getX() >= canvas.getFitWidth() || e.getY() < 0 || e.getY() >= canvas.getFitHeight())
			{
				robot.mouseMove(x, y);
			}
		}
	}

	@Override
	public float getAccelerometerX()
	{
		return 0;
	}

	@Override
	public float getAccelerometerY()
	{
		return 0;
	}

	@Override
	public float getAccelerometerZ()
	{
		return 0;
	}

	@Override
	public void getTextInput(final TextInputListener listener, final String title, final String text)
	{
		SwingUtilities.invokeLater(() -> {
			String output = JOptionPane.showInputDialog(null, title, text);
			if (output != null)
				listener.input(output);
			else
				listener.canceled();

		});
	}

	public void getPlaceholderTextInput(final TextInputListener listener, final String title, final String placeholder)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				JPanel panel = new JPanel(new FlowLayout());

				JPanel textPanel = new JPanel()
				{
					public boolean isOptimizedDrawingEnabled()
					{
						return false;
					}
				};

				textPanel.setLayout(new OverlayLayout(textPanel));
				panel.add(textPanel);

				final JTextField textField = new JTextField(20);
				textField.setAlignmentX(0.0f);
				textPanel.add(textField);

				final JLabel placeholderLabel = new JLabel(placeholder);
				placeholderLabel.setForeground(Color.GRAY);
				placeholderLabel.setAlignmentX(0.0f);
				textPanel.add(placeholderLabel, 0);

				textField.getDocument().addDocumentListener(new DocumentListener()
				{

					@Override
					public void removeUpdate(DocumentEvent arg0)
					{
						this.updated();
					}

					@Override
					public void insertUpdate(DocumentEvent arg0)
					{
						this.updated();
					}

					@Override
					public void changedUpdate(DocumentEvent arg0)
					{
						this.updated();
					}

					private void updated()
					{
						placeholderLabel.setVisible(textField.getText().length() == 0);
					}
				});

				JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, null, null);

				pane.setInitialValue(null);
				pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());

				Border border = textField.getBorder();
				placeholderLabel.setBorder(new EmptyBorder(border.getBorderInsets(textField)));

				JDialog dialog = pane.createDialog(null, title);
				pane.selectInitialValue();

				dialog.addWindowFocusListener(new WindowFocusListener()
				{

					@Override
					public void windowLostFocus(WindowEvent arg0)
					{
					}

					@Override
					public void windowGainedFocus(WindowEvent arg0)
					{
						textField.requestFocusInWindow();
					}
				});

				dialog.setVisible(true);
				dialog.dispose();

				Object selectedValue = pane.getValue();

				if ((selectedValue instanceof Integer) && (Integer) selectedValue == JOptionPane.OK_OPTION)
				{
					listener.input(textField.getText());
				}
				else
				{
					listener.canceled();
				}

			}
		});
	}

	@Override
	public int getX()
	{
		return touchX;
	}

	@Override
	public int getX(int pointer)
	{
		if (pointer == 0)
			return touchX;
		else
			return 0;
	}

	@Override
	public int getY()
	{
		return touchY;
	}

	@Override
	public int getY(int pointer)
	{
		if (pointer == 0)
			return touchY;
		else
			return 0;
	}

	@Override
	public boolean isKeyPressed(int key)
	{
		synchronized (this)
		{
			if (key == Input.Keys.ANY_KEY)
				return keys.size() > 0;
			else
				return keys.contains(key);
		}
	}

	@Override
	public boolean isTouched()
	{
		return touchDown;
	}

	@Override
	public boolean isTouched(int pointer)
	{
		if (pointer == 0)
			return touchDown;
		else
			return false;
	}

	void processEvents()
	{
		synchronized (this)
		{
			justTouched = false;

			if (processor != null)
			{
				InputProcessor processor = this.processor;

				int len = keyEvents.size();
				for (int i = 0; i < len; i++)
				{
					KeyEvent e = keyEvents.get(i);
					currentEventTimeStamp = e.timeStamp;
					switch (e.type) {
						case KeyEvent.KEY_DOWN -> processor.keyDown(e.keyCode);
						case KeyEvent.KEY_UP -> processor.keyUp(e.keyCode);
						case KeyEvent.KEY_TYPED -> processor.keyTyped(e.keyChar);
					}
					usedKeyEvents.free(e);
				}

				len = touchEvents.size();
				for (int i = 0; i < len; i++)
				{
					TouchEvent e = touchEvents.get(i);
					currentEventTimeStamp = e.timeStamp;
					switch (e.type) {
						case TouchEvent.TOUCH_DOWN -> {
							processor.touchDown(e.x, e.y, e.pointer, e.button);
							justTouched = true;
						}
						case TouchEvent.TOUCH_UP -> processor.touchUp(e.x, e.y, e.pointer, e.button);
						case TouchEvent.TOUCH_DRAGGED -> processor.touchDragged(e.x, e.y, e.pointer);
						case TouchEvent.TOUCH_MOVED -> processor.mouseMoved(e.x, e.y);
						case TouchEvent.TOUCH_SCROLLED -> processor.scrolled(e.scrollAmount);
					}
					usedTouchEvents.free(e);
				}
			}
			else
			{
				int len = touchEvents.size();
				for (int i = 0; i < len; i++)
				{
					TouchEvent event = touchEvents.get(i);
					if (event.type == TouchEvent.TOUCH_DOWN)
						justTouched = true;
					usedTouchEvents.free(event);
				}

				len = keyEvents.size();
				for (int i = 0; i < len; i++)
				{
					usedKeyEvents.free(keyEvents.get(i));
				}
			}

			if (touchEvents.size() == 0)
			{
				deltaX = 0;
				deltaY = 0;
			}

			keyEvents.clear();
			touchEvents.clear();
		}
	}

	@Override
	public void setCatchBackKey(boolean catchBack)
	{

	}

	@Override
	public void setOnscreenKeyboardVisible(boolean visible)
	{

	}

	//
	// public void mouseDragged(MouseEvent e)
	// {
	// synchronized (this)
	// {
	// TouchEvent event = usedTouchEvents.obtain();
	// event.pointer = 0;
	// event.x = e.getX();
	// event.y = e.getY();
	// event.type = TouchEvent.TOUCH_DRAGGED;
	// event.timeStamp = System.nanoTime();
	// touchEvents.add(event);
	//
	// deltaX = event.x - touchX;
	// deltaY = event.y - touchY;
	// touchX = event.x;
	// touchY = event.y;
	// checkCatched(e);
	// Gdx.graphics.requestRendering();
	// }
	// }
	//
	// //
	// // public void mouseClicked (MouseEvent arg0) {
	// // }
	//
	// @Override
	// public void mouseEntered(MouseEvent e)
	// {
	// touchX = e.getX();
	// touchY = e.getY();
	// checkCatched(e);
	// Gdx.graphics.requestRendering();
	// }
	//
	// @Override
	// public void mouseExited(MouseEvent e)
	// {
	// checkCatched(e);
	// Gdx.graphics.requestRendering();
	// }
	//
	// private void checkCatched(MouseEvent e)
	// {
	// if (catched && robot != null && canvas.isVisible())
	// {
	// int x = (int) (Math.max(0, Math.min(e.getX(), canvas.getFitWidth()) - 1) + canvas.getX());
	// int y = (int) (Math.max(0, Math.min(e.getY(), canvas.getFitHeight()) - 1) + canvas.getY());
	// if (e.getX() < 0 || e.getX() >= canvas.getFitWidth() || e.getY() < 0 || e.getY() >= canvas.getFitHeight())
	// {
	// robot.mouseMove(x, y);
	// }
	// }
	// }
	//
	// private int toGdxButton(int swingButton)
	// {
	// if (swingButton == MouseEvent.BUTTON1)
	// return Buttons.LEFT;
	// if (swingButton == MouseEvent.BUTTON2)
	// return Buttons.MIDDLE;
	// if (swingButton == MouseEvent.BUTTON3)
	// return Buttons.RIGHT;
	// return Buttons.LEFT;
	// }
	//
	// @Override
	// public void mousePressed(MouseEvent e)
	// {
	// synchronized (this)
	// {
	// TouchEvent event = usedTouchEvents.obtain();
	// event.pointer = 0;
	// event.x = e.getX();
	// event.y = e.getY();
	// event.type = TouchEvent.TOUCH_DOWN;
	// event.button = toGdxButton(e.getButton());
	// event.timeStamp = System.nanoTime();
	// touchEvents.add(event);
	//
	// deltaX = event.x - touchX;
	// deltaY = event.y - touchY;
	// touchX = event.x;
	// touchY = event.y;
	// touchDown = true;
	// pressedButtons.add(event.button);
	// Gdx.graphics.requestRendering();
	// }
	// }
	//
	// @Override
	// public void mouseReleased(MouseEvent e)
	// {
	// synchronized (this)
	// {
	// TouchEvent event = usedTouchEvents.obtain();
	// event.pointer = 0;
	// event.x = e.getX();
	// event.y = e.getY();
	// event.button = toGdxButton(e.getButton());
	// event.type = TouchEvent.TOUCH_UP;
	// event.timeStamp = System.nanoTime();
	// touchEvents.add(event);
	//
	// deltaX = event.x - touchX;
	// deltaY = event.y - touchY;
	// touchX = event.x;
	// touchY = event.y;
	// pressedButtons.remove(event.button);
	// if (pressedButtons.size() == 0)
	// touchDown = false;
	// Gdx.graphics.requestRendering();
	// }
	// }
	//
	// @Override
	// public void mouseWheelMoved(MouseWheelEvent e)
	// {
	// synchronized (this)
	// {
	// TouchEvent event = usedTouchEvents.obtain();
	// event.pointer = 0;
	// event.type = TouchEvent.TOUCH_SCROLLED;
	// event.scrollAmount = e.getWheelRotation();
	// event.timeStamp = System.nanoTime();
	// touchEvents.add(event);
	// Gdx.graphics.requestRendering();
	// }
	// }
	//
	// @Override
	// public void keyPressed(java.awt.event.KeyEvent e)
	// {
	// synchronized (this)
	// {
	// KeyEvent event = usedKeyEvents.obtain();
	// event.keyChar = 0;
	// event.keyCode = translateKeyCode(e.getKeyCode());
	// event.type = KeyEvent.KEY_DOWN;
	// event.timeStamp = System.nanoTime();
	// keyEvents.add(event);
	// keys.add(event.keyCode);
	// Gdx.graphics.requestRendering();
	// }
	// }
	//
	// @Override
	// public void keyReleased(java.awt.event.KeyEvent e)
	// {
	// synchronized (this)
	// {
	// KeyEvent event = usedKeyEvents.obtain();
	// event.keyChar = 0;
	// event.keyCode = translateKeyCode(e.getKeyCode());
	// event.type = KeyEvent.KEY_UP;
	// event.timeStamp = System.nanoTime();
	// keyEvents.add(event);
	// keys.remove(event.keyCode);
	// Gdx.graphics.requestRendering();
	// }
	// }
	//
	// @Override
	// public void keyTyped(java.awt.event.KeyEvent e)
	// {
	// synchronized (this)
	// {
	// KeyEvent event = usedKeyEvents.obtain();
	// event.keyChar = e.getKeyChar();
	// event.keyCode = 0;
	// event.type = KeyEvent.KEY_TYPED;
	// event.timeStamp = System.nanoTime();
	// keyEvents.add(event);
	// Gdx.graphics.requestRendering();
	// }
	// }

	protected static int translateKeyCode(int keyCode)
	{
		if (keyCode == java.awt.event.KeyEvent.VK_ADD)
			return Input.Keys.PLUS;
		if (keyCode == java.awt.event.KeyEvent.VK_SUBTRACT)
			return Input.Keys.MINUS;
		if (keyCode == java.awt.event.KeyEvent.VK_0)
			return Input.Keys.NUM_0;
		if (keyCode == java.awt.event.KeyEvent.VK_1)
			return Input.Keys.NUM_1;
		if (keyCode == java.awt.event.KeyEvent.VK_2)
			return Input.Keys.NUM_2;
		if (keyCode == java.awt.event.KeyEvent.VK_3)
			return Input.Keys.NUM_3;
		if (keyCode == java.awt.event.KeyEvent.VK_4)
			return Input.Keys.NUM_4;
		if (keyCode == java.awt.event.KeyEvent.VK_5)
			return Input.Keys.NUM_5;
		if (keyCode == java.awt.event.KeyEvent.VK_6)
			return Input.Keys.NUM_6;
		if (keyCode == java.awt.event.KeyEvent.VK_7)
			return Input.Keys.NUM_7;
		if (keyCode == java.awt.event.KeyEvent.VK_8)
			return Input.Keys.NUM_8;
		if (keyCode == java.awt.event.KeyEvent.VK_9)
			return Input.Keys.NUM_9;
		if (keyCode == java.awt.event.KeyEvent.VK_A)
			return Input.Keys.A;
		if (keyCode == java.awt.event.KeyEvent.VK_B)
			return Input.Keys.B;
		if (keyCode == java.awt.event.KeyEvent.VK_C)
			return Input.Keys.C;
		if (keyCode == java.awt.event.KeyEvent.VK_D)
			return Input.Keys.D;
		if (keyCode == java.awt.event.KeyEvent.VK_E)
			return Input.Keys.E;
		if (keyCode == java.awt.event.KeyEvent.VK_F)
			return Input.Keys.F;
		if (keyCode == java.awt.event.KeyEvent.VK_G)
			return Input.Keys.G;
		if (keyCode == java.awt.event.KeyEvent.VK_H)
			return Input.Keys.H;
		if (keyCode == java.awt.event.KeyEvent.VK_I)
			return Input.Keys.I;
		if (keyCode == java.awt.event.KeyEvent.VK_J)
			return Input.Keys.J;
		if (keyCode == java.awt.event.KeyEvent.VK_K)
			return Input.Keys.K;
		if (keyCode == java.awt.event.KeyEvent.VK_L)
			return Input.Keys.L;
		if (keyCode == java.awt.event.KeyEvent.VK_M)
			return Input.Keys.M;
		if (keyCode == java.awt.event.KeyEvent.VK_N)
			return Input.Keys.N;
		if (keyCode == java.awt.event.KeyEvent.VK_O)
			return Input.Keys.O;
		if (keyCode == java.awt.event.KeyEvent.VK_P)
			return Input.Keys.P;
		if (keyCode == java.awt.event.KeyEvent.VK_Q)
			return Input.Keys.Q;
		if (keyCode == java.awt.event.KeyEvent.VK_R)
			return Input.Keys.R;
		if (keyCode == java.awt.event.KeyEvent.VK_S)
			return Input.Keys.S;
		if (keyCode == java.awt.event.KeyEvent.VK_T)
			return Input.Keys.T;
		if (keyCode == java.awt.event.KeyEvent.VK_U)
			return Input.Keys.U;
		if (keyCode == java.awt.event.KeyEvent.VK_V)
			return Input.Keys.V;
		if (keyCode == java.awt.event.KeyEvent.VK_W)
			return Input.Keys.W;
		if (keyCode == java.awt.event.KeyEvent.VK_X)
			return Input.Keys.X;
		if (keyCode == java.awt.event.KeyEvent.VK_Y)
			return Input.Keys.Y;
		if (keyCode == java.awt.event.KeyEvent.VK_Z)
			return Input.Keys.Z;
		if (keyCode == java.awt.event.KeyEvent.VK_ALT)
			return Input.Keys.ALT_LEFT;
		if (keyCode == java.awt.event.KeyEvent.VK_ALT_GRAPH)
			return Input.Keys.ALT_RIGHT;
		if (keyCode == java.awt.event.KeyEvent.VK_BACK_SLASH)
			return Input.Keys.BACKSLASH;
		if (keyCode == java.awt.event.KeyEvent.VK_COMMA)
			return Input.Keys.COMMA;
		if (keyCode == java.awt.event.KeyEvent.VK_DELETE)
			return Input.Keys.DEL;
		if (keyCode == java.awt.event.KeyEvent.VK_LEFT)
			return Input.Keys.DPAD_LEFT;
		if (keyCode == java.awt.event.KeyEvent.VK_RIGHT)
			return Input.Keys.DPAD_RIGHT;
		if (keyCode == java.awt.event.KeyEvent.VK_UP)
			return Input.Keys.DPAD_UP;
		if (keyCode == java.awt.event.KeyEvent.VK_DOWN)
			return Input.Keys.DPAD_DOWN;
		if (keyCode == java.awt.event.KeyEvent.VK_ENTER)
			return Input.Keys.ENTER;
		if (keyCode == java.awt.event.KeyEvent.VK_HOME)
			return Input.Keys.HOME;
		if (keyCode == java.awt.event.KeyEvent.VK_MINUS)
			return Input.Keys.MINUS;
		if (keyCode == java.awt.event.KeyEvent.VK_PERIOD)
			return Input.Keys.PERIOD;
		if (keyCode == java.awt.event.KeyEvent.VK_PLUS)
			return Input.Keys.PLUS;
		if (keyCode == java.awt.event.KeyEvent.VK_SEMICOLON)
			return Input.Keys.SEMICOLON;
		if (keyCode == java.awt.event.KeyEvent.VK_SHIFT)
			return Input.Keys.SHIFT_LEFT;
		if (keyCode == java.awt.event.KeyEvent.VK_SLASH)
			return Input.Keys.SLASH;
		if (keyCode == java.awt.event.KeyEvent.VK_SPACE)
			return Input.Keys.SPACE;
		if (keyCode == java.awt.event.KeyEvent.VK_TAB)
			return Input.Keys.TAB;
		if (keyCode == java.awt.event.KeyEvent.VK_BACK_SPACE)
			return Input.Keys.DEL;
		if (keyCode == java.awt.event.KeyEvent.VK_CONTROL)
			return Input.Keys.CONTROL_LEFT;
		if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE)
			return Input.Keys.ESCAPE;
		if (keyCode == java.awt.event.KeyEvent.VK_END)
			return Input.Keys.END;
		if (keyCode == java.awt.event.KeyEvent.VK_INSERT)
			return Input.Keys.INSERT;
		if (keyCode == java.awt.event.KeyEvent.VK_NUMPAD5)
			return Input.Keys.DPAD_CENTER;
		if (keyCode == java.awt.event.KeyEvent.VK_PAGE_UP)
			return Input.Keys.PAGE_UP;
		if (keyCode == java.awt.event.KeyEvent.VK_PAGE_DOWN)
			return Input.Keys.PAGE_DOWN;
		if (keyCode == java.awt.event.KeyEvent.VK_F1)
			return Input.Keys.F1;
		if (keyCode == java.awt.event.KeyEvent.VK_F2)
			return Input.Keys.F2;
		if (keyCode == java.awt.event.KeyEvent.VK_F3)
			return Input.Keys.F3;
		if (keyCode == java.awt.event.KeyEvent.VK_F4)
			return Input.Keys.F4;
		if (keyCode == java.awt.event.KeyEvent.VK_F5)
			return Input.Keys.F5;
		if (keyCode == java.awt.event.KeyEvent.VK_F6)
			return Input.Keys.F6;
		if (keyCode == java.awt.event.KeyEvent.VK_F7)
			return Input.Keys.F7;
		if (keyCode == java.awt.event.KeyEvent.VK_F8)
			return Input.Keys.F8;
		if (keyCode == java.awt.event.KeyEvent.VK_F9)
			return Input.Keys.F9;
		if (keyCode == java.awt.event.KeyEvent.VK_F10)
			return Input.Keys.F10;
		if (keyCode == java.awt.event.KeyEvent.VK_F11)
			return Input.Keys.F11;
		if (keyCode == java.awt.event.KeyEvent.VK_F12)
			return Input.Keys.F12;
		if (keyCode == java.awt.event.KeyEvent.VK_COLON)
			return Input.Keys.COLON;
		if (keyCode == java.awt.event.KeyEvent.VK_NUMPAD0)
			return Input.Keys.NUM_0;
		if (keyCode == java.awt.event.KeyEvent.VK_NUMPAD1)
			return Input.Keys.NUM_1;
		if (keyCode == java.awt.event.KeyEvent.VK_NUMPAD2)
			return Input.Keys.NUM_2;
		if (keyCode == java.awt.event.KeyEvent.VK_NUMPAD3)
			return Input.Keys.NUM_3;
		if (keyCode == java.awt.event.KeyEvent.VK_NUMPAD4)
			return Input.Keys.NUM_4;
		if (keyCode == java.awt.event.KeyEvent.VK_NUMPAD5)
			return Input.Keys.NUM_5;
		if (keyCode == java.awt.event.KeyEvent.VK_NUMPAD6)
			return Input.Keys.NUM_6;
		if (keyCode == java.awt.event.KeyEvent.VK_NUMPAD7)
			return Input.Keys.NUM_7;
		if (keyCode == java.awt.event.KeyEvent.VK_NUMPAD8)
			return Input.Keys.NUM_8;
		if (keyCode == java.awt.event.KeyEvent.VK_NUMPAD9)
			return Input.Keys.NUM_9;

		return Input.Keys.UNKNOWN;
	}

	@Override
	public void setInputProcessor(InputProcessor processor)
	{
		synchronized (this)
		{
			this.processor = processor;
		}
	}

	@Override
	public InputProcessor getInputProcessor()
	{
		return this.processor;
	}

	@Override
	public void vibrate(int milliseconds)
	{
	}

	@Override
	public boolean justTouched()
	{
		return justTouched;
	}

	@Override
	public boolean isButtonPressed(int button)
	{
		return pressedButtons.contains(button);
	}

	@Override
	public void vibrate(long[] pattern, int repeat)
	{
	}

	@Override
	public void cancelVibrate()
	{
	}

	@Override
	public float getAzimuth()
	{
		return 0;
	}

	@Override
	public float getPitch()
	{
		return 0;
	}

	@Override
	public float getRoll()
	{
		return 0;
	}

	@Override
	public boolean isPeripheralAvailable(Peripheral peripheral)
	{
		return peripheral == Peripheral.HardwareKeyboard;
	}

	@Override
	public int getRotation()
	{
		return 0;
	}

	@Override
	public Orientation getNativeOrientation()
	{
		return Orientation.Landscape;
	}

	@Override
	public void setCursorCatched(boolean catched)
	{
		this.catched = catched;
	}

	@Override
	public boolean isCursorCatched()
	{
		return catched;
	}

	@Override
	public int getDeltaX()
	{
		return deltaX;
	}

	@Override
	public int getDeltaX(int pointer)
	{
		if (pointer == 0)
			return deltaX;
		return 0;
	}

	@Override
	public int getDeltaY()
	{
		return deltaY;
	}

	@Override
	public int getDeltaY(int pointer)
	{
		if (pointer == 0)
			return deltaY;
		return 0;
	}

	@Override
	public void setCursorPosition(int x, int y)
	{

	}

	@Override
	public void setCursorImage(Pixmap pixmap, int xHotspot, int yHotspot)
	{
	}

	@Override
	public void setCatchMenuKey(boolean catchMenu)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public long getCurrentEventTime()
	{
		return currentEventTimeStamp;
	}

	@Override
	public void getRotationMatrix(float[] matrix)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isKeyJustPressed(int key)
	{
		return false;
	}

	@Override
	public boolean isCatchBackKey()
	{
		return false;
	}
}
