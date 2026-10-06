import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Badge,
  Button,
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
import ControlPanel from './components/ControlPanel';
import ExportPanel from './components/ExportPanel';
import { useSpineRenderer } from './spine/useSpineRenderer';
import { useExport } from './export/useExport';
import { defaultExportOptions } from './export/presets';

const useStyles = makeStyles({
  shell: {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalL,
    padding: tokens.spacingVerticalXXL,
    boxSizing: 'border-box',
    maxWidth: '1600px',
    marginInline: 'auto',
    width: '100%',
  },
  // 左右两栏：左为预览，右为控制；窄屏堆叠为单列
  columns: {
    display: 'grid',
    gridTemplateColumns: 'minmax(0, 1.6fr) minmax(340px, 1fr)',
    gap: tokens.spacingHorizontalL,
    alignItems: 'start',
    '@media (max-width: 900px)': {
      gridTemplateColumns: '1fr',
    },
  },
  column: {
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalM,
    minWidth: 0,
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
  const exportControls = useMemo(
    () => ({
      getLoadInfo: renderer.getLoadInfo,
      pausePlayback: renderer.pausePlayback,
      resumePlayback: renderer.resumePlayback,
      showFrame: renderer.showFrame,
    }),
    [renderer.getLoadInfo, renderer.pausePlayback, renderer.resumePlayback, renderer.showFrame],
  );
  const exporter = useExport(exportControls);
  const loaded = renderer.status === 'playing';

  return (
    <div className={styles.shell}>
      <Navbar />

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

      <div className={styles.columns}>
        {/* 左栏：预览 + 运行时状态 */}
        <div className={styles.column}>
          <Card>
            <PreviewCanvas frame={renderer.frame} width={width} height={height} />
          </Card>

          <div className={styles.status}>
            {renderer.status === 'loading' && <Spinner size="tiny" label={t('dropzone.busy')} />}
            {renderer.status === 'idle' && <Text size={200} className={styles.muted}>{t('preview.empty')}</Text>}
            {loaded && <Badge appearance="tint">{t('status.playing')}</Badge>}
            {renderer.version && <Text size={200}>{t('status.version')}: {renderer.version}</Text>}
            {renderer.runtime && (
              <Text size={200} className={styles.muted}>
                {t('status.runtime')}: {renderer.runtime} · {renderer.packId} · {renderer.backend}
              </Text>
            )}
            {renderer.bones !== null && <Text size={200} className={styles.muted}>{t('status.bones')}: {renderer.bones}</Text>}
            {renderer.animationCount !== null && (
              <Text size={200} className={styles.muted}>{t('status.animations')}: {renderer.animationCount}</Text>
            )}
            {renderer.animation && <Text size={200} className={styles.muted}>{t('status.animation')}: {renderer.animation}</Text>}
            <Text size={200} className={styles.muted}>{t('status.canvas')}: {width}×{height}</Text>
            <Text size={200} className={styles.muted}>{t('preview.checkerboardHint')}</Text>
            <Button appearance="subtle" size="small" disabled={renderer.status === 'idle' || exporter.running} onClick={renderer.reset}>
              {t('control.reset')}
            </Button>
          </div>
        </div>

        {/* 右栏：资源载入 + 播放控制 + 导出 */}
        <div className={styles.column}>
          <div className={styles.tools}>
            <FileDropZone
              onFiles={(files) => void renderer.loadFiles(files)}
              busy={renderer.status === 'loading' || exporter.running}
            />
            <div className={styles.version}>
              <Text size={200} className={styles.muted}>
                {t('version.label')}:
              </Text>
              <VersionSelector value={manualPackId} onChange={setManualPackId} disabled={exporter.running} />
            </div>
          </div>

          <Card>
            <ControlPanel
              enabled={loaded}
              playing={renderer.playing}
              elapsedMs={renderer.elapsedMs}
              durationMs={renderer.duration * 1000}
              loop={renderer.loop}
              animation={renderer.animation ?? ''}
              animations={renderer.animations}
              skin={renderer.skin}
              skins={renderer.skins}
              offsetX={renderer.offsetX}
              offsetY={renderer.offsetY}
              scale={renderer.scale}
              premultiplied={renderer.premultiplied}
              onTogglePlay={() => void renderer.togglePlay()}
              onSeek={(ms) => void renderer.seekTo(ms)}
              onLoopChange={(value) => void renderer.changeLoop(value)}
              onAnimationChange={(name) => void renderer.changeAnimation(name)}
              onSkinChange={(name) => void renderer.changeSkin(name)}
              onOffsetXChange={(value) => void renderer.changeTransform({ offsetX: value })}
              onOffsetYChange={(value) => void renderer.changeTransform({ offsetY: value })}
              onScaleChange={(value) => void renderer.changeTransform({ scale: value })}
              onResetTransform={() => void renderer.changeTransform({ offsetX: 0, offsetY: 0, scale: 1 })}
              onPremultipliedChange={(value) => void renderer.changePremultiplied(value)}
            />
          </Card>

          <Card>
            <ExportPanel
              options={exporter.options}
              onChange={exporter.setOptions}
              state={exporter.state}
              running={exporter.running}
              enabled={loaded}
              onStart={() => void exporter.start()}
              onCancel={exporter.cancel}
            />
          </Card>
        </div>
      </div>

      <Footer />
    </div>
  );
}
