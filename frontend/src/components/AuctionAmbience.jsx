import { useEffect, useRef, useState } from 'react'
import { formatPrice } from '../api.js'
import { useLiveBids } from '../hero/useLiveBids.js'
import { Icon } from './icons.jsx'

// Quiet auction atmosphere in the empty margins of wide screens: the site's real latest bids, numbered bid
// paddles, gavels, coins and now and then a "VANDUT!" stamp drift slowly upwards. A new live bid appears right
// away, highlighted. Only on screens wider than 1500px (where the margins are empty), never over the content,
// never clickable, and off when the visitor prefers reduced motion.
const MAX_PER_LANE = 6
const ICONS = ['gavel', 'coins', 'trophy', 'timer', 'bolt', 'tagCheck']

let nextId = 1

export default function AuctionAmbience() {
  const [items, setItems] = useState([])
  const bidsRef = useRef([])
  const bidIndex = useRef(0)
  const lane = useRef(0)
  const enabled = useRef(false)

  const spawn = (kind, data = {}) => {
    if (!enabled.current || document.visibilityState === 'hidden') return
    const side = lane.current++ % 2 === 0 ? 'left' : 'right'
    const item = {
      id: nextId++,
      side,
      kind,
      data,
      // Bid labels are wide: keep them near the outer edge so they never get cut off
      x: kind === 'bid' ? 4 + Math.random() * 26 : 8 + Math.random() * 62,
      duration: 16 + Math.random() * 8,
      scale: 0.85 + Math.random() * 0.35,
      sway: (Math.random() - 0.5) * 60,
    }
    setItems((current) => {
      const sameSide = current.filter((existing) => existing.side === side)
      const kept = sameSide.length >= MAX_PER_LANE ? current.filter((existing) => existing.id !== sameSide[0].id) : current
      return [...kept, item]
    })
    setTimeout(() => setItems((current) => current.filter((existing) => existing.id !== item.id)), item.duration * 1000)
  }

  const bids = useLiveBids((event) => spawn('bid', { amount: event.amount, username: event.username, live: true }))
  bidsRef.current = bids

  useEffect(() => {
    const wide = window.matchMedia('(min-width: 1500px)')
    const calm = window.matchMedia('(prefers-reduced-motion: reduce)')
    const update = () => {
      enabled.current = wide.matches && !calm.matches
    }
    update()
    wide.addEventListener('change', update)

    const tick = () => {
      const roll = Math.random()
      const recent = bidsRef.current
      if (roll < 0.45 && recent.length) {
        const bid = recent[bidIndex.current++ % recent.length]
        spawn('bid', { amount: bid.amount, username: bid.bidder?.username })
      } else if (roll < 0.65) {
        spawn('paddle', { number: 10 + Math.floor(Math.random() * 90) })
      } else if (roll < 0.93) {
        spawn('icon', { name: ICONS[Math.floor(Math.random() * ICONS.length)] })
      } else {
        spawn('sold')
      }
    }
    const first = setTimeout(tick, 600)
    const id = setInterval(tick, 2800)
    return () => {
      clearTimeout(first)
      clearInterval(id)
      wide.removeEventListener('change', update)
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div aria-hidden="true" className="pointer-events-none fixed inset-0 -z-[5] hidden overflow-hidden min-[1500px]:block">
      {['left', 'right'].map((side) => (
        <div
          key={side}
          className={`absolute inset-y-0 ${side === 'left' ? 'left-0' : 'right-0'}`}
          style={{ width: 'calc((100vw - 72rem) / 2)' }}
        >
          {items
            .filter((item) => item.side === side)
            .map((item) => (
              <div
                key={item.id}
                className="ambience-float absolute bottom-0"
                style={{ left: `${item.x}%`, animationDuration: `${item.duration}s`, '--sway': `${item.sway}px`, '--scale': item.scale }}
              >
                <Floating item={item} />
              </div>
            ))}
        </div>
      ))}
    </div>
  )
}

function Floating({ item }) {
  if (item.kind === 'bid') {
    return (
      <span
        className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold whitespace-nowrap shadow-lg ring-1 ${
          item.data.live
            ? 'bg-gradient-to-r from-emerald-500 to-teal-500 text-white shadow-emerald-500/30 ring-white/30'
            : 'bg-white/80 text-slate-700 shadow-indigo-500/10 ring-slate-200 backdrop-blur'
        }`}
      >
        <Icon name="gavel" size={14} mono={item.data.live} /> +{formatPrice(item.data.amount)}
        {item.data.username && <span className="font-normal opacity-70">· {item.data.username}</span>}
      </span>
    )
  }
  if (item.kind === 'paddle') {
    return (
      <span className="flex flex-col items-center opacity-80">
        <span className="grid h-11 w-11 place-items-center rounded-full bg-gradient-to-br from-indigo-500 to-violet-600 text-sm font-bold text-white shadow-lg shadow-indigo-500/30">
          {item.data.number}
        </span>
        <span className="h-7 w-1.5 rounded-full bg-slate-400" />
      </span>
    )
  }
  if (item.kind === 'sold') {
    return (
      <span className="inline-block -rotate-12 rounded-lg border-2 border-rose-500 px-2.5 py-1 text-sm font-black tracking-widest text-rose-500 opacity-80">
        VANDUT!
      </span>
    )
  }
  return (
    <span className="grid h-12 w-12 place-items-center rounded-2xl bg-white/80 shadow-md ring-1 ring-slate-200 backdrop-blur">
      <Icon name={item.data.name} size={28} />
    </span>
  )
}
