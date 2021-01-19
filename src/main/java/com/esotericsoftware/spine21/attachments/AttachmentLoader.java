package com.esotericsoftware.spine21.attachments;

import com.esotericsoftware.spine21.Skin;

public interface AttachmentLoader {
    RegionAttachment newRegionAttachment(Skin skin, String name, String path);

    MeshAttachment newMeshAttachment(Skin skin, String name, String path);

    SkinnedMeshAttachment newSkinnedMeshAttachment(Skin skin, String name, String path);

    BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name);
}
