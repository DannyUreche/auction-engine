import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { api, formatDate, formatPrice } from '../api.js'
import { Icon } from './icons.jsx'
import { Alert, Card } from './ui.jsx'

const TYPE_ICONS = { TOP_UP: 'coins', PURCHASE: 'cart', SALE: 'tagCheck', REFUND: 'shield' }

// Virtual wallet: balance, money reserved by the auctions you lead, money in escrow (orders), test top-ups,
// card top-ups through Stripe Checkout (when the server has Stripe keys) and the history
export default function WalletPanel({ wallet, onChange }) {
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState('')
  const [params, setParams] = useSearchParams()
  const confirming = useRef(false)

  // Back from Stripe: ?stripe_session=cs_... -> the server checks the payment with Stripe and credits it once
  useEffect(() => {
    const session = params.get('stripe_session')
    const cancelled = params.get('stripe') === 'cancel'
    if ((!session && !cancelled) || confirming.current) return
    confirming.current = true
    const clean = () => {
      const next = new URLSearchParams(params)
      next.delete('stripe_session')
      next.delete('stripe')
      setParams(next, { replace: true })
    }
    if (cancelled) {
      setNotice('Plata cu cardul a fost anulata.')
      clean()
      return
    }
    api('/wallet/checkout/confirm', { method: 'POST', body: { sessionId: session } })
      .then(async (result) => {
        setNotice(result.credited ? `Plata reusita: ${formatPrice(result.amount)} au fost adaugati in portofel.` : 'Plata era deja inregistrata.')
        onChange(await api('/wallet'))
      })
      .catch((err) => setError(err.message))
      .finally(clean)
  }, [params]) // eslint-disable-line react-hooks/exhaustive-deps

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
      {(Number(wallet.inEscrow) > 0 || Number(wallet.pendingSales) > 0) && (
        <div className="grid gap-3 sm:grid-cols-2">
          <Amount label="Platit, in escrow (pana confirmi primirea)" value={wallet.inEscrow} />
          <Amount label="De incasat (cand cumparatorii confirma)" value={wallet.pendingSales} />
        </div>
      )}
      {notice && <p className="rounded-xl bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800 ring-1 ring-emerald-200">{notice}</p>}
      <CardTopUp />

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

// Card payment through Stripe Checkout; shown only when the server has Stripe keys
function CardTopUp() {
  const [config, setConfig] = useState(null)
  const [amount, setAmount] = useState(100)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/wallet/card')
      .then(setConfig)
      .catch(() => setConfig(null))
  }, [])

  if (!config?.enabled) return null

  async function pay() {
    setBusy(true)
    setError('')
    try {
      const { url } = await api('/wallet/checkout', { method: 'POST', body: { amount } })
      window.location.assign(url)
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="flex items-center gap-2 font-semibold text-slate-900">
            <Icon name="wallet" size={20} /> Plateste cu cardul
            {config.testMode && <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-bold text-amber-800 uppercase">mod test</span>}
          </p>
          <p className="text-sm text-slate-500">
            Plata securizata prin Stripe ({config.min}-{config.max} RON).
            {config.testMode && ' Foloseste cardul de test 4242 4242 4242 4242, orice data viitoare si orice CVC.'}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {[50, 100, 250, 500].map((value) => (
            <button
              key={value}
              type="button"
              onClick={() => setAmount(value)}
              className={`rounded-lg px-3 py-2 text-sm font-semibold transition ${
                amount === value ? 'bg-indigo-600 text-white shadow-md' : 'bg-white text-slate-700 shadow-sm ring-1 ring-slate-300 hover:ring-indigo-400'
              }`}
            >
              {value} RON
            </button>
          ))}
          <button
            type="button"
            disabled={busy}
            onClick={pay}
            className="rounded-lg bg-gradient-to-r from-emerald-600 to-teal-600 px-4 py-2 text-sm font-semibold text-white shadow-md transition hover:brightness-110 disabled:opacity-50"
          >
            {busy ? 'Te trimitem la Stripe...' : `Plateste ${amount} RON`}
          </button>
        </div>
      </div>
      {error && (
        <div className="mt-3">
          <Alert>{error}</Alert>
        </div>
      )}
    </Card>
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
