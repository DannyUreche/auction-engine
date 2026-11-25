import { useEffect, useState } from 'react'
import { api } from '../api.js'
import CollectionCard from './CollectionCard.jsx'
import { Icon } from './icons.jsx'
import { Alert, Loading } from './ui.jsx'

// Account tab "Colectii": the user's lists, a form for a new one and deleting
export default function CollectionsPanel() {
  const [lists, setLists] = useState(null)
  const [form, setForm] = useState({ title: '', description: '', publicList: true })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = () =>
    api('/collections/mine')
      .then(setLists)
      .catch((err) => setError(err.message))
  useEffect(() => {
    load()
  }, [])

  async function create(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api('/collections', { method: 'POST', body: form })
      setForm({ title: '', description: '', publicList: true })
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function remove(list) {
    if (!window.confirm(`Stergi colectia "${list.title}"? Obiectele raman la licitatie.`)) return
    try {
      await api(`/collections/${list.id}`, { method: 'DELETE' })
      setLists((current) => current.filter((l) => l.id !== list.id))
    } catch (err) {
      setError(err.message)
    }
  }

  if (!lists) return error ? <Alert>{error}</Alert> : <Loading />

  return (
    <div className="space-y-6">
      <form onSubmit={create} className="rounded-2xl bg-white/85 p-5 shadow-sm ring-1 ring-slate-200/80">
        <p className="flex items-center gap-2 font-semibold text-slate-900">
          <Icon name="bookmark" size={22} /> Colectie noua
        </p>
        <p className="mt-1 text-sm text-slate-500">
          Grupeaza licitatii in liste (ex: „Cadouri de Craciun”, „Ceasuri vintage”). O colectie publica se poate trimite cu un link si apare pe profilul tau.
        </p>
        <div className="mt-4 grid gap-3 sm:grid-cols-[1fr_1.4fr_auto]">
          <input
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            maxLength={80}
            placeholder="Numele colectiei"
            className="rounded-xl border-0 bg-white px-3 py-2.5 text-sm shadow-sm ring-1 ring-slate-300 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
          />
          <input
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            maxLength={500}
            placeholder="Descriere (optional)"
            className="rounded-xl border-0 bg-white px-3 py-2.5 text-sm shadow-sm ring-1 ring-slate-300 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
          />
          <button
            type="submit"
            disabled={busy || !form.title.trim()}
            className="rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-500 disabled:opacity-50"
          >
            Creeaza
          </button>
        </div>
        <label className="mt-3 inline-flex cursor-pointer items-center gap-2 text-sm text-slate-600">
          <input type="checkbox" checked={form.publicList} onChange={(e) => setForm({ ...form, publicList: e.target.checked })} className="h-4 w-4 accent-indigo-600" />
          Publica (oricine are linkul o poate vedea)
        </label>
        {error && <p className="mt-3 text-sm text-rose-600">{error}</p>}
      </form>

      {lists.length === 0 ? (
        <p className="text-center text-sm text-slate-500">Nu ai inca nicio colectie. Pe pagina unui obiect apasa „Salveaza”.</p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {lists.map((list, index) => (
            <CollectionCard key={list.id} list={list} index={index}>
              <button
                type="button"
                onClick={() => remove(list)}
                className="absolute top-2 right-2 grid h-8 w-8 place-items-center rounded-full bg-black/50 text-lg text-white backdrop-blur transition sm:opacity-0 sm:group-hover:opacity-100 hover:bg-rose-600 focus:opacity-100"
                aria-label={`Sterge ${list.title}`}
              >
                ×
              </button>
            </CollectionCard>
          ))}
        </div>
      )}
    </div>
  )
}
