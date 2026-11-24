import { useEffect, useRef, useState } from 'react'

// true cat timp <html> are clasa .dark (urmareste si schimbarile facute de ThemeToggle)
export function useIsDark() {
  const [dark, setDark] = useState(() => document.documentElement.classList.contains('dark'))
  useEffect(() => {
    const observer = new MutationObserver(() => setDark(document.documentElement.classList.contains('dark')))
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
    return () => observer.disconnect()
  }, [])
  return dark
}

// Fundalul animat din modul intunecat, desenat pe un singur <canvas>:
//  - stele care sclipesc lent,
//  - din cand in cand o stea cazatoare,
//  - o retea de particule care plutesc si se unesc prin linii fine; se departeaza de cursorul mouse-ului.
export default function NightSky() {
  const dark = useIsDark()
  const canvasRef = useRef(null)

  useEffect(() => {
    if (!dark) return
    const canvas = canvasRef.current
    const ctx = canvas.getContext('2d')
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    const mouse = { x: -9999, y: -9999 }
    let width, height, stars, particles, shooting, nextShooting, frame

    function setup() {
      const ratio = Math.min(window.devicePixelRatio || 1, 2)
      width = window.innerWidth
      height = window.innerHeight
      canvas.width = width * ratio
      canvas.height = height * ratio
      ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
      const area = width * height
      // Mai putine elemente pe ecrane mici (telefon): animatia ramane fluida
      stars = Array.from({ length: Math.round(area / 5000) }, () => ({
        x: Math.random() * width,
        y: Math.random() * height,
        r: Math.random() * 1.1 + 0.2,
        phase: Math.random() * Math.PI * 2,
        speed: Math.random() * 0.02 + 0.005,
      }))
      particles = Array.from({ length: Math.min(70, Math.round(area / 22000)) }, () => ({
        x: Math.random() * width,
        y: Math.random() * height,
        vx: (Math.random() - 0.5) * 0.35,
        vy: (Math.random() - 0.5) * 0.35,
      }))
      shooting = null
      nextShooting = performance.now() + 2500
    }

    function launchShootingStar(now) {
      const fromLeft = Math.random() < 0.5
      shooting = {
        x: fromLeft ? Math.random() * width * 0.5 : width * (0.5 + Math.random() * 0.5),
        y: Math.random() * height * 0.35,
        vx: (fromLeft ? 1 : -1) * (9 + Math.random() * 5),
        vy: 4 + Math.random() * 2.5,
        life: 0,
      }
      nextShooting = now + 4000 + Math.random() * 6000
    }

    function draw(now) {
      ctx.clearRect(0, 0, width, height)

      // Stele
      for (const s of stars) {
        s.phase += s.speed
        ctx.globalAlpha = 0.35 + Math.sin(s.phase) * 0.35
        ctx.fillStyle = '#e0e7ff'
        ctx.beginPath()
        ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2)
        ctx.fill()
      }
      ctx.globalAlpha = 1

      // Particule: se misca, se lovesc de margini, fug de mouse
      for (const p of particles) {
        const dx = p.x - mouse.x
        const dy = p.y - mouse.y
        const dist = Math.hypot(dx, dy)
        if (dist < 140 && dist > 0) {
          p.vx += (dx / dist) * 0.06
          p.vy += (dy / dist) * 0.06
        }
        p.vx = Math.max(-1.2, Math.min(1.2, p.vx * 0.995))
        p.vy = Math.max(-1.2, Math.min(1.2, p.vy * 0.995))
        p.x += p.vx
        p.y += p.vy
        if (p.x < 0 || p.x > width) p.vx *= -1
        if (p.y < 0 || p.y > height) p.vy *= -1
      }
      // Linii intre particulele apropiate (cu cat sunt mai aproape, cu atat linia e mai vizibila)
      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const a = particles[i]
          const b = particles[j]
          const d = Math.hypot(a.x - b.x, a.y - b.y)
          if (d < 130) {
            ctx.strokeStyle = `rgba(129, 140, 248, ${(1 - d / 130) * 0.35})`
            ctx.lineWidth = 1
            ctx.beginPath()
            ctx.moveTo(a.x, a.y)
            ctx.lineTo(b.x, b.y)
            ctx.stroke()
          }
        }
      }
      for (const p of particles) {
        ctx.fillStyle = 'rgba(165, 180, 252, 0.8)'
        ctx.beginPath()
        ctx.arc(p.x, p.y, 1.6, 0, Math.PI * 2)
        ctx.fill()
      }

      // Steaua cazatoare: un cap luminos cu o coada care se estompeaza
      if (!shooting && now > nextShooting) launchShootingStar(now)
      if (shooting) {
        const s = shooting
        s.x += s.vx
        s.y += s.vy
        s.life++
        const tailX = s.x - s.vx * 12
        const tailY = s.y - s.vy * 12
        const gradient = ctx.createLinearGradient(s.x, s.y, tailX, tailY)
        gradient.addColorStop(0, 'rgba(255, 255, 255, 0.95)')
        gradient.addColorStop(1, 'rgba(165, 180, 252, 0)')
        ctx.strokeStyle = gradient
        ctx.lineWidth = 2
        ctx.lineCap = 'round'
        ctx.beginPath()
        ctx.moveTo(s.x, s.y)
        ctx.lineTo(tailX, tailY)
        ctx.stroke()
        if (s.life > 90 || s.x < -200 || s.x > width + 200 || s.y > height + 200) shooting = null
      }

      if (!reduceMotion) frame = requestAnimationFrame(draw)
    }

    const onMove = (e) => {
      mouse.x = e.clientX
      mouse.y = e.clientY
    }
    const onLeave = () => {
      mouse.x = -9999
      mouse.y = -9999
    }

    setup()
    frame = requestAnimationFrame(draw)
    window.addEventListener('resize', setup)
    window.addEventListener('mousemove', onMove)
    document.addEventListener('mouseleave', onLeave)
    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('resize', setup)
      window.removeEventListener('mousemove', onMove)
      document.removeEventListener('mouseleave', onLeave)
    }
  }, [dark])

  if (!dark) return null
  return <canvas ref={canvasRef} aria-hidden="true" className="absolute inset-0 h-full w-full" />
}
