import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { FluentProvider } from '@fluentui/react-components';
import App from './App';
import './i18n';
import './styles/index.css';

const container = document.getElementById('root');
if (!container) throw new Error('#root not found');

createRoot(container).render(
  <StrictMode>
    <FluentProvider className="h-full bg-transparent">
      <App />
    </FluentProvider>
  </StrictMode>,
);
