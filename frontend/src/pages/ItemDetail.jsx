import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, formatDate, formatPrice } from '../api.js'
import { useAuth } from '../auth.jsx'
import Countdown from '../components/Countdown.jsx'
import { Alert, Badge, Button, Card, EmptyState, Input, Loading } from '../components/ui.jsx'

export default function ItemDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const [item, setItem] = useState(null)
  const [bids, setBids] = useState([])
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      const [itemData, bidData] = await Promise.all([api(`/items/${id}`), api(`/bids/${id}`)])
      setItem(itemData)
      setBids(bidData)
      setError('')
    } catch (err) {
      setError(err.message)
    }
  }, [id])

  // Reimprospatam la fiecare 5 secunde ca sa vedem ofertele altora aproape in timp real
  useEffect(() => {
    load()
    const intervalId = setInterval(load, 5000)
    return () => clearInterval(intervalId)
  }, [load])

  if (error && !item) {
    return (
      <div className="space-y-4">
        <Alert>{error}</Alert>
        <Link to="/" className="text-sm font-semibold text-indigo-600">
          ← Inapoi la licitatii
        </Link>
      </div>
    )
  }
  if (!item) return <Loading />

  const isSeller = item.seller?.id === user?.id
  const leadingBid = bids[0]

  return (
    <div>
      <Link to="/" className="text-sm font-medium text-slate-500 hover:text-slate-900">
        ← Inapoi la licitatii
      </Link>

      <div className="mt-4 grid gap-6 lg:grid-cols-3">
        {/* Detaliile obiectului */}
        <div className="space-y-6 lg:col-span-2">
          <Card>
            <div className="flex flex-wrap items-center gap-2">
              <Countdown endTime={item.endTime} active={item.active} />
              {isSeller && <Badge tone="indigo">Obiectul tau</Badge>}
            </div>
            <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-900">{item.name}</h1>
            <p className="mt-4 whitespace-pre-line text-slate-600">{item.description || 'Fara descriere.'}</p>

            <dl className="mt-8 grid grid-cols-2 gap-4 border-t border-slate-100 pt-6 sm:grid-cols-3">
              <Detail label="Vanzator" value={item.seller?.username} />
              <Detail label="Pret de pornire" value={formatPrice(item.startPrice)} />
              <Detail label={item.active ? 'Se termina' : 'S-a terminat'} value={formatDate(item.endTime)} />
            </dl>
          </Card>

          <Card>
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-900">Istoricul ofertelor</h2>
              <span className="text-sm text-slate-500">{bids.length} oferte</span>
            </div>
            <div className="mt-4">
              {bids.length === 0 ? (
                <EmptyState title="Nicio oferta inca">Fii primul care liciteaza!</EmptyState>
              ) : (
                <ul className="divide-y divide-slate-100">
                  {bids.map((bid, index) => (
                    <li key={bid.id} className="flex items-center justify-between gap-3 py-3">
                      <div className="flex min-w-0 items-center gap-3">
                        <span
                          className={`grid h-9 w-9 shrink-0 place-items-center rounded-full text-sm font-bold ${
                            index === 0 ? 'bg-amber-100 text-amber-700' : 'bg-slate-100 text-slate-500'
                          }`}
                        >
                          {bid.bidder?.username?.[0]?.toUpperCase()}
                        </span>
                        <div className="min-w-0">
                          <p className="truncate text-sm font-medium text-slate-900">
                            {bid.bidder?.username}
                            {bid.bidder?.id === user?.id && <span className="text-slate-400"> (tu)</span>}
                          </p>
                          <p className="text-xs text-slate-500">{formatDate(bid.createdAt)}</p>
                        </div>
                      </div>
                      <div className="flex shrink-0 flex-col items-end gap-1 sm:flex-row sm:items-center sm:gap-2">
                        {index === 0 && <Badge tone="amber">{item.active ? 'Conduce' : 'Castigatoare'}</Badge>}
                        <span className="font-semibold whitespace-nowrap text-slate-900">{formatPrice(bid.amount)}</span>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </Card>
        </div>

        {/* Pretul curent si formularul de licitare */}
        <div className="lg:sticky lg:top-24 lg:self-start">
          <Card>
            <p className="text-xs font-medium tracking-wide text-slate-400 uppercase">
              {item.active ? (leadingBid ? 'Oferta curenta' : 'Pret de pornire') : 'Pret final'}
            </p>
            <p className="mt-1 text-4xl font-bold tracking-tight text-slate-900">{formatPrice(item.currentPrice)}</p>
            {leadingBid && (
              <p className="mt-1 text-sm text-slate-500">
                de {leadingBid.bidder?.username}
                {leadingBid.bidder?.id === user?.id && ' (tu)'}
              </p>
            )}

            <div className="mt-6 border-t border-slate-100 pt-6">
              <BidPanel item={item} user={user} isSeller={isSeller} leadingBid={leadingBid} onBid={load} />
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}

function Detail({ label, value }) {
  return (
    <div>
      <dt className="text-xs font-medium tracking-wide text-slate-400 uppercase">{label}</dt>
      <dd className="mt-1 text-sm font-semibold text-slate-900">{value}</dd>
    </div>
  )
}

function BidPanel({ item, user, isSeller, leadingBid, onBid }) {
  const minimum = Math.floor(Number(item.currentPrice)) + 1
  const [amount, setAmount] = useState(String(minimum))
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // Licitatia s-a incheiat: aratam rezultatul
  if (!item.active) {
    if (!item.winner) return <p className="text-sm text-slate-500">Licitatia s-a incheiat fara nicio oferta.</p>
    const iWon = item.winner.id === user?.id
    return (
      <div className={`rounded-xl p-4 text-center ${iWon ? 'bg-amber-50' : 'bg-slate-50'}`}>
        <p className="text-3xl">🏆</p>
        <p className="mt-2 font-semibold text-slate-900">
          {iWon ? 'Felicitari, ai castigat!' : `Castigat de ${item.winner.username}`}
        </p>
      </div>
    )
  }

  if (isSeller) {
    return <p className="text-sm text-slate-500">Acesta este obiectul tau. Urmareste ofertele in timp real.</p>
  }

  if (new Date(item.endTime) <= new Date()) {
    return <p className="text-sm text-slate-500">Timpul a expirat. Licitatia se inchide in cateva secunde.</p>
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      await api('/bids', { method: 'POST', body: { itemId: item.id, amount: Number(amount) } })
      setSuccess(`Oferta de ${formatPrice(amount)} a fost plasata!`)
      // Propunem direct urmatoarea oferta posibila
      setAmount(String(Math.floor(Number(amount)) + 1))
      await onBid()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  const iAmLeading = leadingBid?.bidder?.id === user?.id

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {iAmLeading && <Alert tone="success">Ai cea mai mare oferta in acest moment.</Alert>}
      <Alert tone="success">{success}</Alert>
      <Alert>{error}</Alert>

      <div>
        <label className="mb-1.5 block text-sm font-medium text-slate-700" htmlFor="amount">
          Oferta ta (RON)
        </label>
        <Input
          id="amount"
          type="number"
          step="0.01"
          min={minimum}
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          required
        />
        <p className="mt-1 text-xs text-slate-500">Trebuie sa fie mai mare de {formatPrice(item.currentPrice)}</p>
      </div>

      <div className="grid grid-cols-3 gap-2">
        {[5, 10, 50].map((step) => (
          <button
            key={step}
            type="button"
            onClick={() => setAmount(String(Number(item.currentPrice) + step))}
            className="rounded-lg bg-slate-100 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-200"
          >
            +{step}
          </button>
        ))}
      </div>

      <Button type="submit" loading={submitting} className="w-full py-3 text-base">
        Liciteaza
      </Button>
    </form>
  )
}
