import { useEffect, useRef, useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Alert } from './ui.jsx'
import { useLanguage } from '../i18n.jsx'

// "Continue with Google" (Google Identity Services). Shown only when the server has a Google client id.
export default function GoogleButton() {
  const { loginWithGoogle } = useAuth()
  const { lang, t } = useLanguage()
  const buttonRef = useRef(null)
  const [clientId, setClientId] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/auth/google/config', { auth: false })
      .then((config) => setClientId(config.clientId))
      .catch(() => {})
  }, [])

  useEffect(() => {
    if (!clientId) return
    const render = () => {
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: async ({ credential }) => {
          try {
            await loginWithGoogle(credential)
          } catch (err) {
            setError(err.message)
          }
        },
      })
      window.google.accounts.id.renderButton(buttonRef.current, { theme: 'outline', size: 'large', text: 'continue_with', locale: lang, width: 320 })
    }
    if (window.google?.accounts?.id) {
      render()
      return
    }
    const script = document.createElement('script')
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true
    script.onload = render
    document.head.appendChild(script)
  }, [clientId, loginWithGoogle, lang])

  if (!clientId) return null
  return (
    <div className="mt-6 space-y-3">
      <div className="flex items-center gap-3 text-xs text-slate-400">
        <span className="h-px flex-1 bg-slate-200" />
        {t('sau')}
        <span className="h-px flex-1 bg-slate-200" />
      </div>
      <div ref={buttonRef} className="flex justify-center" />
      <Alert>{error}</Alert>
    </div>
  )
}
