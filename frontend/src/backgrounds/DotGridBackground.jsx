import { useEffect, useRef } from 'react'
import { useIsDark } from '../useIsDark.js'
import { useAuctionEvents } from '../events.jsx'

// "Magnetic grid" background: a regular grid of small dots. Near the cursor the dots are pushed away
// (like iron filings around a magnet) and light up in the site gradient. Waves run through the grid on
// every click, on every live bid and, quietly, every few seconds - so the page always feels alive.

const WAVE_LIFE = 2.6 // seconds
const WAVE_SPEED = 430 // px per second
const LENS_RADIUS = 160
const INDIGO = [99, 102, 241]
const FUCHSIA = [217, 70, 239]
const SKY = [56, 189, 248]

export default function DotGridBackground() {
  const containerRef = useRef(null)
  const dark = useIsDark()
  const darkRef = useRef(dark)
  const waveRef = useRef(null)

  useEffect(() => {
    darkRef.current = dark
  }, [dark])

  useAuctionEvents((event) => {
    if (event.type === 'bid') waveRef.current?.(Math.random() * window.innerWidth, Math.random() * window.innerHeight, 1, true)
  })

  useEffect(() => {
    const canvas = document.createElement('canvas')
    canvas.className = 'absolute inset-0 h-full w-full'
    containerRef.current.appendChild(canvas)
    const ctx = canvas.getContext('2d')
    const ratio = Math.min(window.devicePixelRatio || 1, 2)
    const spacing = window.innerWidth < 640 ? 24 : 28
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

    let width = 0
    let height = 0
    let count = 0
    let baseX, baseY, offX, offY, glow
    const setup = () => {
      width = window.innerWidth
      height = window.innerHeight
      canvas.width = width * ratio
      canvas.height = height * ratio
      ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
      const cols = Math.ceil(width / spacing) + 1
      const rows = Math.ceil(height / spacing) + 1
      const startX = (width - (cols - 1) * spacing) / 2
      const startY = (height - (rows - 1) * spacing) / 2
      count = cols * rows
      baseX = new Float32Array(count)
      baseY = new Float32Array(count)
      offX = new Float32Array(count)
      offY = new Float32Array(count)
      glow = new Float32Array(count)
      for (let r = 0, i = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++, i++) {
          baseX[i] = startX + c * spacing
          baseY[i] = startY + r * spacing
        }
      }
    }
    setup()

    const mouse = { x: -9999, y: -9999, sx: -9999, sy: -9999, active: false }
    const waves = []
    waveRef.current = (x, y, strength = 1, bid = false) => {
      waves.push({ x, y, start: performance.now(), strength, color: bid ? SKY : null })
      if (waves.length > 6) waves.shift()
    }
    let nextAmbient = performance.now() + 2500

    const onMove = (event) => {
      mouse.x = event.clientX
      mouse.y = event.clientY
      if (!mouse.active) {
        mouse.sx = mouse.x
        mouse.sy = mouse.y
      }
      mouse.active = true
    }
    const onLeave = () => {
      mouse.active = false
    }
    const onDown = (event) => waveRef.current?.(event.clientX, event.clientY, 1.2)
    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerdown', onDown)
    window.addEventListener('resize', setup)
    document.addEventListener('mouseleave', onLeave)

    const rgba = ([r, g, b], a) => `rgba(${r | 0},${g | 0},${b | 0},${a.toFixed(3)})`
    const mix = (a, b, t) => [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t]

    let frame
    const draw = (now) => {
      const isDark = darkRef.current
      ctx.clearRect(0, 0, width, height)

      if (!reduceMotion && now > nextAmbient) {
        waveRef.current?.(Math.random() * width, Math.random() * height, 0.55)
        nextAmbient = now + 4000 + Math.random() * 4000
      }
      for (let w = waves.length - 1; w >= 0; w--) {
        if ((now - waves[w].start) / 1000 > WAVE_LIFE) waves.splice(w, 1)
      }

      // Soft light that follows the cursor
      mouse.sx += (mouse.x - mouse.sx) * 0.15
      mouse.sy += (mouse.y - mouse.sy) * 0.15
      if (mouse.active) {
        const light = ctx.createRadialGradient(mouse.sx, mouse.sy, 0, mouse.sx, mouse.sy, 300)
        light.addColorStop(0, isDark ? 'rgba(129,140,248,0.16)' : 'rgba(99,102,241,0.09)')
        light.addColorStop(1, 'rgba(99,102,241,0)')
        ctx.fillStyle = light
        ctx.fillRect(mouse.sx - 300, mouse.sy - 300, 600, 600)
      }

      const lit = []
      ctx.beginPath()
      for (let i = 0; i < count; i++) {
        let tx = 0
        let ty = 0
        let energy = 0
        let tint = null

        if (mouse.active) {
          const dx = baseX[i] - mouse.sx
          const dy = baseY[i] - mouse.sy
          const d2 = dx * dx + dy * dy
          if (d2 < LENS_RADIUS * LENS_RADIUS) {
            const d = Math.sqrt(d2) || 1
            const f = 1 - d / LENS_RADIUS
            const push = f * f * 26
            tx = (dx / d) * push
            ty = (dy / d) * push
            energy = f
          }
        }
        for (const wave of waves) {
          const age = (now - wave.start) / 1000
          const wx = baseX[i] - wave.x
          const wy = baseY[i] - wave.y
          const wd = Math.sqrt(wx * wx + wy * wy) || 1
          const band = Math.abs(wd - age * WAVE_SPEED)
          if (band < 46) {
            const a = (1 - band / 46) * (1 - age / WAVE_LIFE) * wave.strength
            tx += (wx / wd) * a * 11
            ty += (wy / wd) * a * 11
            if (a > energy) {
              energy = a
              tint = wave.color
            }
          }
        }

        offX[i] += (tx - offX[i]) * 0.2
        offY[i] += (ty - offY[i]) * 0.2
        glow[i] += (Math.min(energy, 1) - glow[i]) * 0.16
        const x = baseX[i] + offX[i]
        const y = baseY[i] + offY[i]
        if (glow[i] < 0.03) {
          ctx.moveTo(x + 1.1, y)
          ctx.arc(x, y, 1.1, 0, Math.PI * 2)
        } else {
          lit.push(i, x, y, tint)
        }
      }
      ctx.fillStyle = isDark ? 'rgba(165,180,252,0.2)' : 'rgba(100,116,139,0.26)'
      ctx.fill()

      // Lit dots: color along the site gradient (left -> right), bigger and with a halo
      for (let k = 0; k < lit.length; k += 4) {
        const i = lit[k]
        const x = lit[k + 1]
        const y = lit[k + 2]
        const g = glow[i]
        const color = lit[k + 3] ?? mix(INDIGO, FUCHSIA, x / width)
        const size = 1.1 + g * 2.6
        ctx.fillStyle = rgba(color, (0.12 + g * 0.2) * (isDark ? 1.3 : 1))
        ctx.beginPath()
        ctx.arc(x, y, size * 2.6, 0, Math.PI * 2)
        ctx.fill()
        ctx.fillStyle = rgba(color, 0.35 + g * 0.65)
        ctx.beginPath()
        ctx.arc(x, y, size, 0, Math.PI * 2)
        ctx.fill()
      }

      frame = requestAnimationFrame(draw)
    }
    frame = requestAnimationFrame(draw)

    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerdown', onDown)
      window.removeEventListener('resize', setup)
      document.removeEventListener('mouseleave', onLeave)
      waveRef.current = null
      canvas.remove()
    }
  }, [])

  return <div ref={containerRef} className="absolute inset-0" />
}
