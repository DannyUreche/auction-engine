import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { api } from '../api.js'
import { useCategories } from '../categories.js'
import ItemCard from '../components/ItemCard.jsx'
import { Alert, EmptyState, Input, Loading, Select } from '../components/ui.jsx'
import { LiveBadge, useAuctionEvents } from '../events.jsx'

const SORTS = {
  ending: { label: 'Se termina curand', compare: (a, b) => a.endTime.localeCompare(b.endTime) },
  'price-asc': { label: 'Pret crescator', compare: (a, b) => Number(a.currentPrice) - Number(b.currentPrice) },
  'price-desc': { label: 'Pret descrescator', compare: (a, b) => Number(b.currentPrice) - Number(a.currentPrice) },
  newest: { label: 'Cele mai noi', compare: (a, b) => b.id - a.id },
}

export default function Home() {
  const [items, setItems] = useState(null)
  const [error, setError] = useState('')
  const categories = useCategories()

  // Filtrele stau in adresa paginii (ex: /?cat=ELECTRONICE&max=500), ca sa poata fi trimise ca link
  // Filtrele sunt tinute local (sursa de adevar) si copiate in adresa paginii (ex: /?cat=ELECTRONICE&max=500),
  // ca un link sa deschida aceleasi filtre. La deschiderea paginii se citesc din adresa.
  const [params, setParams] = useSearchParams()
  const [filters, setFilters] = useState(() => ({
    cat: params.get('cat') ?? '',
    q: params.get('q') ?? '',
    min: params.get('min') ?? '',
    max: params.get('max') ?? '',
    sort: SORTS[params.get('sort')] ? params.get('sort') : 'ending',
  }))
  const { cat: category, q: search, min: minPrice, max: maxPrice, sort } = filters

  useEffect(() => {
    const next = new URLSearchParams()
    for (const [key, value] of Object.entries(filters)) {
      if (value && !(key === 'sort' && value === 'ending')) next.set(key, value)
    }
    // replace: tastarea in cautare nu umple istoricul browserului
    setParams(next, { replace: true })
  }, [filters, setParams])

  const setFilter = (name, value) => setFilters((current) => ({ ...current, [name]: value }))
  const clearFilters = () => setFilters((current) => ({ cat: '', q: '', min: '', max: '', sort: current.sort }))

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

  const all = items ?? []
  const term = search.trim().toLowerCase()

  // Filtrele de cautare si pret (fara categorie), ca sa putem afisa cate obiecte are fiecare categorie
  const matchesOtherFilters = (item) =>
    (!term || item.name.toLowerCase().includes(term) || (item.description ?? '').toLowerCase().includes(term)) &&
    (!minPrice || Number(item.currentPrice) >= Number(minPrice)) &&
    (!maxPrice || Number(item.currentPrice) <= Number(maxPrice))

  const filteredWithoutCategory = all.filter(matchesOtherFilters)
  const visible = filteredWithoutCategory
    .filter((item) => !category || item.category === category)
    .sort(SORTS[sort].compare)

  const countFor = (code) => filteredWithoutCategory.filter((item) => item.category === code).length
  const hasFilters = Boolean(category || term || minPrice || maxPrice)

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

      {/* Categorii */}
      <div className="mt-8 -mx-4 flex gap-2 overflow-x-auto px-4 pb-1 sm:mx-0 sm:flex-wrap sm:px-0">
        <CategoryChip active={!category} onClick={() => setFilter('cat', '')}>
          Toate <span className="opacity-60">{filteredWithoutCategory.length}</span>
        </CategoryChip>
        {categories.map((c) => (
          <CategoryChip key={c.code} active={category === c.code} onClick={() => setFilter('cat', c.code)}>
            {c.icon} {c.label} <span className="opacity-60">{countFor(c.code)}</span>
          </CategoryChip>
        ))}
      </div>

      {/* Cautare, pret, sortare */}
      <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-[1fr_9rem_9rem_12rem]">
        <Input placeholder="Cauta un obiect..." value={search} onChange={(e) => setFilter('q', e.target.value)} />
        <Input
          type="number"
          min="0"
          placeholder="Pret minim"
          value={minPrice}
          onChange={(e) => setFilter('min', e.target.value)}
        />
        <Input
          type="number"
          min="0"
          placeholder="Pret maxim"
          value={maxPrice}
          onChange={(e) => setFilter('max', e.target.value)}
        />
        <Select value={sort} onChange={(e) => setFilter('sort', e.target.value)}>
          {Object.entries(SORTS).map(([key, s]) => (
            <option key={key} value={key}>
              {s.label}
            </option>
          ))}
        </Select>
      </div>

      {items && (
        <div className="mt-3 flex items-center justify-between text-sm text-slate-500">
          <span>
            {visible.length} {visible.length === 1 ? 'rezultat' : 'rezultate'}
          </span>
          {hasFilters && (
            <button
              onClick={clearFilters}
              className="font-semibold text-indigo-600 hover:text-indigo-500"
            >
              Sterge filtrele
            </button>
          )}
        </div>
      )}

      <div className="mt-4">
        <Alert>{error}</Alert>
        {items === null && !error && <Loading />}

        {items && visible.length === 0 && (
          <EmptyState title={hasFilters ? 'Niciun obiect nu se potriveste filtrelor' : 'Nu exista licitatii active'}>
            {!hasFilters && (
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

function CategoryChip({ active, onClick, children }) {
  return (
    <button
      onClick={onClick}
      className={`shrink-0 rounded-full px-3.5 py-1.5 text-sm font-medium whitespace-nowrap transition ${
        active
          ? 'bg-indigo-600 text-white shadow-sm'
          : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-50 hover:text-slate-900'
      }`}
    >
      {children}
    </button>
  )
}
