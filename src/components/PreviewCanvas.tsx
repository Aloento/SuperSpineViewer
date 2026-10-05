import { useEffect, useRef } from 'react';
import { makeStyles, tokens } from '@fluentui/react-components';

const useStyles = makeStyles({
  frame: {
    width: '100%',
    overflow: 'hidden',
    borderRadius: tokens.borderRadiusMedium,
    border: `1px solid ${tokens.colorNeutralStroke2}`,
  },
  canvas: {
    display: 'block',
    width: '100%',
    height: '100%',
  },
});

interface PreviewCanvasProps {
  frame: ImageBitmap | null;
  width: number;
  height: number;
}

export default function PreviewCanvas({ frame, width, height }: PreviewCanvasProps) {
  const styles = useStyles();
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !frame) return;
    const context = canvas.getContext('2d');
    if (!context) return;
    context.clearRect(0, 0, width, height);
    context.drawImage(frame, 0, 0);
  }, [frame, width, height]);

  return (
    <div className={`ssv-checkerboard ${styles.frame}`} style={{ aspectRatio: `${width} / ${height}` }}>
      <canvas ref={canvasRef} className={styles.canvas} width={width} height={height} />
    </div>
  );
}
