// Efectul "dezintegrare": un element (de obicei o poza) se sparge in bilute mici care zboara si dispar.
// Bilutele au culorile reale ale pozei (citite pixel cu pixel dintr-un <canvas>); se desprind de la stanga
// la dreapta, ca o "maturare". Elementul ramane ascuns la final (keepHidden) sau reapare (implicit).
const FALLBACK_COLORS = ['#818cf8', '#a78bfa', '#e879f9', '#38bdf8', '#c7d2fe']

export function disintegrate(element, { keepHidden = false, duration = 1700 } = {}) {
  if (!element || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return Promise.resolve()
  const rect = element.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) return Promise.resolve()

  // Cel mult ~3000 de bilute, indiferent de marimea pozei
  const step = Math.max(4, Math.ceil(Math.sqrt((rect.width * rect.height) / 3000)))
  const pieces = samplePieces(element, rect, step)

  const canvas = document.createElement('canvas')
  const ratio = Math.min(window.devicePixelRatio || 1, 2)
  canvas.width = window.innerWidth * ratio
  canvas.height = window.innerHeight * ratio
  canvas.style.cssText = 'position:fixed;inset:0;width:100%;height:100%;pointer-events:none;z-index:55'
  document.body.appendChild(canvas)
  const ctx = canvas.getContext('2d')
  ctx.scale(ratio, ratio)

  element.style.visibility = 'hidden'

  return new Promise((resolve) => {
    const start = performance.now()
    function frame(now) {
      const elapsed = now - start
      ctx.clearRect(0, 0, window.innerWidth, window.innerHeight)
      for (const p of pieces) {
        // Fiecare bilut porneste cu intarziere, in functie de pozitie (stanga -> dreapta)
        const t = Math.max(0, elapsed - p.delay) / 1000
        if (t > 0) {
          p.x += p.vx
          p.y += p.vy
          p.vy -= 0.02 // plutesc usor in sus, ca cenusa
          p.alpha = Math.max(0, p.alpha - 0.012)
        }
        if (p.alpha <= 0) continue
        ctx.globalAlpha = p.alpha
        ctx.fillStyle = p.color
        ctx.beginPath()
        ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
        ctx.fill()
      }
      if (elapsed < duration + 600) {
        requestAnimationFrame(frame)
      } else {
        canvas.remove()
        if (!keepHidden) element.style.visibility = ''
        resolve()
      }
    }
    requestAnimationFrame(frame)
  })
}

// Culorile pozei, luate dintr-o grila de puncte (pentru elemente fara poza: culori din tema)
function samplePieces(element, rect, step) {
  const cols = Math.ceil(rect.width / step)
  const rows = Math.ceil(rect.height / step)
  let pixels = null

  if (element instanceof HTMLImageElement && element.complete && element.naturalWidth) {
    try {
      const off = document.createElement('canvas')
      off.width = cols
      off.height = rows
      const offCtx = off.getContext('2d')
      drawFitted(offCtx, element, cols, rows)
      pixels = offCtx.getImageData(0, 0, cols, rows).data
    } catch {
      pixels = null // poza de pe alt domeniu: browserul nu ne lasa sa citim pixelii
    }
  }

  const pieces = []
  for (let row = 0; row < rows; row++) {
    for (let col = 0; col < cols; col++) {
      let color = FALLBACK_COLORS[(row * 7 + col * 3) % FALLBACK_COLORS.length]
      if (pixels) {
        const i = (row * cols + col) * 4
        if (pixels[i + 3] < 20) continue // pixel transparent
        color = `rgb(${pixels[i]}, ${pixels[i + 1]}, ${pixels[i + 2]})`
      }
      const angle = -Math.PI / 4 + (Math.random() - 0.5) * 1.6
      const speed = Math.random() * 2.2 + 0.6
      pieces.push({
        x: rect.left + col * step + step / 2,
        y: rect.top + row * step + step / 2,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        size: step * 0.36,
        color,
        alpha: 1,
        delay: (col / cols) * 900 + Math.random() * 250,
      })
    }
  }
  return pieces
}

// Deseneaza poza la fel cum o afiseaza CSS-ul (object-fit: cover sau contain)
function drawFitted(ctx, img, width, height) {
  const fit = getComputedStyle(img).objectFit
  const scale =
    fit === 'contain'
      ? Math.min(width / img.naturalWidth, height / img.naturalHeight)
      : fit === 'cover'
        ? Math.max(width / img.naturalWidth, height / img.naturalHeight)
        : null
  if (scale == null) {
    ctx.drawImage(img, 0, 0, width, height)
    return
  }
  const w = img.naturalWidth * scale
  const h = img.naturalHeight * scale
  ctx.drawImage(img, (width - w) / 2, (height - h) / 2, w, h)
}
