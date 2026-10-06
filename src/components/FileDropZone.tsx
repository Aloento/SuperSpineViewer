import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Text, makeStyles, tokens } from '@fluentui/react-components';

const ACCEPT = '.skel,.json,.atlas,.png,.jpg,.jpeg,.webp';

const useStyles = makeStyles({
  zone: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    gap: tokens.spacingVerticalXS,
    padding: tokens.spacingVerticalXXL,
    border: `1px dashed ${tokens.colorNeutralStroke1}`,
    borderRadius: tokens.borderRadiusLarge,
    backgroundColor: tokens.colorNeutralBackground2,
    cursor: 'pointer',
    textAlign: 'center',
    flex: '1 1 20rem',
    minWidth: 0,
  },
  active: {
    border: `1px dashed ${tokens.colorBrandStroke1}`,
    backgroundColor: tokens.colorBrandBackground2,
  },
  busy: {
    cursor: 'progress',
    opacity: 0.6,
  },
  hint: {
    color: tokens.colorNeutralForeground3,
  },
  input: {
    display: 'none',
  },
});

interface FileDropZoneProps {
  onFiles: (files: File[]) => void;
  busy?: boolean;
}

async function collectEntry(entry: FileSystemEntry, files: File[]): Promise<void> {
  if (entry.isFile) {
    const file = await new Promise<File | null>((resolve) =>
      (entry as FileSystemFileEntry).file(resolve, () => resolve(null)),
    );
    if (file) files.push(file);
    return;
  }
  if (!entry.isDirectory) return;
  const reader = (entry as FileSystemDirectoryEntry).createReader();
  const readBatch = () => new Promise<FileSystemEntry[]>((resolve) => reader.readEntries(resolve, () => resolve([])));
  for (let batch = await readBatch(); batch.length > 0; batch = await readBatch()) {
    for (const child of batch) await collectEntry(child, files);
  }
}

async function collectFiles(dataTransfer: DataTransfer): Promise<File[]> {
  const entries = Array.from(dataTransfer.items)
    .filter((item) => item.kind === 'file')
    .map((item) => item.webkitGetAsEntry())
    .filter((entry): entry is FileSystemEntry => entry !== null);
  if (entries.length === 0) return Array.from(dataTransfer.files);

  const files: File[] = [];
  for (const entry of entries) await collectEntry(entry, files);
  return files.length > 0 ? files : Array.from(dataTransfer.files);
}

export default function FileDropZone({ onFiles, busy }: FileDropZoneProps) {
  const styles = useStyles();
  const { t } = useTranslation();
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  const open = () => {
    if (!busy) inputRef.current?.click();
  };

  return (
    <div
      className={`${styles.zone} ${dragging ? styles.active : ''} ${busy ? styles.busy : ''}`}
      role="button"
      tabIndex={busy ? -1 : 0}
      aria-disabled={busy}
      onClick={open}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          open();
        }
      }}
      onDragOver={(event) => {
        event.preventDefault();
        if (!busy) setDragging(true);
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={(event) => {
        event.preventDefault();
        setDragging(false);
        if (busy) return;
        void collectFiles(event.dataTransfer).then((files) => {
          if (files.length > 0) onFiles(files);
        });
      }}
    >
      <Text weight="semibold">{busy ? t('dropzone.busy') : t('dropzone.title')}</Text>
      <Text size={200} className={styles.hint}>
        {t('dropzone.hint')}
      </Text>
      <input
        ref={inputRef}
        className={styles.input}
        type="file"
        multiple
        accept={ACCEPT}
        onChange={(event) => {
          const files = Array.from(event.target.files ?? []);
          event.target.value = '';
          if (files.length > 0) onFiles(files);
        }}
      />
    </div>
  );
}
