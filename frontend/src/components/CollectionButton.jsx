import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { Link, useNavigate } from 'react-router'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Icon } from './icons.jsx'

// "Salveaza in colectie" on the item page: a dialog with the user's lists (tick = the item is in it) and a field
// to create a new list that already contains the item.
export default function CollectionButton({ item }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [inside, setInside] = useState([])

  useEffect(() => {
    if (!user) return
    api(`/collections/containing/${item.id}`)
      .then(setInside)
      .catch(() => {})
  }, [user, item.id])

  function click() {
    if (!user) navigate('/login', { state: { from: `/items/${item.id}` } })
    else setOpen(true)
  }

  return (
    <>
      <button type="button" onClick={click} className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-600 hover:text-indigo-600">
        <Icon name="bookmark" size={18} />
        {inside.length === 0 ? 'Salveaza' : inside.length === 1 ? 'Intr-o colectie' : `In ${inside.length} colectii`}
      </button>
      {open && createPortal(<CollectionDialog item={item} inside={inside} setInside={setInside} onClose={() => setOpen(false)} />, document.body)}
    </>
  )
}

function CollectionDialog({ item, inside, setInside, onClose }) {
  const [lists, setLists] = useState(null)
  const [title, setTitle] = useState('')
  const [busy, setBusy] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/collections/mine')
      .then(setLists)
      .catch((err) => setError(err.message))
    const onKey = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  async function toggle(list) {
    const has = inside.includes(list.id)
    setBusy(list.id)
    setError('')
    try {
      await api(`/collections/${list.id}/items/${item.id}`, { method: has ? 'DELETE' : 'PUT' })
      setInside(has ? inside.filter((id) => id !== list.id) : [...inside, list.id])
      setLists((current) => current.map((l) => (l.id === list.id ? { ...l, items: l.items + (has ? -1 : 1) } : l)))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(null)
    }
  }

  async function create(e) {
    e.preventDefault()
    if (!title.trim()) return
    setBusy('new')
    setError('')
    try {
      const list = await api('/collections', { method: 'POST', body: { title } })
      await api(`/collections/${list.id}/items/${item.id}`, { method: 'PUT' })
      setLists((current) => [{ ...list, items: 1 }, ...current])
      setInside([...inside, list.id])
      setTitle('')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/60 p-4 backdrop-blur-sm" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Salveaza in colectie"
        onClick={(e) => e.stopPropagation()}
        className="animate-fade-up w-full max-w-md overflow-hidden rounded-3xl bg-white shadow-2xl ring-1 ring-slate-200"
      >
        <div className="flex items-center gap-3 border-b border-slate-200 px-5 py-4">
          <Icon name="bookmark" size={26} />
          <div className="min-w-0 flex-1">
            <p className="font-bold text-slate-900">Salveaza in colectie</p>
            <p className="truncate text-xs text-slate-500">{item.name}</p>
          </div>
          <button type="button" onClick={onClose} className="grid h-8 w-8 place-items-center rounded-full text-xl text-slate-500 hover:bg-slate-100" aria-label="Inchide">
            ×
          </button>
        </div>

        <div className="max-h-[50vh] overflow-y-auto px-3 py-3">
          {!lists ? (
            <p className="px-2 py-6 text-center text-sm text-slate-500">Se incarca...</p>
          ) : lists.length === 0 ? (
            <p className="px-2 py-6 text-center text-sm text-slate-500">Nu ai inca nicio colectie. Creeaza prima mai jos.</p>
          ) : (
            <ul className="space-y-1">
              {lists.map((list) => {
                const has = inside.includes(list.id)
                return (
                  <li key={list.id}>
                    <button
                      type="button"
                      disabled={busy === list.id}
                      onClick={() => toggle(list)}
                      className={`flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-left transition ${has ? 'bg-indigo-50 ring-1 ring-indigo-200' : 'hover:bg-slate-50'}`}
                    >
                      <span
                        className={`grid h-6 w-6 shrink-0 place-items-center rounded-lg text-sm font-bold transition ${
                          has ? 'bg-indigo-600 text-white' : 'bg-white ring-1 ring-slate-300'
                        }`}
                      >
                        {has ? '✓' : ''}
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block truncate font-medium text-slate-900">{list.title}</span>
                        <span className="text-xs text-slate-500">
                          {list.items} {list.items === 1 ? 'obiect' : 'obiecte'} · {list.publicList ? 'publica' : 'privata'}
                        </span>
                      </span>
                    </button>
                  </li>
                )
              })}
            </ul>
          )}
        </div>

        <form onSubmit={create} className="flex gap-2 border-t border-slate-200 bg-slate-50 px-5 py-4">
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={80}
            placeholder="Colectie noua, ex: Cadouri de Craciun"
            className="block w-full rounded-xl border-0 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm ring-1 ring-slate-300 placeholder:text-slate-400 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
          />
          <button
            type="submit"
            disabled={!title.trim() || busy === 'new'}
            className="shrink-0 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-500 disabled:opacity-50"
          >
            Creeaza
          </button>
        </form>
        {error && <p className="bg-rose-50 px-5 py-2 text-sm text-rose-700">{error}</p>}
        <p className="px-5 pb-4 text-xs text-slate-500">
          Toate colectiile tale sunt in{' '}
          <Link to="/account?tab=collections" onClick={onClose} className="font-semibold text-indigo-600 hover:underline">
            Contul meu → Colectii
          </Link>
          .
        </p>
      </div>
    </div>
  )
}
