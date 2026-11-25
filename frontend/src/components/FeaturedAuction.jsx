import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, formatPrice, parseDate } from '../api.js'
import { findCategory, useCategories } from '../categories.js'
import { launchConfetti } from '../confetti.js'
import { useAuctionEvents } from '../events.jsx'
import { useT } from '../i18n.jsx'
import { useNow } from './Countdown.jsx'
import { CategoryIcon, Icon } from './icons.jsx'
import ItemImage from './ItemImage.jsx'

// "Licitatia saptamanii": the highlighted auction on the home page - a stage with moving spotlights, the photo
// in a golden frame, a flip-clock countdown, the top bidders and a big call to action. Updates live; when it
// sells, confetti and a "VANDUT" stamp, then the next featured auction takes its place.
export default function FeaturedAuction() {
  const t = useT()
  const categories = useCategories()
  const [featured, setFeatured] = useState(null)
  const [sold, setSold] = useState(null) // { username, amount } shown for a few seconds after it closes

  const load = useCallback(
    () =>
      api('/items/featured', { auth: false })
        .then((data) => setFeatured(data || null))
        .catch(() => {}),
    [],
  )

  useEffect(() => {
    load()
  }, [load])

  useAuctionEvents((event) => {
    if (!featured || event.itemId !== featured.item.id) return
    if (event.type === 'closed') {
      setSold({ username: event.username, amount: event.amount })
      launchConfetti()
      setTimeout(() => {
        setSold(null)
        load()
      }, 8000)
    } else {
      load()
    }
  })

  if (!featured) return null
  const { item, leaderboard, bids, watchers, chosenByAdmin } = featured
  const category = findCategory(categories, item.category)
  const top = leaderboard[0]?.amount ?? 1

  return (
    <section className="stay-dark relative mt-10 overflow-hidden rounded-3xl bg-slate-950 text-white shadow-2xl shadow-indigo-950/30 ring-1 ring-white/10">
      {/* Stage: two slow spotlights, a warm glow under the photo, fine sparkles */}
      <div aria-hidden="true" className="pointer-events-none absolute inset-0">
        <div className="featured-beam absolute -top-1/2 left-1/4 h-[200%] w-40 bg-gradient-to-b from-amber-200/25 via-amber-100/5 to-transparent blur-2xl" />
        <div className="featured-beam absolute -top-1/2 left-2/3 h-[200%] w-32 bg-gradient-to-b from-fuchsia-300/20 via-fuchsia-200/5 to-transparent blur-2xl [animation-delay:-6s]" />
        <div className="absolute -bottom-32 left-10 h-72 w-[36rem] rounded-full bg-amber-500/15 blur-3xl" />
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_1px_1px,rgb(255_255_255/0.07)_1px,transparent_0)] bg-[size:22px_22px]" />
      </div>

      <div className="relative grid gap-8 p-6 sm:p-8 lg:grid-cols-[1.05fr_1fr] lg:items-center">
        {/* Photo in a golden frame with the ribbon */}
        <Link to={`/items/${item.id}`} className="group relative block">
          <div className="absolute -inset-1 rounded-[1.4rem] bg-gradient-to-br from-amber-300 via-amber-500 to-orange-600 opacity-80 blur-sm transition group-hover:opacity-100" />
          <div className="relative overflow-hidden rounded-2xl ring-1 ring-amber-200/40">
            <ItemImage item={item} className="aspect-[4/3] w-full transition duration-700 group-hover:scale-105" />
            <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/50 to-transparent" />
          </div>
          <div className="featured-ribbon absolute -top-3 -left-3 flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-amber-300 to-orange-400 px-3 py-1.5 text-xs font-black tracking-widest text-slate-950 uppercase shadow-lg shadow-amber-500/30">
            <Icon name="trophy" size={16} mono /> {t('Licitatia saptamanii')}
          </div>
          {sold && (
            <div className="absolute inset-0 grid place-items-center rounded-2xl bg-slate-950/55 backdrop-blur-[2px]">
              <span className="animate-stamp rounded-xl border-4 border-rose-400 px-5 py-2 text-3xl font-black tracking-widest text-rose-300">
                {t('VANDUT!')}
              </span>
            </div>
          )}
        </Link>

        <div className="min-w-0">
          <p className="flex flex-wrap items-center gap-2 text-xs font-medium text-amber-100/80">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-white/10 px-2.5 py-1 ring-1 ring-white/15">
              <CategoryIcon category={category} size={14} /> {t(category.label)}
            </span>
            {item.county && (
              <span className="inline-flex items-center gap-1 rounded-full bg-white/10 px-2.5 py-1 ring-1 ring-white/15">
                <Icon name="pin" size={13} /> {item.county}
              </span>
            )}
            <span className="inline-flex items-center gap-1 rounded-full bg-amber-400/15 px-2.5 py-1 text-amber-200 ring-1 ring-amber-300/30">
              <Icon name="sparkles" size={13} /> {chosenByAdmin ? t('Aleasa de echipa') : t('Cea mai urmarita')}
            </span>
          </p>
          <h2 className="mt-3 text-2xl leading-tight font-bold tracking-tight sm:text-3xl">{item.name}</h2>

          <div className="mt-5 flex flex-wrap items-end gap-x-8 gap-y-3">
            <div>
              <p className="text-xs font-medium tracking-wide text-white/50 uppercase">{sold ? t('Vandut cu') : t('Oferta curenta')}</p>
              <p key={item.currentPrice} className="origin-left animate-price-flash bg-gradient-to-r from-amber-200 via-amber-300 to-orange-300 bg-clip-text text-4xl font-black tracking-tight text-transparent">
                {formatPrice(sold?.amount ?? item.currentPrice)}
              </p>
            </div>
            <p className="pb-1 text-sm text-white/60">
              <b className="text-white">{bids}</b> {bids === 1 ? t('oferta') : t('oferte')} · <b className="text-white">{watchers}</b> {t('urmaritori')}
            </p>
          </div>

          {sold ? (
            <p className="mt-6 rounded-2xl bg-emerald-400/10 p-4 text-emerald-200 ring-1 ring-emerald-300/30">
              <Icon name="trophy" size={18} /> {sold.username ? t('Castigata de {u}!', { u: sold.username }) : t('Licitatia s-a incheiat.')}
            </p>
          ) : (
            <FlipCountdown endTime={item.endTime} />
          )}

          {leaderboard.length > 0 && (
            <ol className="mt-6 space-y-1.5">
              {leaderboard.map((bidder, index) => (
                <li key={bidder.username} className="flex items-center gap-3 text-sm">
                  <span
                    className={`grid h-7 w-7 shrink-0 place-items-center rounded-full text-xs font-black ${
                      ['bg-amber-300 text-slate-950 shadow-md shadow-amber-400/40', 'bg-slate-200 text-slate-900', 'bg-orange-400 text-slate-950'][index] ??
                      'bg-white/10 text-white/70'
                    }`}
                  >
                    {index + 1}
                  </span>
                  <span className="w-28 shrink-0 truncate font-medium">{bidder.username}</span>
                  <span className="h-1.5 flex-1 overflow-hidden rounded-full bg-white/10">
                    <span
                      className={`block h-full rounded-full ${index === 0 ? 'bg-gradient-to-r from-amber-300 to-orange-400' : 'bg-white/40'}`}
                      style={{ width: `${(Number(bidder.amount) / Number(top)) * 100}%` }}
                    />
                  </span>
                  <span className="w-24 shrink-0 text-right font-semibold tabular-nums">{formatPrice(bidder.amount)}</span>
                </li>
              ))}
            </ol>
          )}

          <div className="mt-7 flex flex-wrap gap-3">
            <Link
              to={`/items/${item.id}`}
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-amber-300 via-amber-400 to-orange-400 px-6 py-3 text-sm font-bold text-slate-950 shadow-lg shadow-amber-500/30 transition hover:-translate-y-0.5 hover:shadow-xl hover:shadow-amber-500/40"
            >
              <Icon name="gavel" size={18} mono /> {leaderboard.length ? t('Liciteaza acum') : t('Fii primul care liciteaza')} →
            </Link>
          </div>
        </div>
      </div>
    </section>
  )
}

