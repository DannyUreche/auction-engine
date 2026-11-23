import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api.js'
import ItemCard from '../components/ItemCard.jsx'
import { Alert, EmptyState, Input, Loading } from '../components/ui.jsx'
import { LiveBadge, useAuctionEvents } from '../events.jsx'

export default function Home() {
  const [items, setItems] = useState(null)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')

  const load = useCallback(
    () =>
      api('/items/active')
        .then((data) => {
          setItems(data)
          setError('')
        })
        .catch((err) => setError(err.message)),
    [],
  )

  // Oferta noua sau licitatie inchisa: reincarcam lista imediat (preturi si licitatii active)
  const live = useAuctionEvents(load)

  // Incarcare initiala + reimprospatare la 30 de secunde (pentru obiectele noi puse la vanzare)
  useEffect(() => {
    load()
    const id = setInterval(load, 30000)
    return () => clearInterval(id)
  }, [load])

  const term = search.trim().toLowerCase()
  const visible = (items ?? []).filter(
    (item) =>
      !term || item.name.toLowerCase().includes(term) || (item.description ?? '').toLowerCase().includes(term),
  )

  return (
    <div>
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-indigo-600 to-violet-700 px-8 py-10 text-white shadow-lg">
        <div className="absolute -top-20 -right-20 h-72 w-72 rounded-full bg-white/10 blur-2xl" />
        <div className="relative flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-3xl font-bold tracking-tight">Licitatii active</h1>
              <LiveBadge connected={live} />
            </div>
            <p className="mt-2 text-indigo-100">
              {items ? `${items.length} obiecte asteapta oferta ta.` : 'Se incarca licitatiile...'}
            </p>
          </div>
          <Link
            to="/sell"
            className="inline-flex items-center justify-center rounded-lg bg-white px-5 py-2.5 text-sm font-semibold text-indigo-700 shadow-sm transition hover:bg-indigo-50"
          >
            + Vinde un obiect
          </Link>
        </div>
      </section>

      <div className="mt-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-slate-500">Cele care se termina cel mai repede sunt primele.</p>
        <div className="sm:w-72">
          <Input placeholder="Cauta un obiect..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
      </div>

      <div className="mt-6">
        <Alert>{error}</Alert>
        {items === null && !error && <Loading />}

        {items && visible.length === 0 && (
          <EmptyState title={term ? 'Niciun obiect nu se potriveste cautarii' : 'Nu exista licitatii active'}>
            {!term && (
              <Link to="/sell" className="font-semibold text-indigo-600 hover:text-indigo-500">
                Pune primul obiect la licitatie
              </Link>
            )}
          </EmptyState>
        )}

        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {visible.map((item) => (
            <ItemCard key={item.id} item={item} />
          ))}
        </div>
      </div>
    </div>
  )
}
