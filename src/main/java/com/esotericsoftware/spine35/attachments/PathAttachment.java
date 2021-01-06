package com.esotericsoftware.spine35.attachments;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.spine35.PathConstraint;

/**
 * An attachment whose vertices make up a composite Bezier curve.
 * <p>
 * See {@link PathConstraint} and <a href="http://esotericsoftware.com/spine-paths">Paths</a> in the Spine User Guide.
 */
public class PathAttachment extends VertexAttachment {
    // Nonessential.
    final Color color = new Color(1, 0.5f, 0, 1);
    float[] lengths;
    boolean closed, constantSpeed;

    public PathAttachment(String name) {
        super(name);
    }

    /**
     * If true, the start and end knots are connected.
     */
    public boolean getClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    /**
     * If true, additional calculations are performed to make calculating positions along the path more accurate. If false, fewer
     * calculations are performed but calculating positions along the path is less accurate.
     */
    public boolean getConstantSpeed() {
        return constantSpeed;
    }

    public void setConstantSpeed(boolean constantSpeed) {
        this.constantSpeed = constantSpeed;
    }

    /**
     * The lengths along the path in the setup pose from the start of the path to the end of each Bezier curve.
     */
    public float[] getLengths() {
        return lengths;
    }

    public void setLengths(float[] lengths) {
        this.lengths = lengths;
    }

    /**
     * The color of the path as it was in Spine. Available only when nonessential data was exported. Paths are not usually
     * rendered at runtime.
     */
    public Color getColor() {
        return color;
    }
}
