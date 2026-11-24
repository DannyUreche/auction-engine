import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, formatPrice, parseDate } from '../api.js'
import { useAuctionEvents } from '../events.jsx'

// "acum 12s", "acum 3 min"...
function timeAgo(value, now) {
  const seconds = Math.max(0, Math.round((now - parseDate(value).getTime()) / 1000))
  if (seconds < 60) return `acum ${seconds}s`
  if (seconds < 3600) return `acum ${Math.floor(seconds / 60)} min`
  if (seconds < 86400) return `acum ${Math.floor(seconds / 3600)} h`
  return `acum ${Math.floor(seconds / 86400)} z`
}

// Ultimele oferte de pe tot site-ul, actualizate in timp real (pe fundal inchis, in partea de sus a paginii)
export default function ActivityFeed() {
  const [bids, setBids] = useState(null)
  const [now, setNow] = useState(Date.now())

  const load = useCallback(() => {
    api('/bids/recent', { auth: false })
      .then(setBids)
      .catch(() => setBids([]))
  }, [])

  useEffect(() => {
    load()
    const id = setInterval(() => setNow(Date.now()), 5000)
    return () => clearInterval(id)
  }, [load])

  useAuctionEvents((event) => {
    if (event.type === 'bid' || event.type === 'deleted') load()
  })

  return (
    <div className="rounded-2xl bg-white/[0.06] p-4 ring-1 ring-white/10 backdrop-blur">
      <div className="mb-3 flex items-center justify-between">
        <p className="text-sm font-semibold text-white">Activitate live</p>
        <span className="relative flex h-2.5 w-2.5">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
          <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-emerald-400" />
        </span>
      </div>

      {bids === null && (
        <div className="space-y-2">
          {[0, 1, 2].map((i) => (
            <div key={i} className="h-11 rounded-xl bg-white/5" />
          ))}
        </div>
      )}
      {bids?.length === 0 && <p className="py-6 text-center text-sm text-indigo-200/70">Inca nu exista oferte. Fii primul!</p>}

      <ul className="space-y-1.5">
        {bids?.slice(0, 5).map((bid) => (
          <li key={bid.id} className="animate-slide-in">
            <Link
              to={`/items/${bid.item.id}`}
              className="flex items-center gap-3 rounded-xl px-2.5 py-2 transition hover:bg-white/10"
            >
              <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-gradient-to-br from-indigo-400 to-fuchsia-500 text-xs font-bold text-white">
                {bid.bidder?.username?.[0]?.toUpperCase()}
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm text-indigo-50">
                  <span className="font-semibold">{bid.bidder?.username}</span> a oferit
                </span>
                <span className="block truncate text-xs text-indigo-200/70">{bid.item.name}</span>
              </span>
              <span className="shrink-0 text-right">
                <span className="block text-sm font-semibold text-emerald-300">{formatPrice(bid.amount)}</span>
                <span className="block text-[11px] text-indigo-200/60">{timeAgo(bid.createdAt, now)}</span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
