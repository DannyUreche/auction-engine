import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { api, formatPrice } from './api.js'
import { useAuth } from './auth.jsx'
import { useAuctionEvents } from './events.jsx'

const WatchlistContext = createContext(null)

// Lista de favorite ("Urmaresc") a utilizatorului logat, plus notificarile pentru obiectele din ea:
// cand cineva liciteaza la un obiect urmarit sau licitatia lui se incheie, apare un mesaj in coltul paginii.
export function WatchlistProvider({ children }) {
  const { user } = useAuth()
  const location = useLocation()
  const userId = user?.id

  // Lista tine minte al cui este: dupa logout (sau alt cont) nu se mai afiseaza cea veche
  const [state, setState] = useState({ userId: null, items: [] })
  const loaded = state.userId === userId
  const items = loaded ? state.items : []

  const reload = useCallback(() => {
    if (!userId) return
    api('/items/watched')
      .then((data) => setState({ userId, items: data }))
      .catch(() => {})
  }, [userId])

  useEffect(() => {
    reload()
  }, [reload])

  const isWatched = (itemId) => items.some((w) => w.id === itemId)

  // Interfata se schimba imediat; daca serverul refuza, reincarcam lista reala
  async function toggle(item) {
    const watching = isWatched(item.id)
    setState((s) => {
      const current = s.userId === userId ? s.items : []
      return { userId, items: watching ? current.filter((w) => w.id !== item.id) : [item, ...current] }
    })
    try {
      await api(`/items/${item.id}/watch`, { method: watching ? 'DELETE' : 'POST' })
    } catch (err) {
      reload()
      throw err
    }
  }

  // ---------- Notificari ----------
  const [toasts, setToasts] = useState([])
  const nextId = useRef(1)

  const dismiss = useCallback((id) => setToasts((list) => list.filter((t) => t.id !== id)), [])

  const notify = useCallback(
    (text, itemId) => {
      const id = nextId.current++
      // Cel mult 3 mesaje deodata; fiecare dispare singur dupa 8 secunde
      setToasts((list) => [...list.slice(-2), { id, text, itemId }])
      setTimeout(() => dismiss(id), 8000)
    },
    [dismiss],
  )

  useAuctionEvents((event) => {
    const watched = items.find((w) => w.id === event.itemId)
    if (!watched) return

    if (event.type === 'deleted') {
      setState((s) => ({ ...s, items: s.items.filter((w) => w.id !== event.itemId) }))
      return
    }
    // Pret si stare noua pentru tabul "Urmarite"
    reload()

    // Pe pagina obiectului schimbarea se vede deja acolo; propriile oferte nu se anunta
    if (location.pathname === `/items/${event.itemId}`) return
    const name = `"${watched.name}"`
    if (event.type === 'bid' && event.username !== user.username) {
      notify(`Oferta noua la ${name}: ${formatPrice(event.amount)} de la ${event.username}`, event.itemId)
    }
    if (event.type === 'closed') {
      if (event.username === user.username) notify(`🏆 Ai castigat ${name}!`, event.itemId)
      else if (event.username) notify(`Licitatia pentru ${name} s-a incheiat la ${formatPrice(event.amount)}`, event.itemId)
      else notify(`Licitatia pentru ${name} s-a incheiat fara oferte`, event.itemId)
    }
  })

  return (
    <WatchlistContext.Provider value={{ items, loaded, isWatched, toggle }}>
      {children}

      <div className="pointer-events-none fixed inset-x-4 bottom-4 z-30 flex flex-col gap-2 sm:left-auto sm:w-96">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            role="status"
            className="stay-dark pointer-events-auto flex animate-slide-in items-start gap-3 rounded-xl bg-slate-900/95 px-4 py-3 text-sm text-white shadow-2xl ring-1 ring-white/10 backdrop-blur"
          >
            <span aria-hidden="true">🔔</span>
            <div className="min-w-0 flex-1">
              <p>{toast.text}</p>
              <Link
                to={`/items/${toast.itemId}`}
                onClick={() => dismiss(toast.id)}
                className="mt-1 inline-block font-semibold text-indigo-300 hover:text-indigo-200"
              >
                Vezi obiectul →
              </Link>
            </div>
            <button
              onClick={() => dismiss(toast.id)}
              aria-label="Inchide notificarea"
              className="text-slate-400 hover:text-white"
            >
              ✕
            </button>
          </div>
        ))}
      </div>
    </WatchlistContext.Provider>
  )
}

export const useWatchlist = () => useContext(WatchlistContext)

// Butonul cu inima: urmareste / nu mai urmari un obiect.
// large: varianta cu text (pagina obiectului); altfel doar iconita rotunda (pe carduri)
export function WatchButton({ item, large = false }) {
  const { user } = useAuth()
  const { isWatched, toggle } = useWatchlist()
  const navigate = useNavigate()
  const location = useLocation()

  // Propriul obiect nu se poate urmari
  if (user && item.seller?.id === user.id) return null
  const watching = isWatched(item.id)

  function handleClick(event) {
    // Pe card butonul e in interiorul link-ului catre obiect: nu vrem sa se deschida si pagina obiectului
    event.preventDefault()
    event.stopPropagation()
    if (!user) {
      navigate('/login', { state: { from: location.pathname } })
      return
    }
    toggle(item).catch(() => {})
  }

  const label = watching ? 'Nu mai urmari' : 'Urmareste'

  if (large) {
    return (
      <button
        type="button"
        onClick={handleClick}
        aria-pressed={watching}
        title={watching ? 'Primesti notificari pentru acest obiect' : 'Primeste notificari cand cineva liciteaza'}
        className={`inline-flex shrink-0 items-center gap-2 rounded-lg px-3.5 py-2 text-sm font-semibold transition ${
          watching
            ? 'bg-rose-50 text-rose-600 ring-1 ring-rose-200 hover:bg-rose-100'
            : 'bg-white text-slate-700 ring-1 ring-slate-200 hover:bg-slate-50'
        }`}
      >
        <Heart filled={watching} />
        {watching ? 'Urmaresti' : 'Urmareste'}
      </button>
    )
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      aria-pressed={watching}
      aria-label={label}
      title={label}
      className={`grid h-9 w-9 place-items-center rounded-full shadow-sm backdrop-blur transition ${
        watching ? 'bg-rose-500 text-white hover:bg-rose-600' : 'bg-white/90 text-slate-600 hover:text-rose-500'
      }`}
    >
      <Heart filled={watching} />
    </button>
  )
}

function Heart({ filled }) {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill={filled ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z"
      />
    </svg>
  )
}
