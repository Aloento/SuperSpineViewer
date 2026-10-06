/**
 * 各版本 data.skins 里都（可能）包含默认皮肤本身，而 UI 用 '' 表示默认皮肤，
 * 直接列出来会出现「默认皮肤 / default」两条重复项。按 defaultSkin 名字剔除。
 */
export function nonDefaultSkinNames(data: any): string[] {
  const defaultName: string | undefined = data?.defaultSkin?.name;
  const names: string[] = (data?.skins ?? []).map((item: any) => String(item.name));
  return defaultName === undefined ? names : names.filter((name) => name !== defaultName);
}
