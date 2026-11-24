import { useCallback, useEffect, useState } from 'react'
import { api, formatDate } from '../api.js'
import { useAuctionEvents } from '../events.jsx'
import { Icon } from './icons.jsx'
import { Alert, Button, Card, Textarea } from './ui.jsx'

// Public questions about the item; the seller answers each one once
export default function Questions({ item, user }) {
  const [questions, setQuestions] = useState(null)
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)
  const isSeller = user?.id === item.seller?.id

  const load = useCallback(() => {
    api(`/items/${item.id}/questions`, { auth: false })
      .then(setQuestions)
      .catch(() => setQuestions([]))
  }, [item.id])

  useEffect(() => {
    load()
  }, [load])

  // A new question / answer for this item arrives as a notification to the other person; reload on it
  useAuctionEvents((event) => {
    if (event.type === 'notification' && event.username === user?.username) load()
  })

  async function ask(event) {
    event.preventDefault()
    setSending(true)
    setError('')
    try {
      await api(`/items/${item.id}/questions`, { method: 'POST', body: { text } })
      setText('')
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSending(false)
    }
  }

  if (!questions) return null
  return (
    <Card>
      <div className="flex items-center justify-between">
        <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-900">
          <Icon name="chat" size={24} /> Intrebari si raspunsuri
        </h2>
        <span className="text-sm text-slate-500">{questions.length}</span>
      </div>

      {questions.length === 0 && <p className="mt-3 text-sm text-slate-500">Nicio intrebare inca.</p>}
      <ul className="mt-4 space-y-4">
        {questions.map((q) => (
          <li key={q.id} className="animate-fade-up">
            <p className="text-sm text-slate-800">
              <span className="font-semibold">{q.authorUsername}:</span> {q.text}
            </p>
            <p className="text-xs text-slate-400">{formatDate(q.createdAt)}</p>
            {q.answer ? (
              <p className="mt-2 rounded-xl bg-indigo-50 px-3 py-2 text-sm text-indigo-900 ring-1 ring-indigo-100">
                <span className="font-semibold">Raspunsul vanzatorului:</span> {q.answer}
              </p>
            ) : isSeller ? (
              <AnswerForm itemId={item.id} questionId={q.id} onDone={load} />
            ) : (
              <p className="mt-1 text-xs text-slate-400 italic">Asteapta raspunsul vanzatorului</p>
            )}
          </li>
        ))}
      </ul>

      {user && !isSeller && (
        <form onSubmit={ask} className="mt-5 space-y-2 border-t border-slate-100 pt-4">
          <Textarea value={text} onChange={(e) => setText(e.target.value)} maxLength={500} placeholder="Intreaba vanzatorul ceva despre obiect..." />
          <Alert>{error}</Alert>
          <Button type="submit" loading={sending} disabled={!text.trim()}>
            Trimite intrebarea
          </Button>
        </form>
      )}
    </Card>
  )
}

function AnswerForm({ itemId, questionId, onDone }) {
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  async function send(event) {
    event.preventDefault()
    try {
      await api(`/items/${itemId}/questions/${questionId}/answer`, { method: 'POST', body: { text } })
      onDone()
    } catch (err) {
      setError(err.message)
    }
  }
  return (
    <form onSubmit={send} className="mt-2 flex gap-2">
      <input
        value={text}
        onChange={(e) => setText(e.target.value)}
        maxLength={500}
        placeholder="Raspunde..."
        className="block w-full rounded-lg border-0 bg-white px-3 py-2 text-sm text-slate-900 ring-1 ring-slate-300 ring-inset focus:ring-2 focus:ring-indigo-600 focus:outline-none"
      />
      <Button type="submit" disabled={!text.trim()}>
        Raspunde
      </Button>
      {error && <span className="text-xs text-red-600">{error}</span>}
    </form>
  )
}
