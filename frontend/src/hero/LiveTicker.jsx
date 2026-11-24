import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { formatPrice, timeAgo } from '../api.js'
import { useT } from '../i18n.jsx'

// One-line "latest bid" pill shown under the 3D scenes; slides in again on every new bid
export default function LiveTicker({ bids }) {
  const t = useT()
  const [now, setNow] = useState(Date.now())
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 5000)
    return () => clearInterval(id)
  }, [])

  const latest = bids[0]
  if (!latest) return null
  return (
    <Link
      key={latest.id}
      to={`/items/${latest.item.id}`}
      className="pointer-events-auto inline-flex max-w-full animate-slide-in items-center gap-2 rounded-full bg-white/10 px-3 py-1.5 text-xs text-indigo-50 ring-1 ring-white/15 backdrop-blur transition hover:bg-white/15"
    >
      <span className="relative flex h-2 w-2 shrink-0">
        <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
        <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
      </span>
      <span className="truncate">
        <b>{latest.bidder?.username}</b> {t('a oferit')} <b className="text-emerald-300">{formatPrice(latest.amount)}</b> ·{' '}
        {latest.item.name}
      </span>
      <span className="shrink-0 text-indigo-200/60">{timeAgo(latest.createdAt, now)}</span>
    </Link>
  )
}
