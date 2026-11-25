# Load test: 200 simultaneous bidders

`bid-storm.mjs` checks two things under heavy load: how fast the API answers, and whether the data stays correct when hundreds of people bid on the same auctions at once. It uses Node 20+ and has no dependencies.

## Running it

```bash
# 1. Postgres + backend with a raised login limit (the script registers 201 accounts)
docker compose up -d
APP_RATE_LIMIT_AUTH_PER_MINUTE=100000 ./mvnw spring-boot:run
#   PowerShell: $env:APP_RATE_LIMIT_AUTH_PER_MINUTE = "100000"; .\mvnw.cmd spring-boot:run

# 2. the storm (defaults shown)
node load-test/bid-storm.mjs --bidders 200 --items 5 --seconds 30 --listeners 50
```

The script refuses any URL other than `localhost` unless you pass `--allow-remote`, because it creates hundreds of `load_<run>_*` accounts. Don't point it at production.

What it does:

1. Registers a seller and N bidders, then logs them in.
2. The seller lists `items` auctions (start price 100 RON, 2 hours).
3. `listeners` clients subscribe to the live stream `GET /api/events`.
4. For `seconds`, every bidder repeats: read a random item (`GET /api/items/{id}`), then bid its `minimumBid` (`POST /api/bids`).
5. Afterwards it checks every auction:
   - every bid the API accepted is stored;
   - no amount is stored twice;
   - the final price equals the highest accepted bid;
   - the leader is the bidder of that bid.
6. It writes `load-test/reports/<timestamp>.md` and `.json`. That folder is git-ignored.

The exit code is 0 when the data is consistent and 2 when it is not.

## Results (2026-11-25)

The test machine was a laptop (Intel i5-1035G1, 4 cores / 8 threads, 8 GB RAM, Windows). The backend, Postgres (Docker) and the load script all ran on that same laptop. Every run used 200 bidders on 5 auctions for 30 s, with 50 live listeners.

| Run | Bid attempts | Accepted | p50 / p99 `POST /api/bids` | p50 `GET /api/items/{id}` | Live events delivered | Bid → screen (p50 / p95) | Consistent |
|---|---:|---:|---:|---:|---:|---:|:-:|
| Before the fast pre-check | 4159 (135/s) | 243 | 763 / 1570 ms | 651 ms | 12150 / 12150 | 444 / 770 ms | yes |
| With the fast pre-check | 4135 (135/s) | 230 | 753 / 1412 ms | 709 ms | 11500 / 11500 | 482 / 861 ms | yes |
| Pre-check + Hikari pool 30 (default 10) | 4778 (155/s) | 265 | 647 / 1130 ms | 602 ms | 13250 / 13250 | 401 / 802 ms | yes |

### What the results show

- **Correctness holds.** Each auction had ~40 bidders reading the same price and bidding the same amount at the same moment. Every run accepted exactly one bid per price step. All bids were stored, no amount appeared twice, and the final prices and leaders were correct.
  - This works because of the row lock (`ItemRepository.findByIdForUpdate`). `ConcurrentBidTest` checks the same rule in the unit tests.
  - About 94% of attempts got a 400 "Oferta minima este ...". That is expected: they arrived after someone else had already taken that price step.
- **Every live event reached every listener.** That is 50 listeners × every accepted bid, with nothing lost. From the moment a bid was sent to the moment the other screens received it took about 0.4–0.5 s (p50) and under 1 s at p95. This was while the server was saturated.
- **The limit is the laptop, not the lock.**
  - Throughput stayed around 135–155 requests/s for each endpoint, whatever we changed. Even a plain read took ~650 ms, because 200 clients were queuing.
  - Raising the database pool from 10 to 30 connections gave only +15%.
  - Login (BCrypt, pure CPU) also took ~500 ms.
  - So the CPU is saturated: 250 client connections, the JVM and Postgres all share the same 4 cores. On separate machines the numbers would be higher.
- **The fast pre-check** (`BidService.placeBid` reads only the price, without a lock, and rejects a bid that is already too low before waiting for the row lock) made no measurable difference here, because the CPU is the limit. It stays in because a rejected bid no longer holds a database connection while it waits for the lock. That matters when the database pool is the bottleneck (for example on Render with a small pool).
- **Possible next steps if real traffic ever got this high:**
  - turn off `spring.jpa.open-in-view`, so a connection is held only during the transaction and not while JSON is written;
  - send the SSE fan-out from a separate thread pool;
  - cache `GET /api/items/{id}` for anonymous visitors for a second.
