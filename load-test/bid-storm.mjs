// Load test: many bidders hammering a few auctions at the same time (Node 20+, no dependencies).
//
//   node load-test/bid-storm.mjs [--bidders 200] [--items 5] [--seconds 30] [--listeners 50] [--base http://localhost:8080]
//
// What it does:
//   1. registers a seller and N bidders (users "load_<run>_<n>") and logs them in;
//   2. the seller lists `items` auctions (start price 100 RON, 2 hours);
//   3. `listeners` clients open the live event stream (GET /api/events, Server-Sent Events);
//   4. for `seconds` every bidder loops: read an item, bid its minimum next price, repeat;
//   5. checks the data: final price = highest accepted bid, every accepted bid is stored, prices only go up;
//   6. writes a Markdown + JSON report to load-test/reports/.
//
// Run it against the LOCAL backend started with a raised login limit, otherwise registrations get 429:
//   APP_RATE_LIMIT_AUTH_PER_MINUTE=100000 ./mvnw spring-boot:run
// It refuses non-localhost URLs unless --allow-remote is given (it creates hundreds of accounts).
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import os from 'node:os'

const args = Object.fromEntries(
  process.argv.slice(2).reduce((pairs, arg, i, all) => {
    if (arg.startsWith('--')) pairs.push([arg.slice(2), all[i + 1] && !all[i + 1].startsWith('--') ? all[i + 1] : true])
    return pairs
  }, []),
)
const BASE = String(args.base ?? 'http://localhost:8080').replace(/\/$/, '')
const BIDDERS = Number(args.bidders ?? 200)
const ITEMS = Number(args.items ?? 5)
const SECONDS = Number(args.seconds ?? 30)
const LISTENERS = Number(args.listeners ?? 50)
const SETUP_CONCURRENCY = 20
const PRICE_CEILING = 8000 // bidders have 10000 RON test credit each: stop before the wallet check fails
const RUN = Date.now().toString(36)

if (!/^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(BASE) && !args['allow-remote']) {
  console.error(`Refusing to load-test ${BASE}: it creates ${BIDDERS + 1} accounts. Pass --allow-remote if you really mean it.`)
  process.exit(1)
}

// ---------- measuring ----------
const samples = new Map() // endpoint -> [ms]
const outcomes = new Map() // label -> count
const count = (label) => outcomes.set(label, (outcomes.get(label) ?? 0) + 1)

async function call(endpoint, path, { method = 'GET', token, body } = {}) {
  const started = performance.now()
  let response
  try {
    response = await fetch(BASE + path, {
      method,
      headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: body ? JSON.stringify(body) : undefined,
    })
  } catch (error) {
    count(`${endpoint}: network error (${error.cause?.code ?? error.message})`)
    return { ok: false, status: 0, data: null }
  }
  const ms = performance.now() - started
  if (!samples.has(endpoint)) samples.set(endpoint, [])
  samples.get(endpoint).push(ms)
  const text = await response.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = text
  }
  return { ok: response.ok, status: response.status, data }
}

const percentile = (values, p) => {
  if (values.length === 0) return 0
  const sorted = [...values].sort((a, b) => a - b)
  return sorted[Math.min(sorted.length - 1, Math.floor(p * sorted.length))]
}

async function inBatches(total, size, task) {
  const results = []
  for (let start = 0; start < total; start += size) {
    results.push(...(await Promise.all(Array.from({ length: Math.min(size, total - start) }, (_, i) => task(start + i)))))
  }
  return results
}

// ---------- 1. accounts ----------
async function account(name) {
  const password = 'load-test-123'
  const reg = await call('POST /api/auth/register', '/api/auth/register', {
    method: 'POST',
    body: { username: name, email: `${name}@load.test`, password },
  })
  if (!reg.ok) throw new Error(`register ${name}: ${reg.status} ${JSON.stringify(reg.data)}`)
  const login = await call('POST /api/auth/login', '/api/auth/login', { method: 'POST', body: { username: name, password } })
  if (!login.ok || !login.data?.token) throw new Error(`login ${name}: ${login.status} ${JSON.stringify(login.data)}`)
  return { name, token: login.data.token }
}

// ---------- 3. live listeners ----------
const sentAt = new Map() // `${itemId}:${amount}` -> when the first bid at that amount was sent
const eventDelays = []
let eventsReceived = 0

function listen(index, stop) {
  const controller = new AbortController()
  stop.push(() => controller.abort())
  return fetch(`${BASE}/api/events`, { signal: controller.signal })
    .then(async (response) => {
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      for (;;) {
        const { value, done } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        let end
        while ((end = buffer.indexOf('\n\n')) >= 0) {
          const block = buffer.slice(0, end)
          buffer = buffer.slice(end + 2)
          if (!block.includes('event:bid')) continue
          const line = block.split('\n').find((l) => l.startsWith('data:'))
          if (!line) continue
          eventsReceived++
          const event = JSON.parse(line.slice(5))
          const answered = sentAt.get(`${event.itemId}:${Number(event.amount)}`)
          // only one listener measures, so the numbers aren't 50 copies of the same delay
          if (index === 0 && answered) eventDelays.push(Math.max(0, performance.now() - answered))
        }
      }
    })
    .catch(() => {})
}

