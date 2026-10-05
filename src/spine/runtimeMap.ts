export interface SpineRuntimeDescriptor {
  readonly range: string;
  readonly loader: () => Promise<unknown>;
}

// M2 填充：version → spine-core runtime 的映射与候选回退顺序
export const runtimeCandidates: SpineRuntimeDescriptor[] = [];

export function resolveRuntimeCandidates(_version: string): SpineRuntimeDescriptor[] {
  return runtimeCandidates;
}
