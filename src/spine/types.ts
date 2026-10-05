export interface SpineFile {
  name: string;
  size: number;
}

export interface SpineVersionInfo {
  major: number;
  minor: number;
  patch: number;
  raw: string;
}

export interface SkeletonAssets {
  skeleton: SpineFile;
  atlas: SpineFile[];
  textures: SpineFile[];
}
