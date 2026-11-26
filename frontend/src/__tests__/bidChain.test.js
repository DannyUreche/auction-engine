import { afterEach, describe, expect, it, vi } from 'vitest'
import { canonical, canonicalTime, checkReceipts, GENESIS, saveReceipt, sha256, verifyChain } from '../bidChain.js'

// A real bid from the database: its hash was computed by the PostgreSQL trigger (V39),
// so this proves the browser builds exactly the same canonical text
const REAL = {
  id: 6797,
  item: { id: 4100 },
  bidder: { id: 4998 },
  amount: 1360.0,
  createdAt: '2026-11-24T17:37:23.293419',
  automatic: false,
  prevHash: '78f01caae011ee48597148e07039b59c7f616669b35ae68228c2bda30e371800',
  hash: 'f29c3969d9d945ce524abe7b0247a8f303d63193c678456d95d342a0e20b7ea6',
}

async function chainOf(amounts) {
  const bids = []
  let previous = GENESIS
  for (const [index, amount] of amounts.entries()) {
    const bid = { id: 10 + index, item: { id: 7 }, bidder: { id: 3 }, amount, createdAt: `2026-11-25T10:00:0${index}.5`, automatic: false, prevHash: previous }
    bid.hash = await sha256(canonical(bid))
    previous = bid.hash
    bids.push(bid)
  }
  return bids
}

describe('bidChain', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('computes the same hash as the database', async () => {
    expect(await sha256(canonical(REAL))).toBe(REAL.hash)
  })

  it('normalizes the times the server sends', () => {
    expect(canonicalTime('2026-11-25T18:04:05.1234')).toBe('2026-11-25T18:04:05.123400')
    expect(canonicalTime('2026-11-25T18:04:05')).toBe('2026-11-25T18:04:05.000000')
    expect(canonicalTime('2026-11-25T18:04')).toBe('2026-11-25T18:04:00.000000')
  })

  it('accepts an intact chain in any order and spots changes', async () => {
    const bids = await chainOf([105, 110, 120])
    const ok = await verifyChain([...bids].reverse())
    expect(ok).toMatchObject({ valid: true, length: 3, head: bids[2].hash })

    const changed = bids.map((bid) => (bid.id === 11 ? { ...bid, amount: 109 } : bid))
    expect(await verifyChain(changed)).toMatchObject({ valid: false, brokenAt: 11, reason: 'data' })

    const removed = bids.filter((bid) => bid.id !== 11)
    expect(await verifyChain(removed)).toMatchObject({ valid: false, brokenAt: 12, reason: 'link' })
  })

  it('keeps receipts of my bids and checks them', async () => {
    const store = new Map()
    vi.stubGlobal('localStorage', { getItem: (k) => store.get(k) ?? null, setItem: (k, v) => store.set(k, v) })
    const bids = await chainOf([105, 110])
    saveReceipt(bids[0])
    saveReceipt(bids[1])
    expect(checkReceipts(7, bids)).toMatchObject({ total: 2, confirmed: 2 })
    expect(checkReceipts(7, [bids[0]])).toMatchObject({ total: 2, confirmed: 1 })
    expect(checkReceipts(8, bids)).toMatchObject({ total: 0, confirmed: 0 })
  })
})
