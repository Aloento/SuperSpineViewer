package com.esotericsoftware.spine35.attachments;

import com.esotericsoftware.spine35.Skin;


public interface AttachmentLoader {
    
    RegionAttachment newRegionAttachment(Skin skin, String name, String path);

    
    MeshAttachment newMeshAttachment(Skin skin, String name, String path);

    
    BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name);

    
    PathAttachment newPathAttachment(Skin skin, String name);
}
