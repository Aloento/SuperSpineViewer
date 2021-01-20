package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Pool;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An implementation of the {@link Input} interface hooking a JavaFX ImageView for input.
 *
 * @author Trixt0r
 */
final public class LwjglFXInput implements Input {
    static public final float keyRepeatInitialTime = 0.4f;
    static public float keyRepeatTime = 0.1f;

    final List<KeyEvent> keyEvents = new ArrayList<>();
    final List<TouchEvent> touchEvents = new ArrayList<>();
    final Set<Integer> pressedButtons = new HashSet<>();
    final ImageView target;
    final Pool<KeyEvent> usedKeyEvents = new Pool<>(16, 1000) {
        protected KeyEvent newObject() {
            return new KeyEvent();
        }
    };
    final Pool<TouchEvent> usedTouchEvents = new Pool<>(16, 1000) {
        protected TouchEvent newObject() {
            return new TouchEvent();
        }
    };
    boolean mousePressed = false;
    int mouseX, mouseY;
    int deltaX, deltaY;
    int pressedKeys = 0;
    boolean justTouched = false;
    InputProcessor processor;
    char lastKeyCharPressed;
    float keyRepeatTimer;
    long currentEventTimeStamp;
    int x, y, lastX, lastY;
    KeyCode lastKeyCode;
    MouseButton lastButton;
    boolean isPressed, hasFocus = false;

    public LwjglFXInput(ImageView target) {
        this.target = target;
        this.target.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> lastButton = e.getButton());
        this.target.getScene().addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (!hasFocus) return;
            lastKeyCode = e.getCode();
            int keyCode = getGdxKeyCode(lastKeyCode);
            char keyChar = e.getText().charAt(0);
            long timeStamp = System.nanoTime();
            KeyEvent event = usedKeyEvents.obtain();
            event.keyCode = keyCode;
            event.keyChar = 0;
            event.type = KeyEvent.KEY_DOWN;
            event.timeStamp = timeStamp;
            keyEvents.add(event);

            event = usedKeyEvents.obtain();
            event.keyCode = 0;
            event.keyChar = keyChar;
            event.type = KeyEvent.KEY_TYPED;
            event.timeStamp = timeStamp;
            keyEvents.add(event);

