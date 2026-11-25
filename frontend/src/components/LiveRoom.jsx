import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { api, formatPrice, getToken, timeAgo } from '../api.js'
import { useNow } from './Countdown.jsx'
import { useAuctionEvents } from '../events.jsx'
import { Icon } from './icons.jsx'
import { Button, Card, Input } from './ui.jsx'

// One random id per browser tab, so the server can count each open page once
function tabId() {
  let id = sessionStorage.getItem('tab-id')
  if (!id) {
    id = Math.random().toString(36).slice(2) + Date.now().toString(36)
    sessionStorage.setItem('tab-id', id)
  }
  return id
}

const HEARTBEAT_MS = 15000

// Live auction room: how many people are watching right now, the public live chat and every new bid
// floating up as a bubble
export default function LiveRoom({ item, user }) {
  const [viewers, setViewers] = useState(null)
  const [comments, setComments] = useState([])
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)
  const [bursts, setBursts] = useState([])
  const listRef = useRef(null)
  const now = useNow()
  const itemId = item.id

  // Presence heartbeat while the page is open; leave when it closes
  useEffect(() => {
    const session = tabId()
    const beat = () => {
      if (document.visibilityState === 'hidden') return
      const headers = { 'Content-Type': 'application/json' }
      if (getToken()) headers.Authorization = `Bearer ${getToken()}`
      fetch(`/api/items/${itemId}/presence`, { method: 'POST', headers, body: JSON.stringify({ session }) })
        .then((response) => (response.ok ? response.json() : null))
        .then((data) => data && setViewers(data.viewers))
        .catch(() => {})
    }
    const leave = () =>
      navigator.sendBeacon?.(`/api/items/${itemId}/presence/leave`, new Blob([JSON.stringify({ session })], { type: 'application/json' }))
    beat()
    const id = setInterval(beat, HEARTBEAT_MS)
    document.addEventListener('visibilitychange', beat)
    window.addEventListener('pagehide', leave)
    return () => {
      clearInterval(id)
      document.removeEventListener('visibilitychange', beat)
      window.removeEventListener('pagehide', leave)
      leave()
    }
  }, [itemId])

  const loadComments = () =>
    api(`/items/${itemId}/comments`, { auth: false })
      .then(setComments)
      .catch(() => {})

  useEffect(() => {
    loadComments()
  }, [itemId]) // eslint-disable-line react-hooks/exhaustive-deps

  useAuctionEvents((event) => {
    if (event.itemId !== itemId) return
    if (event.type === 'comment') loadComments()
    if (event.type === 'bid') {
      const burst = { id: `${Date.now()}-${Math.random()}`, amount: event.amount, username: event.username, x: 10 + Math.random() * 60 }
      setBursts((current) => [...current.slice(-4), burst])
      setTimeout(() => setBursts((current) => current.filter((b) => b.id !== burst.id)), 3300)
    }
  })

  // Keep the newest message in view
  useEffect(() => {
    const list = listRef.current
    if (list) list.scrollTop = list.scrollHeight
  }, [comments])

  async function send(event) {
    event.preventDefault()
    if (!text.trim()) return
    setSending(true)
    setError('')
    try {
      const comment = await api(`/items/${itemId}/comments`, { method: 'POST', body: { text } })
      setComments((current) => (current.some((c) => c.id === comment.id) ? current : [...current, comment]))
      setText('')
    } catch (err) {
      setError(err.message)
    } finally {
      setSending(false)
    }
  }

  return (
    <Card className="relative overflow-hidden">
      {/* New bids float up over the room */}
      <div aria-hidden="true" className="pointer-events-none absolute inset-x-0 bottom-16 z-10 h-40">
        {bursts.map((burst) => (
          <span
            key={burst.id}
            style={{ left: `${burst.x}%` }}
            className="absolute bottom-0 animate-rise rounded-full bg-gradient-to-r from-emerald-500 to-teal-500 px-3 py-1 text-sm font-bold whitespace-nowrap text-white shadow-lg shadow-emerald-500/30"
          >
            +{formatPrice(burst.amount)} · {burst.username}
          </span>
        ))}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-900">
          <Icon name="chat" size={22} /> Camera live
        </h2>
        {viewers != null && (
          <span className="inline-flex items-center gap-2 rounded-full bg-teal-50 px-3 py-1 text-sm font-medium text-teal-700 ring-1 ring-teal-200">
            <span className="relative flex h-2 w-2">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-teal-400 opacity-75" />
              <span className="relative inline-flex h-2 w-2 rounded-full bg-teal-500" />
            </span>
            <Icon name="eye" size={16} /> {viewers === 1 ? 'Doar tu urmaresti acum' : `${viewers} persoane urmaresc acum`}
          </span>
        )}
      </div>

      <ul ref={listRef} className="mt-4 max-h-72 space-y-2.5 overflow-y-auto pr-1">
        {comments.length === 0 && <li className="py-6 text-center text-sm text-slate-500">Niciun mesaj inca. Spune ceva!</li>}
        {comments.map((comment) => {
          const fromSeller = comment.authorUsername === item.seller?.username
          return (
            <li key={comment.id} className="flex gap-2.5 text-sm">
              <span
                className={`grid h-7 w-7 shrink-0 place-items-center rounded-full text-xs font-bold text-white ${
                  fromSeller ? 'bg-gradient-to-br from-amber-500 to-orange-500' : 'bg-gradient-to-br from-indigo-500 to-fuchsia-500'
                }`}
              >
                {comment.authorUsername[0]?.toUpperCase()}
              </span>
              <span className="min-w-0">
                <span className="font-semibold text-slate-900">{comment.authorUsername}</span>
                {fromSeller && <span className="ml-1.5 rounded bg-amber-100 px-1.5 py-0.5 text-[10px] font-semibold text-amber-800">VANZATOR</span>}
                <span className="ml-1.5 text-xs text-slate-400">{timeAgo(comment.createdAt, now)}</span>
                <span className="block break-words text-slate-700">{comment.text}</span>
              </span>
            </li>
          )
        })}
      </ul>

      {item.active ? (
        user ? (
          <form onSubmit={send} className="mt-4 flex gap-2">
            <Input value={text} onChange={(e) => setText(e.target.value)} maxLength={300} placeholder="Scrie in camera live..." aria-label="Mesaj in camera live" />
            <Button type="submit" loading={sending} disabled={!text.trim()} className="shrink-0">
              Trimite
            </Button>
          </form>
        ) : (
          <p className="mt-4 text-sm text-slate-500">
            <Link to="/login" state={{ from: `/items/${itemId}` }} className="font-semibold text-indigo-600 hover:text-indigo-500">
              Intra in cont
            </Link>{' '}
            ca sa scrii in camera live.
          </p>
        )
      ) : (
        <p className="mt-4 text-sm text-slate-500">Licitatia s-a incheiat, camera live este inchisa.</p>
      )}
      {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
    </Card>
  )
}
