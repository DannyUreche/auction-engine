import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api.js'
import { Icon } from './icons.jsx'

// Home page strip shown only while a live show is running
export default function LiveNowBanner() {
  const [show, setShow] = useState(null)
  useEffect(() => {
    const load = () =>
      api('/shows', { auth: false })
        .then((shows) => setShow(shows.find((s) => s.status === 'LIVE') ?? null))
        .catch(() => {})
    load()
    const id = setInterval(load, 30000)
    return () => clearInterval(id)
  }, [])
  if (!show) return null
  return (
    <Link
      to={`/live/${show.id}`}
      className="stay-dark group mt-6 flex animate-fade-up items-center gap-4 overflow-hidden rounded-2xl bg-gradient-to-r from-red-600 via-rose-600 to-fuchsia-600 px-5 py-3.5 text-white shadow-lg shadow-red-500/25 transition hover:-translate-y-0.5"
    >
      <span className="inline-flex shrink-0 items-center gap-2 rounded-full bg-white/20 px-3 py-1 text-xs font-black tracking-widest">
        <span className="h-2 w-2 animate-pulse rounded-full bg-white" /> LIVE ACUM
      </span>
      <span className="min-w-0 flex-1 truncate font-semibold">
        {show.title} <span className="font-normal text-white/80">· cu {show.sellerUsername}</span>
      </span>
      <span className="hidden shrink-0 items-center gap-1.5 text-sm font-bold sm:flex">
        <Icon name="gavel" size={16} mono /> Intra in sala →
      </span>
    </Link>
  )
}
