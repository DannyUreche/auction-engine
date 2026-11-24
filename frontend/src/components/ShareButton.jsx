import { useEffect, useState } from 'react'
import QRCode from 'qrcode'
import { Icon } from './icons.jsx'

// Share the item: the phone's share sheet (or copy the link) and a QR code to scan / download
export default function ShareButton({ item }) {
  const [copied, setCopied] = useState(false)
  const [qr, setQr] = useState(null)
  const url = `${window.location.origin}/items/${item.id}`

  async function share() {
    if (navigator.share) {
      try {
        await navigator.share({ title: item.name, text: `${item.name} - liciteaza pe Licitatii.live`, url })
        return
      } catch {
        // cancelled: fall back to copying
      }
    }
    await navigator.clipboard?.writeText(url)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  async function showQr() {
    setQr(await QRCode.toDataURL(url, { width: 280, margin: 1, color: { dark: '#1e1b4b', light: '#ffffff' } }))
  }

  return (
    <>
      <button type="button" onClick={share} className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-600 hover:text-indigo-600">
        <Icon name="share" size={18} /> {copied ? 'Link copiat!' : 'Distribuie'}
      </button>
      <button type="button" onClick={showQr} className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-600 hover:text-indigo-600">
        <Icon name="qr" size={18} /> Cod QR
      </button>
      {qr && <QrDialog image={qr} name={item.name} onClose={() => setQr(null)} />}
    </>
  )
}

function QrDialog({ image, name, onClose }) {
  useEffect(() => {
    const onKey = (event) => event.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="fixed inset-0 z-50 grid animate-fade-in place-items-center bg-slate-950/60 p-4 backdrop-blur-sm" onClick={onClose}>
      <div className="w-full max-w-xs animate-fade-up rounded-2xl bg-white p-6 text-center shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <p className="font-semibold text-slate-900">Scaneaza ca sa deschizi obiectul</p>
        <p className="mt-0.5 truncate text-sm text-slate-500">{name}</p>
        <img src={image} alt="Cod QR" className="mx-auto mt-4 h-56 w-56 rounded-lg" />
        <div className="mt-4 flex justify-center gap-3 text-sm font-semibold">
          <a href={image} download="licitatie-qr.png" className="text-indigo-600 hover:text-indigo-500">
            Descarca
          </a>
          <button type="button" onClick={onClose} className="text-slate-500 hover:text-slate-800">
            Inchide
          </button>
        </div>
      </div>
    </div>
  )
}
