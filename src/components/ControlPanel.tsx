import { useTranslation } from 'react-i18next';
import {
  Button,
  Dropdown,
  Field,
  Option,
  Slider,
  Switch,
  Text,
  makeStyles,
  tokens,
} from '@fluentui/react-components';

const useStyles = makeStyles({
  root: {
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalM,
    minWidth: 0,
  },
  transport: {
    display: 'flex',
    alignItems: 'center',
    gap: tokens.spacingHorizontalM,
  },
  playButton: {
    minWidth: '6.5rem',
    flexShrink: 0,
  },
  time: {
    fontVariantNumeric: 'tabular-nums',
    color: tokens.colorNeutralForeground2,
    whiteSpace: 'nowrap',
  },
  progress: {
    flexGrow: 1,
    flexBasis: '12rem',
    minWidth: 0,
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: tokens.spacingHorizontalM,
  },
  full: {
    gridColumn: '1 / -1',
  },
  switches: {
    display: 'flex',
    alignItems: 'center',
    gap: tokens.spacingHorizontalL,
    flexWrap: 'wrap',
  },
  section: {
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalS,
  },
  sectionTitle: {
    color: tokens.colorNeutralForeground3,
  },
  sliderValue: {
    fontVariantNumeric: 'tabular-nums',
    color: tokens.colorNeutralForeground2,
    whiteSpace: 'nowrap',
    minWidth: '3.5rem',
    textAlign: 'right',
  },
  sliderRow: {
    display: 'flex',
    alignItems: 'center',
    gap: tokens.spacingHorizontalS,
  },
  slider: {
    flexGrow: 1,
    minWidth: 0,
  },
  hint: {
    color: tokens.colorNeutralForeground3,
  },
});

interface ControlPanelProps {
  /** 已加载骨架（status==='playing'），未加载时整块禁用 */
  enabled: boolean;
  playing: boolean;
  elapsedMs: number;
  durationMs: number;
  loop: boolean;
  animation: string;
  animations: string[];
  skin: string;
  skins: string[];
  offsetX: number;
  offsetY: number;
  scale: number;
  premultiplied: boolean;
  onTogglePlay: () => void;
  onSeek: (timeMs: number) => void;
  onLoopChange: (loop: boolean) => void;
  onAnimationChange: (name: string) => void;
  onSkinChange: (name: string) => void;
  onOffsetXChange: (value: number) => void;
  onOffsetYChange: (value: number) => void;
  onScaleChange: (value: number) => void;
  onResetTransform: () => void;
  onPremultipliedChange: (value: boolean) => void;
}

/** 秒数文本：0:03.4 / 1:02.0 */
function formatTime(ms: number): string {
  const total = Math.max(0, ms) / 1000;
  const minutes = Math.floor(total / 60);
  const seconds = total - minutes * 60;
  return minutes > 0
    ? `${minutes}:${seconds.toFixed(1).padStart(4, '0')}`
    : `${seconds.toFixed(1)}s`;
}

