import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { AuthProvider } from './auth.jsx'
import { LanguageProvider } from './i18n.jsx'
import App from './App.jsx'
import { WatchlistProvider } from './watchlist.jsx'
import './index.css'
import { registerServiceWorker } from './pwa.js'

registerServiceWorker()

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <LanguageProvider>
      <BrowserRouter>
        <AuthProvider>
          <WatchlistProvider>
            <App />
          </WatchlistProvider>
        </AuthProvider>
      </BrowserRouter>
    </LanguageProvider>
  </StrictMode>,
)
