package com.esotericsoftware.spine32.attachments;

import com.esotericsoftware.spine32.Skin;

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
    WeightedMeshAttachment newWeightedMeshAttachment(Skin skin, String name, String path);

    /**
     * @return May be null to not load any attachment.
     */
    BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name);
}
