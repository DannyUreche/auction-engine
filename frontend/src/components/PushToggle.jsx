import { useEffect, useState } from 'react'
import { disablePush, enablePush, isPushEnabled, pushSupported } from '../pwa.js'
import { Icon } from './icons.jsx'
import { useT } from '../i18n.jsx'

// Switch in the notifications panel: get notifications on this device even when the site is closed
export default function PushToggle() {
  const t = useT()
  const [enabled, setEnabled] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    isPushEnabled().then(setEnabled).catch(() => {})
  }, [])

  if (!pushSupported()) return null

  async function toggle() {
    setBusy(true)
    setError('')
    try {
      if (enabled) await disablePush()
      else await enablePush()
      setEnabled(!enabled)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="border-t border-slate-100 px-4 py-3">
      <button type="button" onClick={toggle} disabled={busy} className="flex w-full items-center gap-3 text-left disabled:opacity-60">
        <Icon name="bell" size={20} />
        <span className="min-w-0 flex-1 text-sm text-slate-700">
          {t('Notificari pe acest dispozitiv')}
          <span className="block text-xs text-slate-400">{t('si cand site-ul e inchis')}</span>
        </span>
        <span
          role="switch"
          aria-checked={enabled}
          className={`relative h-6 w-11 shrink-0 rounded-full transition ${enabled ? 'bg-indigo-600' : 'bg-slate-300'}`}
        >
          <span className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-all ${enabled ? 'left-[1.375rem]' : 'left-0.5'}`} />
        </span>
      </button>
      {error && <p className="mt-2 text-xs text-rose-600">{error}</p>}
    </div>
  )
}
