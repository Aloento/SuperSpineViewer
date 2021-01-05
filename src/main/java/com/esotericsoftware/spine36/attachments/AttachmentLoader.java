package com.esotericsoftware.spine36.attachments;

import com.esotericsoftware.spine36.Skin;

/** The interface which can be implemented to customize creating and populating attachments.
 * <p>
 * See <a href='http://esotericsoftware.com/spine-loading-skeleton-data#AttachmentLoader'>Loading skeleton data</a> in the Spine
 * Runtimes Guide. */
public interface AttachmentLoader {
	/** @return May be null to not load the attachment. */
	RegionAttachment newRegionAttachment(Skin skin, String name, String path);

	/** @return May be null to not load the attachment. */
	MeshAttachment newMeshAttachment(Skin skin, String name, String path);

	/** @return May be null to not load the attachment. */
	BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name);
	
	/** @return May be null to not load the attachment. */
	ClippingAttachment newClippingAttachment(Skin skin, String name);

	/** @return May be null to not load the attachment. */
	PathAttachment newPathAttachment(Skin skin, String name);

	/** @return May be null to not load the attachment. */
	PointAttachment newPointAttachment(Skin skin, String name);
}
