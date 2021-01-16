package com.esotericsoftware.spine37.attachments;

import com.badlogic.gdx.graphics.Color;


public class BoundingBoxAttachment extends VertexAttachment {

    final Color color = new Color(0.38f, 0.94f, 0, 1);

    public BoundingBoxAttachment(String name) {
        super(name);
    }

    
    public Color getColor() {
        return color;
    }
}
