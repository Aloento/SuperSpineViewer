package com.archive.SpineStandard.attachments;

import com.archive.SpineStandard.Skin;

public interface AttachmentLoader {
    RegionAttachment newRegionAttachment(Skin skin, String name, String path);

    MeshAttachment newMeshAttachment(Skin skin, String name, String path);

    BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name);

    ClippingAttachment newClippingAttachment(Skin skin, String name);

    PathAttachment newPathAttachment(Skin skin, String name);

    PointAttachment newPointAttachment(Skin skin, String name);
}
