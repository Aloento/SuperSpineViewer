package com.esotericsoftware.SpinePreview.utils;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import com.esotericsoftware.SpinePreview.SkeletonBinary;
import com.esotericsoftware.SpinePreview.SkeletonData;
import com.esotericsoftware.SpinePreview.SkeletonJson;
import com.esotericsoftware.SpinePreview.attachments.AtlasAttachmentLoader;
import com.esotericsoftware.SpinePreview.attachments.AttachmentLoader;


public class SkeletonDataLoader extends AsynchronousAssetLoader<SkeletonData, SkeletonDataLoader.SkeletonDataParameter> {
    private SkeletonData skeletonData;

    public SkeletonDataLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    public void loadAsync(AssetManager manager, String fileName, FileHandle file, @Null SkeletonDataParameter parameter) {
        float scale = 1;
        AttachmentLoader attachmentLoader = null;
        if (parameter != null) {
            scale = parameter.scale;
            if (parameter.attachmentLoader != null)
                attachmentLoader = parameter.attachmentLoader;
            else if (parameter.atlasName != null)
                attachmentLoader = new AtlasAttachmentLoader(manager.get(parameter.atlasName, TextureAtlas.class));
        }
        if (attachmentLoader == null)
            attachmentLoader = new AtlasAttachmentLoader(manager.get(file.pathWithoutExtension() + ".atlas", TextureAtlas.class));

        if (file.extension().equalsIgnoreCase("skel")) {
            SkeletonBinary skeletonBinary = new SkeletonBinary(attachmentLoader);
            skeletonBinary.setScale(scale);
            skeletonData = skeletonBinary.readSkeletonData(file);
        } else {
            SkeletonJson skeletonJson = new SkeletonJson(attachmentLoader);
            skeletonJson.setScale(scale);
            skeletonData = skeletonJson.readSkeletonData(file);
        }
    }

    public SkeletonData loadSync(AssetManager manager, String fileName, FileHandle file, @Null SkeletonDataParameter parameter) {
        SkeletonData skeletonData = this.skeletonData;
        this.skeletonData = null;
        return skeletonData;
    }

    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, @Null SkeletonDataParameter parameter) {
        if (parameter == null) return null;
        if (parameter.attachmentLoader != null) return null;
        Array<AssetDescriptor> dependencies = new Array();
        dependencies.add(new AssetDescriptor(parameter.atlasName, TextureAtlas.class));
        return dependencies;
    }

    static public class SkeletonDataParameter extends AssetLoaderParameters<SkeletonData> {
        public String atlasName;
        public AttachmentLoader attachmentLoader;
        public float scale = 1;

        public SkeletonDataParameter() {
        }

        public SkeletonDataParameter(String atlasName) {
            this.atlasName = atlasName;
        }

        public SkeletonDataParameter(String atlasName, float scale) {
            this.atlasName = atlasName;
            this.scale = scale;
        }

        public SkeletonDataParameter(AttachmentLoader attachmentLoader) {
            this.attachmentLoader = attachmentLoader;
        }

        public SkeletonDataParameter(AttachmentLoader attachmentLoader, float scale) {
            this.attachmentLoader = attachmentLoader;
            this.scale = scale;
        }
    }
}
