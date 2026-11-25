import { useCallback, useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { api, formatPrice, timeAgo } from '../api.js'
import { useAuth } from '../auth.jsx'
import { useAuctionEvents } from '../events.jsx'
import { useWallet } from '../wallet.js'
import { useNow } from './Countdown.jsx'
import { Icon } from './icons.jsx'
import { Alert, Button, Card, Input, Textarea } from './ui.jsx'

// "Fa o oferta": private price negotiation before anyone bids (OfferService on the server).
// BuyerOffer lives in the bid panel, SellerOffers is a card for the seller on the item page.

const STATUS = {
  PENDING: { label: 'In asteptare', tone: 'bg-amber-100 text-amber-800 ring-amber-200' },
  COUNTERED: { label: 'Contraoferta', tone: 'bg-violet-100 text-violet-800 ring-violet-200' },
  ACCEPTED: { label: 'Acceptata', tone: 'bg-emerald-100 text-emerald-800 ring-emerald-200' },
  REJECTED: { label: 'Refuzata', tone: 'bg-rose-100 text-rose-800 ring-rose-200' },
  WITHDRAWN: { label: 'Retrasa', tone: 'bg-slate-100 text-slate-600 ring-slate-200' },
  EXPIRED: { label: 'Expirata', tone: 'bg-slate-100 text-slate-600 ring-slate-200' },
}

function StatusChip({ status }) {
  const s = STATUS[status]
  return <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ring-1 ${s.tone}`}>{s.label}</span>
}

// Offers of the item (all for the seller, own for a buyer), reloaded on notifications (new offer, reply)
function useOffers(itemId) {
  const [offers, setOffers] = useState(null)
  const load = useCallback(
    () =>
      api(`/items/${itemId}/offers`)
        .then(setOffers)
        .catch(() => setOffers([])),
    [itemId],
  )
  useEffect(() => {
    load()
  }, [load])
  useAuctionEvents((event) => {
    if (event.type === 'notification' || event.itemId === itemId) load()
  })
  return [offers, load]
}

async function reply(offerId, action, counterAmount) {
  return api(`/offers/${offerId}/reply`, { method: 'POST', body: { action, counterAmount } })
}

export function BuyerOffer({ item, onSold }) {
  const { user } = useAuth()
  const { wallet } = useWallet(user)
  const [offers, load] = useOffers(item.id)
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')
  if (!offers) return null
  const current = offers.find((offer) => offer.status === 'PENDING' || offer.status === 'COUNTERED')
  const last = offers[0]

  async function act(action) {
    setError('')
    try {
      const result = await reply(current.id, action)
      await load()
      if (result.status === 'ACCEPTED') onSold()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="mt-4 border-t border-slate-100 pt-4">
      {current?.status === 'PENDING' && (
        <div className="rounded-2xl bg-amber-50 p-4 ring-1 ring-amber-200">
          <p className="flex items-center justify-between gap-2 text-sm font-semibold text-amber-900">
            <span className="flex items-center gap-2">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-amber-400 opacity-75" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-amber-500" />
              </span>
              Ai oferit {formatPrice(current.amount)}
            </span>
            <StatusChip status="PENDING" />
          </p>
          <p className="mt-1 text-xs text-amber-800/80">Vanzatorul primeste o notificare si iti raspunde aici.</p>
          <button onClick={() => act('WITHDRAW')} className="mt-2 text-xs font-semibold text-amber-900 underline-offset-2 hover:underline">
            Retrage oferta
          </button>
        </div>
      )}

      {current?.status === 'COUNTERED' && (
        <div className="offer-glow rounded-2xl bg-gradient-to-br from-violet-50 to-fuchsia-50 p-4 ring-1 ring-violet-200">
          <p className="text-xs font-semibold tracking-wide text-violet-700 uppercase">Contraoferta de la vanzator</p>
          <p className="mt-1 flex items-baseline gap-2">
            <span className="text-2xl font-black text-violet-900">{formatPrice(current.counterAmount)}</span>
            <span className="text-sm text-slate-500 line-through">{formatPrice(current.amount)}</span>
          </p>
          <div className="mt-3 flex gap-2">
            <Button onClick={() => act('ACCEPT')} className="flex-1">
              Accept si cumpar
            </Button>
            <Button variant="secondary" onClick={() => act('REJECT')}>
              Refuz
            </Button>
          </div>
        </div>
      )}

      {!current && (
        <>
          {last && last.status !== 'ACCEPTED' && (
            <p className="mb-2 flex items-center gap-2 text-xs text-slate-500">
              Ultima ta oferta: {formatPrice(last.amount)} <StatusChip status={last.status} />
            </p>
          )}
          <button
            onClick={() => setOpen(true)}
            className="group flex w-full items-center justify-center gap-2 rounded-xl border-2 border-dashed border-violet-300 bg-violet-50/50 px-4 py-2.5 text-sm font-semibold text-violet-700 transition hover:border-violet-400 hover:bg-violet-50"
          >
            <Icon name="coins" size={18} /> Fa o oferta vanzatorului
          </button>
          <p className="mt-1.5 text-center text-xs text-slate-500">Negociezi direct pretul, inainte sa inceapa licitarea.</p>
        </>
      )}
      <Alert>{error}</Alert>
      {/* Portal: the bid card has backdrop-blur, which would trap a fixed overlay inside it */}
      {open && createPortal(<OfferDialog item={item} wallet={wallet} onClose={() => setOpen(false)} onSent={load} />, document.body)}
    </div>
  )
}

function OfferDialog({ item, wallet, onClose, onSent }) {
  const reference = Number(item.buyNowPrice ?? item.startPrice)
  const suggestions = [0.9, 0.8, 0.7].map((share) => Math.max(1, Math.round(reference * share)))
  const [amount, setAmount] = useState(String(suggestions[0]))
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)

  async function send(event) {
    event.preventDefault()
    setSending(true)
    setError('')
    try {
      await api(`/items/${item.id}/offers`, { method: 'POST', body: { amount: Number(amount), message } })
      await onSent()
      onClose()
    } catch (err) {
      setError(err.message)
      setSending(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 grid animate-fade-in place-items-center bg-black/60 p-4 backdrop-blur-sm" onClick={onClose}>
      <form onSubmit={send} onClick={(e) => e.stopPropagation()} className="w-full max-w-md animate-fade-up overflow-hidden rounded-3xl bg-white shadow-2xl">
        <div className="stay-dark relative overflow-hidden bg-gradient-to-br from-violet-600 via-indigo-600 to-fuchsia-600 px-6 py-5 text-white">
          <div aria-hidden="true" className="absolute -top-10 -right-6 h-32 w-32 rounded-full bg-white/15 blur-2xl" />
          <p className="flex items-center gap-2 text-xs font-semibold tracking-widest text-white/80 uppercase">
            <Icon name="coins" size={16} mono /> Oferta privata
          </p>
          <p className="mt-1 truncate text-lg font-bold">{item.name}</p>
          <p className="text-sm text-white/80">
            Pret de pornire {formatPrice(item.startPrice)}
            {item.buyNowPrice && <> · Cumpara acum {formatPrice(item.buyNowPrice)}</>}
          </p>
        </div>
        <div className="space-y-4 p-6">
          <label className="block">
            <span className="text-sm font-medium text-slate-700">Cat oferi?</span>
            <div className="relative mt-1.5">
              <Input type="number" min="1" step="1" value={amount} onChange={(e) => setAmount(e.target.value)} className="!py-3 !pr-14 !text-2xl !font-bold" required />
              <span className="pointer-events-none absolute top-1/2 right-4 -translate-y-1/2 text-sm font-semibold text-slate-400">RON</span>
            </div>
          </label>
          <div className="flex gap-2">
            {suggestions.map((value, index) => (
              <button
                key={value}
                type="button"
                onClick={() => setAmount(String(value))}
                className={`flex-1 rounded-xl px-2 py-2 text-sm font-semibold ring-1 transition ${
                  Number(amount) === value ? 'bg-violet-600 text-white ring-violet-600' : 'bg-white text-slate-700 ring-slate-300 hover:ring-violet-400'
                }`}
              >
                {formatPrice(value)}
                <span className="block text-[10px] font-medium opacity-70">-{(index + 1) * 10}%</span>
              </button>
            ))}
          </div>
          <Textarea value={message} onChange={(e) => setMessage(e.target.value)} maxLength={300} placeholder="Mesaj pentru vanzator (optional), de ex. cum preferi livrarea" />
          {wallet && <p className="text-xs text-slate-500">Disponibil in portofel: {formatPrice(wallet.available)}. Plata se face doar daca oferta e acceptata.</p>}
          <Alert>{error}</Alert>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={onClose}>
              Renunta
            </Button>
            <Button type="submit" loading={sending}>
              Trimite oferta
            </Button>
          </div>
        </div>
      </form>
    </div>
  )
}

export function SellerOffers({ item, onSold }) {
  const [offers, load] = useOffers(item.id)
  const [counterFor, setCounterFor] = useState(null)
  const [counter, setCounter] = useState('')
  const [error, setError] = useState('')
  const now = useNow()
  if (!offers || offers.length === 0) return null
  const open = offers.filter((offer) => offer.status === 'PENDING' || offer.status === 'COUNTERED')
  const history = offers.filter((offer) => !open.includes(offer))

  async function act(offer, action, amount) {
    setError('')
    try {
      const result = await reply(offer.id, action, amount)
      setCounterFor(null)
      await load()
      if (result.status === 'ACCEPTED') onSold()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <Card>
      <div className="flex items-center justify-between gap-3">
        <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-900">
          <Icon name="coins" size={22} /> Oferte private
        </h2>
        {open.length > 0 && <span className="rounded-full bg-violet-600 px-2.5 py-0.5 text-xs font-bold text-white">{open.length} noi</span>}
      </div>
      <p className="mt-1 text-sm text-slate-500">Cumparatorii iti propun un pret inainte sa liciteze. Daca accepti, obiectul se vinde imediat.</p>
      <Alert>{error}</Alert>

      <ul className="mt-4 space-y-3">
        {open.map((offer) => (
          <li key={offer.id} className="animate-fade-up rounded-2xl bg-white p-4 shadow-sm ring-1 ring-violet-200">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                <span className="grid h-10 w-10 place-items-center rounded-full bg-gradient-to-br from-violet-500 to-fuchsia-500 font-bold text-white">
                  {offer.buyerUsername[0]?.toUpperCase()}
                </span>
                <span>
                  <span className="block font-semibold text-slate-900">{offer.buyerUsername}</span>
                  <span className="block text-xs text-slate-500">{timeAgo(offer.createdAt, now)}</span>
                </span>
              </div>
              <span className="text-right">
                <span className="block text-2xl font-black text-slate-900">{formatPrice(offer.amount)}</span>
                <StatusChip status={offer.status} />
              </span>
            </div>
            {offer.message && <p className="mt-3 rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-700">„{offer.message}”</p>}
            {offer.status === 'COUNTERED' ? (
              <p className="mt-3 text-sm text-violet-700">Ai propus {formatPrice(offer.counterAmount)} · astepti raspunsul cumparatorului.</p>
            ) : counterFor === offer.id ? (
              <form
                className="mt-3 flex gap-2"
                onSubmit={(event) => {
                  event.preventDefault()
                  act(offer, 'COUNTER', Number(counter))
                }}
              >
                <Input type="number" min="1" value={counter} onChange={(e) => setCounter(e.target.value)} placeholder="Pretul tau" required />
                <Button type="submit" className="shrink-0">
                  Trimite
                </Button>
                <Button type="button" variant="ghost" onClick={() => setCounterFor(null)}>
                  Renunta
                </Button>
              </form>
            ) : (
              <div className="mt-3 flex flex-wrap gap-2">
                <button onClick={() => act(offer, 'ACCEPT')} className="rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white shadow-md shadow-emerald-500/25 transition hover:bg-emerald-500">
                  Accept · vand cu {formatPrice(offer.amount)}
                </button>
                <Button
                  variant="secondary"
                  onClick={() => {
                    setCounterFor(offer.id)
                    setCounter(String(Math.round(Number(offer.amount) * 1.1)))
                  }}
                >
                  Contraoferta
                </Button>
                <button onClick={() => act(offer, 'REJECT')} className="rounded-xl px-3 py-2 text-sm font-semibold text-rose-600 hover:bg-rose-50">
                  Refuz
                </button>
              </div>
            )}
          </li>
        ))}
      </ul>

      {history.length > 0 && (
        <details className="mt-4 text-sm">
          <summary className="cursor-pointer font-medium text-slate-600">Istoric ({history.length})</summary>
          <ul className="mt-2 divide-y divide-slate-100">
            {history.map((offer) => (
              <li key={offer.id} className="flex items-center justify-between gap-2 py-2">
                <span className="text-slate-700">
                  {offer.buyerUsername} · {formatPrice(offer.counterAmount ?? offer.amount)}
                </span>
                <StatusChip status={offer.status} />
              </li>
            ))}
          </ul>
        </details>
      )}
    </Card>
  )
}
