package com.archive.SpineLegacy.attachments;

import com.archive.SpineLegacy.Skin;

public interface AttachmentLoader {
    RegionAttachment newRegionAttachment(Skin skin, String name, String path);

    MeshAttachment newMeshAttachment(Skin skin, String name, String path);

    WeightedMeshAttachment newWeightedMeshAttachment(Skin skin, String name, String path);

    BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name);
}
