import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { api, formatPrice } from '../api.js'
import { useCategories } from '../categories.js'
import ItemCard from '../components/ItemCard.jsx'
import ActivityFeed from '../components/ActivityFeed.jsx'
import CountUp from '../components/CountUp.jsx'
import { Alert, CardSkeleton, EmptyState, Input, Select } from '../components/ui.jsx'
import { useAuctionEvents } from '../events.jsx'

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

  // Cifrele din partea de sus (licitatii active, oferte, vanzari)
  const [stats, setStats] = useState(null)
  const loadStats = useCallback(
    () =>
      api('/stats', { auth: false })
        .then(setStats)
        .catch(() => {}),
    [],
  )
  useEffect(() => {
    loadStats()
  }, [loadStats])

  // Oferta noua sau licitatie inchisa: reincarcam imediat lista (preturi, licitatii active) si cifrele
  useAuctionEvents(() => {
    load()
    loadStats()
  })

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
      <section className="stay-dark relative overflow-hidden rounded-3xl bg-slate-950 ring-1 ring-white/10 px-6 py-10 text-white shadow-2xl shadow-indigo-950/20 sm:px-10 sm:py-12">
        {/* Fundal animat: pete de culoare care plutesc + grila fina */}
        <div aria-hidden="true" className="pointer-events-none absolute inset-0">
          <div className="absolute -top-32 -left-20 h-96 w-96 animate-float rounded-full bg-indigo-600/40 blur-3xl" />
          <div className="absolute -right-24 -bottom-40 h-[28rem] w-[28rem] animate-float-slow rounded-full bg-fuchsia-600/30 blur-3xl" />
          <div className="absolute top-10 left-1/2 h-72 w-72 animate-float rounded-full bg-sky-500/20 blur-3xl [animation-delay:-6s]" />
          <div className="absolute inset-0 bg-[linear-gradient(to_right,rgb(255_255_255/0.04)_1px,transparent_1px),linear-gradient(to_bottom,rgb(255_255_255/0.04)_1px,transparent_1px)] bg-[size:40px_40px]" />
        </div>

        <div className="relative grid gap-10 lg:grid-cols-[1.35fr_1fr] lg:items-center">
          <div className="min-w-0 animate-fade-up">
            <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-medium text-indigo-100 ring-1 ring-white/15 backdrop-blur">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
              </span>
              {items ? `${items.length} licitatii active chiar acum` : 'Se conecteaza la licitatii...'}
            </span>
            <h1 className="mt-5 text-4xl leading-tight font-bold tracking-tight sm:text-5xl">
              Liciteaza <span className="text-gradient">in timp real</span>.
              <br className="hidden sm:block" /> Castiga ce iti place.
            </h1>
            <p className="mt-4 max-w-xl text-indigo-100/80">
              Ofertele apar instant pe ecranul tuturor. Cronometru live, prelungire automata in ultimele minute si
              optiunea „Cumpara acum” pentru cei grabiti.
            </p>
            <div className="mt-7 flex flex-wrap gap-3">
              <Link
                to="/sell"
                className="inline-flex items-center gap-2 rounded-xl bg-white px-5 py-3 text-sm font-semibold text-indigo-700 shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl"
              >
                + Vinde un obiect
              </Link>
              <a
                href="#licitatii"
                className="inline-flex items-center gap-2 rounded-xl px-5 py-3 text-sm font-semibold text-white ring-1 ring-white/25 transition hover:bg-white/10"
              >
                Exploreaza licitatiile ↓
              </a>
            </div>

            <dl className="mt-10 grid grid-cols-2 gap-3 sm:grid-cols-4">
              <HeroStat label="Licitatii active" value={stats?.activeItems} />
              <HeroStat label="Oferte plasate" value={stats?.bids} />
              <HeroStat label="Obiecte vandute" value={stats?.soldItems} />
              <HeroStat label="Valoare vanduta" value={stats?.soldValue} format={(n) => formatPrice(Math.round(n))} />
            </dl>
          </div>

          <div className="min-w-0 animate-fade-up [animation-delay:150ms]">
            <ActivityFeed />
          </div>
        </div>
      </section>

      {/* Categorii */}
      <div id="licitatii" className="mt-8 -mx-4 flex scroll-mt-24 gap-2 overflow-x-auto px-4 pb-1 sm:mx-0 sm:flex-wrap sm:px-0">
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
        {items === null && !error && (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {[0, 1, 2, 3, 4, 5].map((i) => (
              <CardSkeleton key={i} />
            ))}
          </div>
        )}

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
          {visible.map((item, index) => (
            <ItemCard key={item.id} item={item} index={index} />
          ))}
        </div>
      </div>
    </div>
  )
}

// O cifra din partea de sus a paginii; numarul "urca" animat pana la valoarea reala
function HeroStat({ label, value, format }) {
  return (
    <div className="rounded-2xl bg-white/[0.06] px-4 py-3 ring-1 ring-white/10 backdrop-blur">
      <dd className="text-xl font-bold tracking-tight sm:text-2xl">
        {value == null ? <span className="text-white/40">—</span> : <CountUp value={value} format={format} />}
      </dd>
      <dt className="mt-0.5 text-xs text-indigo-200/70">{label}</dt>
    </div>
  )
}

function CategoryChip({ active, onClick, children }) {
  return (
    <button
      onClick={onClick}
      className={`shrink-0 rounded-full px-3.5 py-1.5 text-sm font-medium whitespace-nowrap transition ${
        active
          ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-500/25'
          : 'bg-white/80 text-slate-600 ring-1 ring-slate-200 backdrop-blur hover:bg-white hover:text-slate-900'
      }`}
    >
      {children}
    </button>
  )
}
