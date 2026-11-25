import { Link, useLocation } from 'react-router'
import { MAX_COMPARE, useCompare } from '../compare.js'
import { Icon } from './icons.jsx'
import ItemImage from './ItemImage.jsx'

// Button on item cards: add to / remove from the comparison (the card itself is a link, so no navigation)
export function CompareToggle({ item }) {
  const compare = useCompare()
  const selected = compare.has(item.id)
  const disabled = !selected && compare.full
  return (
    <button
      type="button"
      title={selected ? 'Scoate din comparatie' : disabled ? `Poti compara maxim ${MAX_COMPARE} obiecte` : 'Adauga la comparatie'}
      aria-pressed={selected}
      disabled={disabled}
      onClick={(event) => {
        event.preventDefault()
        event.stopPropagation()
        compare.toggle(item)
      }}
      className={`grid h-9 w-9 place-items-center rounded-full shadow-md backdrop-blur transition disabled:opacity-40 ${
        selected ? 'bg-indigo-600 text-white ring-2 ring-white' : 'bg-white/90 text-slate-600 hover:scale-110 hover:text-indigo-600'
      }`}
    >
      <Icon name="columns" size={18} mono={selected} />
    </button>
  )
}

// Floating tray at the bottom of the screen with the picked items and the "Compara" button
export default function CompareTray() {
  const compare = useCompare()
  const location = useLocation()
  if (compare.items.length === 0 || location.pathname === '/compara') return null
  const slots = Array.from({ length: MAX_COMPARE }, (_, index) => compare.items[index])

  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-4 z-30 flex justify-center px-4">
      <div className="stay-dark pointer-events-auto flex animate-fade-up items-center gap-3 rounded-2xl bg-slate-900/95 p-2.5 pl-4 text-white shadow-2xl ring-1 shadow-indigo-950/40 ring-white/10 backdrop-blur">
        <span className="hidden items-center gap-2 text-sm font-semibold sm:flex">
          <Icon name="columns" size={20} /> Compara
        </span>
        <div className="flex gap-2">
          {slots.map((item, index) =>
            item ? (
              <span key={item.id} className="group relative">
                <ItemImage item={item} small className="h-11 w-11 rounded-xl ring-2 ring-indigo-400" />
                <button
                  onClick={() => compare.remove(item.id)}
                  aria-label={`Scoate ${item.name}`}
                  className="absolute -top-1.5 -right-1.5 grid h-5 w-5 place-items-center rounded-full bg-rose-500 text-xs font-bold text-white shadow"
                >
                  ×
                </button>
              </span>
            ) : (
              <span key={`empty-${index}`} className="grid h-11 w-11 place-items-center rounded-xl border-2 border-dashed border-white/25 text-lg text-white/30">
                +
              </span>
            ),
          )}
        </div>
        <Link
          to={`/compara?ids=${compare.items.map((item) => item.id).join(',')}`}
          className={`rounded-xl px-4 py-2.5 text-sm font-semibold transition ${
            compare.items.length >= 2 ? 'bg-gradient-to-r from-indigo-500 to-violet-500 shadow-lg shadow-indigo-500/30 hover:brightness-110' : 'pointer-events-none bg-white/10 text-white/50'
          }`}
        >
          {compare.items.length >= 2 ? `Compara (${compare.items.length})` : 'Alege inca unul'}
        </Link>
        <button onClick={compare.clear} className="px-1 text-xs text-white/50 hover:text-white" aria-label="Goleste comparatia">
          Goleste
        </button>
      </div>
    </div>
  )
}
