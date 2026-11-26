import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { saveReceipt } from '../bidChain.js'
import { api, formatPrice, parseDate } from '../api.js'
import { useAuth } from '../auth.jsx'
import { launchConfetti } from '../confetti.js'
import { useNow } from '../components/Countdown.jsx'
import { Icon } from '../components/icons.jsx'
import ItemImage from '../components/ItemImage.jsx'
import { Loading } from '../components/ui.jsx'
import { useAuctionEvents } from '../events.jsx'

// The live show "stage": the item being auctioned now in the spotlight with a countdown ring, quick bid buttons and
// the live bid feed; a gavel comes down with "ADJUDECAT!" when an item's slot ends; the line-up runs along the bottom.
export default function LiveShowStage() {
  const { id } = useParams()
  const { user } = useAuth()
  const now = useNow()
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [bids, setBids] = useState([])
  const [hammer, setHammer] = useState(null) // { name, winner, price } shown for a few seconds
  const [bidError, setBidError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(
    () =>
      api(`/shows/${id}`, { auth: false })
        .then(setData)
        .catch((err) => setError(err.message)),
    [id],
  )
  useEffect(() => {
    load()
    const timer = setInterval(load, 15000)
    return () => clearInterval(timer)
  }, [load])

  const items = data?.items ?? []
  const current = items.find((item) => item.active && parseDate(item.startTime).getTime() <= now)
  const currentId = current?.id

  const loadBids = useCallback(() => {
    if (!currentId) return
    api(`/bids/${currentId}`, { auth: false })
      .then(setBids)
      .catch(() => {})
  }, [currentId])
  useEffect(() => {
    setBids([])
    loadBids()
  }, [loadBids])

  const ids = useRef(new Set())
  ids.current = new Set(items.map((item) => item.id))
  useAuctionEvents((event) => {
    if (!ids.current.has(event.itemId)) return
    load()
    if (event.itemId === currentId) loadBids()
    if (event.type === 'closed') {
      const item = items.find((candidate) => candidate.id === event.itemId)
      setHammer({ name: item?.name, winner: event.username, price: event.amount })
      if (event.username && event.username === user?.username) launchConfetti()
      setTimeout(() => setHammer(null), 4500)
    }
  })

  async function bid(amount) {
    setBusy(true)
    setBidError('')
    try {
      saveReceipt(await api('/bids', { method: 'POST', body: { itemId: current.id, amount } }))
      await Promise.all([load(), loadBids()])
    } catch (err) {
      setBidError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (error) return <p className="text-red-600">{error}</p>
  if (!data) return <Loading />
  const { show } = data
  const startsIn = parseDate(show.startTime).getTime() - now
  const ended = show.status === 'ENDED' || (items.length > 0 && items.every((item) => !item.active))
  const sold = items.filter((item) => item.winner)
  const total = sold.reduce((sum, item) => sum + Number(item.currentPrice), 0)

  return (
    <div className="stay-dark relative overflow-hidden rounded-3xl bg-slate-950 px-5 py-8 text-white shadow-2xl ring-1 shadow-indigo-950/30 ring-white/10 sm:px-10">
      {/* Stage lights */}
      <div aria-hidden="true" className="pointer-events-none absolute inset-0">
        <div className="featured-beam absolute -top-1/3 left-[12%] h-[160%] w-48 bg-gradient-to-b from-fuchsia-400/25 via-fuchsia-300/5 to-transparent blur-2xl" />
        <div className="featured-beam absolute -top-1/3 right-[12%] h-[160%] w-48 bg-gradient-to-b from-sky-300/25 via-sky-200/5 to-transparent blur-2xl [animation-delay:-6s]" />
        <div className="absolute -bottom-40 left-1/2 h-80 w-[60rem] -translate-x-1/2 rounded-full bg-amber-500/15 blur-3xl" />
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_1px_1px,rgb(255_255_255/0.06)_1px,transparent_0)] bg-[size:24px_24px]" />
      </div>

      <div className="relative mx-auto max-w-6xl">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <Link to="/live" className="text-sm text-white/50 hover:text-white">
              ← Licitatii live
            </Link>
            <h1 className="mt-1 text-3xl font-black tracking-tight sm:text-4xl">{show.title}</h1>
            <p className="text-sm text-white/60">
              cu{' '}
              <Link to={`/users/${encodeURIComponent(show.sellerUsername)}`} className="font-semibold text-white hover:underline">
                {show.sellerUsername}
              </Link>{' '}
              · {items.length} obiecte · {Math.round(show.slotSeconds / 60)} min fiecare
            </p>
          </div>
          <OnAir status={ended ? 'ENDED' : startsIn > 0 ? 'SCHEDULED' : 'LIVE'} />
        </div>

        <div className="mt-8">
          {startsIn > 0 ? (
            <Waiting startsIn={startsIn} first={items[0]} />
          ) : ended ? (
            <Finale sold={sold.length} total={total} count={items.length} />
          ) : current ? (
            <CurrentLot
              item={current}
              slotSeconds={show.slotSeconds}
              now={now}
              bids={bids}
              user={user}
              isSeller={user?.username === show.sellerUsername}
              onBid={bid}
              busy={busy}
              error={bidError}
            />
          ) : (
            <p className="py-24 text-center text-white/60">Urmatorul obiect intra pe scena imediat...</p>
          )}
        </div>

        <LineUp items={items} currentId={currentId} />
      </div>

      {hammer && <HammerOverlay {...hammer} />}
    </div>
  )
}

function OnAir({ status }) {
  if (status === 'LIVE') {
    return (
      <span className="inline-flex items-center gap-2 rounded-full bg-red-600 px-4 py-2 text-sm font-black tracking-widest shadow-lg shadow-red-600/40">
        <span className="relative flex h-2.5 w-2.5">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-white opacity-75" />
          <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-white" />
        </span>
        LIVE
      </span>
    )
  }
  return (
    <span className="rounded-full bg-white/10 px-4 py-2 text-sm font-bold tracking-widest text-white/70 ring-1 ring-white/15">
      {status === 'ENDED' ? 'S-A INCHEIAT' : 'IN CURAND'}
    </span>
  )
}

function clock(ms) {
  const total = Math.max(0, Math.floor(ms / 1000))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = String(total % 60).padStart(2, '0')
  return h > 0 ? `${h}:${String(m).padStart(2, '0')}:${s}` : `${m}:${s}`
}

function Waiting({ startsIn, first }) {
  return (
    <div className="grid place-items-center py-10 text-center">
      <p className="text-sm font-semibold tracking-[0.3em] text-amber-200/80 uppercase">Spectacolul incepe in</p>
      <p className="mt-3 bg-gradient-to-b from-white to-white/60 bg-clip-text text-7xl font-black text-transparent tabular-nums sm:text-8xl">{clock(startsIn)}</p>
      {first && (
        <div className="mt-8 flex items-center gap-4 rounded-2xl bg-white/5 p-3 pr-5 ring-1 ring-white/10">
          <ItemImage item={first} className="h-16 w-20 rounded-xl" />
          <span className="text-left">
            <span className="block text-xs text-white/50">Primul pe scena</span>
            <span className="block font-semibold">{first.name}</span>
            <span className="block text-sm text-amber-200">pornire {formatPrice(first.startPrice)}</span>
          </span>
        </div>
      )}
    </div>
  )
}

function CurrentLot({ item, slotSeconds, now, bids, user, isSeller, onBid, busy, error }) {
  const left = parseDate(item.endTime).getTime() - now
  const share = Math.max(0, Math.min(1, left / (slotSeconds * 1000)))
  const minimum = Number(item.minimumBid)
  const step = Math.max(1, minimum - Number(item.currentPrice))
  const leader = bids[0]?.bidder?.username
  const urgent = left < 30000
  const radius = 54
  const circumference = 2 * Math.PI * radius

  return (
    <div className="grid gap-8 lg:grid-cols-[1.2fr_1fr] lg:items-center">
      <div className="relative">
        <div className="absolute -inset-2 rounded-[1.8rem] bg-gradient-to-br from-amber-300 via-fuchsia-500 to-sky-400 opacity-60 blur-md" />
        <Link to={`/items/${item.id}`} className="relative block overflow-hidden rounded-3xl ring-1 ring-white/20">
          <ItemImage key={item.id} item={item} full className="aspect-[4/3] w-full animate-fade-in" />
          <span className="absolute top-4 left-4 rounded-xl bg-black/60 px-3 py-1.5 text-xs font-bold tracking-widest backdrop-blur">
            LOTUL {item.showPosition + 1}
          </span>
        </Link>
      </div>

      <div>
        <h2 className="text-2xl leading-tight font-bold sm:text-3xl">{item.name}</h2>
        <div className="mt-6 flex items-center gap-6">
          {/* Countdown ring for this lot */}
          <div className="relative h-32 w-32 shrink-0">
            <svg viewBox="0 0 128 128" className="h-full w-full -rotate-90">
              <circle cx="64" cy="64" r={radius} fill="none" stroke="rgb(255 255 255 / 0.1)" strokeWidth="10" />
              <circle
                cx="64"
                cy="64"
                r={radius}
                fill="none"
                stroke={urgent ? '#f43f5e' : '#fbbf24'}
                strokeWidth="10"
                strokeLinecap="round"
                strokeDasharray={circumference}
                strokeDashoffset={circumference * (1 - share)}
                className="transition-[stroke-dashoffset] duration-1000 ease-linear"
              />
            </svg>
            <span className={`absolute inset-0 grid place-items-center text-2xl font-black tabular-nums ${urgent ? 'animate-pulse text-rose-300' : ''}`}>
              {left > 0 ? clock(left) : '0:00'}
            </span>
          </div>
          <div>
            <p className="text-xs font-semibold tracking-widest text-white/50 uppercase">{leader ? 'Oferta curenta' : 'Pret de pornire'}</p>
            <p key={item.currentPrice} className="origin-left animate-price-flash bg-gradient-to-r from-amber-200 to-orange-300 bg-clip-text text-5xl font-black text-transparent">
              {formatPrice(item.currentPrice)}
            </p>
            <p className="mt-1 text-sm text-white/60">{leader ? <>conduce <b className="text-white">{leader}</b></> : 'Nicio oferta inca'}</p>
          </div>
        </div>

        {left <= 0 ? (
          <p className="mt-6 animate-pulse text-lg font-bold text-amber-200">Se adjudeca...</p>
        ) : !user ? (
          <Link to="/login" state={{ from: window.location.pathname }} className="mt-6 inline-block rounded-xl bg-white px-6 py-3 font-bold text-slate-900">
            Intra in cont ca sa licitezi
          </Link>
        ) : isSeller ? (
          <p className="mt-6 text-white/60">Esti vanzatorul: urmaresti ofertele in timp real.</p>
        ) : (
          <div className="mt-6 grid grid-cols-3 gap-2">
            {[minimum, minimum + step * 2, minimum + step * 5].map((amount, index) => (
              <button
                key={amount}
                onClick={() => onBid(amount)}
                disabled={busy}
                className={`rounded-2xl px-3 py-4 text-center font-black transition hover:-translate-y-0.5 disabled:opacity-50 ${
                  index === 0
                    ? 'bg-gradient-to-br from-amber-300 to-orange-500 text-slate-950 shadow-lg shadow-amber-500/30'
                    : 'bg-white/10 text-white ring-1 ring-white/20 hover:bg-white/15'
                }`}
              >
                <span className="block text-lg">{formatPrice(amount)}</span>
                <span className="block text-[11px] font-semibold opacity-70">{index === 0 ? 'oferta minima' : `+${index === 1 ? 2 : 5} pasi`}</span>
              </button>
            ))}
          </div>
        )}
        {error && <p className="mt-3 text-sm font-medium text-rose-300">{error}</p>}

        <ol className="mt-6 max-h-44 space-y-1.5 overflow-hidden">
          {bids.slice(0, 5).map((entry, index) => (
            <li
              key={entry.id}
              className={`flex animate-slide-in items-center justify-between rounded-xl px-3 py-2 text-sm ${index === 0 ? 'bg-emerald-400/15 ring-1 ring-emerald-300/30' : 'bg-white/5'}`}
            >
              <span>
                <b>{entry.bidder?.username}</b> <span className="text-white/50">a oferit</span>
              </span>
              <span className="font-bold tabular-nums">{formatPrice(entry.amount)}</span>
            </li>
          ))}
        </ol>
      </div>
    </div>
  )
}

function Finale({ sold, total, count }) {
  useEffect(() => {
    if (sold > 0) launchConfetti()
  }, [sold])
  return (
    <div className="grid place-items-center py-12 text-center">
      <span className="leaderboard-trophy grid h-24 w-24 place-items-center rounded-3xl bg-gradient-to-br from-amber-300 to-orange-500 shadow-xl shadow-amber-500/40">
        <Icon name="trophy" size={52} mono className="text-amber-950" />
      </span>
      <h2 className="mt-6 text-4xl font-black">Spectacolul s-a incheiat</h2>
      <p className="mt-2 text-white/70">
        {sold} din {count} obiecte vandute · total <b className="text-amber-200">{formatPrice(total)}</b>
      </p>
    </div>
  )
}

function LineUp({ items, currentId }) {
  return (
    <div className="mt-12">
      <p className="mb-3 text-xs font-semibold tracking-widest text-white/50 uppercase">Programul serii</p>
      <ol className="flex gap-3 overflow-x-auto pb-3">
        {items.map((item) => {
          const isCurrent = item.id === currentId
          const state = isCurrent
            ? { label: 'ACUM', tone: 'bg-red-600 text-white' }
            : !item.active
              ? item.winner
                ? { label: `VANDUT ${formatPrice(item.currentPrice)}`, tone: 'bg-emerald-500/20 text-emerald-200' }
                : { label: 'NEVANDUT', tone: 'bg-white/10 text-white/60' }
              : { label: `la ${parseDate(item.startTime).toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' })}`, tone: 'bg-white/10 text-white/70' }
          return (
            <li key={item.id} className="w-44 shrink-0">
              <Link
                to={`/items/${item.id}`}
                className={`block overflow-hidden rounded-2xl ring-1 transition hover:-translate-y-1 ${
                  isCurrent ? 'scale-105 ring-2 ring-amber-300 shadow-lg shadow-amber-500/30' : 'ring-white/10'
                } ${!item.active && !isCurrent ? 'opacity-60' : ''}`}
              >
                <ItemImage item={item} className="aspect-[4/3] w-full" />
                <span className="block bg-white/5 p-2.5">
                  <span className="block truncate text-sm font-semibold">
                    {item.showPosition + 1}. {item.name}
                  </span>
                  <span className={`mt-1.5 inline-block rounded-md px-2 py-0.5 text-[10px] font-black tracking-wider ${state.tone}`}>{state.label}</span>
                </span>
              </Link>
            </li>
          )
        })}
      </ol>
    </div>
  )
}

function HammerOverlay({ name, winner, price }) {
  return (
    <div className="pointer-events-none fixed inset-0 z-40 grid animate-fade-in place-items-center bg-black/60 backdrop-blur-sm">
      <div className="text-center">
        <div className="hammer-slam mx-auto h-28 w-28">
          <svg viewBox="0 0 24 24" className="h-full w-full drop-shadow-2xl">
            <path d="M12.4 10 14 11.6 6.2 19.4a1.1 1.1 0 0 1-1.6-1.6z" fill="#b45309" />
            <path d="M9.2 6.8 13.8 2.2 21.8 10.2 17.2 14.8z" fill="#a78bfa" />
            <path d="M10.3 7.9 14.9 3.3 15.9 4.3 11.3 8.9zM15.1 12.7 19.7 8.1 20.7 9.1 16.1 13.7z" fill="#fcd34d" />
          </svg>
        </div>
        <p className="animate-stamp mt-2 text-6xl font-black tracking-widest text-amber-300 drop-shadow-[0_0_30px_rgba(251,191,36,0.6)]">ADJUDECAT!</p>
        <p className="mt-4 text-xl font-semibold text-white">{name}</p>
        <p className="mt-1 text-white/80">
          {winner ? (
            <>
              <b className="text-emerald-300">{winner}</b> castiga cu <b>{formatPrice(price)}</b>
            </>
          ) : (
            'Fara oferte - obiectul ramane nevandut'
          )}
        </p>
      </div>
    </div>
  )
}
