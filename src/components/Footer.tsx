import { useTranslation } from 'react-i18next';
import { Link, Text, makeStyles, tokens } from '@fluentui/react-components';

const AUTHOR_URL = 'https://aloen.to/';
const REPO_URL = 'https://github.com/Aloento/SuperSpineViewer';

const useStyles = makeStyles({
  footer: {
    marginTop: 'auto',
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'center',
    gap: tokens.spacingHorizontalXS,
    paddingBlock: tokens.spacingVerticalL,
    color: tokens.colorNeutralForeground3,
  },
  copyright: {
    width: '100%',
    textAlign: 'center',
  },
});

export default function Footer() {
  const styles = useStyles();
  const { t } = useTranslation();
  const year = new Date().getFullYear();

  return (
    <footer className={styles.footer}>
      <Text size={200}>{t('footer.authorPrefix')}</Text>
      <Link href={AUTHOR_URL} target="_blank" rel="noreferrer noopener">
        Aloento
      </Link>
      <Text size={200}>·</Text>
      <Text size={200}>{t('footer.starPrefix')}</Text>
      <Link href={REPO_URL} target="_blank" rel="noreferrer noopener">
        {t('footer.starAction')}
      </Link>
      <Text size={200} className={styles.copyright}>
        © {year} Aloento
      </Text>
    </footer>
  );
}
