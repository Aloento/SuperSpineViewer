package com.archive.Spine40;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.archive.Spine40.SkeletonJson.LinkedMesh;
import com.archive.Spine40.attachments.AtlasAttachmentLoader;
import com.archive.Spine40.attachments.AttachmentLoader;

import java.io.InputStream;

/**
 * Base class for loading skeleton data from a file.
 * <p>
 * See <a href="http://esotericsoftware.com/spine-loading-skeleton-data#JSON-and-binary-data">JSON and binary data</a> in the
 * Spine Runtimes Guide.
 */
abstract public class SkeletonLoader {
    final AttachmentLoader attachmentLoader;
    final Array<LinkedMesh> linkedMeshes = new Array();
    float scale = 1;

    /**
     * Creates a skeleton loader that loads attachments using an {@link AtlasAttachmentLoader} with the specified atlas.
     */
    public SkeletonLoader(TextureAtlas atlas) {
        attachmentLoader = new AtlasAttachmentLoader(atlas);
    }

    /**
     * Creates a skeleton loader that loads attachments using the specified attachment loader.
     * <p>
     * See <a href='http://esotericsoftware.com/spine-loading-skeleton-data#JSON-and-binary-data'>Loading skeleton data</a> in the
     * Spine Runtimes Guide.
     */
    public SkeletonLoader(AttachmentLoader attachmentLoader) {
        if (attachmentLoader == null) throw new IllegalArgumentException("attachmentLoader cannot be null.");
        this.attachmentLoader = attachmentLoader;
    }

    /**
     * Scales bone positions, image sizes, and translations as they are loaded. This allows different size images to be used at
     * runtime than were used in Spine.
     * <p>
     * See <a href="http://esotericsoftware.com/spine-loading-skeleton-data#Scaling">Scaling</a> in the Spine Runtimes Guide.
     */
    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        if (scale == 0) throw new IllegalArgumentException("scale cannot be 0.");
        this.scale = scale;
    }

    abstract public SkeletonData readSkeletonData(FileHandle file);

    abstract public SkeletonData readSkeletonData(InputStream input);
}
