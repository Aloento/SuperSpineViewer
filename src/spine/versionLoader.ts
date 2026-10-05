import type { SpineVersionInfo } from './types';

// M1/M2 实现：读取 .skel/.json 头部 version 字符串
export async function detectSpineVersion(_data: ArrayBuffer): Promise<SpineVersionInfo | null> {
  return null;
}
