package ben.ui.widget;

import ben.ui.action.IAction;
import ben.ui.action.IActionListener;
import ben.ui.input.key.BasicKeyHandler;
import ben.ui.input.key.IKeyHandler;
import ben.ui.input.mouse.BasicMouseHandler;
import ben.ui.input.mouse.IMouseHandler;
import ben.math.PmvMatrix;
import ben.math.Vec2i;
import ben.ui.resource.GlResourceManager;
import ben.math.Vec3f;
import net.jcip.annotations.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.jogamp.opengl.GL3;

/**
 * Abstract Widget.
 */
@ThreadSafe
public abstract class AbstractWidget implements IWidget {

    /**
     * The mouse handler for the widget.
     */
    private final BasicMouseHandler mouseHandler = new BasicMouseHandler();

    /**
     * The Key Handler for the widget.
     */
    private final BasicKeyHandler keyHandler = new BasicKeyHandler();

    /**
     * The action listener for the widget.
     * <p>
     *     Updates the enabled state of the widget when the action changes.
     * </p>
     */
    private final ActionListener actionListener = new ActionListener();

    /**
     * The name of the widget.
     */
    @Nullable
    private final String name;

    /**
     * The position of the widget with respect to its parent.
     */
    @NotNull
    private Vec2i position = new Vec2i(0, 0);

    /**
     * The size of the widget.
     */
    @NotNull
    private Vec2i size = new Vec2i(0, 0);

    /**
     * Is the drawing of the widget initialised?
     * <p>
     *     Used to build VAOs and stuff.
     * </p>
     */
    private boolean isInitialised = false;

    /**
     * Is the widget dirty?
     * <p>
     *     If true, the updateDraw method will be called before the next doDraw.
     * </p>
     */
    private boolean isDirty = false;

    /**
     * Is the widget visible?
     * <p>
     *     initDraw, doDraw and updateDraw will not be called if it's not visible.
     * </p>
     */
    private boolean isVisible = true;

    /**
     * The action associated to this widget.
     * <p>
     *     It's up to the implementing sub class to execute it!
     * </p>
     */
    @Nullable
    private IAction action;

    /**
     * True if the widget is enabled.
     * <p>
     *     This is tied to the action.
     * </p>
     */
    private boolean enabled = true;

    /**
     * True if the widget is focused.
     */
    private boolean focused;

    /**
     * Constructor.
     * @param name the name of the widget
     */
    protected AbstractWidget(@Nullable String name) {
        this.name = name;
    }

    @Override
    public final void setPosition(@NotNull Vec2i position) {
        this.position = position;
    }

    @NotNull
    @Override
    public final Vec2i getPosition() {
        return position;
    }

    @Override
    public final void setSize(@NotNull Vec2i size) {
//        assert size.getX() >= 0 : "Canvas size must not be negative";
//        assert size.getY() >= 0 : "Canvas size must not be negative";
        this.size = size;
        setDirty();
    }

    @NotNull
    @Override
    public final Vec2i getSize() {
        return size;
    }

    @Nullable
    @Override
    public String getName() {
        return name;
    }

    @Override
    public final void draw(@NotNull GL3 gl, @NotNull PmvMatrix pmvMatrix, @NotNull GlResourceManager glResourceManager) {
        preDraw();
        if (isVisible) {
            if (!isInitialised) {
                initDraw(gl, glResourceManager);
                isInitialised = true;
            } else if (isDirty) {
                updateDraw(gl);
                isDirty = false;
            }
            pmvMatrix.push();
            pmvMatrix.translate(new Vec3f(position.getX(), position.getY(), 0));
            doDraw(gl, pmvMatrix);
            pmvMatrix.pop();
        }
    }

    /**
     * Pre Draw.
     * <p>
     *     Calculations that need to be done every frame should be put in here.
     *     Called even if the widget is not drawn.
     * </p>
     */
    protected void preDraw() { }

    /**
     * Initialise Draw.
     * <p>
     *     Called once, before the first time the widget is drawn.
     * </p>
     * @param gl the OpenGL interface
     * @param glResourceManager the OpenGL Resource Manager
     */
    protected abstract void initDraw(@NotNull GL3 gl, @NotNull GlResourceManager glResourceManager);

    /**
     * Update the draw.
     * <p>
     *     Called before doDraw if the widget is flagged as dirty.
     * </p>
     * @param gl the OpenGL interface
     */
    protected abstract void updateDraw(@NotNull GL3 gl);

    /**
     * Do the draw.
     * @param gl the OpenGL interface
     * @param pmvMatrix the Projection Model View Matrix
     */
    protected abstract void doDraw(@NotNull GL3 gl, @NotNull PmvMatrix pmvMatrix);

    /**
     * Flag the widget as dirty so that it is updated before the next time it's drawn.
     */
    protected final void setDirty() {
        isDirty = true;
    }

    @NotNull
    @Override
    public final IMouseHandler getMouseHandler() {
        return mouseHandler;
    }

    @NotNull
    @Override
    public final IKeyHandler getKeyHandler() {
        return keyHandler;
    }

    @Override
    public final boolean contains(@NotNull Vec2i pos) {
        return (pos.getX() >= position.getX()) && (pos.getY() >= position.getY()) && (pos.getX() <= position.getX() + size.getX()) && (pos.getY() <= position.getY() + size.getY());
    }

    @Override
    public final void setFocused(boolean focused) {
        if (this.focused != focused && enabled) {
            this.focused = focused;
            setDirty();
        }
    }

    /**
     * Is the widget focused.
     * @return true if the widget is focused
     */
    public final boolean isFocused() {
        return focused;
    }

    /**
     * Set the visible state of the widget.
     * @param isVisible true if it's visible
     */
    public final void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    @Override
    public final boolean isVisible() {
        return isVisible;
    }

    /**
     * Set the action of the widget, null if it has no action.
     * @param action the action
     */
    public final void setAction(@Nullable IAction action) {
        if (action != this.action) {
            if (this.action != null) {
                this.action.removeListener(actionListener);
            }
            this.action = action;
            if (this.action != null) {
                this.action.addListener(actionListener);
                setEnabled(this.action.isExecutable());
            }
            else {
                setEnabled(true);
            }
        }
    }

    /**
     * Get the action that is installed on this widget.
     * @return the action
     */
    @Nullable
    protected final IAction getAction() {
        return action;
    }

    /**
     * Set if the widget is enabled.
     * <p>
     *     Warning: don't use if the widget has an action
     * </p>
     * @param enabled true if it's enabled
     */
    public final void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            keyHandler.setEnabled(enabled);
            if (!enabled) {
                setFocused(false);
            }
        }
    }

    /**
     * Is the widget enabled.
     * @return true if it's enabled
     */
    protected final boolean isEnabled() {
        return enabled;
    }

    @Override
    public void remove(@NotNull GL3 gl) {
        isInitialised = false;
        isDirty = false;
    }

    /**
     * The action listener that is installed on the current action.
     */
    private class ActionListener implements IActionListener {

        @Override
        public void actionChanged() {
            assert action != null : "Listener can't be fired if action is null";
            setEnabled(action.isExecutable());
        }
    }
}