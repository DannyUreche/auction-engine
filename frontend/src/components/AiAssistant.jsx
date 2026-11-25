import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Icon } from './icons.jsx'
import { Button } from './ui.jsx'

// "Fill in with AI": sends the first chosen photo to the server, where Claude suggests the name, description,
// category and starting price. Shown only when the server has an API key (GET /api/ai/config).
export default function AiAssistant({ image, onSuggestion }) {
  const [enabled, setEnabled] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState({ ok: true, text: '' })

  useEffect(() => {
    api('/ai/config')
      .then((config) => setEnabled(config.enabled))
      .catch(() => setEnabled(false))
  }, [])

  if (!enabled) return null

  async function suggest() {
    setBusy(true)
    setMessage({ ok: true, text: '' })
    try {
      const body = new FormData()
      body.append('image', image)
      const suggestion = await api('/ai/listing-suggestion', { method: 'POST', body })
      onSuggestion(suggestion)
      setMessage({ ok: true, text: 'Gata! Am completat numele, descrierea, categoria si pretul. Verifica-le inainte sa publici.' })
    } catch (err) {
      setMessage({ ok: false, text: err.message })
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="rounded-2xl bg-gradient-to-r from-violet-50 to-fuchsia-50 p-4 ring-1 ring-violet-200">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="flex items-center gap-2 font-semibold text-slate-900">
            <Icon name="sparkles" size={20} /> Completeaza cu AI
          </p>
          <p className="mt-0.5 text-sm text-slate-600">
            {image ? 'Asistentul se uita la prima poza si propune anuntul.' : 'Alege intai o poza a obiectului.'}
          </p>
        </div>
        <Button type="button" onClick={suggest} loading={busy} disabled={!image} className="shrink-0">
          {busy ? 'Analizez poza...' : 'Completeaza din poza'}
        </Button>
      </div>
      {message.text && <p className={`mt-2 text-sm font-medium ${message.ok ? 'text-emerald-700' : 'text-red-600'}`}>{message.text}</p>}
    </div>
  )
}
