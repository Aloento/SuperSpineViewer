package com.esotericsoftware.spine36.attachments;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.spine36.SkeletonBounds;

/**
 * An attachment with vertices that make up a polygon. Can be used for hit detection, creating physics bodies, spawning particle
 * effects, and more.
 * <p>
 * See {@link SkeletonBounds} and <a href="http://esotericsoftware.com/spine-bounding-boxes">Bounding Boxes</a> in the Spine User
 * Guide.
 */
public class BoundingBoxAttachment extends VertexAttachment {
    // Nonessential.
    final Color color = new Color(0.38f, 0.94f, 0, 1); // 60f000ff

    public BoundingBoxAttachment(String name) {
        super(name);
    }

    /**
     * The color of the bounding box as it was in Spine. Available only when nonessential data was exported. Bounding boxes are
     * not usually rendered at runtime.
     */
    public Color getColor() {
        return color;
    }
}
