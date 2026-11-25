import { useCallback, useEffect, useRef, useState } from 'react'
import { api, formatDate } from '../api.js'
import { useAuctionEvents } from '../events.jsx'
import { Icon } from './icons.jsx'
import { Alert } from './ui.jsx'

// Chat-ul dintre castigator si vanzator (dupa vanzare). Mesajele noi apar instant:
// serverul anunta destinatarul printr-un eveniment live "message", iar chat-ul isi reincarca mesajele.
// Live extras: "... scrie" while the other one types (SSE "typing") and read receipts - a message counts as read
// when the recipient has the chat on screen (POST .../messages/read, SSE "read" back to the sender).
export default function ChatBox({ item, user }) {
  const [messages, setMessages] = useState(null)
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)
  const listRef = useRef(null)
  const [typing, setTyping] = useState(false)
  const typingTimer = useRef(null)
  const lastTypingSent = useRef(0)
  const other = user.id === item.seller?.id ? item.winner?.username : item.seller?.username

  const load = useCallback(() => {
    api(`/items/${item.id}/messages`)
      .then(setMessages)
      .catch((err) => setError(err.message))
  }, [item.id])

  useEffect(() => {
    load()
  }, [load])

  useAuctionEvents((event) => {
    if (event.itemId !== item.id || event.username !== user.username) return
    if (event.type === 'message') {
      setTyping(false)
      load()
    } else if (event.type === 'read') {
      load()
    } else if (event.type === 'typing') {
      setTyping(true)
      clearTimeout(typingTimer.current)
      typingTimer.current = setTimeout(() => setTyping(false), 4000)
    }
  })
  useEffect(() => () => clearTimeout(typingTimer.current), [])

  // Messages from the other participant that I haven't read: mark them read while the chat is visible
  const unread = messages?.some((m) => m.senderUsername !== user.username && !m.readAt)
  useEffect(() => {
    if (!unread) return
    const markRead = () => {
      if (document.visibilityState !== 'visible') return
      api(`/items/${item.id}/messages/read`, { method: 'POST' })
        .then(() => setMessages((list) => list?.map((m) => (m.senderUsername !== user.username && !m.readAt ? { ...m, readAt: new Date().toISOString() } : m))))
        .catch(() => {})
    }
    markRead()
    document.addEventListener('visibilitychange', markRead)
    return () => document.removeEventListener('visibilitychange', markRead)
  }, [unread, item.id, user.username])

  function onType(value) {
    setText(value)
    const now = Date.now()
    if (value.trim() && now - lastTypingSent.current > 2500) {
      lastTypingSent.current = now
      api(`/items/${item.id}/messages/typing`, { method: 'POST' }).catch(() => {})
    }
  }

  // "Citit" under the conversation when the other one has read my latest message
  const myLast = messages?.filter((m) => m.senderUsername === user.username).at(-1)

  // Derulam la ultimul mesaj
  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, typing])

  async function send(event) {
    event.preventDefault()
    if (!text.trim()) return
    setSending(true)
    setError('')
    try {
      const message = await api(`/items/${item.id}/messages`, { method: 'POST', body: { text } })
      setMessages((list) => [...(list ?? []), message])
      setText('')
    } catch (err) {
      setError(err.message)
    } finally {
      setSending(false)
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-900">
          <Icon name="chat" size={24} /> Mesaje cu {other}
        </h2>
        <span className="text-xs text-slate-400">vizibile doar pentru voi doi</span>
      </div>

      <div ref={listRef} className="mt-4 max-h-80 min-h-32 space-y-2 overflow-y-auto rounded-xl bg-slate-50 p-3 ring-1 ring-slate-200">
        {messages?.length === 0 && (
          <p className="py-8 text-center text-sm text-slate-400">Scrie primul mesaj ca sa stabiliti plata si livrarea.</p>
        )}
        {messages?.map((m) => {
          const mine = m.senderUsername === user.username
          return (
            <div key={m.id} className={`flex animate-fade-up ${mine ? 'justify-end' : 'justify-start'}`}>
              <div
                className={`max-w-[80%] rounded-2xl px-3.5 py-2 text-sm shadow-sm ${
                  mine
                    ? 'rounded-br-md bg-gradient-to-r from-indigo-600 to-violet-600 text-white'
                    : 'rounded-bl-md bg-white text-slate-800 ring-1 ring-slate-200'
                }`}
              >
                <p className="break-words whitespace-pre-line">{m.text}</p>
                <p className={`mt-0.5 flex items-center justify-end gap-1 text-[10px] ${mine ? 'text-white/70' : 'text-slate-400'}`}>
                  {formatDate(m.createdAt)}
                  {mine && (
                    <span className={m.readAt ? 'font-bold' : ''} style={m.readAt ? { color: '#7dd3fc' } : undefined} title={m.readAt ? `Citit ${formatDate(m.readAt)}` : 'Trimis'}>
                      {m.readAt ? '✓✓' : '✓'}
                    </span>
                  )}
                </p>
              </div>
            </div>
          )
        })}
        {myLast?.readAt && messages.at(-1)?.id === myLast.id && (
          <p className="text-right text-[11px] font-medium text-slate-400">Citit {formatDate(myLast.readAt)}</p>
        )}
        {typing && (
          <div className="flex animate-fade-up items-center gap-2" aria-live="polite">
            <span className="flex items-center gap-1 rounded-2xl rounded-bl-md bg-white px-3.5 py-3 shadow-sm ring-1 ring-slate-200">
              {[0, 1, 2].map((dot) => (
                <span key={dot} className="typing-dot h-1.5 w-1.5 rounded-full bg-slate-400" style={{ animationDelay: `${dot * 160}ms` }} />
              ))}
            </span>
            <span className="text-xs text-slate-400">{other} scrie...</span>
          </div>
        )}
      </div>

      <form onSubmit={send} className="mt-3 flex gap-2">
        <input
          value={text}
          onChange={(e) => onType(e.target.value)}
          maxLength={1000}
          placeholder="Scrie un mesaj..."
          className="block w-full rounded-lg border-0 bg-white px-3.5 py-2.5 text-sm text-slate-900 ring-1 ring-slate-300 ring-inset placeholder:text-slate-400 focus:ring-2 focus:ring-indigo-600 focus:outline-none"
        />
        <button
          type="submit"
          disabled={sending || !text.trim()}
          className="shrink-0 rounded-lg bg-gradient-to-r from-indigo-600 to-violet-600 px-4 text-sm font-semibold text-white shadow-md transition hover:brightness-110 disabled:opacity-50"
        >
          Trimite
        </button>
      </form>
      {error && (
        <div className="mt-2">
          <Alert>{error}</Alert>
        </div>
      )}
    </div>
  )
}
