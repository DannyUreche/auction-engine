// Verifiable auctions, browser side: the bids of an item form a SHA-256 chain (database trigger from V39,
// same rule as BidChainService.java). The browser recomputes every hash itself, so the history it shows can't
// have been changed on the server without the check failing. Your own bids are also kept as "receipts" in
// localStorage: if one of them ever disappeared or changed, the page would say so.
//
// hash = sha256("id|prevHash|itemId|bidderId|amount|createdAt|automatic")

export const GENESIS = '0'.repeat(64)
const RECEIPTS_KEY = 'bidReceipts'
const MAX_RECEIPTS = 300

// The server sends LocalDateTime without trailing zeros ("...:05.1234", "...:05", even "...T18:04");
// the chain always uses seconds and 6 decimals
export function canonicalTime(value) {
  const [main, fraction = ''] = String(value).replace(/Z$/, '').split('.')
  const withSeconds = main.length === 16 ? `${main}:00` : main
  return `${withSeconds}.${fraction.padEnd(6, '0').slice(0, 6)}`
}

export function canonical(bid) {
  const itemId = bid.item?.id ?? bid.itemId
  return [bid.id, bid.prevHash, itemId, bid.bidder?.id, Number(bid.amount).toFixed(2), canonicalTime(bid.createdAt), Boolean(bid.automatic)].join('|')
}

export async function sha256(text) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))
  return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, '0')).join('')
}

// { valid, length, head } or { valid: false, brokenAt, reason }
export async function verifyChain(bids) {
  const chain = [...bids].sort((a, b) => a.id - b.id)
  let previous = GENESIS
  for (const bid of chain) {
    if (bid.prevHash !== previous) return { valid: false, length: chain.length, brokenAt: bid.id, reason: 'link' }
    if ((await sha256(canonical(bid))) !== bid.hash) return { valid: false, length: chain.length, brokenAt: bid.id, reason: 'data' }
    previous = bid.hash
  }
  return { valid: true, length: chain.length, head: previous, chain }
}

function readReceipts() {
  try {
    const value = JSON.parse(localStorage.getItem(RECEIPTS_KEY) ?? '[]')
    return Array.isArray(value) ? value : []
  } catch {
    return []
  }
}

// Called with the bid the server answered after POST /api/bids or buy-now
export function saveReceipt(bid) {
  if (!bid?.id || !bid.hash) return
  const itemId = bid.item?.id ?? bid.itemId
  const receipts = readReceipts().filter((r) => r.id !== bid.id)
  receipts.push({ id: bid.id, itemId, hash: bid.hash })
  localStorage.setItem(RECEIPTS_KEY, JSON.stringify(receipts.slice(-MAX_RECEIPTS)))
}

// My receipts for this item: how many are still in the chain unchanged
export function checkReceipts(itemId, bids) {
  const byId = new Map(bids.map((bid) => [bid.id, bid.hash]))
  const mine = readReceipts().filter((r) => r.itemId === itemId)
  const missing = mine.filter((r) => byId.get(r.id) !== r.hash)
  const confirmedIds = mine.filter((r) => byId.get(r.id) === r.hash).map((r) => r.id)
  return { total: mine.length, confirmed: confirmedIds.length, confirmedIds, missing }
}
