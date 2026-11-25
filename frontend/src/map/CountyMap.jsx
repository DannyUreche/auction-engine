import { useLayoutEffect, useMemo, useRef, useState } from 'react'
import { Icon } from '../components/icons.jsx'
import { COUNTY_PATHS, VIEW_BOX } from './romaniaCounties.js'

// Interactive map of Romania on the home page: every county is colored by its number of active auctions
// (one hue, light -> dark), lifts on hover with a tooltip, filters the list on click and pulses when a live
// bid arrives for an item there. The side panel has the legend and the counties with the most auctions.

const NAMES = Object.keys(COUNTY_PATHS)

// Sequential scale: indigo-100 -> indigo-700; counties without auctions stay neutral
function fillFor(count, max) {
  if (!count) return 'var(--color-slate-200)'
  const t = 0.18 + 0.82 * Math.sqrt(count / max)
  return `color-mix(in oklab, var(--color-indigo-700) ${Math.round(t * 100)}%, var(--color-indigo-100))`
}

export default function CountyMap({ counties, selected, onSelect, pulses = [] }) {
  const counts = useMemo(() => Object.fromEntries(counties.map((c) => [c.county, c.items])), [counties])
  const max = Math.max(1, ...counties.map((c) => c.items))
  const total = counties.reduce((sum, c) => sum + c.items, 0)
  const withItems = counties.filter((c) => c.items > 0).length
  const top = [...counties].filter((c) => c.items > 0).sort((a, b) => b.items - a.items).slice(0, 6)

  const [hover, setHover] = useState(null) // { name, x, y } in px inside the map box
  const boxRef = useRef(null)
  const pathRefs = useRef({})
  const [centers, setCenters] = useState({})

  // Center of every county (for the pulses and the number badges), measured once from the drawn paths
  useLayoutEffect(() => {
    const next = {}
    for (const name of NAMES) {
      const box = pathRefs.current[name]?.getBBox()
      if (box) next[name] = { x: box.x + box.width / 2, y: box.y + box.height / 2 }
    }
    setCenters(next)
  }, [])

  const move = (name) => (event) => {
    const rect = boxRef.current.getBoundingClientRect()
    setHover({ name, x: event.clientX - rect.left, y: event.clientY - rect.top })
  }

  const pick = (name) => onSelect(selected === name ? '' : name)
  const hovered = hover?.name

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_17rem] lg:items-center">
      <div ref={boxRef} className="relative" onPointerLeave={() => setHover(null)}>
        <svg viewBox={VIEW_BOX} className="block h-auto w-full overflow-visible" role="group" aria-label="Harta Romaniei cu licitatiile pe judete">
          <defs>
            <filter id="county-lift" x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow dx="0" dy="3" stdDeviation="3" floodColor="#312e81" floodOpacity="0.35" />
            </filter>
          </defs>
          {NAMES.map((name, index) => (
            <path
              key={name}
              ref={(el) => {
                pathRefs.current[name] = el
              }}
              d={COUNTY_PATHS[name]}
              role="button"
              tabIndex={0}
              aria-label={`${name}: ${counts[name] ?? 0} licitatii`}
              aria-pressed={selected === name}
              onPointerMove={move(name)}
              onClick={() => pick(name)}
              onKeyDown={(event) => (event.key === 'Enter' || event.key === ' ') && (event.preventDefault(), pick(name))}
              onFocus={() => setHover({ name, x: (centers[name]?.x ?? 300) * ((boxRef.current?.clientWidth ?? 613) / 613), y: (centers[name]?.y ?? 200) * ((boxRef.current?.clientWidth ?? 613) / 613) })}
              onBlur={() => setHover(null)}
              className="county-shape cursor-pointer outline-none"
              style={{ fill: fillFor(counts[name], max), animationDelay: `${index * 18}ms` }}
              stroke="var(--color-slate-50)"
              strokeWidth="1"
            />
          ))}

          {/* The hovered and the selected county are drawn again on top, lifted */}
          {[selected, hovered].filter(Boolean).filter((name, i, all) => all.indexOf(name) === i).map((name) => (
            <path
              key={`top-${name}`}
              d={COUNTY_PATHS[name]}
              className="pointer-events-none"
              style={{ fill: fillFor(counts[name], max), transform: 'translateY(-2px)' }}
              stroke={name === selected ? 'var(--color-fuchsia-500)' : 'var(--color-indigo-400)'}
              strokeWidth={name === selected ? 2.5 : 2}
              filter="url(#county-lift)"
            />
          ))}

          {/* Number badges on the counties with the most auctions */}
          {top.map((c) => {
            const center = centers[c.county]
            if (!center) return null
            return (
              <g key={`badge-${c.county}`} className="pointer-events-none" transform={`translate(${center.x} ${center.y})`}>
                {/* Fixed colors: the badge must read the same in light and dark mode */}
                <circle r="9" fill="#ffffff" opacity="0.94" />
                <text textAnchor="middle" dy="3.5" fontSize="10" fontWeight="700" fill="#3730a3">
                  {c.items}
                </text>
              </g>
            )
          })}

          {/* Live bids: a ring grows from the county */}
          {pulses.map((pulse) => {
            const center = centers[pulse.county]
            if (!center) return null
            return (
              <g key={pulse.id} className="pointer-events-none" transform={`translate(${center.x} ${center.y})`}>
                <circle r="10" fill="none" stroke="var(--color-emerald-500)" strokeWidth="2.5" className="county-pulse" />
                <circle r="4" fill="var(--color-emerald-500)" />
              </g>
            )
          })}
        </svg>

        {hover && (
          <div
            className="stay-dark pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-full rounded-xl bg-slate-900/95 px-3 py-2 text-xs whitespace-nowrap text-white shadow-xl ring-1 ring-white/10"
            style={{ left: hover.x, top: hover.y - 14 }}
          >
            <span className="block text-sm font-semibold">{hover.name}</span>
            <span className="text-slate-300">
              {counts[hover.name] ? `${counts[hover.name]} ${counts[hover.name] === 1 ? 'licitatie activa' : 'licitatii active'}` : 'Nicio licitatie acum'}
            </span>
            {counts[hover.name] > 0 && <span className="mt-0.5 block font-medium text-indigo-300">Apasa ca sa le vezi →</span>}
          </div>
        )}
      </div>

      <aside>
        <p className="text-3xl font-bold tracking-tight text-slate-900">
          {total} <span className="text-base font-medium text-slate-500">licitatii active</span>
        </p>
        <p className="text-sm text-slate-500">in {withItems} din 42 de judete</p>

        {/* Legend for the color scale */}
        <div className="mt-5">
          <div className="h-2 rounded-full" style={{ background: `linear-gradient(to right, ${fillFor(1, 50)}, ${fillFor(max, max)})` }} />
          <div className="mt-1 flex justify-between text-[11px] text-slate-500">
            <span>putine</span>
            <span className="flex items-center gap-1">
              <span className="inline-block h-2.5 w-2.5 rounded-sm" style={{ background: 'var(--color-slate-200)' }} /> niciuna
            </span>
            <span>{max} max</span>
          </div>
        </div>

        <p className="mt-5 text-xs font-semibold tracking-wide text-slate-500 uppercase">Cele mai active judete</p>
        <ul className="mt-2 space-y-1.5">
          {top.map((c) => (
            <li key={c.county}>
              <button
                onClick={() => pick(c.county)}
                onPointerEnter={() => setHover(null)}
                className={`group flex w-full items-center gap-2 rounded-lg px-2 py-1 text-left text-sm transition ${
                  selected === c.county ? 'bg-fuchsia-50 ring-1 ring-fuchsia-300' : 'hover:bg-indigo-50'
                }`}
              >
                <span className="w-24 shrink-0 truncate font-medium text-slate-800">{c.county}</span>
                <span className="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-200">
                  <span className="block h-full rounded-full bg-indigo-600 transition-all" style={{ width: `${(c.items / max) * 100}%` }} />
                </span>
                <span className="w-6 text-right text-xs font-semibold text-slate-600 tabular-nums">{c.items}</span>
              </button>
            </li>
          ))}
        </ul>
        <p className="mt-4 flex items-center gap-1.5 text-xs text-slate-500">
          <Icon name="pin" size={14} />
          {selected ? (
            <>
              Afisezi doar <b className="text-slate-700">{selected}</b> ·{' '}
              <button onClick={() => onSelect('')} className="font-semibold text-indigo-600 hover:text-indigo-500">
                toata tara
              </button>
            </>
          ) : (
            'Apasa pe un judet ca sa vezi licitatiile de acolo.'
          )}
        </p>
      </aside>
    </div>
  )
}
