import { useState } from 'react';
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
import VersionSelector from './components/VersionSelector';
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
  tools: {
    display: 'flex',
    alignItems: 'stretch',
    gap: tokens.spacingHorizontalM,
    flexWrap: 'wrap',
  },
  version: {
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
    gap: tokens.spacingHorizontalXS,
    flexShrink: 0,
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
  const [manualPackId, setManualPackId] = useState<string | null>(null);
  const renderer = useSpineRenderer(width, height, manualPackId);

  return (
    <div className={styles.shell}>
      <Navbar />

      <div className={styles.panel}>
        <div className={styles.tools}>
          <FileDropZone onFiles={(files) => void renderer.loadFiles(files)} busy={renderer.status === 'loading'} />
          <div className={styles.version}>
            <Text size={200} className={styles.muted}>
              {t('version.label')}:
            </Text>
            <VersionSelector value={manualPackId} onChange={setManualPackId} />
          </div>
        </div>

        {renderer.error && (
          <MessageBar intent="error" data-ssv="error">
            <MessageBarBody>{t(renderer.error.key, renderer.error.values)}</MessageBarBody>
          </MessageBar>
        )}
        {!renderer.error && renderer.warning && (
          <MessageBar intent="warning" data-ssv="warning">
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
          {renderer.packId && (
            <Text size={200} className={styles.muted}>
              {t('status.runtime')}: {renderer.runtime} / {renderer.packId} ({renderer.backend})
            </Text>
          )}
          {renderer.bones !== null && (
            <Text size={200} className={styles.muted}>
              {t('status.bones')}: {renderer.bones} · {t('status.animations')}: {renderer.animationCount}
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
