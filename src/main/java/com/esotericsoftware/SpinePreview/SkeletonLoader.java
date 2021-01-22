package com.esotericsoftware.SpinePreview;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.SpinePreview.SkeletonJson.LinkedMesh;
import com.esotericsoftware.SpinePreview.attachments.AtlasAttachmentLoader;
import com.esotericsoftware.SpinePreview.attachments.AttachmentLoader;

abstract public class SkeletonLoader {
    final AttachmentLoader attachmentLoader;
    final Array<LinkedMesh> linkedMeshes = new Array<>();
    float scale = 1;

    public SkeletonLoader(TextureAtlas atlas) {
        attachmentLoader = new AtlasAttachmentLoader(atlas);
    }

    public SkeletonLoader(AttachmentLoader attachmentLoader) {
        if (attachmentLoader == null) throw new IllegalArgumentException("attachmentLoader cannot be null.");
        this.attachmentLoader = attachmentLoader;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        if (scale == 0) throw new IllegalArgumentException("scale cannot be 0.");
        this.scale = scale;
    }

    abstract public SkeletonData readSkeletonData(FileHandle file);
}
