package com.esotericsoftware.SpinePreview.attachments;

import com.badlogic.gdx.utils.Null;
import com.esotericsoftware.SpinePreview.Skin;

public interface AttachmentLoader {
    @Null
    RegionAttachment newRegionAttachment(Skin skin, String name, String path);

    @Null
    MeshAttachment newMeshAttachment(Skin skin, String name, String path);

    @Null
    BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name);

    @Null
    ClippingAttachment newClippingAttachment(Skin skin, String name);

    @Null
    PathAttachment newPathAttachment(Skin skin, String name);

    @Null
    PointAttachment newPointAttachment(Skin skin, String name);
}
