import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import {
  api,
  checkImage,
  dutchPrice,
  deleteItemImage,
  formatDate,
  formatPrice,
  IMAGE_TYPES,
  MAX_IMAGES,
  parseDate,
  setMainItemImage,
  uploadItemImage,
} from '../api.js'
import { useAuth } from '../auth.jsx'
import { findCategory, useCategories } from '../categories.js'
import Countdown, { CountdownTiles, useNow } from '../components/Countdown.jsx'
import { useWallet } from '../wallet.js'
import EditItemForm from '../components/EditItemForm.jsx'
import ItemImage from '../components/ItemImage.jsx'
import PriceChart from '../components/PriceChart.jsx'
import { ItemReviews } from '../components/Reviews.jsx'
import ChatBox from '../components/ChatBox.jsx'
import Questions from '../components/Questions.jsx'
import LiveRoom from '../components/LiveRoom.jsx'
import Badges from '../components/Badges.jsx'
import ReportButton from '../components/ReportButton.jsx'
import ShareButton from '../components/ShareButton.jsx'
import { CategoryIcon, Icon } from '../components/icons.jsx'
import { launchConfetti } from '../confetti.js'
import { disintegrate } from '../disintegrate.js'
import { Alert, Badge, Button, Card, EmptyState, Input, Loading } from '../components/ui.jsx'
import { LiveBadge, useAuctionEvents } from '../events.jsx'
import { WatchButton } from '../watchlist.jsx'
import { useT } from '../i18n.jsx'

