import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, formatPrice } from '../api.js'
import { BidsPerDay } from './AdminCharts.jsx'
import { Icon } from './icons.jsx'
import ItemImage from './ItemImage.jsx'
import { Alert, Badge, Card, EmptyState, Loading } from './ui.jsx'

// Account tab "Statistici": how the user's listings perform (views, bids, watchers), bids per day, revenue
export default function SellerStats() {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/users/me/seller-stats')
      .then(setStats)
      .catch((err) => setError(err.message))
  }, [])

  if (error) return <Alert>{error}</Alert>
  if (!stats) return <Loading />
  const { totals, bidsPerDay, items } = stats
  if (items.length === 0) {
    return (
      <EmptyState title="Inca nu ai pus nimic la vanzare">
        <Link to="/sell" className="font-semibold text-indigo-600 hover:text-indigo-500">
          Pune primul obiect la licitatie
        </Link>
      </EmptyState>
    )
  }
  const maxViews = Math.max(1, ...items.map((item) => item.views))

  return (
    <div className="space-y-6">
      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        <Tile icon="eye" label="Vizualizari" value={totals.views} />
        <Tile icon="coins" label="Oferte primite" value={totals.bids} />
        <Tile icon="heart" label="Urmaritori" value={totals.watchers} />
        <Tile icon="tagCheck" label="Vandute" value={`${totals.sold} / ${totals.items}`} />
        <Tile icon="wallet" label="Incasari" value={formatPrice(totals.revenue)} />
        <Tile icon="star" label="Nota medie" value={totals.rating.count ? `${totals.rating.average.toFixed(1)} (${totals.rating.count})` : '—'} />
      </dl>

      <Card>
        <div className="mb-4 flex items-baseline justify-between gap-3">
          <h2 className="font-semibold text-slate-900">Oferte primite pe zi</h2>
          <span className="text-xs text-slate-400">ultimele 14 zile</span>
        </div>
        <BidsPerDay days={bidsPerDay} />
      </Card>

      <Card className="overflow-x-auto !p-0">
        <table className="w-full min-w-[40rem] text-sm">
          <thead>
            <tr className="border-b border-slate-100 text-left text-xs tracking-wide text-slate-500 uppercase">
              <th className="px-5 py-3 font-medium">Obiect</th>
              <th className="px-3 py-3 font-medium">Pret</th>
              <th className="px-3 py-3 font-medium">Vizualizari</th>
              <th className="px-3 py-3 text-right font-medium">Oferte</th>
              <th className="px-3 py-3 text-right font-medium">Ofertanti</th>
              <th className="px-5 py-3 text-right font-medium">Urmaritori</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {items.map((item) => (
              <tr key={item.id} className="transition hover:bg-slate-50">
                <td className="px-5 py-3">
                  <Link to={`/items/${item.id}`} className="flex items-center gap-3">
                    <ItemImage item={item} className="h-10 w-12 shrink-0 rounded-lg" />
                    <span className="min-w-0">
                      <span className="block truncate font-medium text-slate-900 hover:text-indigo-600">{item.name}</span>
                      {item.active ? <Badge tone="green">Activa</Badge> : item.sold ? <Badge tone="indigo">Vandut</Badge> : <Badge tone="gray">Nevandut</Badge>}
                    </span>
                  </Link>
                </td>
                <td className="px-3 py-3 font-semibold whitespace-nowrap text-slate-900">{formatPrice(item.currentPrice)}</td>
                <td className="px-3 py-3">
                  {/* Inline bar: views compared with the most viewed item */}
                  <span className="flex items-center gap-2">
                    <span className="h-2 w-20 overflow-hidden rounded-full bg-slate-100">
                      <span className="block h-full rounded-full bg-indigo-500" style={{ width: `${(item.views / maxViews) * 100}%` }} />
                    </span>
                    <span className="tabular-nums text-slate-700">{item.views}</span>
                  </span>
                </td>
                <td className="px-3 py-3 text-right tabular-nums text-slate-700">{item.bids}</td>
                <td className="px-3 py-3 text-right tabular-nums text-slate-700">{item.bidders}</td>
                <td className="px-5 py-3 text-right tabular-nums text-slate-700">{item.watchers}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
      <p className="text-xs text-slate-500">Vizualizarile numara vizitele unice ale paginii obiectului (fara vizitele tale).</p>
    </div>
  )
}

function Tile({ icon, label, value }) {
  return (
    <div className="rounded-2xl bg-white/85 p-4 shadow-sm ring-1 ring-slate-200/80">
      <dt className="flex items-center gap-1.5 text-xs font-medium text-slate-500">
        <Icon name={icon} size={16} /> {label}
      </dt>
      <dd className="mt-1 text-lg font-bold tracking-tight whitespace-nowrap text-slate-900">{value}</dd>
    </div>
  )
}
