import { useCallback, useEffect, useState } from 'react'
import { api, formatDate, formatPrice } from '../api.js'
import { useAuctionEvents } from '../events.jsx'
import { Icon } from './icons.jsx'
import { Card } from './ui.jsx'

// The order after a sale, with escrow (GET /api/items/{id}/order): a stepper paid -> shipped -> received ->
// money to the seller, and the actions of whoever is looking (seller ships with an AWB, buyer confirms or
// opens a dispute). Reloads on live notifications, since every order step notifies the other side.
const STEPS = [
  { key: 'PAID', label: 'Platita', hint: 'banii stau in escrow', icon: 'wallet' },
  { key: 'SHIPPED', label: 'Expediata', hint: 'cu AWB sau predare', icon: 'cart' },
  { key: 'RECEIVED', label: 'Primita', hint: 'confirmata de cumparator', icon: 'tagCheck' },
  { key: 'COMPLETED', label: 'Platita vanzatorului', hint: 'banii ies din escrow', icon: 'coins' },
]

function stepIndex(order) {
  if (order.status === 'COMPLETED') return 3
  if (order.status === 'SHIPPED') return 1
  if (order.status === 'DISPUTED' || order.status === 'REFUNDED') return order.shippedAt ? 1 : 0
  return 0
}

export default function OrderPanel({ item, user }) {
  const [order, setOrder] = useState(null)
  const [couriers, setCouriers] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(() => {
    api(`/items/${item.id}/order`)
      .then((data) => setOrder(data ?? null))
      .catch(() => setOrder(null))
  }, [item.id])

  useEffect(() => {
    load()
  }, [load])
  useAuctionEvents((event) => {
    if (event.type === 'notification' && event.username === user.username) load()
  })
  useEffect(() => {
    if (order?.role === 'SELLER' && order.status === 'PAID' && couriers.length === 0) {
      api('/orders/couriers').then(setCouriers).catch(() => {})
    }
  }, [order, couriers.length])

  if (!order) return null

  async function act(path, body = {}) {
    setBusy(true)
    setError('')
    try {
      setOrder(await api(`/orders/${order.id}/${path}`, { method: 'POST', body }))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const current = stepIndex(order)
  const stopped = order.status === 'DISPUTED' || order.status === 'REFUNDED'

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-900">
          <Icon name="shield" size={24} /> Comanda #{order.id}
        </h2>
        <span className="text-sm text-slate-500">
          {formatPrice(order.amount)} · {order.seller} → {order.buyer}
        </span>
      </div>

      <ol className="mt-5 grid grid-cols-4 gap-1">
        {STEPS.map((step, index) => {
          const done = index <= current
          const active = !stopped && index === current && order.status !== 'COMPLETED'
          const failed = stopped && index === current + 1
          return (
            <li key={step.key} className="relative flex flex-col items-center text-center">
              {index > 0 && (
                <span className="absolute top-5 right-1/2 -z-0 h-1 w-full -translate-y-1/2 rounded-full bg-slate-200">
                  <span
                    className={`block h-full rounded-full transition-all duration-700 ${failed ? 'bg-rose-300' : 'bg-gradient-to-r from-indigo-500 to-emerald-500'}`}
                    style={{ width: done || failed ? '100%' : '0%' }}
                  />
                </span>
              )}
              <span
                className={`relative z-10 grid h-10 w-10 place-items-center rounded-full ring-4 transition ${
                  failed
                    ? 'bg-rose-500 text-white ring-rose-100'
                    : done
                      ? 'bg-gradient-to-br from-indigo-500 to-emerald-500 text-white ring-emerald-100'
                      : 'bg-white text-slate-400 ring-slate-100'
                } ${active ? 'order-pulse' : ''}`}
              >
                {failed ? <span className="text-lg font-bold">✕</span> : <Icon name={step.icon} size={20} mono={done} />}
              </span>
              <span className={`mt-2 text-xs font-semibold sm:text-sm ${done ? 'text-slate-900' : 'text-slate-500'}`}>{step.label}</span>
              <span className="hidden text-[11px] text-slate-500 sm:block">{step.hint}</span>
            </li>
          )
        })}
      </ol>

      <div className="mt-5 space-y-3 text-sm">
        {order.status === 'PAID' && order.role === 'SELLER' && (
          <ShipForm couriers={couriers} busy={busy} shipBy={order.shipBy} onShip={(courier, awb) => act('ship', { courier, awb })} />
        )}
        {order.status === 'PAID' && order.role === 'BUYER' && (
          <BuyerBox
            text={`Ai platit; banii stau in escrow. Vanzatorul trebuie sa expedieze pana la ${formatDate(order.shipBy)}, altfel primesti banii inapoi automat.`}
            confirmLabel="Am primit deja obiectul (predare personala)"
            busy={busy}
            onConfirm={() => act('confirm')}
            onDispute={(reason) => act('dispute', { reason })}
          />
        )}
        {order.status === 'SHIPPED' && (
          <Info tone="indigo" icon="cart">
            Expediata {order.courier === 'Predare personala' ? 'prin predare personala' : `prin ${order.courier}, AWB `}
            {order.awb && <b className="font-mono">{order.awb}</b>} · {formatDate(order.shippedAt)}.{' '}
            {order.role === 'SELLER' && `Banii ajung la tine cand cumparatorul confirma, cel tarziu pe ${formatDate(order.autoConfirmAt)}.`}
          </Info>
        )}
        {order.status === 'SHIPPED' && order.role === 'BUYER' && (
          <BuyerBox
            text={`Confirma cand ai obiectul. Daca nu faci nimic, comanda se confirma automat pe ${formatDate(order.autoConfirmAt)}.`}
            confirmLabel="Am primit obiectul"
            busy={busy}
            onConfirm={() => act('confirm')}
            onDispute={(reason) => act('dispute', { reason })}
          />
        )}
        {order.status === 'DISPUTED' && (
          <Info tone="rose" icon="flag">
            <b>Disputa deschisa</b> ({formatDate(order.disputedAt)}): „{order.disputeReason}”. Banii raman blocati pana decide un administrator.
          </Info>
        )}
        {order.status === 'COMPLETED' && (
          <Info tone="emerald" icon="tagCheck">
            <b>Finalizata.</b> {formatPrice(order.amount)} au ajuns la {order.seller}. {order.resolution}
          </Info>
        )}
        {order.status === 'REFUNDED' && (
          <Info tone="rose" icon="coins">
            <b>Rambursata.</b> {formatPrice(order.amount)} s-au intors la {order.buyer}. {order.resolution}
          </Info>
        )}
        {error && <p className="rounded-xl bg-rose-50 px-3 py-2 text-rose-700 ring-1 ring-rose-200">{error}</p>}
      </div>
    </Card>
  )
}

function Info({ tone, icon, children }) {
  const tones = {
    indigo: 'bg-indigo-50 text-indigo-900 ring-indigo-200',
    rose: 'bg-rose-50 text-rose-900 ring-rose-200',
    emerald: 'bg-emerald-50 text-emerald-900 ring-emerald-200',
  }
  return (
    <p className={`flex items-start gap-2 rounded-xl px-3 py-2.5 ring-1 ${tones[tone]}`}>
      <span className="mt-0.5 shrink-0">
        <Icon name={icon} size={18} />
      </span>
      <span>{children}</span>
    </p>
  )
}

function ShipForm({ couriers, busy, shipBy, onShip }) {
  const [courier, setCourier] = useState('')
  const [awb, setAwb] = useState('')
  const pickup = courier === 'Predare personala'
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        onShip(courier, awb)
      }}
      className="rounded-2xl bg-amber-50 p-4 ring-1 ring-amber-200"
    >
      <p className="font-semibold text-slate-900">Expediaza obiectul pana la {formatDate(shipBy)}</p>
      <p className="mt-0.5 text-slate-600">Cumparatorul a platit; primesti banii cand confirma primirea.</p>
      <div className="mt-3 grid gap-2 sm:grid-cols-[1fr_1fr_auto]">
        <select
          value={courier}
          onChange={(e) => setCourier(e.target.value)}
          required
          className="rounded-xl border-0 bg-white px-3 py-2.5 shadow-sm ring-1 ring-slate-300 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
        >
          <option value="">Alege curierul</option>
          {couriers.map((name) => (
            <option key={name}>{name}</option>
          ))}
        </select>
        <input
          value={awb}
          onChange={(e) => setAwb(e.target.value)}
          disabled={pickup}
          placeholder={pickup ? 'Nu e nevoie de AWB' : 'Numar AWB'}
          className="rounded-xl border-0 bg-white px-3 py-2.5 font-mono shadow-sm ring-1 ring-slate-300 focus:ring-2 focus:ring-indigo-500 focus:outline-none disabled:opacity-60"
        />
        <button
          type="submit"
          disabled={busy || !courier || (!pickup && awb.trim().length < 4)}
          className="rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-2.5 font-semibold text-white shadow-md transition hover:brightness-110 disabled:opacity-50"
        >
          Marcheaza expedierea
        </button>
      </div>
    </form>
  )
}

