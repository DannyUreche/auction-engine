import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, formatPrice } from '../api.js'
import { Icon } from '../components/icons.jsx'
import ItemCard from '../components/ItemCard.jsx'
import { Alert, EmptyState, Loading } from '../components/ui.jsx'

// A personal list of auctions (/colectii/:id): public lists open for anyone with the link; the owner can
// rename it, switch it between public and private and take items out.
export default function Collection() {
  const { id } = useParams()
  const [list, setList] = useState(null)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(false)
  const [copied, setCopied] = useState(false)

  const load = useCallback(
    () =>
      api(`/collections/${id}`)
        .then(setList)
        .catch((err) => setError(err.message)),
    [id],
  )
  useEffect(() => {
    load()
  }, [load])

  if (error) return <Alert>{error}</Alert>
  if (!list) return <Loading />

  const url = `${window.location.origin}/colectii/${list.id}`

  async function share() {
    if (navigator.share) {
      try {
        await navigator.share({ title: list.title, text: `${list.title} - o colectie pe Licitatii.live`, url })
        return
      } catch {
        // cancelled: copy instead
      }
    }
    await navigator.clipboard?.writeText(url)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  async function save(changes) {
    try {
      await api(`/collections/${list.id}`, {
        method: 'PUT',
        body: { title: list.title, description: list.description, publicList: list.publicList, ...changes },
      })
      setEditing(false)
      await load()
    } catch (err) {
      window.alert(err.message)
    }
  }

  async function removeItem(itemId) {
    await api(`/collections/${list.id}/items/${itemId}`, { method: 'DELETE' })
    setList((current) => ({ ...current, items: current.items.filter((item) => item.id !== itemId) }))
  }

  return (
    <div>
      <section className="stay-dark relative overflow-hidden rounded-3xl bg-slate-950 px-6 py-8 text-white shadow-2xl ring-1 shadow-indigo-950/20 ring-white/10 sm:px-10">
        <div aria-hidden="true" className="pointer-events-none absolute inset-0">
          <div className="animate-float absolute -top-24 -right-10 h-72 w-72 rounded-full bg-amber-500/25 blur-3xl" />
          <div className="animate-float-slow absolute -bottom-28 -left-16 h-80 w-80 rounded-full bg-indigo-600/40 blur-3xl" />
        </div>
        <div className="relative flex flex-wrap items-end justify-between gap-6">
          <div className="min-w-0 flex-1">
            <p className="flex items-center gap-2 text-xs font-semibold tracking-[0.2em] text-amber-200/80 uppercase">
              <Icon name="bookmark" size={16} /> Colectie {list.publicList ? 'publica' : 'privata'}
            </p>
            {editing ? (
              <EditForm list={list} onSave={save} onCancel={() => setEditing(false)} />
            ) : (
              <>
                <h1 className="mt-2 text-3xl font-black tracking-tight break-words sm:text-4xl">{list.title}</h1>
                {list.description && <p className="mt-2 max-w-2xl text-indigo-100/80">{list.description}</p>}
                <p className="mt-3 text-sm text-white/60">
                  de{' '}
                  <Link to={`/users/${encodeURIComponent(list.owner)}`} className="font-semibold text-white hover:underline">
                    {list.owner}
                  </Link>
                </p>
              </>
            )}
          </div>
          <div className="flex flex-wrap gap-3">
            <Stat label="obiecte" value={list.items.length} />
            <Stat label="active acum" value={list.activeItems} />
            <Stat label="valoare totala" value={formatPrice(list.totalValue)} />
          </div>
        </div>
        <div className="relative mt-6 flex flex-wrap gap-2">
          {list.publicList && (
            <button
              type="button"
              onClick={share}
              className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2 text-sm font-semibold text-slate-900 shadow-lg transition hover:bg-amber-100"
            >
              <Icon name="share" size={16} mono /> {copied ? 'Link copiat!' : 'Distribuie colectia'}
            </button>
          )}
          {list.mine && !editing && (
            <>
              <button type="button" onClick={() => setEditing(true)} className="inline-flex items-center gap-2 rounded-xl bg-white/10 px-4 py-2 text-sm font-semibold ring-1 ring-white/20 transition hover:bg-white/20">
                <Icon name="pencil" size={16} mono /> Editeaza
              </button>
              <button
                type="button"
                onClick={() => save({ publicList: !list.publicList })}
                className="inline-flex items-center gap-2 rounded-xl bg-white/10 px-4 py-2 text-sm font-semibold ring-1 ring-white/20 transition hover:bg-white/20"
              >
                <Icon name={list.publicList ? 'shield' : 'globe'} size={16} mono /> {list.publicList ? 'Fa-o privata' : 'Fa-o publica'}
              </button>
            </>
          )}
        </div>
      </section>

      <div className="mt-8">
        {list.items.length === 0 ? (
          <EmptyState title="Colectia e goala">
            {list.mine ? 'Deschide un obiect si apasa „Salveaza” ca sa-l adaugi aici.' : 'Proprietarul nu a adaugat inca obiecte.'}
          </EmptyState>
        ) : (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {list.items.map((item, index) => (
              <div key={item.id} className="group/remove relative">
                <ItemCard item={item} index={index} />
                {list.mine && (
                  <button
                    type="button"
                    onClick={() => removeItem(item.id)}
                    className="absolute -top-2 -right-2 z-10 grid h-8 w-8 place-items-center rounded-full bg-rose-600 text-lg text-white shadow-lg transition sm:opacity-0 sm:group-hover/remove:opacity-100 hover:bg-rose-500 focus:opacity-100"
                    aria-label={`Scoate ${item.name} din colectie`}
                    title="Scoate din colectie"
                  >
                    ×
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function Stat({ label, value }) {
  return (
    <div className="rounded-2xl bg-white/[0.06] px-4 py-3 text-center ring-1 ring-white/10">
      <p className="text-xl font-black tabular-nums">{value}</p>
      <p className="text-[11px] text-white/60">{label}</p>
    </div>
  )
}

function EditForm({ list, onSave, onCancel }) {
  const [title, setTitle] = useState(list.title)
  const [description, setDescription] = useState(list.description ?? '')
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        onSave({ title, description })
      }}
      className="mt-3 max-w-xl space-y-2"
    >
      <input
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        maxLength={80}
        className="block w-full rounded-xl border-0 bg-white/10 px-3 py-2 text-xl font-bold text-white ring-1 ring-white/20 focus:ring-2 focus:ring-amber-300 focus:outline-none"
      />
      <textarea
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        maxLength={500}
        rows={2}
        placeholder="Descriere (optional)"
        className="block w-full rounded-xl border-0 bg-white/10 px-3 py-2 text-sm text-white ring-1 ring-white/20 placeholder:text-white/40 focus:ring-2 focus:ring-amber-300 focus:outline-none"
      />
      <div className="flex gap-2">
        <button type="submit" className="rounded-xl bg-amber-400 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-amber-300">
          Salveaza
        </button>
        <button type="button" onClick={onCancel} className="rounded-xl px-4 py-2 text-sm font-semibold text-white/70 hover:text-white">
          Renunta
        </button>
      </div>
    </form>
  )
}
