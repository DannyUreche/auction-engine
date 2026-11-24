import { useState } from 'react'
import { Link } from 'react-router'
import { api, formatDate, formatPrice } from '../api.js'
import { Icon } from './icons.jsx'
import { Alert, Card } from './ui.jsx'

const TYPE_ICONS = { TOP_UP: 'coins', PURCHASE: 'cart', SALE: 'tagCheck' }

// Virtual wallet: balance, money reserved by the auctions you lead, test top-ups and the history
export default function WalletPanel({ wallet, onChange }) {
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function topUp(amount) {
    setError('')
    setBusy(true)
    try {
      onChange(await api('/wallet/top-up', { method: 'POST', body: { amount } }))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (!wallet) return null
  return (
    <div className="space-y-5">
      <div className="grid gap-3 sm:grid-cols-3">
        <Amount label="Sold" value={wallet.balance} />
        <Amount label="Rezervat (licitatii la care conduci)" value={wallet.reserved} />
        <Amount label="Disponibil pentru oferte" value={wallet.available} highlight />
      </div>

      <Card>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="font-semibold text-slate-900">Alimenteaza portofelul</p>
            <p className="text-sm text-slate-500">Credite de test, gratuite (maxim 5.000 RON odata).</p>
          </div>
          <div className="flex gap-2">
            {[500, 1000, 5000].map((amount) => (
              <button
                key={amount}
                type="button"
                disabled={busy}
                onClick={() => topUp(amount)}
                className="rounded-lg bg-gradient-to-r from-indigo-600 to-violet-600 px-3.5 py-2 text-sm font-semibold text-white shadow-md transition hover:brightness-110 disabled:opacity-50"
              >
                +{amount.toLocaleString('ro-RO')}
              </button>
            ))}
          </div>
        </div>
        {error && (
          <div className="mt-3">
            <Alert>{error}</Alert>
          </div>
        )}
      </Card>

      <Card className="p-0">
        <p className="px-6 pt-5 font-semibold text-slate-900">Istoric</p>
        {wallet.transactions.length === 0 ? (
          <p className="px-6 py-8 text-center text-sm text-slate-500">Inca nu ai nicio tranzactie.</p>
        ) : (
          <ul className="mt-2 divide-y divide-slate-100">
            {wallet.transactions.map((t) => (
              <li key={t.id} className="flex items-center gap-3 px-6 py-3">
                <Icon name={TYPE_ICONS[t.type] ?? 'coins'} size={24} />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm text-slate-800">
                    {t.itemId ? (
                      <Link to={`/items/${t.itemId}`} className="hover:underline">
                        {t.description}
                      </Link>
                    ) : (
                      t.description
                    )}
                  </p>
                  <p className="text-xs text-slate-400">{formatDate(t.createdAt)}</p>
                </div>
                <span className={`shrink-0 font-semibold ${Number(t.amount) >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                  {Number(t.amount) >= 0 ? '+' : ''}
                  {formatPrice(t.amount)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}

function Amount({ label, value, highlight }) {
  return (
    <div className={`rounded-2xl p-4 ring-1 ${highlight ? 'bg-indigo-50 ring-indigo-200' : 'bg-white/85 ring-slate-200'}`}>
      <p className="text-xs font-medium text-slate-500">{label}</p>
      <p className={`mt-1 text-xl font-bold ${highlight ? 'text-indigo-700' : 'text-slate-900'}`}>{formatPrice(value)}</p>
    </div>
  )
}
