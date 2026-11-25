import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { api } from '../api.js'
import { useCategories } from '../categories.js'
import ItemCard from '../components/ItemCard.jsx'
import CountUp from '../components/CountUp.jsx'
import Recommendations from '../components/Recommendations.jsx'
import { CategoryIcon, Icon } from '../components/icons.jsx'
import { Alert, Button, CardSkeleton, EmptyState, Input, Select } from '../components/ui.jsx'
import { useAuctionEvents } from '../events.jsx'
import Hero3D from '../hero/Hero3D.jsx'
import { useT } from '../i18n.jsx'
import { useAuth } from '../auth.jsx'
import { COUNTIES } from '../counties.js'

const SORTS = {
  ending: { label: 'Se termina curand' },
  'price-asc': { label: 'Pret crescator' },
  'price-desc': { label: 'Pret descrescator' },
  newest: { label: 'Cele mai noi' },
}

// Auction type filter: [key, label, icon]
const TYPES = [
  ['', 'Toate tipurile', null],
  ['buy', 'Cu Cumpara acum', 'bolt'],
  ['dutch', 'Olandeze', 'trendDown'],
  ['today', 'Se termina azi', 'hourglass'],
  ['soon', 'Incep curand', 'timer'],
]

const PAGE_SIZE = 12

// /items/search URL for the filters and one page of results (the server does the filtering, see ItemSearchService)
function searchUrl(filters, page, size = PAGE_SIZE) {
  const params = new URLSearchParams({ page, size })
  const { cat, q, min, max, county, type, sort } = filters
  if (cat) params.set('cat', cat)
  if (county) params.set('county', county)
  if (q.trim()) params.set('q', q.trim())
  if (min) params.set('min', min)
  if (max) params.set('max', max)
  if (type) params.set('type', type)
  if (sort) params.set('sort', sort)
  return `/items/search?${params}`
}

