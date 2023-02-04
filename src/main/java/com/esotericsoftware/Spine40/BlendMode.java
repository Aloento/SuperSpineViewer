package com.esotericsoftware.Spine40;

import com.badlogic.gdx.graphics.g2d.Batch;

import static com.badlogic.gdx.graphics.GL20.*;

/**
 * Determines how images are blended with existing pixels when drawn.
 */
public enum BlendMode {
    normal(GL_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE), //
    additive(GL_SRC_ALPHA, GL_ONE, GL_ONE, GL_ONE), //
    multiply(GL_DST_COLOR, GL_DST_COLOR, GL_ONE_MINUS_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA), //
    screen(GL_ONE, GL_ONE, GL_ONE_MINUS_SRC_COLOR, GL_ONE_MINUS_SRC_COLOR);

    static public final BlendMode[] values = values();
    public final int source, sourcePMA, destColor, sourceAlpha;

    BlendMode(int source, int sourcePMA, int destColor, int sourceAlpha) {
        this.source = source;
        this.sourcePMA = sourcePMA;
        this.destColor = destColor;
        this.sourceAlpha = sourceAlpha;
    }

    public void apply(Batch batch, boolean premultipliedAlpha) {
        batch.setBlendFunctionSeparate(premultipliedAlpha ? sourcePMA : source, destColor, sourceAlpha, destColor);
    }
}
