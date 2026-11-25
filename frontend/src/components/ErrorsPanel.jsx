import { useEffect, useState } from 'react'
import { api, formatDate } from '../api.js'
import { Alert, Badge, Button, EmptyState, Loading } from './ui.jsx'

// Admin "Erori" tab: unexpected server errors and JavaScript errors from browsers, grouped with a counter
export default function ErrorsPanel() {
  const [errors, setErrors] = useState(null)
  const [open, setOpen] = useState(null)
  const [failure, setFailure] = useState('')

  const load = () =>
    api('/admin/errors')
      .then(setErrors)
      .catch((err) => setFailure(err.message))

  useEffect(() => {
    load()
  }, [])

  async function resolve(id) {
    await api(`/admin/errors/${id}/resolve`, { method: 'POST' }).catch((err) => setFailure(err.message))
    load()
  }

  async function deleteResolved() {
    await api('/admin/errors/resolved', { method: 'DELETE' }).catch((err) => setFailure(err.message))
    load()
  }

  if (failure) return <div className="p-5"><Alert>{failure}</Alert></div>
  if (!errors) return <Loading />
  if (errors.length === 0) return <EmptyState title="Nicio eroare inregistrata">Totul merge bine.</EmptyState>
  const hasResolved = errors.some((error) => error.resolved)

  return (
    <div>
      <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-5 py-3 text-sm text-slate-600">
        <span>
          {errors.filter((e) => !e.resolved).length} nerezolvate · se grupeaza dupa mesaj si locul din cod
        </span>
        {hasResolved && (
          <Button variant="secondary" onClick={deleteResolved}>
            Sterge cele rezolvate
          </Button>
        )}
      </div>
      <ul className="divide-y divide-slate-100">
        {errors.map((error) => (
          <li key={error.id} className={`px-5 py-3 ${error.resolved ? 'opacity-50' : ''}`}>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <button onClick={() => setOpen(open === error.id ? null : error.id)} className="min-w-0 flex-1 text-left">
                <span className="flex flex-wrap items-center gap-2">
                  <Badge tone={error.source === 'SERVER' ? 'red' : 'amber'}>{error.source === 'SERVER' ? 'Server' : 'Browser'}</Badge>
                  <span className="font-mono text-xs text-slate-500">x{error.occurrences}</span>
                  {error.resolved && <Badge tone="green">Rezolvata</Badge>}
                </span>
                <span className="mt-1 block font-medium break-words text-slate-900">{error.message}</span>
                <span className="mt-0.5 block text-xs text-slate-500">
                  {error.path} · ultima data {formatDate(error.lastSeen)}
                  {error.username && ` · ${error.username}`}
                </span>
              </button>
              {!error.resolved && (
                <Button variant="secondary" onClick={() => resolve(error.id)} className="shrink-0">
                  Marcheaza rezolvata
                </Button>
              )}
            </div>
            {open === error.id && error.details && (
              <pre className="stay-dark mt-3 max-h-72 overflow-auto rounded-xl bg-slate-900 p-3 text-xs text-slate-100">{error.details}</pre>
            )}
          </li>
        ))}
      </ul>
    </div>
  )
}
