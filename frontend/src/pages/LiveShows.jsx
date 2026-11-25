import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, parseDate } from '../api.js'
import { useAuth } from '../auth.jsx'
import { useNow } from '../components/Countdown.jsx'
import { Icon } from '../components/icons.jsx'
import { EmptyState, Loading } from '../components/ui.jsx'

// List of live shows: running ones first, then upcoming, then the ones that just ended
export default function LiveShows() {
  const { user } = useAuth()
  const [shows, setShows] = useState(null)
  const now = useNow()

  useEffect(() => {
    const load = () =>
      api('/shows', { auth: false })
        .then(setShows)
        .catch(() => setShows([]))
    load()
    const id = setInterval(load, 20000)
    return () => clearInterval(id)
  }, [])

  return (
    <div>
      <section className="stay-dark relative overflow-hidden rounded-3xl bg-slate-950 px-6 py-10 text-white shadow-2xl ring-1 ring-white/10 sm:px-10">
        <div aria-hidden="true" className="pointer-events-none absolute inset-0">
          <div className="featured-beam absolute -top-1/2 left-1/3 h-[200%] w-40 bg-gradient-to-b from-fuchsia-300/25 to-transparent blur-2xl" />
          <div className="absolute -right-20 -bottom-24 h-72 w-72 animate-float rounded-full bg-red-500/30 blur-3xl" />
        </div>
        <div className="relative flex flex-wrap items-end justify-between gap-6">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full bg-red-600 px-3 py-1 text-xs font-black tracking-widest">
              <span className="h-2 w-2 animate-pulse rounded-full bg-white" /> ON AIR
            </span>
            <h1 className="mt-4 text-4xl font-black tracking-tight">Licitatii live</h1>
            <p className="mt-2 max-w-xl text-white/70">
              Obiectele intra pe scena unul dupa altul, cu cronometru, ca la o casa de licitatii adevarata. Liciteaza in direct si
              vezi ciocanul cazand.
            </p>
          </div>
          {user && (
            <Link to="/live/nou" className="inline-flex items-center gap-2 rounded-xl bg-white px-5 py-3 text-sm font-bold text-slate-900 shadow-lg transition hover:-translate-y-0.5">
              <Icon name="gavel" size={18} mono /> Organizeaza o licitatie live
            </Link>
          )}
        </div>
      </section>

      <div className="mt-8">
        {!shows ? (
          <Loading />
        ) : shows.length === 0 ? (
          <EmptyState title="Nicio licitatie live programata">Vanzatorii pot pune pe scena 2-10 obiecte, fiecare cu cateva minute de licitare.</EmptyState>
        ) : (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {shows.map((show, index) => {
              const startsIn = parseDate(show.startTime).getTime() - now
              const live = show.status === 'LIVE' || (show.status === 'SCHEDULED' && startsIn <= 0)
              return (
                <Link
                  key={show.id}
                  to={`/live/${show.id}`}
                  style={{ animationDelay: `${index * 70}ms` }}
                  className={`group relative animate-fade-up overflow-hidden rounded-3xl bg-white/85 shadow-sm ring-1 transition hover:-translate-y-1 hover:shadow-xl ${
                    live ? 'ring-2 ring-red-400 shadow-red-500/10' : 'ring-slate-200/80'
                  } ${show.status === 'ENDED' ? 'opacity-70' : ''}`}
                >
                  <div className="stay-dark relative aspect-[16/9] overflow-hidden bg-slate-900">
                    {show.coverUrl && <img src={`${show.coverUrl}?size=thumb`} alt="" className="h-full w-full object-cover opacity-80 transition duration-700 group-hover:scale-105" />}
                    <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />
                    <span
                      className={`absolute top-3 left-3 inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-black tracking-wider text-white ${
                        live ? 'bg-red-600' : show.status === 'ENDED' ? 'bg-slate-600' : 'bg-indigo-600'
                      }`}
                    >
                      {live && <span className="h-2 w-2 animate-pulse rounded-full bg-white" />}
                      {live ? 'LIVE ACUM' : show.status === 'ENDED' ? 'S-A INCHEIAT' : `IN ${Math.max(1, Math.round(startsIn / 60000))} MIN`}
                    </span>
                    <p className="absolute right-4 bottom-3 left-4 truncate text-lg font-bold text-white">{show.title}</p>
                  </div>
                  <div className="flex items-center justify-between p-4 text-sm text-slate-600">
                    <span>
                      cu <b className="text-slate-900">{show.sellerUsername}</b>
                    </span>
                    <span>
                      {show.items} obiecte · {Math.round(show.slotSeconds / 60)} min
                    </span>
                  </div>
                </Link>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
