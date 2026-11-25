import { useEffect, useRef, useState } from 'react'

// O singura conexiune la /api/events pentru toata pagina, impartita intre toate componentele care asculta.
// (Browserul permite doar cateva conexiuni deschise in acelasi timp catre un server; cu cate o conexiune
// pentru fiecare componenta, cateva taburi deschise ar bloca restul cererilor.)
const listeners = new Set()
const statusListeners = new Set()
let source = null
let connected = false

function setConnected(value) {
  connected = value
  statusListeners.forEach((listener) => listener(value))
}

function subscribe(listener, onStatus) {
  listeners.add(listener)
  statusListeners.add(onStatus)

  if (!source) {
    source = new EventSource('/api/events')
    const forward = (type) => (message) => {
      const event = { type, ...JSON.parse(message.data) }
      listeners.forEach((l) => l(event))
    }
    source.addEventListener('connected', () => setConnected(true))
    for (const type of ['bid', 'closed', 'deleted', 'notification', 'message', 'update', 'comment']) source.addEventListener(type, forward(type))
    source.onerror = () => setConnected(false)
  }
  onStatus(connected)

  return () => {
    listeners.delete(listener)
    statusListeners.delete(onStatus)
    // Nimeni nu mai asculta: inchidem conexiunea
    if (listeners.size === 0) {
      source.close()
      source = null
      connected = false
    }
  }
}

// Asculta evenimentele in timp real de la server si apeleaza onEvent pentru fiecare.
// Evenimente: { type: 'bid' | 'closed' | 'deleted' | 'notification' | 'message', itemId, amount, username }
// Intoarce true cat timp conexiunea este activa. EventSource se reconecteaza singur daca pica.
export function useAuctionEvents(onEvent) {
  const handlerRef = useRef(onEvent)
  const [isConnected, setIsConnected] = useState(false)

  // Pastram mereu ultima versiune a functiei, fara sa redeschidem conexiunea
  useEffect(() => {
    handlerRef.current = onEvent
  })

  useEffect(() => subscribe((event) => handlerRef.current(event), setIsConnected), [])

  return isConnected
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
