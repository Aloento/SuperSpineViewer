import { useTranslation } from 'react-i18next';
import {
  Button,
  Dropdown,
  Field,
  Input,
  Option,
  ProgressBar,
  Slider,
  Text,
  makeStyles,
  tokens,
} from '@fluentui/react-components';
import { canvasPresets, MAX_CANVAS_SIZE, type ExportFormat, type ExportOptions } from '../export/presets';
import type { ExportState } from '../export/useExport';

const CUSTOM_CANVAS = 'custom';

const useStyles = makeStyles({
  root: {
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalM,
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'flex-end',
    gap: tokens.spacingHorizontalM,
  },
  customSize: {
    display: 'flex',
    alignItems: 'flex-end',
    gap: tokens.spacingHorizontalXS,
  },
  sizeInput: {
    width: '6.5rem',
  },
  status: {
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalXS,
  },
  hint: {
    color: tokens.colorNeutralForeground3,
  },
});

interface ExportPanelProps {
  options: ExportOptions;
  onChange: (options: ExportOptions) => void;
  state: ExportState;
  running: boolean;
  enabled: boolean;
  onStart: () => void;
  onCancel: () => void;
}

export default function ExportPanel({ options, onChange, state, running, enabled, onStart, onCancel }: ExportPanelProps) {
  const styles = useStyles();
  const { t } = useTranslation();

  const matched = canvasPresets.find((preset) => preset.width === options.width && preset.height === options.height);
  const presetId = matched ? matched.id : CUSTOM_CANVAS;
  const isCustom = presetId === CUSTOM_CANVAS;

  const set = (patch: Partial<ExportOptions>) => onChange({ ...options, ...patch });

  const onPreset = (_: unknown, data: { optionValue?: string | unknown }) => {
    const value = String(data.optionValue ?? '');
    if (value === CUSTOM_CANVAS) {
      // 切自定义时给一对合法默认值，VP9 要求偶数边长
      set({ width: 800, height: 800 });
      return;
    }
    const preset = canvasPresets.find((item) => item.id === value);
    if (preset) set({ width: preset.width, height: preset.height });
  };

  const percent = state.total > 0 ? Math.min(1, state.encoded / state.total) : 0;

  return (
    <div className={styles.root} data-ssv="export-panel">
      <div className={styles.row}>
        <Field label={t('export.format.label')} size="small">
          <Dropdown
            data-ssv="export-format"
            disabled={running}
            value={t(`export.format.${options.format}`)}
            selectedOptions={[options.format]}
            onOptionSelect={(_, data) => set({ format: String(data.optionValue) as ExportFormat })}
          >
            <Option value="vp9">{t('export.format.vp9')}</Option>
            <Option value="apng">{t('export.format.apng')}</Option>
          </Dropdown>
        </Field>

        <Field label={t('export.fps')} size="small">
          <Dropdown
            data-ssv="export-fps"
            disabled={running}
            value={`${options.fps}`}
            selectedOptions={[`${options.fps}`]}
            onOptionSelect={(_, data) => set({ fps: Number(data.optionValue) === 60 ? 60 : 30 })}
          >
            <Option value="30">30</Option>
            <Option value="60">60</Option>
          </Dropdown>
        </Field>

        <Field label={t('export.canvas.label')} size="small">
          <Dropdown
            data-ssv="export-canvas"
            disabled={running}
            value={isCustom ? t('export.canvas.custom') : t(matched?.labelKey ?? '')}
            selectedOptions={[presetId]}
            onOptionSelect={onPreset}
          >
            {canvasPresets.map((preset) => (
              <Option key={preset.id} value={preset.id}>
                {t(preset.labelKey)}
              </Option>
            ))}
            <Option value={CUSTOM_CANVAS}>{t('export.canvas.custom')}</Option>
          </Dropdown>
        </Field>

        {isCustom && (
          <Field label={t('export.canvas.size')} size="small">
            <div className={styles.customSize}>
              <Input
                data-ssv="export-width"
                type="number"
                min={16}
                max={MAX_CANVAS_SIZE}
                disabled={running}
                value={`${options.width}`}
                onChange={(_, data) => set({ width: clamp(data.value) })}
                className={styles.sizeInput}
              />
              <Text>×</Text>
              <Input
                data-ssv="export-height"
                type="number"
                min={16}
                max={MAX_CANVAS_SIZE}
                disabled={running}
                value={`${options.height}`}
                onChange={(_, data) => set({ height: clamp(data.value) })}
                className={styles.sizeInput}
              />
            </div>
          </Field>
        )}

        {options.format === 'vp9' && (
          <Field label={`${t('export.bitrate')} (${(options.bitrate / 1_000_000).toFixed(1)} Mbps)`} size="small">
            <Slider
              data-ssv="export-bitrate"
              min={1}
              max={20}
              step={0.5}
              value={options.bitrate / 1_000_000}
              disabled={running}
              onChange={(_, data) => set({ bitrate: Math.round(data.value * 1_000_000) })}
            />
          </Field>
        )}
      </div>

      <div className={styles.row}>
        {!running ? (
          <Button data-ssv="export-start" appearance="primary" disabled={!enabled} onClick={onStart}>
            {t('export.start')}
          </Button>
        ) : (
          <Button data-ssv="export-cancel" appearance="subtle" onClick={onCancel}>
            {t('export.cancel')}
          </Button>
        )}
        <Text size={200} className={styles.hint}>
          {t('export.alphaHint')}
        </Text>
      </div>

      {state.phase !== 'idle' && (
        <div className={styles.status}>
          <ProgressBar data-ssv="export-progress" value={state.phase === 'done' ? 1 : percent} />
          <Text size={200} data-ssv="export-phase">
            {phaseLabel(state, t)}
          </Text>
          {state.phase === 'failed' && (
            <Text size={200} role="alert">
              {t('export.failed', { detail: state.errorDetail })}
            </Text>
          )}
        </div>
      )}
    </div>
  );
}

function phaseLabel(state: ExportState, t: (key: string, values?: Record<string, string | number>) => string): string {
  switch (state.phase) {
    case 'preparing':
      return t('export.phase.preparing');
    case 'encoding':
      return t('export.phase.encoding', { percent: Math.round((state.encoded / Math.max(1, state.total)) * 100) });
    case 'packaging':
      return t('export.phase.packaging');
    case 'done':
      return t('export.phase.done');
    default:
      return t('export.phase.failed');
  }
}

function clamp(raw: string): number {
  const value = Math.floor(Number(raw));
  if (!Number.isFinite(value)) return MAX_CANVAS_SIZE;
  return Math.min(MAX_CANVAS_SIZE, Math.max(16, value));
}
