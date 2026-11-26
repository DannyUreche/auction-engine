import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, formatDate, formatPrice } from '../api.js'
import { Alert, Loading } from './ui.jsx'

// Admin tab "Dispute": open disputes, oldest first; refund the buyer or pay the seller (recorded in the audit log)
export default function DisputesPanel() {
  const [disputes, setDisputes] = useState(null)
  const [notes, setNotes] = useState({})
  const [error, setError] = useState('')

  const load = () =>
    api('/admin/disputes')
      .then(setDisputes)
      .catch((err) => setError(err.message))
  useEffect(() => {
    load()
  }, [])

  async function resolve(order, refund) {
    const who = refund ? `returnezi ${formatPrice(order.amount)} lui ${order.buyer}` : `platesti ${formatPrice(order.amount)} lui ${order.seller}`
    if (!window.confirm(`Sigur ${who}?`)) return
    try {
      await api(`/admin/orders/${order.id}/resolve`, { method: 'POST', body: { refund, note: notes[order.id] ?? '' } })
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  if (!disputes) return error ? <Alert>{error}</Alert> : <Loading />
  return (
    <div className="space-y-3">
      {error && <Alert>{error}</Alert>}
      {disputes.length === 0 && <p className="rounded-2xl bg-white/85 p-8 text-center text-slate-500 ring-1 ring-slate-200/80">Nicio disputa deschisa.</p>}
      {disputes.map((order) => (
        <div key={order.id} className="rounded-2xl bg-white/85 p-4 shadow-sm ring-1 ring-rose-200">
          <div className="flex flex-wrap items-baseline justify-between gap-2">
            <Link to={`/items/${order.itemId}`} className="font-semibold text-slate-900 hover:text-indigo-700">
              #{order.id} · {order.itemName}
            </Link>
            <span className="text-sm text-slate-500">
              {formatPrice(order.amount)} · cumparator <b>{order.buyer}</b> · vanzator <b>{order.seller}</b>
            </span>
          </div>
          <p className="mt-2 rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-900 ring-1 ring-rose-100">
            „{order.disputeReason}” <span className="text-rose-700/70">· {formatDate(order.disputedAt)}</span>
          </p>
          <p className="mt-1 text-xs text-slate-500">
            {order.shippedAt ? `Expediat ${formatDate(order.shippedAt)} prin ${order.courier}${order.awb ? `, AWB ${order.awb}` : ''}` : 'Neexpediat'}
          </p>
          <div className="mt-3 flex flex-col gap-2 sm:flex-row">
            <input
              value={notes[order.id] ?? ''}
              onChange={(e) => setNotes({ ...notes, [order.id]: e.target.value })}
              placeholder="Motivul deciziei (il vad amandoi)"
              className="flex-1 rounded-xl border-0 bg-white px-3 py-2 text-sm shadow-sm ring-1 ring-slate-300 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
            <button type="button" onClick={() => resolve(order, true)} className="rounded-xl bg-rose-600 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-500">
              Ramburseaza cumparatorul
            </button>
            <button type="button" onClick={() => resolve(order, false)} className="rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-500">
              Plateste vanzatorul
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}
