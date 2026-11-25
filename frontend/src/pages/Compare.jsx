import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { api, formatPrice, parseDate } from '../api.js'
import { findCategory, useCategories } from '../categories.js'
import { useCompare } from '../compare.js'
import Countdown from '../components/Countdown.jsx'
import { CategoryIcon, Icon } from '../components/icons.jsx'
import ItemImage from '../components/ItemImage.jsx'
import { deliveryLabel } from '../components/LocationFields.jsx'
import { Alert, EmptyState, Loading } from '../components/ui.jsx'

// Side-by-side comparison of up to 3 items (/compara?ids=1,2,3): price, bids, time left, type, county,
// delivery, seller and the price estimate, with the best value of each row highlighted.
export default function Compare() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const compare = useCompare()
  const categories = useCategories()
  const ids = (params.get('ids') ?? '').split(',').map(Number).filter(Boolean).slice(0, 3)
  const [columns, setColumns] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (ids.length === 0) {
      setColumns([])
      return
    }
    Promise.all(
      ids.map(async (id) => {
        const [item, bids, estimate] = await Promise.all([
          api(`/items/${id}`, { auth: false }),
          api(`/bids/${id}`, { auth: false }).catch(() => []),
          api(`/items/${id}/price-estimate`, { auth: false }).catch(() => null),
        ])
        return { item, bids: bids.length, estimate }
      }),
    )
      .then(setColumns)
      .catch((err) => setError(err.message))
  }, [params]) // eslint-disable-line react-hooks/exhaustive-deps

  if (error) return <Alert>{error}</Alert>
  if (!columns) return <Loading />
  if (columns.length < 2) {
    return (
      <EmptyState title="Alege cel putin doua obiecte">
        Apasa butonul de comparatie de pe cardurile licitatiilor (langa inimioara), apoi revino aici.{' '}
        <Link to="/" className="font-semibold text-indigo-600 hover:text-indigo-500">
          Vezi licitatiile
        </Link>
      </EmptyState>
    )
  }

  const active = columns.filter((c) => c.item.active)
  const best = (pick, compareFn) => {
    if (active.length < 2) return null
    return active.reduce((winner, c) => (compareFn(pick(c), pick(winner)) ? c : winner)).item.id
  }
  const cheapest = best((c) => Number(c.item.currentPrice), (a, b) => a < b)
  const busiest = best((c) => c.bids, (a, b) => a > b)
  const soonest = best((c) => parseDate(c.item.endTime).getTime(), (a, b) => a < b)

  function removeColumn(id) {
    compare.remove(id)
    const rest = ids.filter((other) => other !== id)
    navigate(rest.length ? `/compara?ids=${rest.join(',')}` : '/', { replace: true })
  }

  const rows = [
    {
      label: 'Pret curent',
      icon: 'coins',
      value: (c) => <span className="text-2xl font-black text-slate-900">{formatPrice(c.item.currentPrice)}</span>,
      winner: cheapest,
      badge: 'Cel mai ieftin',
    },
    { label: 'Oferte', icon: 'gavel', value: (c) => <span className="text-lg font-bold">{c.bids}</span>, winner: busiest, badge: 'Cel mai disputat' },
    {
      label: 'Timp ramas',
      icon: 'timer',
      value: (c) => <Countdown endTime={c.item.endTime} active={c.item.active} startTime={c.item.startTime} />,
      winner: soonest,
      badge: 'Se termina primul',
    },
    { label: 'Urmatoarea oferta', icon: 'trendDown', value: (c) => (c.item.active ? `minim ${formatPrice(c.item.minimumBid)}` : '—') },
    { label: 'Cumpara acum', icon: 'bolt', value: (c) => (c.item.buyNowAvailable && c.item.buyNowPrice ? formatPrice(c.item.buyNowPrice) : '—') },
    {
      label: 'Tip',
      icon: 'sparkles',
      value: (c) => (c.item.auctionType === 'DUTCH' ? 'Olandeza (pretul scade)' : !c.item.started ? 'Programata' : 'Clasica'),
    },
    {
      label: 'Categorie',
      icon: 'tagCheck',
      value: (c) => {
        const category = findCategory(categories, c.item.category)
        return (
          <span className="inline-flex items-center gap-1.5">
            <CategoryIcon category={category} size={16} /> {category.label}
          </span>
        )
      },
    },
    { label: 'Livrare', icon: 'pin', value: (c) => `${c.item.county ? `jud. ${c.item.county} · ` : ''}${deliveryLabel(c.item.delivery)}` },
    {
      label: 'Vanzator',
      icon: 'star',
      value: (c) => (
        <Link to={`/users/${encodeURIComponent(c.item.seller.username)}`} className="font-medium text-indigo-600 hover:underline">
          {c.item.seller.username}
        </Link>
      ),
    },
    {
      label: 'Fata de piata',
      icon: 'chart',
      value: (c) => {
        if (!c.estimate) return <span className="text-slate-400">prea putine vanzari</span>
        const price = Number(c.item.currentPrice)
        const [text, tone] =
          price < Number(c.estimate.low)
            ? ['Pret bun', 'bg-emerald-50 text-emerald-800 ring-emerald-200']
            : price > Number(c.estimate.high)
              ? ['Peste piata', 'bg-amber-50 text-amber-800 ring-amber-200']
              : ['In linie cu piata', 'bg-sky-50 text-sky-800 ring-sky-200']
        return <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ${tone}`}>{text}</span>
      },
    },
  ]

  const template = { gridTemplateColumns: `9.5rem repeat(${columns.length}, minmax(0, 1fr))` }

  return (
    <div>
      <h1 className="flex items-center gap-3 text-3xl font-bold tracking-tight text-slate-900">
        <Icon name="columns" size={32} /> Compara obiecte
      </h1>
      <p className="mt-1 text-slate-500">Cel mai bun pe fiecare rand e evidentiat cu verde.</p>
      <p className="mt-2 flex items-center gap-1.5 text-xs font-medium text-indigo-600 md:hidden">↔ Gliseaza tabelul ca sa vezi toate obiectele</p>

      <div className="mt-8 overflow-x-auto pb-2">
        <div style={{ minWidth: `${9.5 + columns.length * 14}rem` }}>
          {/* Photos and names */}
          <div className="grid gap-4" style={template}>
            <span />
            {columns.map((c, index) => (
              <div key={c.item.id} className="animate-fade-up" style={{ animationDelay: `${index * 90}ms` }}>
                <div className="group relative overflow-hidden rounded-2xl shadow-lg ring-1 ring-slate-200">
                  <ItemImage item={c.item} className="aspect-[4/3] w-full transition duration-500 group-hover:scale-105" />
                  <button
                    onClick={() => removeColumn(c.item.id)}
                    className="absolute top-2 right-2 grid h-8 w-8 place-items-center rounded-full bg-black/50 text-lg text-white backdrop-blur transition hover:bg-rose-600"
                    aria-label={`Scoate ${c.item.name}`}
                  >
                    ×
                  </button>
                </div>
                <Link to={`/items/${c.item.id}`} className="mt-3 line-clamp-2 block font-semibold text-slate-900 hover:text-indigo-600">
                  {c.item.name}
                </Link>
              </div>
            ))}
          </div>

          {/* One row per property */}
          <div className="mt-6 overflow-hidden rounded-2xl bg-white/85 shadow-sm ring-1 ring-slate-200/80">
            {rows.map((row, rowIndex) => (
              <div key={row.label} className={`grid items-center gap-4 px-4 py-3 ${rowIndex % 2 ? 'bg-slate-50/60' : ''}`} style={template}>
                <span className="flex items-center gap-2 text-xs font-semibold tracking-wide text-slate-500 uppercase">
                  <Icon name={row.icon} size={16} /> {row.label}
                </span>
                {columns.map((c) => {
                  const wins = row.winner === c.item.id
                  return (
                    <div
                      key={c.item.id}
                      className={`relative rounded-xl px-3 py-2 text-sm text-slate-700 transition ${wins ? 'bg-emerald-50 ring-2 ring-emerald-300' : ''}`}
                    >
                      {row.value(c)}
                      {wins && (
                        <span className="mt-1 flex items-center gap-1 text-[11px] font-bold text-emerald-700">
                          <Icon name="trophy" size={13} /> {row.badge}
                        </span>
                      )}
                    </div>
                  )
                })}
              </div>
            ))}
          </div>

          <div className="mt-4 grid gap-4" style={template}>
            <span />
            {columns.map((c) => (
              <Link
                key={c.item.id}
                to={`/items/${c.item.id}`}
                className="rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-2.5 text-center text-sm font-semibold text-white shadow-md shadow-indigo-500/25 transition hover:brightness-110"
              >
                {c.item.active ? 'Liciteaza' : 'Vezi obiectul'} →
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
