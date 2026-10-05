import { useTranslation } from 'react-i18next';
import {
  Badge,
  Card,
  MessageBar,
  MessageBarBody,
  Spinner,
  Text,
  makeStyles,
  tokens,
} from '@fluentui/react-components';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import FileDropZone from './components/FileDropZone';
import PreviewCanvas from './components/PreviewCanvas';
import { useSpineRenderer } from './spine/useSpineRenderer';
import { defaultExportOptions } from './export/presets';

const useStyles = makeStyles({
  shell: {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalL,
    padding: tokens.spacingVerticalXXL,
    boxSizing: 'border-box',
    maxWidth: '960px',
    marginInline: 'auto',
    width: '100%',
  },
  panel: {
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalM,
  },
  status: {
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: tokens.spacingHorizontalM,
  },
  muted: {
    color: tokens.colorNeutralForeground3,
  },
});

export default function App() {
  const styles = useStyles();
  const { t } = useTranslation();
  const { width, height } = defaultExportOptions;
  const renderer = useSpineRenderer(width, height);

  return (
    <div className={styles.shell}>
      <Navbar />

      <div className={styles.panel}>
        <FileDropZone onFiles={(files) => void renderer.loadFiles(files)} busy={renderer.status === 'loading'} />

        {renderer.error && (
          <MessageBar intent="error">
            <MessageBarBody>{t(renderer.error.key, renderer.error.values)}</MessageBarBody>
          </MessageBar>
        )}
        {!renderer.error && renderer.warning && (
          <MessageBar intent="warning">
            <MessageBarBody>{t(renderer.warning.key, renderer.warning.values)}</MessageBarBody>
          </MessageBar>
        )}

        <Card>
          <PreviewCanvas frame={renderer.frame} width={width} height={height} />
        </Card>

        <div className={styles.status}>
          {renderer.status === 'loading' && <Spinner size="tiny" />}
          {renderer.status === 'idle' && <Text className={styles.muted}>{t('preview.empty')}</Text>}
          {renderer.status !== 'idle' && renderer.status !== 'error' && (
            <Badge appearance="tint" color="brand">
              {t('status.playing')}
            </Badge>
          )}
          {renderer.version && (
            <Text size={200}>
              {t('status.version')}: {renderer.version}
            </Text>
          )}
          {renderer.animation && (
            <Text size={200}>
              {t('status.animation')}: {renderer.animation}
            </Text>
          )}
          <Text size={200}>
            {t('status.canvas')}: {width}×{height}
          </Text>
          <Text size={200} className={styles.muted}>
            {t('preview.checkerboardHint')}
          </Text>
        </div>
      </div>

      <Footer />
    </div>
  );
}