// Flip clock: each digit pair turns over when it changes
function FlipCountdown({ endTime }) {
  const t = useT()
  const now = useNow()
  const left = Math.max(0, Math.floor((parseDate(endTime).getTime() - now) / 1000))
  const parts = [
    [t('zile'), Math.floor(left / 86400)],
    [t('ore'), Math.floor((left % 86400) / 3600)],
    [t('min'), Math.floor((left % 3600) / 60)],
    [t('sec'), left % 60],
  ]
  return (
    <div className="mt-6 flex gap-2.5" role="timer" aria-label={t('Timp ramas')}>
      {parts.map(([label, value]) => (
        <div key={label} className="text-center">
          <div className="relative h-16 w-16 [perspective:300px] sm:h-[4.5rem] sm:w-[4.5rem]">
            <div className="absolute inset-0 rounded-xl bg-gradient-to-b from-slate-800 to-slate-900 shadow-lg ring-1 ring-white/10" />
            <div className="absolute inset-x-0 top-1/2 h-px bg-black/50" />
            <span key={value} className="flip-digit absolute inset-0 grid place-items-center text-3xl font-black tabular-nums">
              {String(value).padStart(2, '0')}
            </span>
          </div>
          <p className="mt-1.5 text-[10px] font-semibold tracking-widest text-white/50 uppercase">{label}</p>
        </div>
      ))}
    </div>
  )
}
