import { Link } from 'react-router'
import { Icon } from './icons.jsx'

// A list card with a 2x2 mosaic of its photos (empty tiles get a soft gradient)
export default function CollectionCard({ list, index = 0, children }) {
  const tiles = [0, 1, 2, 3].map((i) => list.previews[i])
  return (
    <div className="animate-fade-up group relative overflow-hidden rounded-2xl bg-white/85 shadow-sm ring-1 ring-slate-200/80 transition hover:-translate-y-0.5 hover:shadow-lg hover:ring-indigo-300" style={{ animationDelay: `${index * 60}ms` }}>
      <Link to={`/colectii/${list.id}`} className="block">
        <div className="grid aspect-[16/10] grid-cols-2 grid-rows-2 gap-0.5 bg-slate-200/70">
          {tiles.map((url, i) =>
            url ? (
              <img key={i} src={`${url}?size=thumb`} alt="" loading="lazy" className="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
            ) : (
              <span key={i} className={`grid place-items-center bg-gradient-to-br ${i % 3 === 0 ? 'from-indigo-50 to-violet-100' : 'from-amber-50 to-orange-100'}`}>
                {i === 0 && list.previews.length === 0 && <Icon name="bookmark" size={28} />}
              </span>
            ),
          )}
        </div>
        <div className="p-4">
          <p className="flex items-center gap-2">
            <span className="min-w-0 flex-1 truncate font-semibold text-slate-900 group-hover:text-indigo-700">{list.title}</span>
            <span
              className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold tracking-wide uppercase ring-1 ${
                list.publicList ? 'bg-emerald-50 text-emerald-700 ring-emerald-200' : 'bg-slate-100 text-slate-600 ring-slate-200'
              }`}
            >
              {list.publicList ? 'Publica' : 'Privata'}
            </span>
          </p>
          <p className="mt-1 text-xs text-slate-500">
            {list.items} {list.items === 1 ? 'obiect' : 'obiecte'}
            {list.description ? ` · ${list.description}` : ''}
          </p>
        </div>
      </Link>
      {children}
    </div>
  )
}
