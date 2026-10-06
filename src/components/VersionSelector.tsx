import { useTranslation } from 'react-i18next';
import { Dropdown, Option, makeStyles } from '@fluentui/react-components';
import { RUNTIME_VERSIONS } from '../spine/runtimeMap';

export const AUTO_VERSION = 'auto';

const useStyles = makeStyles({
  root: {
    minWidth: '12rem',
  },
});

interface VersionSelectorProps {
  value: string | null;
  onChange: (version: string | null) => void;
  /** 导出进行中禁止换运行时，避免与导出会话的 pack 锁定打架 */
  disabled?: boolean;
}

/** 嗅探失败或嗅探错误时的兜底入口，选中的是运行时 pack 而非文件声明版本 */
export default function VersionSelector({ value, onChange, disabled }: VersionSelectorProps) {
  const styles = useStyles();
  const { t } = useTranslation();
  const current = value ?? AUTO_VERSION;

  return (
    <Dropdown
      className={styles.root}
      data-ssv="version-select"
      aria-label={t('version.label')}
      disabled={disabled}
      value={value === null ? t('version.auto') : value}
      selectedOptions={[current]}
      onOptionSelect={(_, data) => {
        if (typeof data.optionValue !== 'string') return;
        onChange(data.optionValue === AUTO_VERSION ? null : data.optionValue);
      }}
    >
      <Option key={AUTO_VERSION} value={AUTO_VERSION}>
        {t('version.auto')}
      </Option>
      {RUNTIME_VERSIONS.map((spec) => (
        <Option key={spec.packId} value={spec.packId}>
          {spec.packId}
        </Option>
      ))}
    </Dropdown>
  );
}
