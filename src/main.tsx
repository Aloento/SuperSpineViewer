import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { FluentProvider, webLightTheme } from '@fluentui/react-components';
import App from './App';
import './i18n';
import './styles/index.css';

const container = document.getElementById('root');
if (!container) throw new Error('#root not found');

// FluentProvider 的 className 会被 applyStylesToPortals 复制到 body 下的浮层挂载节点
// （position:absolute; top/left/right:0; z-index:1000000），加高度类会把它撑成整屏白幕，
// 展开任意下拉框就盖住全屏；整页高度由 App 的 minHeight 承担。
createRoot(container).render(
  <StrictMode>
    <FluentProvider theme={webLightTheme}>
      <App />
    </FluentProvider>
  </StrictMode>,
);
