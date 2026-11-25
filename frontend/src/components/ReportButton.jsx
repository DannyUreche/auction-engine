import { createPortal } from 'react-dom'
import { useState } from 'react'
import { api } from '../api.js'
import { Icon } from './icons.jsx'
import { Alert, Button, Select, Textarea } from './ui.jsx'

const REASONS = {
  FRAUD: 'Posibila inselatorie',
  PROHIBITED: 'Obiect interzis',
  WRONG_CATEGORY: 'Categorie gresita',
  OFFENSIVE: 'Continut ofensator',
  OTHER: 'Altceva',
}

// "Raporteaza": sends the item to the admins' moderation list
export default function ReportButton({ item }) {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('FRAUD')
  const [details, setDetails] = useState('')
  const [result, setResult] = useState('')
  const [error, setError] = useState('')

  async function send(event) {
    event.preventDefault()
    setError('')
    try {
      const data = await api(`/items/${item.id}/report`, { method: 'POST', body: { reason, details } })
      setResult(data.mesaj)
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <button type="button" onClick={() => setOpen(true)} className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-500 hover:text-red-600">
        <Icon name="flag" size={18} /> Raporteaza
      </button>
      {/* Portal: the card around the button has backdrop-blur, which would trap a fixed overlay */}
      {open &&
        createPortal(
        <div className="fixed inset-0 z-50 grid animate-fade-in place-items-center bg-black/60 p-4 backdrop-blur-sm" onClick={() => setOpen(false)}>
          <form onSubmit={send} className="w-full max-w-sm animate-fade-up space-y-3 rounded-2xl bg-white p-6 shadow-2xl" onClick={(e) => e.stopPropagation()}>
            <p className="font-semibold text-slate-900">Raporteaza obiectul</p>
            {result ? (
              <>
                <Alert tone="success">{result}</Alert>
                <Button type="button" variant="secondary" onClick={() => setOpen(false)} className="w-full">
                  Inchide
                </Button>
              </>
            ) : (
              <>
                <Select value={reason} onChange={(e) => setReason(e.target.value)}>
                  {Object.entries(REASONS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </Select>
                <Textarea value={details} onChange={(e) => setDetails(e.target.value)} maxLength={500} placeholder="Detalii (optional)" />
                <Alert>{error}</Alert>
                <div className="flex justify-end gap-2">
                  <Button type="button" variant="secondary" onClick={() => setOpen(false)}>
                    Renunta
                  </Button>
                  <Button type="submit">Trimite</Button>
                </div>
              </>
            )}
          </form>
        </div>
,
          document.body,
        )}
    </>
  )
}
