// Confetti pentru momentele importante (ai castigat o licitatie). Fara biblioteci: un <canvas>
// temporar peste pagina, cu ~150 de bucati colorate care cad sub gravitatie si apoi dispare.
const COLORS = ['#6366f1', '#a855f7', '#ec4899', '#f59e0b', '#10b981', '#0ea5e9']

export function launchConfetti() {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

  const canvas = document.createElement('canvas')
  canvas.style.cssText = 'position:fixed;inset:0;width:100%;height:100%;pointer-events:none;z-index:60'
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  document.body.appendChild(canvas)
  const ctx = canvas.getContext('2d')

  const pieces = Array.from({ length: 150 }, () => ({
    x: canvas.width / 2 + (Math.random() - 0.5) * 200,
    y: canvas.height / 3,
    vx: (Math.random() - 0.5) * 16,
    vy: Math.random() * -14 - 4,
    size: Math.random() * 7 + 5,
    rotation: Math.random() * Math.PI,
    spin: (Math.random() - 0.5) * 0.3,
    color: COLORS[Math.floor(Math.random() * COLORS.length)],
  }))

  const start = performance.now()
  function frame(now) {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    for (const p of pieces) {
      p.vy += 0.35 // gravitatie
      p.vx *= 0.99
      p.x += p.vx
      p.y += p.vy
      p.rotation += p.spin
      ctx.save()
      ctx.translate(p.x, p.y)
      ctx.rotate(p.rotation)
      ctx.fillStyle = p.color
      ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2)
      ctx.restore()
    }
    if (now - start < 3500) requestAnimationFrame(frame)
    else canvas.remove()
  }
  requestAnimationFrame(frame)
}