function BuyerBox({ text, confirmLabel, busy, onConfirm, onDispute }) {
  const [disputing, setDisputing] = useState(false)
  const [reason, setReason] = useState('')
  return (
    <div className="rounded-2xl bg-white/80 p-4 ring-1 ring-slate-200">
      <p className="text-slate-700">{text}</p>
      {!disputing ? (
        <div className="mt-3 flex flex-wrap gap-2">
          <button
            type="button"
            disabled={busy}
            onClick={() => window.confirm('Confirmi ca ai primit obiectul in regula? Banii ajung la vanzator.') && onConfirm()}
            className="rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 px-4 py-2.5 font-semibold text-white shadow-md transition hover:brightness-110 disabled:opacity-50"
          >
            {confirmLabel}
          </button>
          <button
            type="button"
            onClick={() => setDisputing(true)}
            className="rounded-xl bg-white px-4 py-2.5 font-semibold text-rose-700 shadow-sm ring-1 ring-rose-200 transition hover:bg-rose-50"
          >
            Am o problema
          </button>
        </div>
      ) : (
        <form
          onSubmit={(e) => {
            e.preventDefault()
            onDispute(reason)
          }}
          className="mt-3 space-y-2"
        >
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={3}
            maxLength={1000}
            placeholder="Ce s-a intamplat? (ex: obiectul nu a sosit, e altul decat cel din anunt...)"
            className="block w-full rounded-xl border-0 bg-white px-3 py-2 shadow-sm ring-1 ring-slate-300 focus:ring-2 focus:ring-rose-500 focus:outline-none"
          />
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={busy || reason.trim().length < 10}
              className="rounded-xl bg-rose-600 px-4 py-2 font-semibold text-white shadow-sm transition hover:bg-rose-500 disabled:opacity-50"
            >
              Deschide disputa
            </button>
            <button type="button" onClick={() => setDisputing(false)} className="px-3 py-2 font-semibold text-slate-500 hover:text-slate-700">
              Renunta
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