            pressedKeys++;
            lastKeyCharPressed = keyChar;
            keyRepeatTimer = keyRepeatInitialTime;
        });

        this.target.getScene().addEventHandler(javafx.scene.input.KeyEvent.KEY_RELEASED, e -> {
            if (!hasFocus) return;
            lastKeyCode = null;
            int keyCode = getGdxKeyCode(e.getCode());
            KeyEvent event = usedKeyEvents.obtain();
            event.keyCode = keyCode;
            event.keyChar = 0;
            event.type = KeyEvent.KEY_UP;
            event.timeStamp = System.nanoTime();
            keyEvents.add(event);
            pressedKeys--;
            lastKeyCharPressed = 0;
        });

        this.target.addEventHandler(MouseEvent.ANY, e -> {
            TouchEvent event = usedTouchEvents.obtain();
            event.x = (int) e.getX();
            event.y = (int) e.getY();
            event.button = toGdxButton(e.getButton());
            event.pointer = 0;
            event.timeStamp = System.nanoTime();
            deltaX = 0;
            deltaY = 0;
            if (e.getEventType() == MouseEvent.MOUSE_DRAGGED)
                event.type = TouchEvent.TOUCH_DRAGGED;
            else if (e.getEventType() == MouseEvent.MOUSE_MOVED)
                event.type = TouchEvent.TOUCH_MOVED;
            else if (e.getEventType() == MouseEvent.MOUSE_PRESSED) {
                event.type = TouchEvent.TOUCH_DOWN;
                pressedButtons.add(event.button);
                justTouched = true;
            } else if (e.getEventType() == MouseEvent.MOUSE_RELEASED) {
                event.type = TouchEvent.TOUCH_UP;
                pressedButtons.remove(event.button);
            } else return;
            touchEvents.add(event);
            lastX = mouseX;
            lastY = mouseY;
            mouseX = event.x;
            mouseY = event.y;
        });

        this.target.addEventHandler(ScrollEvent.SCROLL, e -> {
            TouchEvent event = usedTouchEvents.obtain();
            event.x = (int) e.getX();
            event.y = (int) e.getY();
            event.timeStamp = System.nanoTime();
            event.type = TouchEvent.TOUCH_SCROLLED;
            event.scrollAmount = (int) -Math.signum(e.getDeltaY());
            touchEvents.add(event);
        });
    }

    public static int getGdxKeyCode(KeyCode code) {
        if (code == null) return Keys.UNKNOWN;
        return switch (code) {
            case LEFT_PARENTHESIS -> Keys.LEFT_BRACKET;
            case RIGHT_PARENTHESIS -> Keys.RIGHT_BRACKET;
            case DEAD_GRAVE -> Keys.GRAVE;
            case MULTIPLY -> Keys.STAR;
            case NUM_LOCK -> Keys.NUM;
            case DECIMAL, PERIOD -> Keys.PERIOD;
            case DIVIDE, SLASH -> Keys.SLASH;
            case META -> Keys.SYM;
            case AT -> Keys.AT;
            case EQUALS -> Keys.EQUALS;
            case DIGIT0 -> Keys.NUM_0;
            case DIGIT1 -> Keys.NUM_1;
            case DIGIT2 -> Keys.NUM_2;
            case DIGIT3 -> Keys.NUM_3;
            case DIGIT4 -> Keys.NUM_4;
            case DIGIT5 -> Keys.NUM_5;
            case DIGIT6 -> Keys.NUM_6;
            case DIGIT7 -> Keys.NUM_7;
            case DIGIT8 -> Keys.NUM_8;
            case DIGIT9 -> Keys.NUM_9;
            case A -> Keys.A;
            case B -> Keys.B;
            case C -> Keys.C;
            case D -> Keys.D;
            case E -> Keys.E;
            case F -> Keys.F;
            case G -> Keys.G;
            case H -> Keys.H;
            case I -> Keys.I;
            case J -> Keys.J;
            case K -> Keys.K;
            case L -> Keys.L;
            case M -> Keys.M;
            case N -> Keys.N;
            case O -> Keys.O;
            case P -> Keys.P;
            case Q -> Keys.Q;
            case R -> Keys.R;
            case S -> Keys.S;
            case T -> Keys.T;
            case U -> Keys.U;
            case V -> Keys.V;
            case W -> Keys.W;
            case X -> Keys.X;
            case Y -> Keys.Y;
            case Z -> Keys.Z;
            case ALT -> Keys.ALT_LEFT;
            case BACK_SLASH -> Keys.BACKSLASH;
            case COMMA -> Keys.COMMA;
            case LEFT -> Keys.DPAD_LEFT;
            case RIGHT -> Keys.DPAD_RIGHT;
            case UP -> Keys.DPAD_UP;
            case DOWN -> Keys.DPAD_DOWN;
            case ENTER -> Keys.ENTER;
            case HOME -> Keys.HOME;
            case MINUS, SUBTRACT -> Keys.MINUS;
            case ADD -> Keys.PLUS;
            case SEMICOLON -> Keys.SEMICOLON;
            case SHIFT -> Keys.SHIFT_LEFT;
            case SPACE -> Keys.SPACE;
            case TAB -> Keys.TAB;
            case CONTROL -> Keys.CONTROL_LEFT;
            case PAGE_DOWN -> Keys.PAGE_DOWN;
            case PAGE_UP -> Keys.PAGE_UP;
            case ESCAPE -> Keys.ESCAPE;
            case END -> Keys.END;
            case INSERT -> Keys.INSERT;
            case DELETE -> Keys.DEL;
            case QUOTE -> Keys.APOSTROPHE;
            case F1 -> Keys.F1;
            case F2 -> Keys.F2;
            case F3 -> Keys.F3;
            case F4 -> Keys.F4;
            case F5 -> Keys.F5;
            case F6 -> Keys.F6;
            case F7 -> Keys.F7;
            case F8 -> Keys.F8;
            case F9 -> Keys.F9;
            case F10 -> Keys.F10;
            case F11 -> Keys.F11;
            case F12 -> Keys.F12;
            case COLON -> Keys.COLON;
            case NUMPAD0 -> Keys.NUMPAD_0;
            case NUMPAD1 -> Keys.NUMPAD_1;
            case NUMPAD2 -> Keys.NUMPAD_2;
            case NUMPAD3 -> Keys.NUMPAD_3;
            case NUMPAD4 -> Keys.NUMPAD_4;
            case NUMPAD5 -> Keys.NUMPAD_5;
            case NUMPAD6 -> Keys.NUMPAD_6;
            case NUMPAD7 -> Keys.NUMPAD_7;
            case NUMPAD8 -> Keys.NUMPAD_8;
            case NUMPAD9 -> Keys.NUMPAD_9;
            default -> Keys.UNKNOWN;
        };
    }

    public static KeyCode getFXKeyCode(int gdxKeyCode) {
        return switch (gdxKeyCode) {
            case Keys.APOSTROPHE -> KeyCode.QUOTE;
            case Keys.LEFT_BRACKET -> KeyCode.LEFT_PARENTHESIS;
            case Keys.RIGHT_BRACKET -> KeyCode.RIGHT_PARENTHESIS;
            case Keys.GRAVE -> KeyCode.DEAD_GRAVE;
            case Keys.STAR -> KeyCode.MULTIPLY;
            case Keys.NUM -> KeyCode.NUM_LOCK;
            case Keys.AT -> KeyCode.AT;
            case Keys.EQUALS -> KeyCode.EQUALS;
            case Keys.SYM -> KeyCode.META;
            case Keys.NUM_0 -> KeyCode.DIGIT0;
            case Keys.NUM_1 -> KeyCode.DIGIT1;
            case Keys.NUM_2 -> KeyCode.DIGIT2;
            case Keys.NUM_3 -> KeyCode.DIGIT3;
            case Keys.NUM_4 -> KeyCode.DIGIT4;
            case Keys.NUM_5 -> KeyCode.DIGIT5;
            case Keys.NUM_6 -> KeyCode.DIGIT6;
            case Keys.NUM_7 -> KeyCode.DIGIT7;
            case Keys.NUM_8 -> KeyCode.DIGIT8;
            case Keys.NUM_9 -> KeyCode.DIGIT9;
            case Keys.A -> KeyCode.A;
            case Keys.B -> KeyCode.B;
            case Keys.C -> KeyCode.C;
            case Keys.D -> KeyCode.D;
            case Keys.E -> KeyCode.E;
            case Keys.F -> KeyCode.F;
            case Keys.G -> KeyCode.G;
            case Keys.H -> KeyCode.H;
            case Keys.I -> KeyCode.I;
            case Keys.J -> KeyCode.J;
            case Keys.K -> KeyCode.K;
            case Keys.L -> KeyCode.L;
            case Keys.M -> KeyCode.M;
            case Keys.N -> KeyCode.N;
            case Keys.O -> KeyCode.O;
            case Keys.P -> KeyCode.P;
            case Keys.Q -> KeyCode.Q;
            case Keys.R -> KeyCode.R;
            case Keys.S -> KeyCode.S;
            case Keys.T -> KeyCode.T;
            case Keys.U -> KeyCode.U;
            case Keys.V -> KeyCode.V;
            case Keys.W -> KeyCode.W;
            case Keys.X -> KeyCode.X;
            case Keys.Y -> KeyCode.Y;
            case Keys.Z -> KeyCode.Z;
            case Keys.ALT_LEFT, Keys.ALT_RIGHT -> KeyCode.ALT;
            case Keys.BACKSLASH -> KeyCode.BACK_SLASH;
            case Keys.COMMA -> KeyCode.COMMA;
            case Keys.FORWARD_DEL, Keys.DEL -> KeyCode.DELETE;
            case Keys.DPAD_LEFT -> KeyCode.LEFT;
            case Keys.DPAD_RIGHT -> KeyCode.RIGHT;
            case Keys.DPAD_UP -> KeyCode.UP;
            case Keys.DPAD_DOWN -> KeyCode.DOWN;
            case Keys.ENTER -> KeyCode.ENTER;
            case Keys.HOME -> KeyCode.HOME;
            case Keys.END -> KeyCode.END;
            case Keys.PAGE_DOWN -> KeyCode.PAGE_DOWN;
            case Keys.PAGE_UP -> KeyCode.PAGE_UP;
            case Keys.INSERT -> KeyCode.INSERT;
            case Keys.MINUS -> KeyCode.MINUS;
            case Keys.PERIOD -> KeyCode.PERIOD;
            case Keys.PLUS -> KeyCode.ADD;
            case Keys.SEMICOLON -> KeyCode.SEMICOLON;
            case Keys.SHIFT_LEFT, Keys.SHIFT_RIGHT -> KeyCode.SHIFT;
            case Keys.SLASH -> KeyCode.SLASH;
            case Keys.SPACE -> KeyCode.SPACE;
            case Keys.TAB -> KeyCode.TAB;
            case Keys.CONTROL_LEFT, Keys.CONTROL_RIGHT -> KeyCode.CONTROL;
            case Keys.ESCAPE -> KeyCode.ESCAPE;
            case Keys.F1 -> KeyCode.F1;
            case Keys.F2 -> KeyCode.F2;
            case Keys.F3 -> KeyCode.F3;
            case Keys.F4 -> KeyCode.F4;
            case Keys.F5 -> KeyCode.F5;
            case Keys.F6 -> KeyCode.F6;
            case Keys.F7 -> KeyCode.F7;
            case Keys.F8 -> KeyCode.F8;
            case Keys.F9 -> KeyCode.F9;
            case Keys.F10 -> KeyCode.F10;
            case Keys.F11 -> KeyCode.F11;
            case Keys.F12 -> KeyCode.F12;
            case Keys.COLON -> KeyCode.COLON;
            case Keys.NUMPAD_0 -> KeyCode.NUMPAD0;
            case Keys.NUMPAD_1 -> KeyCode.NUMPAD1;
            case Keys.NUMPAD_2 -> KeyCode.NUMPAD2;
            case Keys.NUMPAD_3 -> KeyCode.NUMPAD3;
            case Keys.NUMPAD_4 -> KeyCode.NUMPAD4;
            case Keys.NUMPAD_5 -> KeyCode.NUMPAD5;
            case Keys.NUMPAD_6 -> KeyCode.NUMPAD6;
            case Keys.NUMPAD_7 -> KeyCode.NUMPAD7;
            case Keys.NUMPAD_8 -> KeyCode.NUMPAD8;
            case Keys.NUMPAD_9 -> KeyCode.NUMPAD9;
            default -> KeyCode.ACCEPT;
        };
    }

    public static int toGdxButton(MouseButton button) {
        if (button == MouseButton.PRIMARY) return Buttons.LEFT;
        if (button == MouseButton.SECONDARY) return Buttons.RIGHT;
        if (button == MouseButton.MIDDLE) return Buttons.MIDDLE;
        return Buttons.LEFT;
    }

    public static MouseButton toLwjglButton(int button) {
        return switch (button) {
            case Buttons.LEFT -> MouseButton.PRIMARY;
            case Buttons.RIGHT -> MouseButton.SECONDARY;
            case Buttons.MIDDLE -> MouseButton.MIDDLE;
            default -> MouseButton.NONE;
        };
    }

    public float getAccelerometerX() {
        return 0;
    }

    public float getAccelerometerY() {
        return 0;
    }

    public float getAccelerometerZ() {
        return 0;
    }

    @Override
    public float getGyroscopeX() {
        return 0;
    }

    @Override
    public float getGyroscopeY() {
        return 0;
    }

    @Override
    public float getGyroscopeZ() {
        return 0;
    }

    @Override
    public int getMaxPointers() {
        return 0;
    }

    public void getTextInput(final TextInputListener listener, final String title, final String text) {
        throw new GdxRuntimeException("Not supported");
    }

    public void getPlaceholderTextInput(final TextInputListener listener, final String title, final String placeholder) {
        throw new GdxRuntimeException("Not supported");
    }

    public int getX() {
        return mouseX;
    }

    public int getY() {
        return mouseY;
    }

    public boolean isAccelerometerAvailable() {
        return false;
    }

    public boolean isKeyPressed(int key) {
        if (key == Input.Keys.ANY_KEY)
            return pressedKeys > 0;
        else
            return getGdxKeyCode(lastKeyCode) == key;
    }

    @Override
    public boolean isKeyJustPressed(int i) {
        return false;
    }

    @Override
    public void getTextInput(TextInputListener textInputListener, String s, String s1, String s2) {

    }

    public boolean isTouched() {
        return target.isPressed();
    }

    public int getX(int pointer) {
        if (pointer > 0)
            return 0;
        else
            return getX();
    }

    public int getY(int pointer) {
        if (pointer > 0)
            return 0;
        else
            return getY();
    }

    public boolean isTouched(int pointer) {
        if (pointer > 0)
            return false;
        else
            return isTouched();
    }

    @Override
    public float getPressure() {
        return 0;
    }

    @Override
    public float getPressure(int i) {
        return 0;
    }

    public boolean supportsMultitouch() {
        return false;
    }

    @Override
    public void setOnscreenKeyboardVisible(boolean visible) {

    }

    @Override
    public boolean isCatchBackKey() {
        return false;
    }

    @Override
    public void setCatchBackKey(boolean catchBack) {

    }

    void processEvents() {
        isPressed = target.isPressed();
        if (isPressed && !hasFocus) {
            hasFocus = true;
            Platform.runLater(target::requestFocus);
        }
        if (!isPressed && hasFocus && target.getScene().getRoot().isPressed()) hasFocus = false;
        synchronized (this) {
            if (processor != null) {
                InputProcessor processor = this.processor;
                int len = keyEvents.size();
                for (int i = 0; i < len; i++) {
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
                for (int i = 0; i < len; i++) {
                    TouchEvent e = touchEvents.get(i);
                    currentEventTimeStamp = e.timeStamp;
                    switch (e.type) {
                        case TouchEvent.TOUCH_DOWN -> processor.touchDown(e.x, e.y, e.pointer, e.button);
                        case TouchEvent.TOUCH_UP -> processor.touchUp(e.x, e.y, e.pointer, e.button);
                        case TouchEvent.TOUCH_DRAGGED -> processor.touchDragged(e.x, e.y, e.pointer);
                        case TouchEvent.TOUCH_MOVED -> processor.mouseMoved(e.x, e.y);
                        case TouchEvent.TOUCH_SCROLLED -> processor.scrolled(e.scrollAmount);
                    }
                    usedTouchEvents.free(e);
                }
            } else {
                int len = touchEvents.size();
                for (int i = 0; i < len; i++) {
                    usedTouchEvents.free(touchEvents.get(i));
                }

                len = keyEvents.size();
                for (int i = 0; i < len; i++) {
                    usedKeyEvents.free(keyEvents.get(i));
                }
            }

            keyEvents.clear();
            touchEvents.clear();
            deltaX = mouseX - lastX;
            deltaY = mouseY - lastY;
            lastX = mouseX;
            lastY = mouseY;

        }
    }

    @Override
    public InputProcessor getInputProcessor() {
        return this.processor;
    }

    @Override
    public void setInputProcessor(InputProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void vibrate(int milliseconds) {
    }

    @Override
    public boolean justTouched() {
        return justTouched;
    }

    @Override
    public boolean isButtonPressed(int button) {
        return target.isPressed() && lastButton == toLwjglButton(button);
    }

    @Override
    public boolean isButtonJustPressed(int i) {
        return false;
    }

    @Override
    public void vibrate(long[] pattern, int repeat) {
    }

    @Override
    public void cancelVibrate() {
    }

    @Override
    public float getAzimuth() {
        return 0;
    }

    @Override
    public float getPitch() {
        return 0;
    }

    @Override
    public float getRoll() {
        return 0;
    }

    @Override
    public boolean isPeripheralAvailable(Peripheral peripheral) {
        return peripheral == Peripheral.HardwareKeyboard;
    }

    @Override
    public int getRotation() {
        return 0;
    }

    @Override
    public Orientation getNativeOrientation() {
        return Orientation.Landscape;
    }

    @Override
    public boolean isCursorCatched() {
        return false;
    }

    @Override
    public void setCursorCatched(boolean catched) {
        //Mouse.setGrabbed(catched);
    }

    @Override
    public int getDeltaX() {
        return deltaX;
    }

    @Override
    public int getDeltaX(int pointer) {
        if (pointer == 0)
            return deltaX;
        else
            return 0;
    }

    @Override
    public int getDeltaY() {
        return -deltaY;
    }

    @Override
    public int getDeltaY(int pointer) {
        if (pointer == 0)
            return -deltaY;
        else
            return 0;
    }

    @Override
    public void setCursorPosition(int x, int y) {
        //TODO
    }

    @Override
    public boolean isCatchMenuKey() {
        return false;
    }

    @Override
    public void setCatchMenuKey(boolean catchMenu) {
    }

    @Override
    public void setCatchKey(int i, boolean b) {

    }

    @Override
    public boolean isCatchKey(int i) {
        return false;
    }

    @Override
    public long getCurrentEventTime() {
        return currentEventTimeStamp;
    }

    @Override
    public void getRotationMatrix(float[] matrix) {
        // TODO Auto-generated method stub

    }

    static class KeyEvent {
        static final int KEY_DOWN = 0;
        static final int KEY_UP = 1;
        static final int KEY_TYPED = 2;

        long timeStamp;
        int type;
        int keyCode;
        char keyChar;
    }

    static class TouchEvent {
        static final int TOUCH_DOWN = 0;
        static final int TOUCH_UP = 1;
        static final int TOUCH_DRAGGED = 2;
        static final int TOUCH_SCROLLED = 3;
        static final int TOUCH_MOVED = 4;

        long timeStamp;
        int type;
        int x;
        int y;
        int scrollAmount;
        int button;
        int pointer;
    }
}