// ---------- 4. the storm ----------
const accepted = [] // { itemId, amount, bidder }

async function bidder(user, items, deadline) {
  while (performance.now() < deadline) {
    const itemId = items[Math.floor(Math.random() * items.length)]
    const read = await call('GET /api/items/{id}', `/api/items/${itemId}`)
    if (!read.ok) {
      count(`read failed (${read.status})`)
      continue
    }
    const minimum = Number(read.data.minimumBid)
    if (minimum > PRICE_CEILING) {
      count('skipped: price over the test ceiling')
      await new Promise((r) => setTimeout(r, 50))
      continue
    }
    const key = `${itemId}:${minimum}`
    if (!sentAt.has(key)) sentAt.set(key, performance.now())
    const bid = await call('POST /api/bids', '/api/bids', { method: 'POST', token: user.token, body: { itemId, amount: minimum } })
    if (bid.ok) {
      count('bid accepted')
      accepted.push({ itemId, amount: minimum, bidder: user.name })
    } else if (bid.status === 400) {
      // someone else got there first (the price moved) or the bidder already leads: expected under contention
      // amounts replaced by N so the same reason is counted once
      const reason = String(bid.data?.eroare ?? 'unknown').replace(/\d+(\.\d+)?/g, 'N')
      count(`bid rejected (400): ${reason}`)
    } else {
      count(`bid failed (${bid.status})`)
    }
  }
}

// ---------- 5. checks ----------
async function verify(items) {
  const checks = []
  for (const itemId of items) {
    const item = (await call('GET /api/items/{id}', `/api/items/${itemId}`)).data
    const bids = (await call('GET /api/bids/{id}', `/api/bids/${itemId}`)).data ?? []
    const mine = accepted.filter((b) => b.itemId === itemId)
    const highest = Math.max(100, ...mine.map((b) => b.amount))
    const amounts = bids.map((b) => Number(b.amount))
    const unique = new Set(amounts).size === amounts.length
    const stored = bids.length === mine.length
    const price = Number(item.currentPrice) === highest
    const leader = bids.length === 0 || bids.reduce((a, b) => (Number(b.amount) > Number(a.amount) ? b : a)).bidder?.username === mine.find((b) => b.amount === highest)?.bidder
    checks.push({ itemId, bids: bids.length, accepted: mine.length, finalPrice: Number(item.currentPrice), highest, stored, unique, price, leader })
  }
  return checks
}

// ---------- main ----------
const health = await call('GET /api/health', '/api/health')
if (!health.ok) {
  console.error(`${BASE}/api/health is not answering - start the backend first.`)
  process.exit(1)
}
console.log(`Load test ${RUN}: ${BIDDERS} bidders, ${ITEMS} items, ${SECONDS}s, ${LISTENERS} live listeners -> ${BASE}`)

const setupStarted = performance.now()
let seller, users
try {
  seller = await account(`load_${RUN}_seller`)
  users = await inBatches(BIDDERS, SETUP_CONCURRENCY, (i) => account(`load_${RUN}_${i}`))
} catch (error) {
  console.error(error.message)
  if (String(error.message).includes('429')) console.error('Too many registrations: start the backend with APP_RATE_LIMIT_AUTH_PER_MINUTE=100000.')
  process.exit(1)
}
const items = []
for (let i = 0; i < ITEMS; i++) {
  const created = await call('POST /api/items', '/api/items', {
    method: 'POST',
    token: seller.token,
    body: { name: `Load test ${RUN} #${i + 1}`, description: 'Creat de load-test/bid-storm.mjs', startPrice: 100, durationInMinutes: 120 },
  })
  if (!created.ok) throw new Error(`create item: ${created.status} ${JSON.stringify(created.data)}`)
  items.push(created.data.id)
}
const setupSeconds = (performance.now() - setupStarted) / 1000
console.log(`Setup done in ${setupSeconds.toFixed(1)}s (items ${items.join(', ')}). Bidding...`)

const stop = []
for (let i = 0; i < LISTENERS; i++) listen(i, stop)
await new Promise((r) => setTimeout(r, 1000))
for (const endpoint of ['GET /api/items/{id}', 'POST /api/bids']) samples.delete(endpoint)

const stormStarted = performance.now()
const deadline = stormStarted + SECONDS * 1000
const progress = setInterval(() => process.stdout.write(`\r  ${accepted.length} accepted bids, ${((performance.now() - stormStarted) / 1000).toFixed(0)}s`), 1000)
await Promise.all(users.map((user) => bidder(user, items, deadline)))
clearInterval(progress)
const stormSeconds = (performance.now() - stormStarted) / 1000
await new Promise((r) => setTimeout(r, 1500)) // let the last live events arrive
stop.forEach((abort) => abort())
console.log('')

