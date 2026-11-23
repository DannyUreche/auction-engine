import { Link, useNavigate } from 'react-router'
import { formatPrice } from '../api.js'
import { findCategory, useCategories } from '../categories.js'
import { WatchButton } from '../watchlist.jsx'
import Countdown from './Countdown.jsx'
import ItemImage from './ItemImage.jsx'

// Cardul unui obiect din liste (pagina principala, contul meu)
export default function ItemCard({ item, extra }) {
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

  return (
    <Link
      to={`/items/${item.id}`}
      className="group flex flex-col overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200 transition hover:-translate-y-0.5 hover:shadow-md hover:ring-indigo-300"
    >
      <div className="relative overflow-hidden">
        <ItemImage item={item} className="aspect-[4/3] w-full transition duration-300 group-hover:scale-105" />
        <div className="absolute top-3 left-3">
          <WatchButton item={item} />
        </div>
        <div className="absolute top-3 right-3 rounded-full shadow-sm">
          <Countdown endTime={item.endTime} active={item.active} />
        </div>
        {item.buyNowAvailable && (
          <div className="absolute bottom-3 left-3 rounded-full bg-amber-500 px-2.5 py-0.5 text-xs font-semibold text-white shadow-sm">
            ⚡ Cumpara acum {formatPrice(item.buyNowPrice)}
          </div>
        )}
      </div>

      <div className="flex flex-1 flex-col p-5">
        <p className="text-xs font-medium text-slate-400">
          {category.icon} {category.label}
        </p>
        <h3 className="mt-0.5 line-clamp-1 font-semibold text-slate-900 group-hover:text-indigo-600">{item.name}</h3>
        <p className="mt-1 line-clamp-2 min-h-10 text-sm text-slate-500">{item.description || 'Fara descriere'}</p>

        <div className="mt-4 flex items-end justify-between border-t border-slate-100 pt-4">
          <div>
            <p className="text-xs font-medium tracking-wide text-slate-400 uppercase">
              {item.active ? (hasBids ? 'Oferta curenta' : 'Pret de pornire') : 'Pret final'}
            </p>
            <p className="text-xl font-bold text-slate-900">{formatPrice(item.currentPrice)}</p>
          </div>
          <p className="text-xs text-slate-500">
            de{' '}
            <span role="link" onClick={openSeller} className="font-medium hover:text-indigo-600 hover:underline">
              {item.seller?.username}
            </span>
          </p>
        </div>

        {extra && <div className="mt-3">{extra}</div>}
      </div>
    </Link>
  )
}
