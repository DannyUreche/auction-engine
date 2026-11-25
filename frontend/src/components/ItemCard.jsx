import { Link, useNavigate } from 'react-router'
import { formatPrice } from '../api.js'
import { findCategory, useCategories } from '../categories.js'
import { WatchButton } from '../watchlist.jsx'
import { CategoryIcon, Icon } from './icons.jsx'
import Countdown from './Countdown.jsx'
import ItemImage from './ItemImage.jsx'
import { useT } from '../i18n.jsx'

// Cardul unui obiect din liste (pagina principala, contul meu, profil).
// index: pozitia in lista - cardurile apar pe rand, cu o mica intarziere intre ele
export default function ItemCard({ item, extra, index = 0 }) {
  const t = useT()
  const hasBids = Number(item.currentPrice) > Number(item.startPrice)
  const category = findCategory(useCategories(), item.category)
  const navigate = useNavigate()

  // Cardul e deja un link (catre obiect), iar link-urile nu pot fi unul in altul:
  // numele vanzatorului deschide profilul lui din cod
  function openSeller(event) {
    event.preventDefault()
    event.stopPropagation()
    navigate(`/users/${encodeURIComponent(item.seller.username)}`)
  }

  // Lumina de pe card urmareste mouse-ul (pozitia se da prin variabilele CSS --x si --y)
  function handleMouseMove(event) {
    const rect = event.currentTarget.getBoundingClientRect()
    event.currentTarget.style.setProperty('--x', `${event.clientX - rect.left}px`)
    event.currentTarget.style.setProperty('--y', `${event.clientY - rect.top}px`)
    // Inclinare 3D usoara dupa pozitia mouse-ului (maxim ~6 grade)
    const rotateY = ((event.clientX - rect.left) / rect.width - 0.5) * 12
    const rotateX = -((event.clientY - rect.top) / rect.height - 0.5) * 12
    event.currentTarget.style.transform = `perspective(900px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`
  }

  function handleMouseLeave(event) {
    event.currentTarget.style.transform = ''
  }

  return (
    <Link
      to={`/items/${item.id}`}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      style={{ animationDelay: `${Math.min(index, 11) * 60}ms` }}
      className="neon-card group relative flex animate-fade-up flex-col overflow-hidden rounded-2xl bg-white/90 shadow-sm ring-1 ring-slate-200/80 backdrop-blur transition duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-indigo-500/10 hover:ring-indigo-300"
    >
      <div className="spotlight pointer-events-none absolute inset-0 z-10 opacity-0 transition duration-300 group-hover:opacity-100" />

      <div className="relative overflow-hidden">
        <ItemImage item={item} className="aspect-[4/3] w-full transition duration-500 group-hover:scale-105" />
        <div className="absolute inset-x-0 bottom-0 h-16 bg-gradient-to-t from-black/25 to-transparent" />
        <div className="absolute top-3 left-3 z-20">
          <WatchButton item={item} />
        </div>
        <div className="absolute top-3 right-3 rounded-full shadow-sm">
          <Countdown endTime={item.endTime} active={item.active} startTime={item.startTime} />
        </div>
        {item.auctionType === 'DUTCH' && item.active && (
          <div className="absolute bottom-3 left-3 flex items-center gap-1 rounded-full bg-gradient-to-r from-sky-500 to-indigo-500 px-2.5 py-0.5 text-xs font-semibold text-white shadow-md">
            <Icon name="trendDown" size={13} mono /> {t('Olandeza · pretul scade')}
          </div>
        )}
        {item.buyNowAvailable && item.auctionType !== 'DUTCH' && (
          <div className="absolute bottom-3 left-3 flex items-center gap-1 rounded-full bg-gradient-to-r from-amber-500 to-orange-500 px-2.5 py-0.5 text-xs font-semibold text-white shadow-md">
            <Icon name="bolt" size={13} mono /> {t('Cumpara acum')} {formatPrice(item.buyNowPrice)}
          </div>
        )}
        {item.imageUrls?.length > 1 && (
          <div className="absolute right-3 bottom-3 flex items-center gap-1 rounded-full bg-black/50 px-2 py-0.5 text-[11px] font-medium text-white backdrop-blur">
            <Icon name="camera" size={13} mono /> {item.imageUrls.length}
          </div>
        )}
      </div>

      <div className="flex flex-1 flex-col p-5">
        <p className="flex items-center gap-1.5 text-xs font-medium text-slate-400">
          <CategoryIcon category={category} size={15} /> {t(category.label)}
          {item.county && (
            <span className="ml-auto flex min-w-0 items-center gap-1">
              <Icon name="pin" size={14} /> <span className="truncate">{item.county}</span>
            </span>
          )}
        </p>
        <h3 className="mt-0.5 line-clamp-1 font-semibold text-slate-900 transition group-hover:text-indigo-600">{item.name}</h3>
        <p className="mt-1 line-clamp-2 min-h-10 text-sm text-slate-500">{item.description || t('Fara descriere')}</p>

        <div className="mt-4 flex items-end justify-between gap-3 border-t border-slate-100 pt-4">
          <div>
            <p className="text-xs font-medium tracking-wide text-slate-400 uppercase">
              {t(item.active ? (hasBids ? 'Oferta curenta' : 'Pret de pornire') : 'Pret final')}
            </p>
            {/* key = pretul: la o oferta noua elementul se recreeaza si animatia porneste din nou */}
            <p key={item.currentPrice} className="origin-left animate-price-flash text-xl font-bold text-slate-900">
              {formatPrice(item.currentPrice)}
            </p>
          </div>
          <p className="min-w-0 truncate text-xs text-slate-500">
            {t('de')}{' '}
            <span role="link" onClick={openSeller} className="relative z-20 font-medium hover:text-indigo-600 hover:underline">
              {item.seller?.username}
            </span>
          </p>
        </div>

        {extra && <div className="mt-3">{extra}</div>}
      </div>
    </Link>
  )
}
