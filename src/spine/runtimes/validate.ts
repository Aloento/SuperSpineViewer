import { RuntimeError } from '../types';
import type { SkeletonSummary, SpineVersionInfo } from '../types';

/**
 * §12.6 解析结果校验：4.x 运行时对版本不匹配的骨架不报错，会静默产出 bones=0 的空数据，
 * 所以必须在解析后立即校验，否则用户看到的是空白而不是错误。
 */
export function validateSkeletonData(data: any, expected: SpineVersionInfo): SkeletonSummary {
  const bones: number = data?.bones?.length ?? 0;
  const animations: number = data?.animations?.length ?? 0;
  if (bones <= 0) throw new RuntimeError('parseInvalid', 'bones=0');
  if (animations <= 0) throw new RuntimeError('parseInvalid', 'animations=0');

  // 官方 spineboy-mesh 一类骨架的 skeleton width/height 就是 0，只能要求是有限数
  const width: number = data?.width ?? 0;
  const height: number = data?.height ?? 0;
  if (!Number.isFinite(width) || !Number.isFinite(height)) throw new RuntimeError('parseInvalid', 'bounds=NaN');

  const declared = String(data?.version ?? '');
  if (declared) {
    const normalized = declared.replace(/-beta.*/i, '');
    if (!normalized.startsWith(`${expected.major}.${expected.minor}.`)) {
      throw new RuntimeError('parseInvalid', `version=${declared}`);
    }
  }

  return {
    declaredVersion: declared,
    bones,
    animationCount: animations,
    width,
    height,
    duration: Number(data?.animations?.[0]?.duration) || 0,
  };
}
