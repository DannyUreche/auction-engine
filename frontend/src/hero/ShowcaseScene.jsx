import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { formatPrice } from '../api.js'
import { findCategory, useCategories } from '../categories.js'
import { CategoryIcon, Icon } from '../components/icons.jsx'
import ItemImage from '../components/ItemImage.jsx'

// "Showcase" hero: the real active auctions as cards on a 3D carousel (CSS 3D transforms, no WebGL).
// It spins slowly, follows the cursor, can be dragged, and every 10-15 s (or when an item gets a live bid)
// one card turns to the front and pops out with its price. Clicking a card opens the item.

const PLACEHOLDER_CODES = ['ELECTRONICE', 'CEASURI_BIJUTERII', 'ARTA_COLECTII', 'MODA', 'SPORT', 'VEHICULE']

export default function ShowcaseScene({ items, onLiveBid }) {
  const navigate = useNavigate()
  const categories = useCategories()
  const stageRef = useRef(null)
  const ringRef = useRef(null)
  const faceRefs = useRef([])
  const [featured, setFeatured] = useState(null)
  const [compact, setCompact] = useState(() => window.innerWidth < 640)

  // At least 6 cards so the ring looks full (repeat the items if there are few)
  const cards = useMemo(() => {
    const list = (items ?? []).slice(0, 10)
    if (list.length === 0) return PLACEHOLDER_CODES.map((code) => ({ placeholder: true, category: code, name: 'Obiectul tau aici' }))
    const result = []
    while (result.length < Math.max(6, list.length)) result.push(...list)
    return result.slice(0, Math.max(6, list.length))
  }, [items])
  const cardsRef = useRef(cards)
  useEffect(() => {
    cardsRef.current = cards
  }, [cards])

  const count = cards.length
  const stepDeg = 360 / count
  const width = compact ? 118 : 158
  const height = compact ? 166 : 218
  const radius = Math.round(width / 2 / Math.tan(Math.PI / count) + (compact ? 24 : 42))
  const state = useRef({ angle: 0, velocity: 0, tiltX: -6, tiltY: 0, targetTiltX: -6, targetTiltY: 0, target: null, dragging: false, lastX: 0, moved: 0, downCard: null })

  // Featured card: rotate it to the front, pop it out for a few seconds
  const featureRef = useRef(null)
  useEffect(() => {
    const st = state.current
    let releaseTimer
    let featureTimer
    featureRef.current = (index) => {
      clearTimeout(releaseTimer)
      const diff = ((((-index * stepDeg - st.angle) % 360) + 540) % 360) - 180
      st.target = st.angle + diff
      setFeatured(index)
      releaseTimer = setTimeout(() => {
        setFeatured(null)
        st.target = null
      }, 4500)
    }
    const schedule = (delay) => {
      featureTimer = setTimeout(() => {
        featureRef.current(Math.floor(Math.random() * cardsRef.current.length))
        schedule(10000 + Math.random() * 5000)
      }, delay)
    }
    if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) schedule(3000)
    onLiveBid.current = (event) => {
      const index = cardsRef.current.findIndex((card) => card.id === event.itemId)
      if (index >= 0) featureRef.current(index)
    }
    return () => {
      clearTimeout(releaseTimer)
      clearTimeout(featureTimer)
      onLiveBid.current = null
    }
  }, [stepDeg, onLiveBid])

  // Animation loop: auto-rotation (or easing to the featured card), cursor tilt, dimming of the back cards
  useEffect(() => {
    const st = state.current
    let frame
    const loop = () => {
      if (!st.dragging) {
        if (st.target != null) st.angle += (st.target - st.angle) * 0.07
        else {
          st.angle += 0.09 + st.velocity
          st.velocity *= 0.94
        }
      }
      st.tiltX += (st.targetTiltX - st.tiltX) * 0.06
      st.tiltY += (st.targetTiltY - st.tiltY) * 0.06
      if (ringRef.current) {
        ringRef.current.style.transform = `translateZ(${-radius}px) rotateX(${st.tiltX}deg) rotateY(${st.angle + st.tiltY}deg)`
      }
      for (let i = 0; i < count; i++) {
        const relative = ((((i * stepDeg + st.angle + st.tiltY) % 360) + 540) % 360) - 180
        faceRefs.current[i]?.style.setProperty('--front', Math.cos((relative * Math.PI) / 180).toFixed(3))
      }
      frame = requestAnimationFrame(loop)
    }
    frame = requestAnimationFrame(loop)

    const onMove = (event) => {
      const rect = stageRef.current?.getBoundingClientRect()
      if (!rect) return
      const mx = Math.max(-1.5, Math.min(1.5, ((event.clientX - rect.left) / rect.width) * 2 - 1))
      const my = Math.max(-1.5, Math.min(1.5, ((event.clientY - rect.top) / rect.height) * 2 - 1))
      st.targetTiltX = -6 - my * 8
      st.targetTiltY = mx * 12
    }
    const onResize = () => setCompact(window.innerWidth < 640)
    window.addEventListener('pointermove', onMove)
    window.addEventListener('resize', onResize)
    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('resize', onResize)
    }
  }, [count, stepDeg, radius])

  // Drag to spin; a press without movement on a card opens it
  const onPointerDown = (event) => {
    const st = state.current
    st.dragging = true
    st.lastX = event.clientX
    st.moved = 0
    st.target = null
    st.downCard = event.target.closest('[data-card]')?.dataset.card ?? null
    event.currentTarget.setPointerCapture(event.pointerId)
  }
  const onPointerMove = (event) => {
    const st = state.current
    if (!st.dragging) return
    const dx = event.clientX - st.lastX
    st.lastX = event.clientX
    st.angle += dx * 0.35
    st.velocity = dx * 0.35
    st.moved += Math.abs(dx)
  }
  const onPointerUp = () => {
    const st = state.current
    st.dragging = false
    const card = st.downCard != null ? cardsRef.current[Number(st.downCard)] : null
    if (st.moved < 6 && card) navigate(card.placeholder ? '/sell' : `/items/${card.id}`)
  }

  const featuredCard = featured != null ? cards[featured] : null

  return (
    <div
      ref={stageRef}
      className="absolute inset-0 cursor-grab touch-pan-y select-none active:cursor-grabbing"
      style={{ perspective: '1100px' }}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onPointerCancel={onPointerUp}
    >
      <div className="pointer-events-none absolute inset-x-[12%] bottom-[9%] h-16 rounded-[100%] bg-indigo-500/35 blur-2xl" />
      <div className="absolute top-[46%] left-1/2" style={{ transformStyle: 'preserve-3d' }}>
        <div ref={ringRef} style={{ transformStyle: 'preserve-3d' }}>
          {cards.map((card, i) => {
            const category = findCategory(categories, card.category)
            const isFeatured = featured === i
            return (
              <div
                key={i}
                className="absolute"
                style={{
                  width,
                  height,
                  left: -width / 2,
                  top: -height / 2,
                  transformStyle: 'preserve-3d',
                  transform: `rotateY(${i * stepDeg}deg) translateZ(${radius + (isFeatured ? 70 : 0)}px) scale(${isFeatured ? 1.08 : 1})`,
                  transition: 'transform 0.7s cubic-bezier(0.2, 0.8, 0.2, 1)',
                }}
              >
                <div
                  data-card={i}
                  ref={(el) => {
                    faceRefs.current[i] = el
                  }}
                  className={`absolute inset-0 overflow-hidden rounded-2xl bg-slate-900 shadow-2xl [backface-visibility:hidden] [&_img]:pointer-events-none ${
                    isFeatured ? 'ring-2 ring-amber-300 shadow-[0_0_40px_rgba(252,211,77,0.45)]' : 'ring-1 ring-white/15'
                  }`}
                >
                  {card.placeholder ? (
                    <div className="grid h-[58%] place-items-center bg-gradient-to-br from-indigo-500/40 to-fuchsia-500/30">
                      <CategoryIcon category={category} size={compact ? 40 : 52} />
                    </div>
                  ) : (
                    <ItemImage item={card} className="h-[58%] w-full" />
                  )}
                  <div className="p-2.5 sm:p-3">
                    <p className="flex items-center gap-1 truncate text-[10px] text-indigo-200/80">
                      <CategoryIcon category={category} size={12} /> {category.label}
                    </p>
                    <p className="mt-0.5 truncate text-xs leading-snug font-semibold text-white sm:text-sm">{card.name}</p>
                    {!card.placeholder && (
                      <p key={card.currentPrice} className="mt-1 origin-left animate-price-flash text-sm font-bold text-white sm:text-base">
                        {formatPrice(card.currentPrice)}
                      </p>
                    )}
                  </div>
                  {/* Cards turning away get darker */}
                  <div className="pointer-events-none absolute inset-0 bg-slate-950" style={{ opacity: 'calc((1 - var(--front, 1)) * 0.5)' }} />
                </div>
                <div className="absolute inset-0 grid place-items-center rounded-2xl bg-gradient-to-br from-indigo-600 via-violet-700 to-fuchsia-700 ring-1 ring-white/10 [backface-visibility:hidden] [transform:rotateY(180deg)]">
                  <Icon name="gavel" size={compact ? 34 : 44} mono className="text-white/60" />
                </div>
              </div>
            )
          })}
        </div>
      </div>

      {featuredCard && !featuredCard.placeholder && (
        <Link
          key={featured}
          to={`/items/${featuredCard.id}`}
          onPointerDown={(event) => event.stopPropagation()}
          className="absolute top-2 left-1/2 flex max-w-[90%] -translate-x-1/2 animate-fade-up items-center gap-2 rounded-full bg-amber-300/15 px-3 py-1.5 text-xs text-amber-100 ring-1 ring-amber-300/40 backdrop-blur transition hover:bg-amber-300/25 sm:text-sm"
        >
          <Icon name="sparkles" size={16} />
          <span className="truncate">
            In prim-plan: <b className="text-white">{featuredCard.name}</b> · {formatPrice(featuredCard.currentPrice)}
          </span>
          <span className="shrink-0 font-semibold text-amber-200">Liciteaza →</span>
        </Link>
      )}
    </div>
  )
}
