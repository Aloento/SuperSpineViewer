package com.esotericsoftware.spine21.attachments;

import com.esotericsoftware.spine21.Skin;

public interface AttachmentLoader {
    /**
     * @return May be null to not load any attachment.
     */
    RegionAttachment newRegionAttachment(Skin skin, String name, String path);

    /**
     * @return May be null to not load any attachment.
     */
    MeshAttachment newMeshAttachment(Skin skin, String name, String path);

    /**
     * @return May be null to not load any attachment.
     */
    SkinnedMeshAttachment newSkinnedMeshAttachment(Skin skin, String name, String path);

    /**
     * @return May be null to not load any attachment.
     */
    BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name);
}
