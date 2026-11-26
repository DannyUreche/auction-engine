import { useEffect, useState } from 'react'
import { checkReceipts, verifyChain } from '../bidChain.js'
import { Icon } from './icons.jsx'

// "Istoric verificat": the browser recomputes the SHA-256 chain of the bids (bidChain.js) and draws its last
// links. My own bids (receipts kept in this browser) are ringed. Hidden without bids or without WebCrypto.
export default function BidChainBadge({ itemId, bids }) {
  const [result, setResult] = useState(null)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (!bids.length || !globalThis.crypto?.subtle) {
      setResult(null)
      return
    }
    let current = true
    verifyChain(bids)
      .then((verification) => current && setResult({ ...verification, receipts: checkReceipts(itemId, bids) }))
      .catch(() => current && setResult(null))
    return () => {
      current = false
    }
  }, [itemId, bids])

  if (!result) return null

  if (!result.valid) {
    return (
      <div className="mt-3 flex items-start gap-2 rounded-xl bg-rose-50 px-3 py-2.5 text-sm text-rose-800 ring-1 ring-rose-200" role="alert">
        <Icon name="shield" size={20} />
        <span>
          <b>Istoricul ofertelor nu se verifica</b> (oferta #{result.brokenAt}{' '}
          {result.reason === 'link' ? 'nu mai e legata de cea dinainte' : 'nu se potriveste cu semnatura ei'}). Anunta un administrator.
        </span>
      </div>
    )
  }

  const myIds = new Set(result.receipts.confirmedIds)
  const last = result.chain.slice(-5)

  return (
    <div className="mt-3 rounded-2xl bg-emerald-50/70 px-3 py-2.5 ring-1 ring-emerald-200">
      <button type="button" onClick={() => setOpen(!open)} className="flex w-full items-center gap-2 text-left" aria-expanded={open}>
        <Icon name="shield" size={20} />
        <span className="min-w-0 flex-1 text-sm">
          <b className="text-emerald-800">Istoric verificat</b>
          <span className="text-emerald-900/70">
            {' '}
            · {result.length} {result.length === 1 ? 'oferta' : 'oferte'} inlantuite, verificate de browserul tau
          </span>
        </span>
        <span className={`text-xs text-emerald-700 transition ${open ? 'rotate-180' : ''}`}>▾</span>
      </button>

      <div className="mt-2.5 flex items-center overflow-x-auto pb-1" aria-hidden="true">
        {result.chain.length > last.length && <span className="mr-1 text-xs text-emerald-700/60">…</span>}
        {last.map((bid, index) => (
          <div key={bid.id} className="flex items-center">
            {index > 0 && <span className="chain-link mx-0.5 h-0.5 w-4 rounded-full bg-emerald-300" />}
            <span
              className={`chain-block flex flex-col items-center rounded-lg bg-white px-2 py-1 font-mono text-[10px] leading-tight shadow-sm ring-1 ${
                myIds.has(bid.id) ? 'ring-2 ring-indigo-500' : 'ring-emerald-200'
              }`}
              style={{ animationDelay: `${index * 90}ms` }}
              title={`Oferta #${bid.id}: ${bid.hash}`}
            >
              <span className="mb-0.5 h-1 w-full rounded-full" style={{ background: hashColor(bid.hash) }} />
              <span className="text-slate-500">#{bid.id}</span>
              <span className="font-semibold text-slate-800">{bid.hash.slice(0, 6)}</span>
            </span>
          </div>
        ))}
      </div>

      {open && (
        <div className="mt-2 space-y-2 border-t border-emerald-200/70 pt-2 text-xs text-slate-600">
          <p>
            Fiecare oferta poarta o amprenta SHA-256 calculata din datele ei si din amprenta ofertei de dinainte. Daca cineva ar modifica, sterge sau
            reordona o oferta - chiar si direct in baza de date - lantul s-ar rupe, iar aceasta verificare, facuta acum in browserul tau, ar arata
            unde.
          </p>
          <p className="break-all">
            Ultima amprenta: <span className="font-mono text-slate-800">{result.head}</span>
          </p>
          {result.receipts.total > 0 && (
            <p className={result.receipts.missing.length ? 'font-semibold text-rose-700' : 'text-indigo-700'}>
              {result.receipts.missing.length
                ? `Atentie: ${result.receipts.missing.length} din ofertele tale salvate in acest browser lipsesc sau difera!`
                : `Ofertele tale (${result.receipts.confirmed}) sunt in lant exact cum le-ai plasat (incercuite).`}
            </p>
          )}
          <a href={`/api/bids/${itemId}/verify`} target="_blank" rel="noreferrer" className="inline-block font-semibold text-emerald-700 hover:underline">
            Verificare si pe server (JSON) →
          </a>
        </div>
      )}
    </div>
  )
}

// A stable color per hash, so the blocks are easy to tell apart
function hashColor(hash) {
  return `hsl(${parseInt(hash.slice(0, 4), 16) % 360} 70% 55%)`
}
