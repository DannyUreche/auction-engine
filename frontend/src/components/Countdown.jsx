import { useEffect, useState } from 'react'
import { parseDate } from '../api.js'
import { Badge } from './ui.jsx'

// Ora curenta, actualizata la fiecare secunda
function useNow() {
  const [now, setNow] = useState(Date.now())
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(id)
  }, [])
  return now
}

function formatRemaining(ms) {
  const total = Math.floor(ms / 1000)
  const days = Math.floor(total / 86400)
  const hours = Math.floor((total % 86400) / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = String(total % 60).padStart(2, '0')
  if (days > 0) return `${days}z ${hours}h ${minutes}m`
  if (hours > 0) return `${hours}h ${minutes}m ${seconds}s`
  return `${minutes}m ${seconds}s`
}

// Timpul ramas pana la finalul licitatiei
export default function Countdown({ endTime, active }) {
  const now = useNow()
  const remaining = parseDate(endTime).getTime() - now

  if (!active) return <Badge tone="gray">Incheiata</Badge>
  // Timpul a expirat, dar serverul inca n-a inchis licitatia (verifica o data pe minut)
  if (remaining <= 0) return <Badge tone="amber">Se inchide...</Badge>

  const urgent = remaining < 5 * 60 * 1000
  return (
    <Badge tone={urgent ? 'red' : 'green'}>
      <span className={`h-1.5 w-1.5 rounded-full ${urgent ? 'bg-red-500 animate-pulse' : 'bg-emerald-500'}`} />
      {formatRemaining(remaining)}
    </Badge>
  )
}

// Cronometrul mare din panoul de licitare: casute separate pentru zile / ore / minute / secunde
export function CountdownTiles({ endTime }) {
  const now = useNow()
  const remaining = Math.max(0, parseDate(endTime).getTime() - now)
  const total = Math.floor(remaining / 1000)
  const parts = [
    ['zile', Math.floor(total / 86400)],
    ['ore', Math.floor((total % 86400) / 3600)],
    ['min', Math.floor((total % 3600) / 60)],
    ['sec', total % 60],
  ]
  const urgent = remaining < 5 * 60 * 1000

  return (
    <div className="grid grid-cols-4 gap-2">
      {parts.map(([label, value]) => (
        <div
          key={label}
          className={`rounded-xl py-2 text-center ring-1 ${
            urgent ? 'bg-red-50 text-red-700 ring-red-200' : 'bg-slate-50 text-slate-900 ring-slate-200'
          }`}
        >
          <p key={value} className="animate-fade-in text-xl font-bold tabular-nums">
            {String(value).padStart(2, '0')}
          </p>
          <p className="text-[10px] font-medium tracking-wide uppercase opacity-60">{label}</p>
        </div>
      ))}
    </div>
  )
}