const checks = await verify(items)
const allGood = checks.every((c) => c.stored && c.unique && c.price && c.leader)

// ---------- 6. report ----------
const bidLatencies = samples.get('POST /api/bids') ?? []
const attempts = bidLatencies.length
const summary = {
  run: RUN,
  date: new Date().toISOString(),
  target: BASE,
  machine: `${os.cpus()[0]?.model ?? 'cpu'} x${os.cpus().length}, ${Math.round(os.totalmem() / 2 ** 30)} GB RAM, ${os.platform()}`,
  settings: { bidders: BIDDERS, items: ITEMS, seconds: SECONDS, listeners: LISTENERS },
  setupSeconds,
  stormSeconds,
  bidAttempts: attempts,
  acceptedBids: accepted.length,
  attemptsPerSecond: attempts / stormSeconds,
  acceptedPerSecond: accepted.length / stormSeconds,
  latency: Object.fromEntries(
    [...samples.entries()].map(([endpoint, values]) => [
      endpoint,
      { count: values.length, p50: percentile(values, 0.5), p90: percentile(values, 0.9), p99: percentile(values, 0.99), max: Math.max(...values) },
    ]),
  ),
  outcomes: Object.fromEntries([...outcomes.entries()].sort((a, b) => b[1] - a[1])),
  liveEvents: {
    received: eventsReceived,
    expected: accepted.length * LISTENERS,
    delayP50: percentile(eventDelays, 0.5),
    delayP95: percentile(eventDelays, 0.95),
    delayMax: eventDelays.length ? Math.max(...eventDelays) : 0,
  },
  checks,
  consistent: allGood,
}

const ms = (v) => `${v.toFixed(0)} ms`
const lines = [
  `# Load test ${RUN}`,
  '',
  `${summary.date} · ${BASE} · ${summary.machine}`,
  '',
  `**${BIDDERS} bidders** on **${ITEMS} auctions** for **${SECONDS} s**, with **${LISTENERS} live (SSE) listeners**. Setup (accounts + items): ${setupSeconds.toFixed(1)} s.`,
  '',
  '## Result',
  '',
  `| | |`,
  `|---|---|`,
  `| Bid attempts | ${attempts} (${summary.attemptsPerSecond.toFixed(1)}/s) |`,
  `| Accepted bids | ${accepted.length} (${summary.acceptedPerSecond.toFixed(1)}/s) |`,
  `| Live events received | ${eventsReceived} of ${summary.liveEvents.expected} expected |`,
  `| Bid sent -> live event received | p50 ${ms(summary.liveEvents.delayP50)}, p95 ${ms(summary.liveEvents.delayP95)}, max ${ms(summary.liveEvents.delayMax)} |`,
  `| Data consistent | ${allGood ? 'YES' : '**NO**'} |`,
  '',
  '## Response times',
  '',
  '| Endpoint | Requests | p50 | p90 | p99 | max |',
  '|---|---:|---:|---:|---:|---:|',
  ...Object.entries(summary.latency).map(([e, l]) => `| ${e} | ${l.count} | ${ms(l.p50)} | ${ms(l.p90)} | ${ms(l.p99)} | ${ms(l.max)} |`),
  '',
  '## Outcomes',
  '',
  '| Outcome | Count |',
  '|---|---:|',
  ...Object.entries(summary.outcomes).map(([label, n]) => `| ${label.replace(/\|/g, '/')} | ${n} |`),
  '',
  'Rejected bids (400) are expected: many bidders read the same price and only the first bid at that price wins the row lock; the rest see the new minimum.',
  '',
  '## Consistency per auction',
  '',
  '| Item | Stored bids | Accepted by API | Final price | Highest accepted | All stored | No duplicate amounts | Price = highest | Leader correct |',
  '|---:|---:|---:|---:|---:|:-:|:-:|:-:|:-:|',
  ...checks.map(
    (c) =>
      `| ${c.itemId} | ${c.bids} | ${c.accepted} | ${c.finalPrice} | ${c.highest} | ${c.stored ? '✓' : '✗'} | ${c.unique ? '✓' : '✗'} | ${c.price ? '✓' : '✗'} | ${c.leader ? '✓' : '✗'} |`,
  ),
  '',
]

const dir = join(dirname(fileURLToPath(import.meta.url)), 'reports')
mkdirSync(dir, { recursive: true })
const stamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19)
writeFileSync(join(dir, `${stamp}.md`), lines.join('\n'))
writeFileSync(join(dir, `${stamp}.json`), JSON.stringify(summary, null, 2))
console.log(lines.slice(6, 16).join('\n'))
console.log(`\nReport: load-test/reports/${stamp}.md`)
process.exit(allGood ? 0 : 2)
