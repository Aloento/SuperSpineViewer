import { useTranslation } from 'react-i18next';
import { Badge, Card, Dropdown, Option, Text, Title3, makeStyles, tokens } from '@fluentui/react-components';
import Footer from './components/Footer';
import { changeLanguage, supportedLanguages, type SupportedLanguage } from './i18n';

const useStyles = makeStyles({
  shell: {
    minHeight: '100%',
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalL,
    padding: tokens.spacingVerticalXXL,
    boxSizing: 'border-box',
  },
  header: {
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: tokens.spacingHorizontalL,
  },
  language: {
    minWidth: '10rem',
  },
});

export default function App() {
  const styles = useStyles();
  const { t, i18n: instance } = useTranslation();
  const current = (supportedLanguages.find((item) => instance.language.startsWith(item)) ?? 'en') as SupportedLanguage;

  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <div>
          <Title3 as="h1" block>
            {t('app.name')}
          </Title3>
          <Text>{t('app.tagline')}</Text>
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
      </header>

      <Card>
        <Badge appearance="tint" color="brand">
          M0
        </Badge>
        <Text>{t('status.scaffolding')}</Text>
      </Card>

      <Footer />
    </div>
  );
}
