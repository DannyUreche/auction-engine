import { useCallback, useEffect, useRef, useState } from 'react'
import { api, formatDate } from '../api.js'
import { useAuctionEvents } from '../events.jsx'
import { Alert } from './ui.jsx'

// Chat-ul dintre castigator si vanzator (dupa vanzare). Mesajele noi apar instant:
// serverul anunta destinatarul printr-un eveniment live "message", iar chat-ul isi reincarca mesajele.
export default function ChatBox({ item, user }) {
  const [messages, setMessages] = useState(null)
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)
  const listRef = useRef(null)
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
    if (event.type === 'message' && event.itemId === item.id && event.username === user.username) load()
  })

  // Derulam la ultimul mesaj
  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages])

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
        <h2 className="text-lg font-semibold text-slate-900">💬 Mesaje cu {other}</h2>
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
                <p className={`mt-0.5 text-[10px] ${mine ? 'text-indigo-100' : 'text-slate-400'}`}>{formatDate(m.createdAt)}</p>
              </div>
            </div>
          )
        })}
      </div>

      <form onSubmit={send} className="mt-3 flex gap-2">
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
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
