import { useEffect, useRef, useState } from 'react'

// Un numar care "urca" animat pana la valoarea noua (ex: statisticile de pe pagina principala).
// format: cum se afiseaza numarul (implicit fara zecimale, cu separator de mii)
export default function CountUp({ value, format = (n) => Math.round(n).toLocaleString('ro-RO'), duration = 1200 }) {
  const [shown, setShown] = useState(0)
  const fromRef = useRef(0)

  useEffect(() => {
    const from = fromRef.current
    const to = Number(value) || 0
    const start = performance.now()
    let frame
    const tick = (now) => {
      const t = Math.min((now - start) / duration, 1)
      const eased = 1 - Math.pow(1 - t, 3) // incetineste spre final
      const current = from + (to - from) * eased
      setShown(current)
      fromRef.current = current
      if (t < 1) frame = requestAnimationFrame(tick)
    }
    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [value, duration])

  return <span className="tabular-nums">{format(shown)}</span>
}
