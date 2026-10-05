import { useTranslation } from 'react-i18next';
import { Badge, Card, Text, makeStyles, tokens } from '@fluentui/react-components';
import Navbar from './components/Navbar';
import Footer from './components/Footer';

const useStyles = makeStyles({
  shell: {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalL,
    padding: tokens.spacingVerticalXXL,
    boxSizing: 'border-box',
  },
});

export default function App() {
  const styles = useStyles();
  const { t } = useTranslation();

  return (
    <div className={styles.shell}>
      <Navbar />

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
