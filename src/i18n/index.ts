import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import en from './en.json';
import zh from './zh.json';

export const supportedLanguages = ['zh', 'en'] as const;
export type SupportedLanguage = (typeof supportedLanguages)[number];

const storageKey = 'ssv.language';

function normalize(language: string | undefined | null): SupportedLanguage | null {
  if (!language) return null;
  const lower = language.toLowerCase();
  return supportedLanguages.find((item) => lower.startsWith(item)) ?? null;
}

function detectLanguage(): SupportedLanguage {
  const stored = normalize(localStorage.getItem(storageKey));
  if (stored) return stored;
  for (const candidate of navigator.languages ?? []) {
    const matched = normalize(candidate);
    if (matched) return matched;
  }
  return normalize(navigator.language) ?? 'en';
}

void i18n.use(initReactI18next).init({
  resources: {
    zh: { translation: zh },
    en: { translation: en },
  },
  lng: detectLanguage(),
  fallbackLng: 'en',
  supportedLngs: supportedLanguages,
  interpolation: { escapeValue: false },
});

export function changeLanguage(language: SupportedLanguage): void {
  localStorage.setItem(storageKey, language);
  void i18n.changeLanguage(language);
}

document.documentElement.lang = i18n.resolvedLanguage ?? i18n.language;

// init 时也会触发 languageChanged，因此这里不写 localStorage，否则首次访问就把系统语言固化
i18n.on('languageChanged', (language) => {
  document.documentElement.lang = normalize(language) ?? 'en';
});

export default i18n;
