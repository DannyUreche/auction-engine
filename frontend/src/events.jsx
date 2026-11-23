import { useEffect, useRef, useState } from 'react'

// Asculta evenimentele in timp real de la server (/api/events) si apeleaza onEvent pentru fiecare.
// Evenimente: { type: 'bid' | 'closed' | 'deleted', itemId, amount, username }
// Intoarce true cat timp conexiunea este activa. EventSource se reconecteaza singur daca pica.
export function useAuctionEvents(onEvent) {
  const handlerRef = useRef(onEvent)
  const [connected, setConnected] = useState(false)

  // Pastram mereu ultima versiune a functiei, fara sa redeschidem conexiunea
  useEffect(() => {
    handlerRef.current = onEvent
  })

  useEffect(() => {
    const source = new EventSource('/api/events')
    const forward = (type) => (message) => handlerRef.current({ type, ...JSON.parse(message.data) })

    source.addEventListener('connected', () => setConnected(true))
    source.addEventListener('bid', forward('bid'))
    source.addEventListener('closed', forward('closed'))
    source.addEventListener('deleted', forward('deleted'))
    source.onerror = () => setConnected(false)

    return () => source.close()
  }, [])

  return connected
}

// Mic indicator "Live" care arata ca pagina primeste actualizari instant
export function LiveBadge({ connected }) {
  return (
    <span
      title={connected ? 'Pagina se actualizeaza instant' : 'Se reconecteaza...'}
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
        connected ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-500'
      }`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${connected ? 'animate-pulse bg-emerald-500' : 'bg-slate-400'}`} />
      {connected ? 'Live' : 'Offline'}
    </span>
  )
}