export default function ItemDetail() {
  const t = useT()
  const { id } = useParams()
  const { user } = useAuth()
  const categories = useCategories()
  // Mesaj venit de pe pagina "Vinde un obiect" (ex: imaginea nu s-a putut incarca)
  const warning = useLocation().state?.warning
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

  // Notificare scurta cand altcineva liciteaza sau licitatia se inchide
  const [notice, setNotice] = useState(null)
  const [editing, setEditing] = useState(false)

  // Ai castigat obiectul: confetti (o singura data pe sesiune pentru fiecare obiect)
  const wonByMe = item && !item.active && item.winner && item.winner.id === user?.id
  useEffect(() => {
    if (!wonByMe) return
    const key = `confetti-${item.id}`
    if (sessionStorage.getItem(key)) return
    sessionStorage.setItem(key, '1')
    launchConfetti()
  }, [wonByMe, item?.id])
  useEffect(() => {
    if (!notice) return
    const timeoutId = setTimeout(() => setNotice(null), 5000)
    return () => clearTimeout(timeoutId)
  }, [notice])

  // Anti-sniping: daca finalul s-a mutat mai tarziu, anuntam prelungirea
  const previousEnd = useRef(null)
  useEffect(() => {
    if (!item) return
    if (previousEnd.current && item.active && parseDate(item.endTime) > parseDate(previousEnd.current)) {
      setNotice('Oferta de ultim moment: licitatia a fost prelungita cu 2 minute!')
    }
    previousEnd.current = item.endTime
  }, [item])

  // Actualizari instant: serverul ne anunta cand se schimba ceva la acest obiect
  const live = useAuctionEvents((event) => {
    if (event.itemId !== Number(id)) return
    if (event.type === 'deleted') {
      setItem(null)
      setError('Acest obiect a fost sters de un administrator.')
      return
    }
    load()
    if (event.type === 'bid' && event.username !== user?.username) {
      setNotice(`Oferta noua: ${formatPrice(event.amount)} de la ${event.username}`)
    }
    if (event.type === 'closed') {
      // Poza "se dezintegreaza" si apoi reapare: semn vizual ca licitatia s-a terminat
      disintegrate(document.querySelector('[data-main-image]'))
      setNotice(event.username ? `Licitatia s-a incheiat! Castigator: ${event.username}` : 'Licitatia s-a incheiat.')
    }
  })

  // Incarcare initiala + reimprospatare rara, de siguranta (in caz ca se pierde conexiunea live)
  useEffect(() => {
    load()
    const intervalId = setInterval(load, 30000)
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
  const category = findCategory(categories, item.category)
  const leadingBid = bids[0]

  return (
    <div>
      <div className="flex items-center justify-between">
        <Link to="/" className="text-sm font-medium text-slate-500 hover:text-slate-900">
          ← Inapoi la licitatii
        </Link>
        {item.active && <LiveBadge connected={live} />}
      </div>

      {notice && (
        <div className="fixed inset-x-0 top-20 z-20 flex justify-center px-4">
          <div className="stay-dark flex animate-bounce items-center gap-2 rounded-full bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white shadow-lg">
            <Icon name="bell" size={20} /> {notice}
          </div>
        </div>
      )}

      {warning && (
        <div className="mt-4">
          <Alert>{warning}</Alert>
        </div>
      )}

      <div className="mt-4 grid gap-6 lg:grid-cols-3">
        {/* Detaliile obiectului */}
        <div className="min-w-0 space-y-6 lg:col-span-2">
          <Gallery item={item} isSeller={isSeller} onChange={setItem} />

          {editing ? (
            <EditItemForm
              item={item}
              onSaved={(updated) => {
                setItem(updated)
                setEditing(false)
              }}
              onCancel={() => setEditing(false)}
            />
          ) : (
          <Card>
            <div className="flex flex-wrap items-center gap-2">
              <Countdown endTime={item.endTime} active={item.active} startTime={item.startTime} />
              {isSeller && <Badge tone="indigo">{t('Obiectul tau')}</Badge>}
              {/* Editarea e permisa doar pana la prima oferta */}
              {isSeller && item.active && bids.length === 0 && (
                <button
                  type="button"
                  onClick={() => setEditing(true)}
                  className="ml-auto inline-flex items-center gap-1.5 text-sm font-semibold text-indigo-600 hover:text-indigo-500"
                >
                  <Icon name="pencil" size={18} /> Editeaza
                </button>
              )}
            </div>
            <div className="mt-3 flex items-start justify-between gap-4">
              <h1 className="min-w-0 text-3xl font-bold tracking-tight break-words text-slate-900">{item.name}</h1>
              <WatchButton item={item} large />
            </div>
            <p className="mt-4 whitespace-pre-line text-slate-600">{item.description || 'Fara descriere.'}</p>
            <div className="mt-4 flex flex-wrap gap-x-5 gap-y-2">
              <ShareButton item={item} />
              {user && !isSeller && <ReportButton item={item} />}
            </div>

            <dl className="mt-8 grid grid-cols-2 gap-4 border-t border-slate-100 pt-6 sm:grid-cols-3">
              <Detail
                label={t('Vanzator')}
                value={
                  <Link
                    to={`/users/${encodeURIComponent(item.seller?.username)}`}
                    className="text-indigo-600 hover:text-indigo-500 hover:underline"
                  >
                    {item.seller?.username}
                  </Link>
                }
                extra={<SellerBadges username={item.seller?.username} />}
              />
              <Detail
                label={t('Categorie')}
                value={
                  <span className="inline-flex items-center gap-1.5">
                    <CategoryIcon category={category} size={18} /> {category.label}
                  </span>
                }
              />
              <Detail label={t('Pret de pornire')} value={formatPrice(item.startPrice)} />
              <Detail label={item.active ? 'Se termina' : 'S-a terminat'} value={formatDate(item.endTime)} />
              {item.buyNowPrice && <Detail label={t('Cumpara acum')} value={formatPrice(item.buyNowPrice)} />}
            </dl>
          </Card>
          )}

          {bids.length > 0 && (
            <Card>
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-slate-900">{t('Evolutia pretului')}</h2>
                <span className="text-sm font-medium text-emerald-600">
                  +{formatPrice(Number(item.currentPrice) - Number(item.startPrice))} fata de pornire
                </span>
              </div>
              <div className="mt-4">
                <PriceChart startPrice={item.startPrice} bids={bids} />
              </div>
            </Card>
          )}

          <Card>
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-900">{t('Istoricul ofertelor')}</h2>
              <span className="text-sm text-slate-500">{bids.length} oferte</span>
            </div>
            <div className="mt-4">
              {bids.length === 0 ? (
                <EmptyState title={t('Nicio oferta inca')}>{t('Fii primul care liciteaza!')}</EmptyState>
              ) : (
                <ul className="divide-y divide-slate-100">
                  {bids.map((bid, index) => (
                    <li key={bid.id} className="flex animate-fade-up items-center justify-between gap-3 py-3">
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
                            {bid.automatic && (
                              <span
                                title="Oferta plasata de licitarea automata"
                                className="ml-1.5 inline-flex items-center gap-1 rounded bg-indigo-100 px-1.5 py-0.5 align-middle text-[10px] font-semibold text-indigo-700"
                              >
                                <Icon name="robot" size={13} /> automat
                              </span>
                            )}
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

          <LiveRoom item={item} user={user} />
          <Questions item={item} user={user} />

          {!item.active && item.winner && user && (user.id === item.seller?.id || user.id === item.winner.id) && (
            <Card>
              <ChatBox item={item} user={user} />
            </Card>
          )}

          {!item.active && item.winner && (
            <Card>
              <ItemReviews item={item} user={user} />
            </Card>
          )}
        </div>

        {/* Pretul curent si formularul de licitare */}
        <div className="min-w-0 lg:sticky lg:top-24 lg:self-start">
          <Card>
            <p className="text-xs font-medium tracking-wide text-slate-400 uppercase">
              {!item.active ? 'Pret final' : item.auctionType === 'DUTCH' ? 'Pret curent (scade)' : leadingBid ? 'Oferta curenta' : 'Pret de pornire'}
            </p>
            {item.active && item.started && item.auctionType === 'DUTCH' ? (
              <LiveDutchPrice item={item} />
            ) : (
              /* key = pretul: la o oferta noua animatia porneste din nou */
              <p key={item.currentPrice} className="mt-1 origin-left animate-price-flash text-4xl font-bold tracking-tight text-slate-900">
                {formatPrice(item.currentPrice)}
              </p>
            )}
            {leadingBid && (
              <p className="mt-1 text-sm text-slate-500">
                de {leadingBid.bidder?.username}
                {leadingBid.bidder?.id === user?.id && ' (tu)'}
              </p>
            )}
            {item.active && (
              <div className="mt-5">
                {!item.started && <p className="mb-2 text-xs font-medium text-indigo-600">{t('Licitatia incepe in')}</p>}
                <CountdownTiles endTime={item.started ? item.endTime : item.startTime} />
              </div>
            )}

            {!item.active && item.winner && (user?.id === item.seller?.id || user?.id === item.winner.id) && (
              <ContactInfo itemId={item.id} />
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

// Galeria de poze: poza mare, sageti si miniaturi.
// Vanzatorul poate adauga poze (maxim MAX_IMAGES), sterge poza afisata sau o poate face poza principala.
function Gallery({ item, isSeller, onChange }) {
  const urls = item.imageUrls ?? []
  const inputRef = useRef(null)
  const [selected, setSelected] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  // Dupa stergerea unei poze, pozitia aleasa poate iesi din lista
  const current = Math.min(selected, Math.max(urls.length - 1, 0))

  async function run(action) {
    setError('')
    setBusy(true)
    try {
      await action()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  function addFiles(fileList) {
    const files = [...(fileList ?? [])]
    if (files.length === 0) return
    const problem = files.map(checkImage).find(Boolean)
    if (problem) {
      setError(problem)
      return
    }
    const room = MAX_IMAGES - urls.length
    run(async () => {
      for (const file of files.slice(0, room)) onChange(await uploadItemImage(item.id, file))
      // Aratam prima poza noua
      setSelected(urls.length)
      if (files.length > room) setError(`Un obiect poate avea maxim ${MAX_IMAGES} poze!`)
    })
  }

  function removeCurrent() {
    if (!window.confirm('Stergi aceasta poza?')) return
    const image = document.querySelector('[data-main-image]')
    run(async () => {
      try {
        // Poza se sparge in bilute in timp ce serverul o sterge
        const [updated] = await Promise.all([
          deleteItemImage(item.id, urls[current]),
          disintegrate(image, { keepHidden: true }),
        ])
        onChange(updated)
      } finally {
        if (image) image.style.visibility = ''
      }
    })
  }

  function makeCurrentMain() {
    run(async () => {
      onChange(await setMainItemImage(item.id, urls[current]))
      setSelected(0)
    })
  }

  // Cumparatorii nu vad nimic daca obiectul nu are poze
  if (urls.length === 0 && !isSeller) return null

  const go = (step) => setSelected((current + step + urls.length) % urls.length)

  return (
    <div>
      <div className="relative overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200">
        {urls.length > 0 ? (
          <img data-main-image src={urls[current]} alt={item.name} className="aspect-[16/10] w-full bg-slate-100 object-contain" />
        ) : (
          <ItemImage item={item} className="aspect-[16/10] w-full" />
        )}

        {urls.length > 1 && (
          <>
            <ArrowButton label="Poza anterioara" className="left-3" onClick={() => go(-1)}>
              ‹
            </ArrowButton>
            <ArrowButton label="Poza urmatoare" className="right-3" onClick={() => go(1)}>
              ›
            </ArrowButton>
            <span className="stay-dark absolute top-4 left-4 rounded-full bg-slate-900/70 px-2.5 py-0.5 text-xs font-semibold text-white">
              {current + 1} / {urls.length}
            </span>
          </>
        )}

        {isSeller && (
          <>
            <input
              ref={inputRef}
              type="file"
              multiple
              accept={IMAGE_TYPES.join(',')}
              className="hidden"
              onChange={(e) => {
                addFiles(e.target.files)
                e.target.value = ''
              }}
            />
            {urls.length < MAX_IMAGES && (
              <Button
                variant="secondary"
                loading={busy}
                onClick={() => inputRef.current.click()}
                className="absolute right-4 bottom-4 bg-white/90 backdrop-blur"
              >
                {urls.length === 0 ? 'Adauga poze' : `Adauga poze (${urls.length}/${MAX_IMAGES})`}
              </Button>
            )}
          </>
        )}
      </div>

      {urls.length > 1 && (
        <div className="mt-3 flex gap-2 overflow-x-auto pb-1">
          {urls.map((url, index) => (
            <button
              key={url}
              type="button"
              onClick={() => setSelected(index)}
              aria-label={`Poza ${index + 1}`}
              className={`shrink-0 overflow-hidden rounded-lg ring-2 transition ${
                index === current ? 'ring-indigo-500' : 'opacity-70 ring-transparent hover:opacity-100'
              }`}
            >
              <img src={url} alt="" className="h-16 w-20 object-cover" />
            </button>
          ))}
        </div>
      )}

      {isSeller && urls.length > 0 && (
        <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm">
          {current === 0 ? (
            <span className="inline-flex items-center gap-1.5 text-slate-500">
              <Icon name="star" size={16} /> Poza principala (apare pe card)
            </span>
          ) : (
            <button
              type="button"
              disabled={busy}
              onClick={makeCurrentMain}
              className="inline-flex items-center gap-1.5 font-semibold text-indigo-600 hover:text-indigo-500 disabled:opacity-50"
            >
              <Icon name="star" size={16} /> Fa-o poza principala
            </button>
          )}
          <button
            type="button"
            disabled={busy}
            onClick={removeCurrent}
            className="font-semibold text-red-600 hover:text-red-500 disabled:opacity-50"
          >
            Sterge poza
          </button>
        </div>
      )}

      {error && (
        <div className="mt-3">
          <Alert>{error}</Alert>
        </div>
      )}
    </div>
  )
}

function ArrowButton({ label, className, onClick, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      className={`absolute top-1/2 grid h-10 w-10 -translate-y-1/2 place-items-center rounded-full bg-white/90 pb-0.5 text-2xl font-bold text-slate-700 shadow-sm backdrop-blur transition hover:bg-white ${className}`}
    >
      {children}
    </button>
  )
}

function Detail({ label, value, extra }) {
  return (
    <div>
      <dt className="text-xs font-medium tracking-wide text-slate-400 uppercase">{label}</dt>
      <dd className="mt-1 text-sm font-semibold text-slate-900">{value}</dd>
      {extra && <dd className="mt-1.5">{extra}</dd>}
    </div>
  )
}

// The seller's reputation badges, next to their name
function SellerBadges({ username }) {
  const [badges, setBadges] = useState([])
  useEffect(() => {
    if (!username) return
    api(`/users/${encodeURIComponent(username)}/badges`, { auth: false })
      .then(setBadges)
      .catch(() => setBadges([]))
  }, [username])
  return <Badges badges={badges} compact />
}

function BidPanel({ item, user, isSeller, leadingBid, onBid }) {
  const t = useT()
  // Oferta minima vine de la server (pretul curent + pasul minim)
  const minimum = Number(item.minimumBid)
  const step = minimum - Number(item.currentPrice)
  const [amount, setAmount] = useState(String(minimum))
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const { wallet } = useWallet(user)

  // Daca altcineva a licitat intre timp, ridicam suma propusa la noul minim
  useEffect(() => {
    setAmount((current) => (Number(current) < minimum ? String(minimum) : current))
  }, [minimum])

  // Licitatia s-a incheiat: aratam rezultatul
  if (!item.active) {
    if (!item.winner) return <p className="text-sm text-slate-500">{t('Licitatia s-a incheiat fara nicio oferta.')}</p>
    const iWon = item.winner.id === user?.id
    return (
      <div className={`rounded-xl p-4 text-center ${iWon ? 'bg-amber-50' : 'bg-slate-50'}`}>
        <Icon name="trophy" size={44} className="mx-auto" />
        <p className="mt-2 font-semibold text-slate-900">
          {iWon ? 'Felicitari, ai castigat!' : `Castigat de ${item.winner.username}`}
        </p>
      </div>
    )
  }

  // Scheduled auction that has not opened yet
  if (!item.started) {
    return (
      <div className="space-y-2 rounded-xl bg-indigo-50/70 p-4 text-center ring-1 ring-indigo-100">
        <Icon name="timer" size={36} className="mx-auto" />
        <p className="font-semibold text-slate-900">Licitatia incepe pe {formatDate(item.startTime)}</p>
        <p className="text-sm text-slate-500">Apasa pe „Urmareste” ca sa primesti o notificare cand porneste.</p>
      </div>
    )
  }

  if (!user) {
    return (
      <div className="space-y-3 text-center">
        <p className="text-sm text-slate-500">{t('Ai nevoie de un cont ca sa licitezi.')}</p>
        <Link
          to="/login"
          state={{ from: `/items/${item.id}` }}
          className="block rounded-lg bg-indigo-600 py-3 text-base font-semibold text-white shadow-sm hover:bg-indigo-500"
        >
          {t('Intra in cont ca sa licitezi')}
        </Link>
        <Link to="/register" className="block text-sm font-semibold text-indigo-600 hover:text-indigo-500">
          sau creeaza un cont nou
        </Link>
      </div>
    )
  }

  if (isSeller) {
    return <p className="text-sm text-slate-500">{t('Acesta este obiectul tau. Urmareste ofertele in timp real.')}</p>
  }

  if (parseDate(item.endTime) <= new Date()) {
    return <p className="text-sm text-slate-500">{t('Timpul a expirat. Licitatia se inchide in cateva secunde.')}</p>
  }

  if (item.auctionType === 'DUTCH') return <DutchPanel item={item} wallet={wallet} onBuy={onBid} />

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      await api('/bids', { method: 'POST', body: { itemId: item.id, amount: Number(amount) } })
      setSuccess(`Oferta de ${formatPrice(amount)} a fost plasata!`)
      await onBid()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleBuyNow() {
    if (!window.confirm(`Cumperi "${item.name}" acum cu ${formatPrice(item.buyNowPrice)}? Licitatia se incheie imediat.`)) {
      return
    }
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      await api(`/items/${item.id}/buy-now`, { method: 'POST' })
      await onBid()
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
    }
  }

  const iAmLeading = leadingBid?.bidder?.id === user?.id
  // Sume rapide: minimul, apoi cu 5 si 10 pasi peste el
  const quickAmounts = [minimum, minimum + 4 * step, minimum + 9 * step]

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {iAmLeading && <Alert tone="success">{t('Ai cea mai mare oferta in acest moment.')}</Alert>}
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
        <p className="mt-1 text-xs text-slate-500">
          Oferta minima: <span className="font-semibold text-slate-700">{formatPrice(minimum)}</span> (pas de{' '}
          {formatPrice(step)})
        </p>
        {wallet && (
          <p className="mt-0.5 text-xs text-slate-500">
            Disponibil in portofel: <span className="font-semibold text-slate-700">{formatPrice(wallet.available)}</span>
          </p>
        )}
      </div>

      <div className="grid grid-cols-3 gap-2">
        {quickAmounts.map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => setAmount(String(value))}
            className="rounded-lg bg-slate-100 py-2 text-xs font-medium text-slate-700 transition hover:bg-slate-200"
          >
            {formatPrice(value)}
          </button>
        ))}
      </div>

      <Button type="submit" loading={submitting} className="w-full py-3 text-base">
        Liciteaza
      </Button>

      <AutoBidBox item={item} minimum={minimum} onChange={onBid} />

      {item.buyNowAvailable && (
        <div className="border-t border-slate-100 pt-4">
          <button
            type="button"
            onClick={handleBuyNow}
            disabled={submitting}
            className="flex w-full items-center justify-center gap-2 rounded-lg bg-amber-500 py-3 text-base font-semibold text-white shadow-sm transition hover:bg-amber-400 disabled:opacity-50"
          >
            <Icon name="bolt" size={20} mono /> Cumpara acum cu {formatPrice(item.buyNowPrice)}
          </button>
          <p className="mt-1.5 text-center text-xs text-slate-500">{t('Castigi imediat, fara sa astepti finalul.')}</p>
        </div>
      )}

      <p className="flex items-center justify-center gap-1.5 text-center text-xs text-slate-400">
        <Icon name="timer" size={15} /> O oferta in ultimele 2 minute prelungeste licitatia cu 2 minute.
      </p>
    </form>
  )
}

// Dupa vanzare: vanzatorul vede datele castigatorului si invers, ca sa se inteleaga pentru plata si livrare
function ContactInfo({ itemId }) {
  const [contact, setContact] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api(`/items/${itemId}/contact`)
      .then(setContact)
      .catch((err) => setError(err.message))
  }, [itemId])

  if (error) {
    return (
      <div className="mt-6">
        <Alert>{error}</Alert>
      </div>
    )
  }
  if (!contact) return null

  return (
    <div className="mt-6 rounded-xl bg-emerald-50 p-4 ring-1 ring-emerald-200">
      <p className="flex items-center gap-2 text-sm font-semibold text-emerald-900">
        <Icon name="sparkles" size={20} />
        {contact.role === 'castigator' ? 'Ai vandut obiectul! Contacteaza castigatorul:' : 'Ai castigat! Contacteaza vanzatorul:'}
      </p>
      <p className="mt-2 text-sm text-emerald-900">
        <Link to={`/users/${encodeURIComponent(contact.username)}`} className="font-semibold hover:underline">
          {contact.username}
        </Link>
      </p>
      <a
        href={`mailto:${contact.email}`}
        className="mt-1 block truncate text-sm font-medium text-emerald-700 underline hover:text-emerald-600"
      >
        {contact.email}
      </a>
      <p className="mt-2 text-xs text-emerald-800/80">Stabiliti impreuna plata si livrarea. Emailul il vedeti doar voi doi.</p>
    </div>
  )
}

// Licitare automata: utilizatorul spune suma maxima, iar serverul liciteaza pentru el cu pasul minim
// de fiecare data cand altcineva il depaseste (maximul il vede doar el)
function AutoBidBox({ item, minimum, onChange }) {
  const [current, setCurrent] = useState(undefined) // undefined = se incarca, null = nu are
  const [open, setOpen] = useState(false)
  const [max, setMax] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api(`/items/${item.id}/auto-bid`)
      .then((data) => setCurrent(data.maxAmount))
      .catch(() => setCurrent(null))
  }, [item.id])

  async function save(method, body) {
    setError('')
    setBusy(true)
    try {
      const data = await api(`/items/${item.id}/auto-bid`, { method, body })
      setCurrent(data.maxAmount)
      setOpen(false)
      await onChange()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (current === undefined) return null
  const exceeded = current != null && Number(item.currentPrice) > Number(current)

  return (
    <div className="rounded-xl bg-indigo-50/70 p-3 ring-1 ring-indigo-100">
      {current != null && !open ? (
        <div className="space-y-2">
          <p className="flex items-center gap-1.5 text-sm text-indigo-900">
            <Icon name="robot" size={18} /> Licitezi automat pana la <span className="font-semibold">{formatPrice(current)}</span>
          </p>
          {exceeded && <p className="text-xs font-medium text-red-600">Maximul tau a fost depasit de alta oferta.</p>}
          <div className="flex gap-3 text-xs font-semibold">
            <button type="button" onClick={() => setOpen(true)} className="text-indigo-600 hover:text-indigo-500">
              Schimba maximul
            </button>
            <button type="button" disabled={busy} onClick={() => save('DELETE')} className="text-red-600 hover:text-red-500">
              Opreste
            </button>
          </div>
        </div>
      ) : !open ? (
        <button type="button" onClick={() => setOpen(true)} className="w-full text-left text-sm">
          <span className="flex items-center gap-1.5 font-semibold text-indigo-700">
            <Icon name="robot" size={20} /> Licitare automata
          </span>
          <span className="block text-xs text-indigo-900/70">
            Spune suma maxima si licitam noi pentru tine, cu pasul minim, doar cat e nevoie.
          </span>
        </button>
      ) : (
        <div className="space-y-2">
          <p className="flex items-center gap-1.5 text-sm font-semibold text-indigo-700">
            <Icon name="robot" size={18} /> Suma maxima pe care o dai
          </p>
          {error && <Alert>{error}</Alert>}
          <div className="flex gap-2">
            <Input
              type="number"
              step="0.01"
              min={minimum}
              placeholder={`minim ${minimum}`}
              value={max}
              onChange={(e) => setMax(e.target.value)}
            />
            <Button type="button" loading={busy} disabled={!max} onClick={() => save('PUT', { maxAmount: Number(max) })}>
              Activeaza
            </Button>
          </div>
          <p className="text-xs text-indigo-900/70">
            Maximul ramane secret. Daca altcineva liciteaza, raspundem automat pana la aceasta suma.
          </p>
          <button type="button" onClick={() => setOpen(false)} className="text-xs font-medium text-slate-500 hover:text-slate-700">
            Renunta
          </button>
        </div>
      )}
    </div>
  )
}

// Live price of a running Dutch auction (ticks every second, drops every minute)
function LiveDutchPrice({ item }) {
  const price = dutchPrice(item, useNow())
  return (
    <p key={price} className="mt-1 origin-left animate-price-flash text-4xl font-bold tracking-tight text-sky-700">
      {formatPrice(price)}
    </p>
  )
}

// Dutch auction: no bids; the first one to accept the current (dropping) price wins
function DutchPanel({ item, wallet, onBuy }) {
  const now = useNow()
  const [error, setError] = useState('')
  const [buying, setBuying] = useState(false)
  const price = dutchPrice(item, now)
  const start = Number(item.startPrice)
  const floor = Number(item.floorPrice)
  const progress = start > floor ? (start - price) / (start - floor) : 1
  const secondsToDrop = 60 - (Math.floor((now - parseDate(item.startTime).getTime()) / 1000) % 60)

  async function buy() {
    if (!window.confirm(`Cumperi "${item.name}" acum cu ${formatPrice(price)}? Licitatia se incheie imediat.`)) return
    setError('')
    setBuying(true)
    try {
      await api(`/items/${item.id}/buy-now`, { method: 'POST' })
      await onBuy()
    } catch (err) {
      setError(err.message)
      setBuying(false)
    }
  }

  return (
    <div className="space-y-4">
      <div className="rounded-xl bg-sky-50 p-4 ring-1 ring-sky-100">
        <p className="flex items-center gap-1.5 text-sm font-semibold text-sky-900">
          <Icon name="trendDown" size={18} /> Licitatie olandeza
        </p>
        <p className="mt-1 text-xs text-sky-900/75">
          Pretul scade cu {formatPrice(item.priceDrop)} in fiecare minut, pana la {formatPrice(item.floorPrice)}. Primul
          care cumpara castiga.
        </p>
        <div className="mt-3 h-2 overflow-hidden rounded-full bg-sky-100">
          <div
            className="h-full rounded-full bg-gradient-to-r from-sky-500 to-indigo-500 transition-all duration-700"
            style={{ width: `${Math.min(100, progress * 100)}%` }}
          />
        </div>
        <p className="mt-2 text-xs font-medium text-sky-900/80">
          {price <= floor ? 'Pretul a ajuns la minim.' : `Urmatoarea scadere in ${secondsToDrop}s`}
        </p>
      </div>
      <Alert>{error}</Alert>
      <button
        type="button"
        onClick={buy}
        disabled={buying}
        className="flex w-full items-center justify-center gap-2 rounded-lg bg-gradient-to-r from-sky-500 to-indigo-600 py-3 text-base font-semibold text-white shadow-md transition hover:brightness-110 disabled:opacity-50"
      >
        <Icon name="bolt" size={20} mono /> Cumpara acum cu {formatPrice(price)}
      </button>
      {wallet && (
        <p className="text-center text-xs text-slate-500">
          Disponibil in portofel: <span className="font-semibold text-slate-700">{formatPrice(wallet.available)}</span>
        </p>
      )}
    </div>
  )
}
