import { useEffect, useRef, useState } from 'react'
import { api, formatPrice } from '../api.js'
import { CategoryIcon } from './icons.jsx'
import { Card } from './ui.jsx'

// Graficele din pagina de administrare. O singura culoare (indigo) pentru marimi, grila discreta,
// detaliile la hover. Fara biblioteci: SVG desenat la latimea reala a containerului.
export default function AdminCharts({ refreshKey }) {
  const [charts, setCharts] = useState(null)

  useEffect(() => {
    api('/admin/charts')
      .then(setCharts)
      .catch(() => setCharts(null))
  }, [refreshKey])

  if (!charts) return null
  return (
    <div className="mt-6 grid gap-4 lg:grid-cols-5">
      <Card className="lg:col-span-3">
        <ChartTitle title="Oferte pe zi" subtitle="ultimele 14 zile" />
        <BidsPerDay days={charts.bidsPerDay} />
      </Card>
      <Card className="lg:col-span-2">
        <ChartTitle title="Vanzari pe categorii" subtitle="valoarea obiectelor vandute" />
        <SalesByCategory rows={charts.salesByCategory} />
      </Card>
      <Card className="lg:col-span-5">
        <ChartTitle title="Top vanzatori" subtitle="dupa valoarea vanduta" />
        <TopSellers rows={charts.topSellers} />
      </Card>
    </div>
  )
}

function ChartTitle({ title, subtitle }) {
  return (
    <div className="mb-4 flex items-baseline justify-between gap-3">
      <h2 className="font-semibold text-slate-900">{title}</h2>
      <span className="text-xs text-slate-400">{subtitle}</span>
    </div>
  )
}

function useWidth() {
  const ref = useRef(null)
  const [width, setWidth] = useState(500)
  useEffect(() => {
    const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width))
    observer.observe(ref.current)
    return () => observer.disconnect()
  }, [])
  return [ref, width]
}

// Coloane: cate oferte au fost in fiecare zi
export function BidsPerDay({ days }) {
  const [ref, width] = useWidth()
  const [hover, setHover] = useState(null)
  const height = 200
  const pad = { left: 32, right: 4, top: 10, bottom: 24 }
  const max = Math.max(1, ...days.map((d) => d.bids))
  const niceMax = Math.ceil(max / 4) * 4 || 4
  const innerW = Math.max(width - pad.left - pad.right, 1)
  const innerH = height - pad.top - pad.bottom
  const slot = innerW / days.length
  const barW = Math.max(4, Math.min(28, slot - 6))
  const y = (v) => pad.top + innerH - (v / niceMax) * innerH
  const label = (day) => new Date(`${day}T00:00:00`).toLocaleDateString('ro-RO', { day: 'numeric', month: 'short' })
  const total = days.reduce((sum, d) => sum + d.bids, 0)

  return (
    <div ref={ref} className="relative">
      <svg width={width} height={height} role="img" aria-label={`${total} oferte in ultimele 14 zile`} className="block">
        {[0, 0.5, 1].map((t) => (
          <g key={t}>
            <line x1={pad.left} x2={width - pad.right} y1={y(niceMax * t)} y2={y(niceMax * t)} stroke="var(--color-slate-200)" strokeDasharray="3 4" />
            <text x={pad.left - 8} y={y(niceMax * t)} dy="0.32em" textAnchor="end" className="fill-slate-400 text-[11px]">
              {niceMax * t}
            </text>
          </g>
        ))}
        {days.map((d, i) => {
          const x = pad.left + i * slot + (slot - barW) / 2
          const h = Math.max(d.bids > 0 ? 2 : 0, pad.top + innerH - y(d.bids))
          return (
            <g key={d.day} onPointerEnter={() => setHover(i)} onPointerLeave={() => setHover(null)}>
              {/* zona de hover mai mare decat coloana */}
              <rect x={pad.left + i * slot} y={pad.top} width={slot} height={innerH} fill="transparent" />
              <path
                d={roundedTop(x, pad.top + innerH - h, barW, h, Math.min(4, barW / 2))}
                fill={hover === i ? 'var(--color-indigo-500)' : 'var(--color-indigo-600)'}
              />
              {(i % Math.ceil(days.length / 7) === 0 || i === days.length - 1) && (
                <text x={pad.left + i * slot + slot / 2} y={height - 6} textAnchor="middle" className="fill-slate-400 text-[11px]">
                  {label(d.day)}
                </text>
              )}
            </g>
          )
        })}
      </svg>
      {hover != null && (
        <div
          className="stay-dark pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-full rounded-lg bg-slate-900 px-3 py-1.5 text-xs whitespace-nowrap text-white shadow-lg"
          style={{ left: Math.min(Math.max(pad.left + hover * slot + slot / 2, 60), width - 60), top: y(days[hover].bids) - 8 }}
        >
          <b>{days[hover].bids}</b> {days[hover].bids === 1 ? 'oferta' : 'oferte'} · {label(days[hover].day)}
        </div>
      )}
    </div>
  )
}

// Dreptunghi cu colturile de sus rotunjite (capatul de date), ancorat pe axa de jos
function roundedTop(x, y, w, h, r) {
  if (h <= 0) return ''
  const radius = Math.min(r, h)
  return `M${x},${y + h} V${y + radius} Q${x},${y} ${x + radius},${y} H${x + w - radius} Q${x + w},${y} ${x + w},${y + radius} V${y + h} Z`
}

// Bare orizontale: valoarea vanduta pe fiecare categorie (de la cea mai mare)
function SalesByCategory({ rows }) {
  if (rows.length === 0) return <p className="py-10 text-center text-sm text-slate-400">Inca nu s-a vandut nimic.</p>
  const max = Math.max(...rows.map((r) => Number(r.value)))
  return (
    <ul className="space-y-3">
      {rows.map((r) => (
        <li key={r.category} title={`${r.sold} ${r.sold === 1 ? 'obiect vandut' : 'obiecte vandute'}`}>
          <div className="mb-1 flex justify-between gap-2 text-sm">
            <span className="flex min-w-0 items-center gap-1.5 truncate text-slate-700">
              <CategoryIcon category={{ code: r.category, icon: r.icon }} size={16} /> {r.label}
            </span>
            <span className="shrink-0 font-semibold text-slate-900 tabular-nums">{formatPrice(r.value)}</span>
          </div>
          <div className="h-2 overflow-hidden rounded-full bg-slate-100">
            <div
              className="h-full origin-left animate-grow rounded-full bg-indigo-600"
              style={{ width: `${(Number(r.value) / max) * 100}%` }}
            />
          </div>
        </li>
      ))}
    </ul>
  )
}

function TopSellers({ rows }) {
  if (rows.length === 0) return <p className="py-6 text-center text-sm text-slate-400">Inca nu s-a vandut nimic.</p>
  const max = Math.max(...rows.map((r) => Number(r.value)))
  return (
    <ol className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
      {rows.map((r, i) => (
        <li key={r.username} className="rounded-xl bg-slate-50 p-3 ring-1 ring-slate-200">
          <p className="text-xs font-semibold text-slate-400">#{i + 1}</p>
          <p className="truncate font-semibold text-slate-900">{r.username}</p>
          <p className="text-sm text-slate-600">
            {formatPrice(r.value)} · {r.sold} {r.sold === 1 ? 'vanzare' : 'vanzari'}
          </p>
          <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-200">
            <div className="h-full rounded-full bg-indigo-600" style={{ width: `${(Number(r.value) / max) * 100}%` }} />
          </div>
        </li>
      ))}
    </ol>
  )
}
