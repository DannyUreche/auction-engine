import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router'
import { api, formatPrice } from '../api.js'
import CountUp from '../components/CountUp.jsx'
import { CategoryIcon, Icon } from '../components/icons.jsx'
import ItemImage from '../components/ItemImage.jsx'
import { Alert, Card, Loading } from '../components/ui.jsx'

// "Piata in cifre" (/piata): public market statistics from GET /api/market - headline numbers, weekly sales
// trend (filterable by category), price and demand per category, the most contested auctions of the month
// and a heatmap of bids per weekday and hour. Single-hue charts (indigo), details on hover.
const DAYS = ['Luni', 'Marti', 'Miercuri', 'Joi', 'Vineri', 'Sambata', 'Duminica']
const SHORT_DAYS = ['Lu', 'Ma', 'Mi', 'Jo', 'Vi', 'Sa', 'Du']

export default function Market() {
  const [market, setMarket] = useState(null)
  const [error, setError] = useState('')
  const [category, setCategory] = useState('')

  useEffect(() => {
    api('/market', { auth: false })
      .then(setMarket)
      .catch((err) => setError(err.message))
  }, [])

  if (error) return <Alert>{error}</Alert>
  if (!market) return <Loading />

  const { totals } = market
  const selected = market.categories.find((c) => c.category === category)

  return (
    <div className="space-y-8">
      <section className="stay-dark relative overflow-hidden rounded-3xl bg-slate-950 px-6 py-8 text-white shadow-2xl ring-1 shadow-indigo-950/20 ring-white/10 sm:px-10">
        <div aria-hidden="true" className="pointer-events-none absolute inset-0">
          <div className="animate-float absolute -top-24 -left-10 h-72 w-72 rounded-full bg-indigo-600/40 blur-3xl" />
          <div className="animate-float-slow absolute -right-16 -bottom-28 h-80 w-80 rounded-full bg-sky-500/25 blur-3xl" />
          <div className="bg-grid absolute inset-0 opacity-30" />
        </div>
        <div className="relative">
          <p className="text-xs font-semibold tracking-[0.2em] text-sky-200/80 uppercase">Piata in cifre</p>
          <h1 className="mt-1 text-3xl font-black tracking-tight sm:text-4xl">Cat, ce si cand se vinde</h1>
          <p className="mt-2 max-w-2xl text-sm text-indigo-100/80">
            Statistici publice calculate din toate licitatiile: preturi pe categorii, cele mai disputate licitatii si orele in care se liciteaza cel mai
            mult. Fara date personale.
          </p>
          <div className="mt-7 grid grid-cols-2 gap-3 lg:grid-cols-5">
            <Kpi label="Vandute in 30 de zile" value={<CountUp value={totals.soldLast30} />} />
            <Kpi label="Valoare vanduta, 30 zile" value={<CountUp value={Number(totals.valueLast30)} format={(n) => `${Math.round(n).toLocaleString('ro-RO')} RON`} />} />
            <Kpi label="Oferte in 30 de zile" value={<CountUp value={totals.bidsLast30} />} />
            <Kpi label="Oferte pe vanzare" value={totals.avgBidsPerSale ?? '—'} hint="in medie" />
            <Kpi
              label="Rata de vanzare"
              value={totals.sellThroughPercent == null ? '—' : `${totals.sellThroughPercent}%`}
              hint="licitatii incheiate cu castigator, 90 zile"
            />
          </div>
        </div>
      </section>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <div className="mb-4 flex flex-wrap items-baseline justify-between gap-2">
            <h2 className="flex items-center gap-2 font-semibold text-slate-900">
              <Icon name="chart" size={20} /> Valoarea vanzarilor pe saptamana
            </h2>
            <span className="text-xs text-slate-500">ultimele 12 saptamani · {selected ? selected.label : 'toate categoriile'}</span>
          </div>
          <WeeklyTrend points={market.trend} category={category} />
          <div className="mt-4 flex flex-wrap gap-1.5">
            <Chip active={!category} onClick={() => setCategory('')}>
              Toate
            </Chip>
            {market.categories.map((c) => (
              <Chip key={c.category} active={category === c.category} onClick={() => setCategory(c.category)}>
                <CategoryIcon category={{ code: c.category }} size={14} /> {c.label}
              </Chip>
            ))}
          </div>
        </Card>

        <Card className="lg:col-span-2">
          <div className="mb-4 flex items-baseline justify-between gap-2">
            <h2 className="flex items-center gap-2 font-semibold text-slate-900">
              <Icon name="timer" size={20} /> Cand se liciteaza
            </h2>
            <span className="text-xs text-slate-500">ultimele {market.heatmap.days} zile, ora Romaniei</span>
          </div>
          <Heatmap heatmap={market.heatmap} />
        </Card>
      </div>

      <Card>
        <div className="mb-4 flex flex-wrap items-baseline justify-between gap-2">
          <h2 className="flex items-center gap-2 font-semibold text-slate-900">
            <Icon name="coins" size={20} /> Preturi pe categorii
          </h2>
          <span className="text-xs text-slate-500">pretul median al obiectelor vandute (bara pe scara logaritmica) · apasa o categorie pentru evolutia ei</span>
        </div>
        <CategoryTable rows={market.categories} selected={category} onSelect={(code) => setCategory(code === category ? '' : code)} />
      </Card>

      <section>
        <h2 className="flex items-center gap-2 text-xl font-bold text-slate-900">
          <Icon name="gavel" size={24} /> Cele mai disputate licitatii
          <span className="text-sm font-normal text-slate-500">· ultimele 30 de zile</span>
        </h2>
        {market.contested.length === 0 ? (
          <p className="mt-3 text-sm text-slate-500">Nicio oferta in ultimele 30 de zile.</p>
        ) : (
          <ol className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {market.contested.map((item, index) => (
              <li key={item.id} className="animate-fade-up" style={{ animationDelay: `${index * 70}ms` }}>
                <Link
                  to={`/items/${item.id}`}
                  className="group flex gap-4 rounded-2xl bg-white/85 p-3 shadow-sm ring-1 ring-slate-200/80 transition hover:-translate-y-0.5 hover:shadow-lg hover:ring-indigo-300"
                >
                  <div className="relative shrink-0">
                    <ItemImage item={item} className="h-24 w-24 rounded-xl" />
                    <span className="absolute -top-2 -left-2 grid h-8 w-8 place-items-center rounded-full bg-gradient-to-br from-indigo-500 to-violet-600 text-sm font-black text-white shadow-lg ring-2 ring-white">
                      {index + 1}
                    </span>
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="line-clamp-2 font-semibold text-slate-900 group-hover:text-indigo-700">{item.name}</p>
                    <p className="mt-1 text-lg font-black text-slate-900">{formatPrice(item.currentPrice)}</p>
                    <p className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
                      <span className="font-semibold text-indigo-700">{item.bids} oferte</span>
                      <span>{item.bidders} licitatori</span>
                      <span className={item.active ? 'font-semibold text-emerald-700' : ''}>{item.active ? '● activa' : 'incheiata'}</span>
                    </p>
                  </div>
                </Link>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  )
}

function Kpi({ label, value, hint }) {
  return (
    <div className="rounded-2xl bg-white/[0.06] px-4 py-3 ring-1 ring-white/10 backdrop-blur">
      <p className="text-[11px] font-semibold tracking-wide text-white/60 uppercase">{label}</p>
      <p className="mt-1 text-xl font-black whitespace-nowrap tabular-nums sm:text-2xl">{value}</p>
      {hint && <p className="text-[11px] text-white/50">{hint}</p>}
    </div>
  )
}

function Chip({ active, onClick, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium transition ${
        active ? 'bg-indigo-600 text-white shadow-sm' : 'bg-white text-slate-600 shadow-sm ring-1 ring-slate-300 hover:text-indigo-700 hover:ring-indigo-400'
      }`}
    >
      {children}
    </button>
  )
}

function useWidth() {
  const ref = useRef(null)
  const [width, setWidth] = useState(600)
  useEffect(() => {
    const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width))
    observer.observe(ref.current)
    return () => observer.disconnect()
  }, [])
  return [ref, width]
}

function niceCeil(value) {
  if (value <= 0) return 1
  const power = 10 ** Math.floor(Math.log10(value))
  const step = [1, 2, 2.5, 5, 10].find((s) => s * power >= value)
  return step * power
}

const shortMoney = (n) => (n >= 1000 ? `${(n / 1000).toLocaleString('ro-RO', { maximumFractionDigits: 1 })}k` : String(Math.round(n)))

// Columns: value sold per week; the tooltip also gives the number of sales
function WeeklyTrend({ points, category }) {
  const [ref, width] = useWidth()
  const [hover, setHover] = useState(null)
  const weeks = useMemo(() => {
    const map = new Map()
    for (const p of points) {
      if (category && p.category !== category) continue
      const week = map.get(p.week) ?? { week: p.week, value: 0, sold: 0 }
      week.value += Number(p.value)
      week.sold += p.sold
      map.set(p.week, week)
    }
    return [...map.values()]
  }, [points, category])
  const height = 220
  const pad = { left: 44, right: 4, top: 12, bottom: 26 }
  const max = niceCeil(Math.max(...weeks.map((w) => w.value)))
  const innerW = Math.max(width - pad.left - pad.right, 1)
  const innerH = height - pad.top - pad.bottom
  const slot = innerW / weeks.length
  const barW = Math.max(6, Math.min(34, slot - 8))
  const y = (v) => pad.top + innerH - (v / max) * innerH
  const label = (week) => new Date(`${week}T00:00:00`).toLocaleDateString('ro-RO', { day: 'numeric', month: 'short' })
  const total = weeks.reduce((sum, w) => sum + w.value, 0)

  return (
    <div ref={ref} className="relative">
      <svg width={width} height={height} role="img" aria-label={`Vanzari de ${formatPrice(total)} in 12 saptamani`} className="block">
        {[0, 0.5, 1].map((t) => (
          <g key={t}>
            <line x1={pad.left} x2={width - pad.right} y1={y(max * t)} y2={y(max * t)} stroke="var(--color-slate-200)" strokeDasharray="3 4" />
            <text x={pad.left - 8} y={y(max * t)} dy="0.32em" textAnchor="end" className="fill-slate-400 text-[11px]">
              {shortMoney(max * t)}
            </text>
          </g>
        ))}
        {weeks.map((w, i) => {
          const x = pad.left + slot * i + (slot - barW) / 2
          const h = Math.max(w.value > 0 ? 3 : 0, innerH - (y(w.value) - pad.top))
          return (
            <g key={w.week} onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)}>
              <rect x={pad.left + slot * i} y={pad.top} width={slot} height={innerH} fill="transparent" />
              <path
                d={barPath(x, pad.top + innerH - h, barW, h)}
                fill="var(--color-indigo-500)"
                opacity={hover == null || hover === i ? 1 : 0.45}
                className="transition-opacity"
              />
              {(i % 2 === 0 || weeks.length <= 6) && (
                <text x={x + barW / 2} y={height - 6} textAnchor="middle" className="fill-slate-400 text-[11px]">
                  {label(w.week)}
                </text>
              )}
            </g>
          )
        })}
      </svg>
      {hover != null && (
        <div
          className="stay-dark pointer-events-none absolute z-10 -translate-x-1/2 rounded-lg bg-slate-900 px-3 py-2 text-xs whitespace-nowrap text-white shadow-lg"
          style={{ left: pad.left + slot * hover + slot / 2, top: Math.max(0, y(weeks[hover].value) - 58) }}
        >
          <p className="font-semibold">saptamana din {label(weeks[hover].week)}</p>
          <p>
            {formatPrice(weeks[hover].value)} · {weeks[hover].sold} {weeks[hover].sold === 1 ? 'vanzare' : 'vanzari'}
          </p>
        </div>
      )}
      {total === 0 && <p className="absolute inset-0 grid place-items-center text-sm text-slate-500">Nicio vanzare in aceasta perioada.</p>}
    </div>
  )
}

// A bar with rounded top corners, anchored to the baseline
function barPath(x, y, w, h) {
  if (h <= 0) return ''
  const r = Math.min(4, w / 2, h)
  return `M${x},${y + h} V${y + r} Q${x},${y} ${x + r},${y} H${x + w - r} Q${x + w},${y} ${x + w},${y + r} V${y + h} Z`
}

// 7 x 24 grid: the darker the cell, the more bids in that hour
function Heatmap({ heatmap }) {
  const [hover, setHover] = useState(null)
  const { bids, max } = heatmap
  let peak = null
  bids.forEach((row, day) =>
    row.forEach((count, hour) => {
      if (count > 0 && (!peak || count > peak.count)) peak = { day, hour, count }
    }),
  )
  const shown = hover ?? peak
  return (
    <div>
      <div className="grid gap-[3px]" style={{ gridTemplateColumns: '1.75rem repeat(24, minmax(0, 1fr))' }} onMouseLeave={() => setHover(null)}>
        {bids.map((row, day) => (
          <div key={day} className="contents">
            <span className="self-center text-[10px] font-medium text-slate-500">{SHORT_DAYS[day]}</span>
            {row.map((count, hour) => (
              <span
                key={hour}
                onMouseEnter={() => setHover({ day, hour, count })}
                className={`aspect-square rounded-[3px] ring-indigo-500 transition ${hover && hover.day === day && hover.hour === hour ? 'ring-2' : ''}`}
                style={{ background: count === 0 ? 'var(--color-slate-100)' : `rgb(99 102 241 / ${0.15 + 0.85 * (count / Math.max(1, max))})` }}
                aria-label={`${DAYS[day]} ${hour}:00 - ${count} oferte`}
              />
            ))}
          </div>
        ))}
        <span />
        {Array.from({ length: 24 }, (_, hour) => (
          <span key={hour} className="text-center text-[9px] text-slate-400">
            {hour % 6 === 0 ? hour : ''}
          </span>
        ))}
      </div>
      <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-slate-600">
          {shown ? (
            <>
              {hover ? '' : 'Varf: '}
              <b className="text-slate-900">
                {DAYS[shown.day]}, {String(shown.hour).padStart(2, '0')}:00–{String((shown.hour + 1) % 24).padStart(2, '0')}:00
              </b>{' '}
              · {shown.count} oferte
            </>
          ) : (
            'Inca nu sunt oferte.'
          )}
        </p>
        <span className="flex items-center gap-1.5 text-[10px] text-slate-500">
          putine
          {[0.15, 0.4, 0.65, 1].map((alpha) => (
            <span key={alpha} className="h-3 w-3 rounded-[3px]" style={{ background: `rgb(99 102 241 / ${alpha})` }} />
          ))}
          multe
        </span>
      </div>
      <Totals bids={bids} />
    </div>
  )
}

// Bids per weekday and per hour of the day (sums of the heatmap rows and columns)
function Totals({ bids }) {
  const perDay = bids.map((row) => row.reduce((a, b) => a + b, 0))
  const perHour = Array.from({ length: 24 }, (_, hour) => bids.reduce((sum, row) => sum + row[hour], 0))
  const dayMax = Math.max(1, ...perDay)
  const bestDay = perDay.indexOf(Math.max(...perDay))
  const bestHour = perHour.indexOf(Math.max(...perHour))
  const total = perDay.reduce((a, b) => a + b, 0)
  if (total === 0) return null
  return (
    <div className="mt-5 grid gap-4 border-t border-slate-200/70 pt-4 sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
      <div>
        <p className="text-[11px] font-semibold tracking-wide text-slate-500 uppercase">Pe zile</p>
        <div className="mt-2 flex h-16 items-end gap-1.5">
          {perDay.map((count, day) => (
            <div key={day} className="flex flex-1 flex-col items-center gap-1" title={`${DAYS[day]}: ${count} oferte`}>
              <span
                className={`w-full rounded-t-[4px] ${day === bestDay ? 'bg-indigo-500' : 'bg-indigo-500/35'}`}
                style={{ height: `${Math.max(3, (count / dayMax) * 44)}px` }}
              />
              <span className="text-[10px] text-slate-500">{SHORT_DAYS[day]}</span>
            </div>
          ))}
        </div>
      </div>
      <div className="grid grid-cols-2 gap-2 text-center">
        <div className="rounded-xl bg-indigo-50 px-2 py-3 ring-1 ring-indigo-100">
          <p className="text-[10px] font-semibold tracking-wide text-indigo-700 uppercase">Ziua de varf</p>
          <p className="mt-1 text-lg font-black text-slate-900">{DAYS[bestDay]}</p>
          <p className="text-[11px] text-slate-500">{Math.round((perDay[bestDay] / total) * 100)}% din oferte</p>
        </div>
        <div className="rounded-xl bg-indigo-50 px-2 py-3 ring-1 ring-indigo-100">
          <p className="text-[10px] font-semibold tracking-wide text-indigo-700 uppercase">Ora de varf</p>
          <p className="mt-1 text-lg font-black text-slate-900">{String(bestHour).padStart(2, '0')}:00</p>
          <p className="text-[11px] text-slate-500">{perHour[bestHour]} oferte</p>
        </div>
      </div>
    </div>
  )
}

function CategoryTable({ rows, selected, onSelect }) {
  // Log scale: one expensive category (cars) would otherwise flatten all the others
  const scale = (value) => Math.log10(1 + Number(value))
  const maxMedian = Math.max(1, ...rows.map((r) => scale(r.medianPrice ?? 0)))
  return (
    <div className="-mx-2 overflow-x-auto">
      <table className="w-full min-w-[40rem] text-sm">
        <thead>
          <tr className="text-left text-[11px] tracking-wide text-slate-500 uppercase">
            <th className="px-2 pb-2 font-semibold">Categorie</th>
            <th className="px-2 pb-2 font-semibold">Pret median</th>
            <th className="px-2 pb-2 text-right font-semibold">Vandute</th>
            <th className="px-2 pb-2 text-right font-semibold">Oferte / vanzare</th>
            <th className="px-2 pb-2 text-right font-semibold">Rata de vanzare</th>
            <th className="px-2 pb-2 text-right font-semibold">Active acum</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={row.category}
              onClick={() => onSelect(row.category)}
              className={`cursor-pointer border-t border-slate-200/70 transition hover:bg-indigo-50/60 ${selected === row.category ? 'bg-indigo-50' : ''}`}
            >
              <td className="px-2 py-2.5">
                <span className="flex items-center gap-2 font-medium text-slate-800">
                  <CategoryIcon category={{ code: row.category }} size={18} /> {row.label}
                </span>
              </td>
              <td className="w-[36%] px-2 py-2.5">
                {row.medianPrice == null ? (
                  <span className="text-slate-400">—</span>
                ) : (
                  <span className="flex items-center gap-3">
                    <span className="h-2 flex-1 overflow-hidden rounded-full bg-slate-100">
                      <span className="block h-full rounded-full bg-indigo-500" style={{ width: `${(scale(row.medianPrice) / maxMedian) * 100}%` }} />
                    </span>
                    <span className="w-24 text-right font-semibold text-slate-900 tabular-nums">{formatPrice(row.medianPrice)}</span>
                  </span>
                )}
              </td>
              <td className="px-2 py-2.5 text-right tabular-nums">{row.sold}</td>
              <td className="px-2 py-2.5 text-right tabular-nums">{row.avgBids ?? '—'}</td>
              <td className="px-2 py-2.5 text-right tabular-nums">{row.sellThroughPercent == null ? '—' : `${row.sellThroughPercent}%`}</td>
              <td className="px-2 py-2.5 text-right tabular-nums">{row.active}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
