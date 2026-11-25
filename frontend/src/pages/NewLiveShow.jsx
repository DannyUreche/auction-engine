import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { api, formatPrice } from '../api.js'
import { Icon } from '../components/icons.jsx'
import ItemImage from '../components/ItemImage.jsx'
import { Alert, Button, Card, EmptyState, Field, Input, Loading } from '../components/ui.jsx'

// Organise a live show: pick 2-10 of your items (the order you pick them is the order on stage), the time per lot
// and when it starts; a preview shows each lot's slot
const SLOTS = [1, 2, 3, 5, 10]

export default function NewLiveShow() {
  const navigate = useNavigate()
  const [eligible, setEligible] = useState(null)
  const [picked, setPicked] = useState([])
  const [title, setTitle] = useState('')
  const [slot, setSlot] = useState(3)
  const [later, setLater] = useState(false)
  const [start, setStart] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api('/shows/eligible-items')
      .then(setEligible)
      .catch((err) => setError(err.message))
  }, [])

  const toggle = (id) => setPicked((current) => (current.includes(id) ? current.filter((x) => x !== id) : current.length < 10 ? [...current, id] : current))
  const startDate = later && start ? new Date(start) : new Date(Date.now() + 60000)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const result = await api('/shows', {
        method: 'POST',
        body: {
          title,
          slotMinutes: slot,
          itemIds: picked,
          startTime: later && start ? new Date(start).toISOString().slice(0, 19) : null,
        },
      })
      navigate(`/live/${result.show.id}`)
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  if (!eligible) return error ? <Alert>{error}</Alert> : <Loading />
  const byId = Object.fromEntries(eligible.map((item) => [item.id, item]))

  return (
    <form onSubmit={submit} className="mx-auto max-w-5xl">
      <h1 className="flex items-center gap-3 text-3xl font-bold tracking-tight text-slate-900">
        <Icon name="gavel" size={32} /> Organizeaza o licitatie live
      </h1>
      <p className="mt-1 text-slate-500">Obiectele tale intra pe scena pe rand. Urmaritorii tai primesc o notificare.</p>

      <div className="mt-8 grid gap-6 lg:grid-cols-[1fr_20rem]">
        <Card>
          <h2 className="font-semibold text-slate-900">1. Alege obiectele, in ordinea in care intra pe scena</h2>
          <p className="mt-1 text-sm text-slate-500">Doar licitatii clasice active, fara oferte. Intre 2 si 10.</p>
          {eligible.length === 0 ? (
            <div className="mt-4">
              <EmptyState title="Nu ai obiecte potrivite">
                <Link to="/sell" className="font-semibold text-indigo-600">
                  Pune ceva la vanzare
                </Link>{' '}
                si revino aici.
              </EmptyState>
            </div>
          ) : (
            <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {eligible.map((item) => {
                const order = picked.indexOf(item.id)
                return (
                  <button
                    type="button"
                    key={item.id}
                    onClick={() => toggle(item.id)}
                    className={`relative overflow-hidden rounded-2xl bg-white text-left ring-1 transition hover:-translate-y-0.5 ${
                      order >= 0 ? 'ring-2 ring-indigo-500 shadow-lg shadow-indigo-500/20' : 'ring-slate-200'
                    }`}
                  >
                    <ItemImage item={item} className="aspect-[4/3] w-full" />
                    {order >= 0 && (
                      <span className="absolute top-2 left-2 grid h-8 w-8 animate-fade-in place-items-center rounded-full bg-indigo-600 text-sm font-black text-white shadow-lg">
                        {order + 1}
                      </span>
                    )}
                    <span className="block p-2.5">
                      <span className="block truncate text-sm font-semibold text-slate-900">{item.name}</span>
                      <span className="text-xs text-slate-500">pornire {formatPrice(item.startPrice)}</span>
                    </span>
                  </button>
                )
              })}
            </div>
          )}
        </Card>

        <div className="space-y-6 lg:sticky lg:top-24 lg:self-start">
          <Card className="space-y-4">
            <h2 className="font-semibold text-slate-900">2. Detalii</h2>
            <Field label="Titlu">
              <Input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={100} placeholder="ex: Seara ceasurilor vintage" />
            </Field>
            <div>
              <span className="mb-1.5 block text-sm font-medium text-slate-700">Minute pentru fiecare obiect</span>
              <div className="flex gap-2">
                {SLOTS.map((value) => (
                  <button
                    type="button"
                    key={value}
                    onClick={() => setSlot(value)}
                    className={`flex-1 rounded-xl py-2 text-sm font-bold transition ${
                      slot === value ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/25' : 'bg-white text-slate-700 ring-1 ring-slate-300 hover:ring-indigo-400'
                    }`}
                  >
                    {value}
                  </button>
                ))}
              </div>
            </div>
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" checked={later} onChange={(e) => setLater(e.target.checked)} className="h-4 w-4 rounded accent-indigo-600" />
              Programeaza pentru mai tarziu
            </label>
            {later && <Input type="datetime-local" value={start} onChange={(e) => setStart(e.target.value)} required />}
          </Card>

          <Card>
            <h2 className="font-semibold text-slate-900">3. Programul</h2>
            {picked.length === 0 ? (
              <p className="mt-2 text-sm text-slate-500">Alege obiectele din stanga.</p>
            ) : (
              <ol className="mt-3 space-y-2">
                {picked.map((id, index) => {
                  const at = new Date(startDate.getTime() + index * slot * 60000)
                  return (
                    <li key={id} className="flex items-center gap-2 text-sm">
                      <span className="w-12 shrink-0 font-mono text-xs text-slate-500">{at.toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' })}</span>
                      <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-indigo-100 text-xs font-bold text-indigo-700">{index + 1}</span>
                      <span className="truncate text-slate-800">{byId[id]?.name}</span>
                    </li>
                  )
                })}
              </ol>
            )}
            <Alert>{error}</Alert>
            <Button type="submit" loading={busy} disabled={picked.length < 2} className="mt-4 w-full">
              Porneste licitatia live
            </Button>
          </Card>
        </div>
      </div>
    </form>
  )
}
