import { useTranslation } from 'react-i18next';
import { Dropdown, Option, makeStyles, tokens } from '@fluentui/react-components';
import { changeLanguage, supportedLanguages, type SupportedLanguage } from '../i18n';

const useStyles = makeStyles({
  bar: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: tokens.spacingHorizontalL,
    paddingBlock: tokens.spacingVerticalM,
    paddingBottom: tokens.spacingVerticalL,
    borderBottom: `1px solid ${tokens.colorNeutralStroke1}`,
  },
  brand: {
    display: 'flex',
    alignItems: 'center',
    gap: tokens.spacingHorizontalM,
    minWidth: 0,
  },
  logo: {
    width: '32px',
    height: '32px',
    borderRadius: tokens.borderRadiusMedium,
    background: `linear-gradient(135deg, ${tokens.colorBrandBackground}, ${tokens.colorBrandBackground2})`,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: tokens.colorBrandForeground1,
    fontWeight: 700,
    fontSize: '14px',
    flexShrink: 0,
  },
  title: {
    fontSize: '1.25rem',
    fontWeight: 600,
    lineHeight: 1.2,
    whiteSpace: 'nowrap',
  },
  tagline: {
    fontSize: '0.75rem',
    color: tokens.colorNeutralForeground3,
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  language: {
    minWidth: '9rem',
    flexShrink: 0,
  },
});

export default function Navbar() {
  const styles = useStyles();
  const { t, i18n: instance } = useTranslation();
  const current = (supportedLanguages.find((item) => instance.language.startsWith(item)) ?? 'en') as SupportedLanguage;

  return (
    <nav className={styles.bar}>
      <div className={styles.brand}>
        <div className={styles.logo}>SS</div>
        <div style={{ minWidth: 0 }}>
          <div className={styles.title}>{t('app.name')}</div>
          <div className={styles.tagline}>{t('app.tagline')}</div>
        </div>
      </div>
      <Dropdown
        className={styles.language}
        aria-label={t('language.label')}
        value={t(`language.${current}`)}
        selectedOptions={[current]}
        onOptionSelect={(_, data) => changeLanguage(data.optionValue as SupportedLanguage)}
      >
        {supportedLanguages.map((language) => (
          <Option key={language} value={language}>
            {t(`language.${language}`)}
          </Option>
        ))}
      </Dropdown>
    </nav>
  );
}
