import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, formatDate, formatPrice } from '../api.js'
import { Icon } from './icons.jsx'
import ItemImage from './ItemImage.jsx'
import { Card } from './ui.jsx'

// "Cat valoreaza?": the price range of similar sold items (PriceEstimateService) as a range bar - the typical
// range (25-75%) as a band, the median as a marker and the current price as a dot - with a verdict and the
// latest similar sales. Hidden when there are not enough sales to compare.
export default function PriceEstimate({ item }) {
  const [estimate, setEstimate] = useState(null)

  useEffect(() => {
    api(`/items/${item.id}/price-estimate`, { auth: false })
      .then((data) => setEstimate(data || null))
      .catch(() => setEstimate(null))
  }, [item.id])

  if (!estimate) return null
  const current = Number(item.currentPrice)
  const low = Number(estimate.low)
  const high = Number(estimate.high)
  const median = Number(estimate.median)
  const lo = Math.min(Number(estimate.min), current)
  const hi = Math.max(Number(estimate.max), current)
  const span = Math.max(hi - lo, 1)
  const pos = (value) => `${4 + ((value - lo) / span) * 92}%`

  const verdict =
    current < low
      ? { label: 'Pret bun', text: 'sub ce se plateste de obicei', icon: 'trendDown', tone: 'bg-emerald-50 text-emerald-800 ring-emerald-200' }
      : current > high
        ? { label: 'Peste piata', text: 'mai scump decat vanzarile similare', icon: 'bolt', tone: 'bg-amber-50 text-amber-800 ring-amber-200' }
        : { label: 'In linie cu piata', text: 'cat se plateste de obicei', icon: 'tagCheck', tone: 'bg-sky-50 text-sky-800 ring-sky-200' }

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-900">
            <Icon name="chart" size={22} /> Cat valoreaza?
          </h2>
          <p className="mt-0.5 text-sm text-slate-500">
            Din {estimate.sales} {estimate.basis === 'SIMILAR' ? 'vanzari de obiecte asemanatoare' : 'vanzari din aceeasi categorie'}
          </p>
        </div>
        <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-semibold ring-1 ${verdict.tone}`}>
          <Icon name={verdict.icon} size={16} /> {verdict.label}
        </span>
      </div>

      {/* Range bar */}
      <div className="relative mt-10 mb-12 h-3" role="img" aria-label={`Vanzari similare: de obicei intre ${formatPrice(low)} si ${formatPrice(high)}, mediana ${formatPrice(median)}. Pretul de acum: ${formatPrice(current)}, ${verdict.text}.`}>
        <div className="absolute inset-0 rounded-full bg-slate-200" />
        <div
          className="estimate-band absolute inset-y-0 rounded-full bg-gradient-to-r from-indigo-300 to-indigo-500"
          style={{ left: pos(low), width: `calc(${pos(high)} - ${pos(low)})` }}
          title={`De obicei: ${formatPrice(low)} - ${formatPrice(high)}`}
        />
        <div className="absolute -top-1.5 h-6 w-1 -translate-x-1/2 rounded-full bg-indigo-800" style={{ left: pos(median) }} title={`Mediana: ${formatPrice(median)}`}>
          <span className="absolute top-7 left-1/2 -translate-x-1/2 text-xs font-semibold whitespace-nowrap text-indigo-800">
            mediana {formatPrice(median)}
          </span>
        </div>
        <div className="estimate-dot absolute top-1/2 -translate-x-1/2 -translate-y-1/2" style={{ left: pos(current) }} title={`Acum: ${formatPrice(current)}`}>
          <span className="block h-5 w-5 rounded-full bg-fuchsia-500 shadow-lg ring-4 ring-fuchsia-200" />
          <span className="absolute bottom-7 left-1/2 -translate-x-1/2 rounded-lg bg-fuchsia-600 px-2 py-0.5 text-xs font-bold whitespace-nowrap text-white shadow">
            acum {formatPrice(current)}
          </span>
        </div>
        <span className="absolute top-5 left-0 text-[11px] text-slate-500">{formatPrice(lo)}</span>
        <span className="absolute top-5 right-0 text-[11px] text-slate-500">{formatPrice(hi)}</span>
      </div>

      <p className="text-sm text-slate-600">
        Obiecte ca acesta s-au vandut de obicei cu{' '}
        <b className="text-slate-900">
          {formatPrice(low)} - {formatPrice(high)}
        </b>
        . Pretul de acum este {verdict.text}.
      </p>

      <ul className="mt-4 divide-y divide-slate-100">
        {estimate.recent.map((sale) => (
          <li key={sale.id}>
            <Link to={`/items/${sale.id}`} className="flex items-center gap-3 py-2.5 transition hover:opacity-80">
              <ItemImage item={sale} className="h-10 w-12 shrink-0 rounded-lg" />
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-medium text-slate-900">{sale.name}</span>
                <span className="block text-xs text-slate-500">vandut {formatDate(sale.soldAt)}</span>
              </span>
              <span className="text-sm font-bold text-slate-900">{formatPrice(sale.price)}</span>
            </Link>
          </li>
        ))}
      </ul>
    </Card>
  )
}
