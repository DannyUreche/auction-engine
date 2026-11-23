import { Link } from 'react-router'
import { formatPrice } from '../api.js'
import Countdown from './Countdown.jsx'

// Cardul unui obiect din liste (pagina principala, contul meu)
export default function ItemCard({ item, extra }) {
  const hasBids = Number(item.currentPrice) > Number(item.startPrice)

  return (
    <Link
      to={`/items/${item.id}`}
      className="group flex flex-col rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200 transition hover:-translate-y-0.5 hover:shadow-md hover:ring-indigo-300"
    >
      <div className="flex items-start justify-between gap-3">
        <h3 className="line-clamp-2 font-semibold text-slate-900 group-hover:text-indigo-600">{item.name}</h3>
        <Countdown endTime={item.endTime} active={item.active} />
      </div>

      <p className="mt-2 line-clamp-2 min-h-10 text-sm text-slate-500">
        {item.description || 'Fara descriere'}
      </p>

      <div className="mt-5 flex items-end justify-between border-t border-slate-100 pt-4">
        <div>
          <p className="text-xs font-medium tracking-wide text-slate-400 uppercase">
            {item.active ? (hasBids ? 'Oferta curenta' : 'Pret de pornire') : 'Pret final'}
          </p>
          <p className="text-xl font-bold text-slate-900">{formatPrice(item.currentPrice)}</p>
        </div>
        <p className="text-xs text-slate-500">de {item.seller?.username}</p>
      </div>

      {extra && <div className="mt-3">{extra}</div>}
    </Link>
  )
}
