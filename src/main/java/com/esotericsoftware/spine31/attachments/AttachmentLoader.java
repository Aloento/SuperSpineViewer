package com.esotericsoftware.spine31.attachments;

import com.esotericsoftware.spine31.Skin;

public interface AttachmentLoader {
    RegionAttachment newRegionAttachment(Skin skin, String name, String path);

    MeshAttachment newMeshAttachment(Skin skin, String name, String path);

    WeightedMeshAttachment newWeightedMeshAttachment(Skin skin, String name, String path);

    BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name);
}