export default function Home() {
  // Results from the server: { items, total, categoryCounts, allCategories, ... }; pages = how many are loaded
  const [result, setResult] = useState(null)
  const [pages, setPages] = useState(1)
  const [loadingMore, setLoadingMore] = useState(false)
  const [heroItems, setHeroItems] = useState(null)
  // All 42 counties with their number of auctions (also the ones with 0)
  const [counties, setCounties] = useState(() => COUNTIES.map((name) => ({ county: name, items: 0 })))
  useEffect(() => {
    api('/items/counties', { auth: false })
      .then(setCounties)
      .catch(() => {})
  }, [])
  const [error, setError] = useState('')
  const categories = useCategories()
  const t = useT()
  const { user } = useAuth()

  // Filtrele stau in adresa paginii (ex: /?cat=ELECTRONICE&max=500), ca sa poata fi trimise ca link
  // Filtrele sunt tinute local (sursa de adevar) si copiate in adresa paginii (ex: /?cat=ELECTRONICE&max=500),
  // ca un link sa deschida aceleasi filtre. La deschiderea paginii se citesc din adresa.
  const [params, setParams] = useSearchParams()
  const [filters, setFilters] = useState(() => ({
    cat: params.get('cat') ?? '',
    q: params.get('q') ?? '',
    min: params.get('min') ?? '',
    max: params.get('max') ?? '',
    county: params.get('county') ?? '',
    type: TYPES.some(([key]) => key === params.get('type')) ? params.get('type') : '',
    sort: SORTS[params.get('sort')] ? params.get('sort') : 'ending',
  }))
  const { cat: category, q: search, min: minPrice, max: maxPrice, county, type, sort } = filters

  useEffect(() => {
    const next = new URLSearchParams()
    for (const [key, value] of Object.entries(filters)) {
      if (value && !(key === 'sort' && value === 'ending')) next.set(key, value)
    }
    // replace: tastarea in cautare nu umple istoricul browserului
    setParams(next, { replace: true })
  }, [filters, setParams])

  const setFilter = (name, value) => setFilters((current) => ({ ...current, [name]: value }))
  const clearFilters = () => setFilters((current) => ({ cat: '', q: '', min: '', max: '', county: '', type: '', sort: current.sort }))

  // Typing in the search or price fields waits a moment before asking the server
  const [query, setQuery] = useState(filters)
  useEffect(() => {
    const id = setTimeout(() => setQuery(filters), filters.q === query.q && filters.min === query.min && filters.max === query.max ? 0 : 300)
    return () => clearTimeout(id)
  }, [filters]) // eslint-disable-line react-hooks/exhaustive-deps

  // Loads the first `count` pages again (new filters, a live bid, the 30 s refresh)
  const pagesRef = useRef(pages)
  pagesRef.current = pages
  const requestId = useRef(0)
  const load = useCallback(
    async (count = pagesRef.current) => {
      const id = ++requestId.current
      try {
        const responses = await Promise.all(Array.from({ length: count }, (_, page) => api(searchUrl(query, page))))
        if (id !== requestId.current) return // a newer request already answered
        setResult({ ...responses[0], items: responses.flatMap((response) => response.items) })
        setError('')
      } catch (err) {
        if (id === requestId.current) setError(err.message)
      }
    },
    [query],
  )
  const loadHero = useCallback(
    () =>
      api('/items/search?size=10&sort=ending')
        .then((data) => setHeroItems(data.items))
        .catch(() => {}),
    [],
  )

  // New filters: back to the first page
  useEffect(() => {
    setPages(1)
    load(1)
  }, [load])

  async function loadMore() {
    setLoadingMore(true)
    try {
      const next = await api(searchUrl(query, pages))
      setResult((current) => ({ ...next, items: [...(current?.items ?? []), ...next.items] }))
      setPages(pages + 1)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoadingMore(false)
    }
  }

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
    loadHero()
    loadStats()
  })

  // Incarcare initiala + reimprospatare la 30 de secunde (pentru obiectele noi puse la vanzare)
  useEffect(() => {
    loadHero()
    const id = setInterval(() => {
      load()
      loadHero()
    }, 30000)
    return () => clearInterval(id)
  }, [load, loadHero])

  const items = result?.items ?? null
  const visible = items ?? []
  const total = result?.total ?? 0
  const countFor = (code) => result?.categoryCounts?.[code] ?? 0
  const term = search.trim()
  const hasFilters = Boolean(category || term || minPrice || maxPrice || county || type)

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

        <div
          className="relative grid gap-10 lg:grid-cols-[1.05fr_1fr] lg:items-center"
        >
          <div className="min-w-0 animate-fade-up">
            <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-medium text-indigo-100 ring-1 ring-white/15 backdrop-blur">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
              </span>
              {stats ? t('{n} licitatii active chiar acum', { n: stats.activeItems }) : t('Se conecteaza la licitatii...')}
            </span>
            <h1 className="mt-5 text-4xl leading-tight font-bold tracking-tight sm:text-5xl">
              {t('Liciteaza')} <span className="text-gradient">{t('in timp real')}</span>.
              <br className="hidden sm:block" /> {t('Castiga ce iti place.')}
            </h1>
            <p className="mt-4 max-w-xl text-indigo-100/80">
              {t('Ofertele apar instant pe ecranul tuturor. Cronometru live, prelungire automata in ultimele minute si optiunea „Cumpara acum” pentru cei grabiti.')}
            </p>
            <div className="mt-7 flex flex-wrap gap-3">
              <Link
                to="/sell"
                className="inline-flex items-center gap-2 rounded-xl bg-white px-5 py-3 text-sm font-semibold text-indigo-700 shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl"
              >
                + {t('Vinde un obiect')}
              </Link>
              <a
                href="#licitatii"
                className="inline-flex items-center gap-2 rounded-xl px-5 py-3 text-sm font-semibold text-white ring-1 ring-white/25 transition hover:bg-white/10"
              >
                {t('Exploreaza licitatiile')} ↓
              </a>
            </div>

            <dl className="mt-10 grid grid-cols-2 gap-3 sm:grid-cols-[1fr_1fr_1fr_1.5fr]">
              <HeroStat label={t('Licitatii active')} value={stats?.activeItems} />
              <HeroStat label={t('Oferte plasate')} value={stats?.bids} />
              <HeroStat label={t('Obiecte vandute')} value={stats?.soldItems} />
              <HeroStat label={t('Valoare vanduta')} value={stats?.soldValue} format={formatMoneyShort} />
            </dl>
          </div>

          <div className="min-w-0 animate-fade-up [animation-delay:150ms]">
            <Hero3D items={heroItems} />
          </div>
        </div>
      </section>

      <Recommendations />

      {/* Filters: one highlighted panel (search, categories, auction type, price, sorting) */}
      <section
        id="licitatii"
        className="relative mt-10 scroll-mt-24 overflow-hidden rounded-3xl bg-white/90 p-4 shadow-xl ring-1 shadow-indigo-500/10 ring-indigo-200/70 backdrop-blur sm:p-6"
      >
        <div className="pointer-events-none absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-indigo-500 via-violet-500 to-fuchsia-500" />
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="flex items-center gap-2 text-lg font-bold text-slate-900 sm:text-xl">
            <Icon name="filter" size={24} /> {t('Gaseste ce cauti')}
          </h2>
          {items && (
            <div className="flex items-center gap-3 text-sm">
              <span className="rounded-full bg-indigo-50 px-3 py-1 font-semibold text-indigo-700 ring-1 ring-indigo-200">
                {total} {t(total === 1 ? 'rezultat' : 'rezultate')}
              </span>
              {hasFilters && (
                <button onClick={clearFilters} className="font-semibold text-indigo-600 hover:text-indigo-500">
                  {t('Sterge filtrele')}
                </button>
              )}
            </div>
          )}
        </div>

        {/* Search */}
        <label className="relative mt-4 block">
          <span className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2">
            <Icon name="search" size={22} />
          </span>
          <input
            type="search"
            value={search}
            onChange={(e) => setFilter('q', e.target.value)}
            placeholder={t('Cauta un obiect...')}
            className="block w-full rounded-2xl border-0 bg-white py-3.5 pr-4 pl-12 text-base text-slate-900 shadow-inner ring-2 ring-indigo-100 transition placeholder:text-slate-400 focus:ring-indigo-500 focus:outline-none"
          />
        </label>

        {/* Categories */}
        <p className="mt-5 text-xs font-semibold tracking-wide text-slate-500 uppercase">{t('Categorii')}</p>
        <div className="-mx-4 mt-2 flex gap-2 overflow-x-auto px-4 pb-1 sm:mx-0 sm:flex-wrap sm:px-0">
          <CategoryChip active={!category} onClick={() => setFilter('cat', '')}>
            {t('Toate')} <span className="opacity-70">{result?.allCategories ?? 0}</span>
          </CategoryChip>
          {categories.map((c) => (
            <CategoryChip key={c.code} active={category === c.code} onClick={() => setFilter('cat', c.code)}>
              <CategoryIcon category={c} size={18} /> {t(c.label)} <span className="opacity-70">{countFor(c.code)}</span>
            </CategoryChip>
          ))}
        </div>

        {/* Auction type, price, sorting */}
        <div className="mt-5 grid gap-4">
          <div>
            <p className="text-xs font-semibold tracking-wide text-slate-500 uppercase">{t('Tip de licitatie')}</p>
            <div className="mt-2 flex flex-wrap gap-2">
              {TYPES.map(([key, label, icon]) => (
                <button
                  key={key}
                  onClick={() => setFilter('type', key)}
                  className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-sm font-medium transition ${
                    type === key
                      ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/25'
                      : 'bg-white text-slate-700 shadow-sm ring-1 ring-slate-300 hover:text-indigo-700 hover:ring-indigo-400'
                  }`}
                >
                  {icon && <Icon name={icon} size={16} mono={type === key} />} {t(label)}
                </button>
              ))}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-[12rem_9rem_9rem_14rem]">
            <div className="col-span-2 sm:col-span-1">
              <Select value={county} onChange={(e) => setFilter('county', e.target.value)} aria-label={t('Judet')}>
                <option value="">{t('Toate judetele')}</option>
                {counties.map((c) => (
                  <option key={c.county} value={c.county}>
                    {c.county} ({c.items})
                  </option>
                ))}
              </Select>
            </div>
            <Input type="number" min="0" placeholder={t('Pret minim')} value={minPrice} onChange={(e) => setFilter('min', e.target.value)} />
            <Input type="number" min="0" placeholder={t('Pret maxim')} value={maxPrice} onChange={(e) => setFilter('max', e.target.value)} />
            <div className="col-span-2 sm:col-span-1">
              <Select value={sort} onChange={(e) => setFilter('sort', e.target.value)} aria-label={t('Sortare')}>
                {Object.entries(SORTS).map(([key, s]) => (
                  <option key={key} value={key}>
                    {t(s.label)}
                  </option>
                ))}
              </Select>
            </div>
          </div>
        </div>

        {/* Active filters as removable pills */}
        {hasFilters && (
          <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-slate-100 pt-4">
            {category && (
              <FilterPill onRemove={() => setFilter('cat', '')}>{t(categories.find((c) => c.code === category)?.label ?? category)}</FilterPill>
            )}
            {term && <FilterPill onRemove={() => setFilter('q', '')}>„{search.trim()}”</FilterPill>}
            {type && <FilterPill onRemove={() => setFilter('type', '')}>{t(TYPES.find(([key]) => key === type)[1])}</FilterPill>}
            {minPrice && <FilterPill onRemove={() => setFilter('min', '')}>{t('de la')} {minPrice} RON</FilterPill>}
            {maxPrice && <FilterPill onRemove={() => setFilter('max', '')}>{t('pana la')} {maxPrice} RON</FilterPill>}
            {county && <FilterPill onRemove={() => setFilter('county', '')}>{t('jud.')} {county}</FilterPill>}
            {user && (term || category || minPrice || maxPrice) && (
              <SaveAlertButton
                key={`${term}|${category}|${minPrice}|${maxPrice}`}
                filters={{ query: term, category: category || null, minPrice: minPrice || null, maxPrice: maxPrice || null }}
              />
            )}
          </div>
        )}
      </section>

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
          <EmptyState title={t(hasFilters ? 'Niciun obiect nu se potriveste filtrelor' : 'Nu exista licitatii active')}>
            {!hasFilters && (
              <Link to="/sell" className="font-semibold text-indigo-600 hover:text-indigo-500">
                {t('Pune primul obiect la licitatie')}
              </Link>
            )}
          </EmptyState>
        )}

        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {visible.map((item, index) => (
            <ItemCard key={item.id} item={item} index={index % PAGE_SIZE} />
          ))}
        </div>

        {items && visible.length < total && (
          <div className="mt-8 flex flex-col items-center gap-2">
            <Button variant="secondary" loading={loadingMore} onClick={loadMore}>
              {t('Incarca mai multe')}
            </Button>
            <p className="text-xs text-slate-500">{t('{shown} din {total}', { shown: visible.length, total })}</p>
          </div>
        )}
      </div>
    </div>
  )
}

// Whole lei, and a short form for big sums, so the value always fits its tile
const compactMoney = new Intl.NumberFormat('ro-RO', { notation: 'compact', maximumFractionDigits: 1 })
const formatMoneyShort = (n) => (n < 100000 ? `${Math.round(n).toLocaleString('ro-RO')} RON` : `${compactMoney.format(n)} RON`)

// O cifra din partea de sus a paginii; numarul "urca" animat pana la valoarea reala
function HeroStat({ label, value, format }) {
  return (
    <div className="rounded-2xl bg-white/[0.06] px-4 py-3 ring-1 ring-white/10 backdrop-blur">
      <dd className="text-lg font-bold tracking-tight whitespace-nowrap sm:text-xl">
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
      aria-pressed={active}
      className={`inline-flex shrink-0 items-center gap-1.5 rounded-xl px-4 py-2 text-sm font-semibold whitespace-nowrap transition ${
        active
          ? 'scale-[1.03] bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-lg shadow-indigo-500/30'
          : 'bg-white text-slate-700 shadow-sm ring-1 ring-slate-300 hover:-translate-y-0.5 hover:text-indigo-700 hover:shadow-md hover:ring-indigo-400'
      }`}
    >
      {children}
    </button>
  )
}

// An applied filter; the x removes just that one
function FilterPill({ children, onRemove }) {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-indigo-50 py-1 pr-1 pl-3 text-sm font-medium text-indigo-700 ring-1 ring-indigo-200">
      {children}
      <button
        onClick={onRemove}
        aria-label="Sterge filtrul"
        className="grid h-5 w-5 place-items-center rounded-full text-indigo-500 transition hover:bg-indigo-100 hover:text-indigo-800"
      >
        ×
      </button>
    </span>
  )
}

// Saves the current search as an alert (notification when a matching auction opens)
function SaveAlertButton({ filters }) {
  const t = useT()
  const [state, setState] = useState({ busy: false, text: '', ok: true })
  async function save() {
    setState({ busy: true, text: '', ok: true })
    try {
      await api('/alerts', { method: 'POST', body: filters })
      setState({ busy: false, text: t('Alerta salvata! O gasesti in Contul meu.'), ok: true })
    } catch (err) {
      setState({ busy: false, text: err.message, ok: false })
    }
  }
  return (
    <span className="ml-auto flex flex-wrap items-center gap-2">
      {state.text && <span className={`text-sm ${state.ok ? 'text-emerald-700' : 'text-red-600'}`}>{state.text}</span>}
      <button
        onClick={save}
        disabled={state.busy || Boolean(state.ok && state.text)}
        className="inline-flex items-center gap-1.5 rounded-xl bg-violet-600 px-3 py-1.5 text-sm font-semibold text-white shadow-md shadow-violet-500/25 transition hover:bg-violet-500 disabled:opacity-60"
      >
        <Icon name="bell" size={16} mono /> {t('Salveaza ca alerta')}
      </button>
    </span>
  )
}
