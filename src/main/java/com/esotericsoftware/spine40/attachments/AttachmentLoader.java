package com.esotericsoftware.spine40.attachments;

import com.badlogic.gdx.utils.Null;

import com.esotericsoftware.spine40.Skin;

/** The interface which can be implemented to customize creating and populating attachments.
 * <p>
 * See <a href='http://esotericsoftware.com/spine-loading-skeleton-data#AttachmentLoader'>Loading skeleton data</a> in the Spine
 * Runtimes Guide. */
public interface AttachmentLoader {
	/** @return May be null to not load the attachment. */
	@Null RegionAttachment newRegionAttachment(Skin skin, String name, String path);

	/** @return May be null to not load the attachment. In that case null should also be returned for child meshes. */
	@Null MeshAttachment newMeshAttachment(Skin skin, String name, String path);

	/** @return May be null to not load the attachment. */
	@Null BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name);

	/** @return May be null to not load the attachment. */
	@Null ClippingAttachment newClippingAttachment(Skin skin, String name);

	/** @return May be null to not load the attachment. */
	@Null PathAttachment newPathAttachment(Skin skin, String name);

	/** @return May be null to not load the attachment. */
	@Null PointAttachment newPointAttachment(Skin skin, String name);
}
