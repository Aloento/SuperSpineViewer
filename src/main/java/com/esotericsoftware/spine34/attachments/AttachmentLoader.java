package com.esotericsoftware.spine34.attachments;

import com.esotericsoftware.spine34.Skin;

public interface AttachmentLoader {
	/** @return May be null to not load any attachment. */
	RegionAttachment newRegionAttachment(Skin skin, String name, String path);

	/** @return May be null to not load any attachment. */
	MeshAttachment newMeshAttachment(Skin skin, String name, String path);

	/** @return May be null to not load any attachment. */
	BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name);

	/** @return May be null to not load any attachment. */
	PathAttachment newPathAttachment(Skin skin, String name);
}