export default function ControlPanel(props: ControlPanelProps) {
  const styles = useStyles();
  const { t } = useTranslation();

  const duration = Math.max(0, props.durationMs);
  // 循环播放时进度条显示取模位置；非循环到终点钳在 duration
  const shown =
    duration > 0 ? (props.loop ? props.elapsedMs % duration : Math.min(props.elapsedMs, duration)) : props.elapsedMs;
  const half = 512;

  return (
    <div className={styles.root} data-ssv="control-panel">
      <div className={styles.transport}>
        <Button
          data-ssv="play-toggle"
          className={styles.playButton}
          appearance="primary"
          disabled={!props.enabled}
          onClick={props.onTogglePlay}
        >
          {props.playing ? t('control.pause') : t('control.play')}
        </Button>
        <Slider
          data-ssv="progress"
          className={styles.progress}
          min={0}
          max={duration || 1}
          step={10}
          value={Math.min(duration || 0, shown)}
          disabled={!props.enabled || duration <= 0}
          aria-label={t('control.progress')}
          onChange={(_, data) => props.onSeek(data.value)}
        />
        <Text size={200} className={styles.time} data-ssv="time">
          {formatTime(shown)} / {formatTime(duration)}
        </Text>
      </div>

      <div className={styles.grid}>
        <Field label={t('control.animation')} size="small">
          <Dropdown
            data-ssv="animation-select"
            disabled={!props.enabled}
            value={props.animation}
            selectedOptions={[props.animation]}
            onOptionSelect={(_, data) => props.onAnimationChange(String(data.optionValue ?? ''))}
          >
            {props.animations.map((name) => (
              <Option key={name} value={name}>
                {name}
              </Option>
            ))}
          </Dropdown>
        </Field>

        <Field label={t('control.skin')} size="small">
          <Dropdown
            data-ssv="skin-select"
            disabled={!props.enabled}
            value={props.skin || t('control.skinDefault')}
            selectedOptions={[props.skin]}
            onOptionSelect={(_, data) => props.onSkinChange(String(data.optionValue ?? ''))}
          >
            <Option value="">{t('control.skinDefault')}</Option>
            {props.skins
              .filter((name) => name !== '')
              .map((name) => (
                <Option key={name} value={name}>
                  {name}
                </Option>
              ))}
          </Dropdown>
        </Field>

        <div className={`${styles.full} ${styles.switches}`}>
          <Switch
            data-ssv="loop-toggle"
            disabled={!props.enabled}
            checked={props.loop}
            onChange={(_, data) => props.onLoopChange(data.checked)}
            label={t('control.loop')}
          />
          <Switch
            data-ssv="premultiplied-toggle"
            disabled={!props.enabled}
            checked={props.premultiplied}
            onChange={(_, data) => props.onPremultipliedChange(data.checked)}
            label={t('control.premultiplied')}
          />
        </div>
      </div>

      <div className={styles.section}>
        <Text size={200} className={styles.sectionTitle}>
          {t('control.transform')}
        </Text>
        <div className={styles.sliderRow}>
          <Text size={200} className={styles.sliderValue}>
            {t('control.offsetX')}
          </Text>
          <Slider
            data-ssv="offset-x"
            className={styles.slider}
            min={-half}
            max={half}
            step={1}
            value={props.offsetX}
            disabled={!props.enabled}
            aria-label={t('control.offsetX')}
            onChange={(_, data) => props.onOffsetXChange(data.value)}
          />
          <Text size={200} className={styles.sliderValue}>
            {props.offsetX}px
          </Text>
        </div>
        <div className={styles.sliderRow}>
          <Text size={200} className={styles.sliderValue}>
            {t('control.offsetY')}
          </Text>
          <Slider
            data-ssv="offset-y"
            className={styles.slider}
            min={-half}
            max={half}
            step={1}
            value={props.offsetY}
            disabled={!props.enabled}
            aria-label={t('control.offsetY')}
            onChange={(_, data) => props.onOffsetYChange(data.value)}
          />
          <Text size={200} className={styles.sliderValue}>
            {props.offsetY}px
          </Text>
        </div>
        <div className={styles.sliderRow}>
          <Text size={200} className={styles.sliderValue}>
            {t('control.scale')}
          </Text>
          <Slider
            data-ssv="scale-slider"
            className={styles.slider}
            min={0.1}
            max={3}
            step={0.05}
            value={props.scale}
            disabled={!props.enabled}
            aria-label={t('control.scale')}
            onChange={(_, data) => props.onScaleChange(data.value)}
          />
          <Text size={200} className={styles.sliderValue}>
            {(props.scale * 100).toFixed(0)}%
          </Text>
        </div>
        <div>
          <Button data-ssv="reset-transform" appearance="subtle" size="small" disabled={!props.enabled} onClick={props.onResetTransform}>
            {t('control.resetTransform')}
          </Button>
        </div>
      </div>

      {!props.enabled && (
        <Text size={200} className={styles.hint}>
          {t('control.disabledHint')}
        </Text>
      )}
    </div>
  );
}
