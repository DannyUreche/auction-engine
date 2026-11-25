import { useEffect, useState } from 'react'

// Small animated auction scene for empty states: a gavel strikes the sound block (with a shock wave),
// the price tag above goes up after every strike and bid paddles pop up with "+5". Pure SVG + CSS.
const PRICES = [120, 125, 135, 150, 160, 175]

export default function AuctionScene({ className = '' }) {
  const [step, setStep] = useState(0)

  // A new price after every strike (the CSS strike lasts 3.2 s)
  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const id = setInterval(() => setStep((current) => (current + 1) % PRICES.length), 3200)
    return () => clearInterval(id)
  }, [])

  return (
    <svg viewBox="0 0 240 150" className={`mx-auto h-44 w-72 overflow-visible ${className}`} aria-hidden="true">
      <defs>
        <linearGradient id="scene-wood" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#b45309" />
          <stop offset="1" stopColor="#78350f" />
        </linearGradient>
        <linearGradient id="scene-head" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#818cf8" />
          <stop offset="1" stopColor="#7c3aed" />
        </linearGradient>
        <linearGradient id="scene-tag" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#34d399" />
          <stop offset="1" stopColor="#0d9488" />
        </linearGradient>
      </defs>

      {/* Shock wave on the table, in time with the strike */}
      <ellipse cx="120" cy="132" rx="46" ry="7" fill="none" stroke="#a78bfa" strokeWidth="2" className="scene-shock" />
      <ellipse cx="120" cy="134" rx="58" ry="6" fill="#0f172a" opacity="0.12" />

      {/* Sound block */}
      <rect x="88" y="114" width="64" height="18" rx="6" fill="url(#scene-wood)" />
      <rect x="94" y="110" width="52" height="8" rx="4" fill="#d97706" />

      {/* Gavel: rotates around the end of the handle */}
      <g className="scene-gavel">
        <rect x="118" y="79" width="78" height="9" rx="4.5" fill="#92400e" />
        <g transform="translate(96 62)">
          <rect x="0" y="0" width="30" height="46" rx="8" fill="url(#scene-head)" />
          <rect x="0" y="8" width="30" height="5" fill="#fcd34d" />
          <rect x="0" y="33" width="30" height="5" fill="#fcd34d" />
        </g>
      </g>

      {/* Bid paddles popping up */}
      <g className="scene-paddle" style={{ animationDelay: '0.2s' }}>
        <rect x="18" y="70" width="5" height="34" rx="2" fill="#475569" />
        <circle cx="20.5" cy="62" r="15" fill="#6366f1" />
        <text x="20.5" y="66.5" textAnchor="middle" fontSize="12" fontWeight="700" fill="#fff">+5</text>
      </g>
      <g className="scene-paddle" style={{ animationDelay: '1.8s' }}>
        <rect x="216" y="76" width="5" height="30" rx="2" fill="#475569" />
        <circle cx="218.5" cy="68" r="14" fill="#db2777" />
        <text x="218.5" y="72" textAnchor="middle" fontSize="11" fontWeight="700" fill="#fff">+10</text>
      </g>

      {/* Price tag swinging on its string; pops on every new price */}
      <g className="scene-tag">
        <line x1="62" y1="0" x2="62" y2="16" stroke="#94a3b8" strokeWidth="1.5" />
        <g key={step} className="scene-tag-pop">
          <path d="M36 16 h56 a7 7 0 0 1 7 7 v22 a7 7 0 0 1 -7 7 h-56 l-12 -18 z" fill="url(#scene-tag)" />
          <circle cx="36" cy="34" r="3" fill="#ecfdf5" />
          <text x="66" y="39.5" textAnchor="middle" fontSize="15" fontWeight="800" fill="#fff">
            {PRICES[step]} lei
          </text>
        </g>
      </g>
    </svg>
  )
}
