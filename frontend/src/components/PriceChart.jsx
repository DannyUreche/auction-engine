import { useEffect, useRef, useState } from 'react'
import { formatDate, formatPrice } from '../api.js'

const HEIGHT = 190
const PAD = { left: 64, right: 16, top: 16, bottom: 16 }

// Evolutia pretului unui obiect: de la pretul de pornire, oferta cu oferta (grafic in trepte:
// pretul ramane acelasi pana la urmatoarea oferta). La hover apare o linie verticala si detaliile ofertei.
// Lista "Istoricul ofertelor" de sub grafic contine aceleasi date sub forma de tabel.
export default function PriceChart({ startPrice, bids }) {
  const boxRef = useRef(null)
  const [width, setWidth] = useState(640)
  const [hover, setHover] = useState(null)

  // Graficul se deseneaza la latimea reala a containerului (textul nu se deformeaza)
  useEffect(() => {
    const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width))
    observer.observe(boxRef.current)
    return () => observer.disconnect()
  }, [])

  // Ofertele vin de la cea mai mare; pe grafic le vrem in ordinea in care au fost facute
  const points = [
    { amount: Number(startPrice), label: 'Pret de pornire' },
    ...[...bids].reverse().map((bid) => ({ amount: Number(bid.amount), label: bid.bidder?.username, time: bid.createdAt })),
  ]

  const amounts = points.map((p) => p.amount)
  const min = Math.min(...amounts)
  const max = Math.max(...amounts)
  const margin = (max - min || max * 0.1 || 1) * 0.15
  const low = Math.max(0, min - margin)
  const high = max + margin

  const innerWidth = Math.max(width - PAD.left - PAD.right, 1)
  const innerHeight = HEIGHT - PAD.top - PAD.bottom
  const x = (i) => PAD.left + (points.length === 1 ? innerWidth : (i * innerWidth) / (points.length - 1))
  const y = (v) => PAD.top + ((high - v) / (high - low)) * innerHeight

  // Linie in trepte: orizontal pana la urmatoarea oferta, apoi vertical la noul pret
  let line = `M ${x(0)} ${y(points[0].amount)}`
  points.slice(1).forEach((p, i) => {
    line += ` H ${x(i + 1)} V ${y(p.amount)}`
  })
  const area = `${line} V ${PAD.top + innerHeight} H ${x(0)} Z`

  const ticks = [0, 1, 2, 3].map((k) => low + ((high - low) * k) / 3)
  const short = (v) => `${Math.round(v).toLocaleString('ro-RO')} RON`

  function handleMove(event) {
    const rect = event.currentTarget.getBoundingClientRect()
    const mouseX = event.clientX - rect.left
    const step = points.length > 1 ? innerWidth / (points.length - 1) : innerWidth
    const index = Math.round((mouseX - PAD.left) / step)
    setHover(Math.min(Math.max(index, 0), points.length - 1))
  }

  const active = hover != null ? points[hover] : null
  const last = points.length - 1

  return (
    <div ref={boxRef} className="relative">
      <svg
        width={width}
        height={HEIGHT}
        role="img"
        aria-label={`Evolutia pretului: de la ${short(points[0].amount)} la ${short(points[last].amount)} in ${last} oferte`}
        onPointerMove={handleMove}
        onPointerLeave={() => setHover(null)}
        className="block touch-none"
      >
        <defs>
          <linearGradient id="price-area" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--color-indigo-500)" stopOpacity="0.22" />
            <stop offset="100%" stopColor="var(--color-indigo-500)" stopOpacity="0" />
          </linearGradient>
        </defs>

        {/* Grila orizontala si etichetele de pret - discrete, ca sa nu concureze cu linia */}
        {ticks.map((t) => (
          <g key={t}>
            <line x1={PAD.left} x2={width - PAD.right} y1={y(t)} y2={y(t)} stroke="var(--color-slate-200)" strokeDasharray="3 4" />
            <text x={PAD.left - 10} y={y(t)} dy="0.32em" textAnchor="end" className="fill-slate-400 text-[11px]">
              {short(t)}
            </text>
          </g>
        ))}

        <path d={area} fill="url(#price-area)" />
        <path d={line} fill="none" stroke="var(--color-indigo-600)" strokeWidth="2" strokeLinejoin="round" />

        {/* Ultimul pret: punct pulsand (licitatia e "vie") */}
        <circle cx={x(last)} cy={y(points[last].amount)} r="10" className="animate-ping-slow fill-indigo-400/40" style={{ transformOrigin: `${x(last)}px ${y(points[last].amount)}px` }} />
        <circle cx={x(last)} cy={y(points[last].amount)} r="5" fill="var(--color-indigo-600)" stroke="white" strokeWidth="2" />

        {active && (
          <g>
            <line x1={x(hover)} x2={x(hover)} y1={PAD.top} y2={PAD.top + innerHeight} stroke="var(--color-slate-400)" strokeWidth="1" />
            <circle cx={x(hover)} cy={y(active.amount)} r="5" fill="var(--color-indigo-600)" stroke="white" strokeWidth="2" />
          </g>
        )}
      </svg>

      {active && (
        <div
          className="stay-dark pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-full rounded-lg bg-slate-900 px-3 py-2 text-xs whitespace-nowrap text-white shadow-lg"
          style={{ left: Math.min(Math.max(x(hover), 80), width - 80), top: y(active.amount) - 10 }}
        >
          <p className="text-sm font-semibold">{formatPrice(active.amount)}</p>
          <p className="text-slate-300">
            {active.label}
            {active.time && ` · ${formatDate(active.time)}`}
          </p>
        </div>
      )}
    </div>
  )
}
