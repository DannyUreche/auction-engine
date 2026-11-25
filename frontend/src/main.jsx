import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { AuthProvider } from './auth.jsx'
import { LanguageProvider } from './i18n.jsx'
import App from './App.jsx'
import { WatchlistProvider } from './watchlist.jsx'
import './index.css'
import { registerServiceWorker } from './pwa.js'
import { installErrorReporting } from './errorReporting.js'
import ErrorBoundary from './components/ErrorBoundary.jsx'

registerServiceWorker()
installErrorReporting()

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ErrorBoundary>
    <LanguageProvider>
      <BrowserRouter>
        <AuthProvider>
          <WatchlistProvider>
            <App />
          </WatchlistProvider>
        </AuthProvider>
      </BrowserRouter>
    </LanguageProvider>
    </ErrorBoundary>
  </StrictMode>,
)
