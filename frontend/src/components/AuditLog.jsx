import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, parseDate, timeAgo } from '../api.js'
import { useNow } from './Countdown.jsx'
import { Icon } from './icons.jsx'
import { Alert, EmptyState, Loading } from './ui.jsx'

// Admin "Jurnal" tab: every admin action as a timeline grouped by day, with filters by kind
const ACTIONS = {
  CLOSE_AUCTION: { text: 'a inchis licitatia', icon: 'hourglass', tone: 'from-amber-400 to-orange-500', group: 'items' },
  DELETE_ITEM: { text: 'a sters obiectul', icon: 'flag', tone: 'from-rose-500 to-red-600', group: 'items' },
  FEATURE_ITEM: { text: 'a ales licitatia saptamanii', icon: 'trophy', tone: 'from-amber-300 to-amber-500', group: 'items' },
  CLEAR_FEATURED: { text: 'a resetat licitatia saptamanii', icon: 'trophy', tone: 'from-slate-400 to-slate-500', group: 'items' },
  DISMISS_REPORT: { text: 'a respins raportarea', icon: 'flag', tone: 'from-slate-400 to-slate-600', group: 'reports' },
  BLOCK_USER: { text: 'a blocat utilizatorul', icon: 'shield', tone: 'from-rose-500 to-pink-600', group: 'users' },
  UNBLOCK_USER: { text: 'a deblocat utilizatorul', icon: 'shield', tone: 'from-emerald-400 to-teal-500', group: 'users' },
  DEMO_GENERATE: { text: 'a generat datele demo', icon: 'clapper', tone: 'from-indigo-400 to-violet-500', group: 'system' },
  DEMO_DELETE: { text: 'a sters datele demo', icon: 'clapper', tone: 'from-slate-400 to-slate-500', group: 'system' },
  RESOLVE_ERROR: { text: 'a marcat rezolvata eroarea', icon: 'tagCheck', tone: 'from-emerald-400 to-green-500', group: 'system' },
  DELETE_RESOLVED_ERRORS: { text: 'a sters erorile rezolvate', icon: 'tagCheck', tone: 'from-slate-400 to-slate-500', group: 'system' },
  TEST_EMAIL: { text: 'a trimis un email de test', icon: 'mail', tone: 'from-sky-400 to-blue-500', group: 'system' },
}

const FILTERS = [
  ['', 'Toate'],
  ['items', 'Obiecte'],
  ['users', 'Utilizatori'],
  ['reports', 'Raportari'],
  ['system', 'Sistem'],
]

function dayLabel(date) {
  const today = new Date()
  const yesterday = new Date(Date.now() - 86400000)
  if (date.toDateString() === today.toDateString()) return 'Azi'
  if (date.toDateString() === yesterday.toDateString()) return 'Ieri'
  return new Intl.DateTimeFormat('ro-RO', { weekday: 'long', day: 'numeric', month: 'long' }).format(date)
}

export default function AuditLog() {
  const [entries, setEntries] = useState(null)
  const [filter, setFilter] = useState('')
  const [error, setError] = useState('')
  const now = useNow()

  useEffect(() => {
    api('/admin/audit')
      .then(setEntries)
      .catch((err) => setError(err.message))
  }, [])

  if (error) return <div className="p-6"><Alert>{error}</Alert></div>
  if (!entries) return <Loading />

  const visible = entries.filter((entry) => !filter || ACTIONS[entry.action]?.group === filter)
  const days = []
  for (const entry of visible) {
    const label = dayLabel(parseDate(entry.createdAt))
    if (days.at(-1)?.label !== label) days.push({ label, entries: [] })
    days.at(-1).entries.push(entry)
  }

  return (
    <div className="p-5 sm:p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-slate-500">Ultimele {entries.length} actiuni ale administratorilor.</p>
        <div className="flex flex-wrap gap-1.5">
          {FILTERS.map(([key, label]) => (
            <button
              key={key}
              onClick={() => setFilter(key)}
              className={`rounded-full px-3 py-1 text-xs font-semibold transition ${
                filter === key ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/25' : 'bg-white text-slate-600 ring-1 ring-slate-300 hover:text-indigo-700'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {days.length === 0 ? (
        <div className="mt-6">
          <EmptyState title="Nicio actiune inca">Aici apar inchiderile, stergerile, blocarile si celelalte actiuni de administrare.</EmptyState>
        </div>
      ) : (
        days.map((day) => (
          <section key={day.label} className="mt-6">
            <h3 className="mb-3 text-xs font-bold tracking-widest text-slate-400 uppercase first-letter:uppercase">{day.label}</h3>
            <ol className="relative ml-4">
              <span aria-hidden="true" className="absolute top-3 bottom-3 left-0 w-0.5 -translate-x-1/2 rounded-full bg-gradient-to-b from-indigo-400 via-violet-300 to-transparent" />
              {day.entries.map((entry, index) => {
                const action = ACTIONS[entry.action] ?? { text: entry.action, icon: 'sparkles', tone: 'from-slate-400 to-slate-500' }
                return (
                  <li key={entry.id} className="relative animate-fade-up pb-5 pl-8 last:pb-1" style={{ animationDelay: `${Math.min(index, 10) * 40}ms` }}>
                    <span className={`absolute top-0 -left-4 grid h-8 w-8 place-items-center rounded-full bg-gradient-to-br text-white shadow-lg shadow-black/10 ${action.tone}`}>
                      <Icon name={action.icon} size={16} mono />
                    </span>
                    <p className="text-sm text-slate-700">
                      <b className="text-slate-900">{entry.admin}</b> {action.text}{' '}
                      {entry.targetLabel &&
                        (entry.targetType === 'USER' ? (
                          <Link to={`/users/${encodeURIComponent(entry.targetLabel)}`} className="font-semibold text-indigo-600 hover:underline">
                            {entry.targetLabel}
                          </Link>
                        ) : entry.targetType === 'ITEM' && entry.action !== 'DELETE_ITEM' ? (
                          <Link to={`/items/${entry.targetId}`} className="font-semibold text-indigo-600 hover:underline">
                            {entry.targetLabel}
                          </Link>
                        ) : (
                          <b className="text-slate-900">{entry.targetLabel}</b>
                        ))}
                      {entry.targetType === 'ERROR' && <b className="text-slate-900">#{entry.targetId}</b>}
                    </p>
                    <p className="mt-0.5 text-xs text-slate-500">
                      {timeAgo(entry.createdAt, now)}
                      {entry.details && <> · {entry.details}</>}
                    </p>
                  </li>
                )
              })}
            </ol>
          </section>
        ))
      )}
    </div>
  )
}
