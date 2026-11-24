import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { api, timeAgo } from '../api.js'
import { useAuth } from '../auth.jsx'
import { useAuctionEvents } from '../events.jsx'

const ICONS = { OUTBID: '📉', NEW_BID: '💰', WON: '🏆', SOLD: '🤝', UNSOLD: '⏳' }

// Clopotelul din bara de sus: notificarile utilizatorului logat, cu numarul celor necitite.
// Lista se reincarca instant cand serverul anunta o notificare noua pentru acest utilizator.
export default function NotificationBell() {
  const { user } = useAuth()
  const [data, setData] = useState({ unread: 0, items: [] })
  const [open, setOpen] = useState(false)
  const [ring, setRing] = useState(0)
  const boxRef = useRef(null)

  const load = useCallback(() => {
    api('/notifications')
      .then(setData)
      .catch(() => {})
  }, [])

  useEffect(() => {
    if (user) load()
  }, [user, load])

  useAuctionEvents((event) => {
    if (event.type === 'notification' && event.username === user?.username) {
      load()
      setRing((n) => n + 1) // clopotelul "suna" (animatie)
    }
  })

  // Click in afara panoului il inchide
  useEffect(() => {
    if (!open) return
    const close = (event) => {
      if (!boxRef.current?.contains(event.target)) setOpen(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [open])

  function toggle() {
    const next = !open
    setOpen(next)
    // La deschidere, notificarile devin citite (iar cele noi raman evidentiate cat timp panoul e deschis)
    if (next && data.unread > 0) {
      api('/notifications/read-all', { method: 'POST' })
        .then((fresh) => setData({ ...fresh, items: data.items }))
        .catch(() => {})
    }
  }

  if (!user) return null
  const now = Date.now()

  return (
    <div ref={boxRef} className="relative">
      <button
        type="button"
        onClick={toggle}
        aria-label={`Notificari${data.unread ? ` (${data.unread} necitite)` : ''}`}
        className="relative grid h-9 w-9 place-items-center rounded-full bg-white/80 text-slate-600 ring-1 ring-slate-200 transition hover:text-indigo-600 hover:ring-indigo-300"
      >
        <span key={ring} className={ring ? 'animate-bell' : ''}>
          <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" />
          </svg>
        </span>
        {data.unread > 0 && (
          <span className="absolute -top-1 -right-1 grid h-5 min-w-5 place-items-center rounded-full bg-gradient-to-r from-rose-500 to-fuchsia-500 px-1 text-[10px] font-bold text-white shadow-md">
            {data.unread > 9 ? '9+' : data.unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-40 mt-2 w-[22rem] max-w-[calc(100vw-2rem)] origin-top-right animate-fade-up overflow-hidden rounded-2xl bg-white/95 shadow-2xl ring-1 ring-slate-200 backdrop-blur-xl">
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
            <p className="font-semibold text-slate-900">Notificari</p>
            <span className="text-xs text-slate-400">ultimele 30</span>
          </div>
          {data.items.length === 0 ? (
            <p className="px-4 py-10 text-center text-sm text-slate-500">Nicio notificare inca.</p>
          ) : (
            <ul className="max-h-96 divide-y divide-slate-100 overflow-y-auto">
              {data.items.map((n) => (
                <li key={n.id}>
                  <Link
                    to={n.itemId ? `/items/${n.itemId}` : '/account'}
                    onClick={() => setOpen(false)}
                    className={`flex gap-3 px-4 py-3 transition hover:bg-slate-50 ${n.read ? '' : 'bg-indigo-50/60'}`}
                  >
                    <span className="text-lg leading-6">{ICONS[n.type] ?? '🔔'}</span>
                    <span className="min-w-0 flex-1">
                      <span className="block text-sm text-slate-700">{n.message}</span>
                      <span className="mt-0.5 block text-xs text-slate-400">{timeAgo(n.createdAt, now)}</span>
                    </span>
                    {!n.read && <span className="mt-2 h-2 w-2 shrink-0 rounded-full bg-indigo-500" />}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
