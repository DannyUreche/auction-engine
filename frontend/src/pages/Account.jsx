import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { api, formatDate, formatPrice } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Icon } from '../components/icons.jsx'
import ItemCard from '../components/ItemCard.jsx'
import { Alert, Badge, Card, EmptyState, Loading } from '../components/ui.jsx'
import WalletPanel from '../components/WalletPanel.jsx'
import { useWallet } from '../wallet.js'
import AccountSettings from '../components/AccountSettings.jsx'
import AlertsPanel from '../components/AlertsPanel.jsx'
import CollectionsPanel from '../components/CollectionsPanel.jsx'
import SellerStats from '../components/SellerStats.jsx'
import { useWatchlist } from '../watchlist.jsx'

const TABS = [
  { key: 'items', label: 'Obiectele mele' },
  { key: 'bids', label: 'Ofertele mele' },
  { key: 'won', label: 'Castigate' },
  { key: 'watched', label: 'Urmarite' },
  { key: 'collections', label: 'Colectii' },
  { key: 'wallet', label: 'Portofel' },
  { key: 'stats', label: 'Statistici' },
  { key: 'alerts', label: 'Alerte' },
  { key: 'settings', label: 'Setari cont' },
]

export default function Account() {
  const { user } = useAuth()
  const [params] = useSearchParams()
  const [tab, setTab] = useState(() => (TABS.some((t) => t.key === params.get('tab')) ? params.get('tab') : 'items'))
  const { wallet, reload: reloadWallet } = useWallet(user)
  const [walletOverride, setWalletOverride] = useState(null)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const watchlist = useWatchlist()

  useEffect(() => {
    Promise.all([api('/items/my'), api('/bids/my'), api('/items/won')])
      .then(([items, bids, won]) => setData({ items, bids, won }))
      .catch((err) => setError(err.message))
  }, [])

  return (
    <div>
      <div className="flex items-center gap-4">
        <span className="grid h-16 w-16 place-items-center rounded-2xl bg-indigo-600 text-2xl font-bold text-white">
          {user?.username?.[0]?.toUpperCase()}
        </span>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">{user?.username}</h1>
          <p className="text-sm text-slate-500">
            {user?.email} · membru din {user?.createdAt && formatDate(user.createdAt)}
          </p>
        </div>
      </div>

      <div className="mt-8 flex gap-1 overflow-x-auto rounded-xl bg-slate-100 p-1 sm:inline-flex">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`flex-1 shrink-0 rounded-lg px-3 py-2 text-sm font-medium whitespace-nowrap transition sm:flex-none sm:px-4 ${
              tab === t.key ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            {t.label}
            {data && (
              <span className="ml-1.5 text-slate-400">
                {t.key === 'watched' ? watchlist.items.length : data[t.key]?.length ?? ''}
              </span>
            )}
          </button>
        ))}
      </div>

      <div className="mt-6">
        <Alert>{error}</Alert>
        {!data && !error && <Loading />}
        {data && tab === 'items' && <MyItems items={data.items} />}
        {data && tab === 'bids' && <MyBids bids={data.bids} userId={user?.id} />}
        {data && tab === 'won' && <WonItems items={data.won} />}
        {data && tab === 'watched' && <WatchedItems items={watchlist.items} loaded={watchlist.loaded} />}
        {data && tab === 'stats' && <SellerStats />}
        {data && tab === 'alerts' && <AlertsPanel />}
        {data && tab === 'collections' && <CollectionsPanel />}
        {data && tab === 'settings' && <AccountSettings user={user} />}
        {data && tab === 'wallet' && (
          <WalletPanel
            wallet={walletOverride ?? wallet}
            onChange={(updated) => {
              setWalletOverride(updated)
              reloadWallet()
            }}
          />
        )}
      </div>
    </div>
  )
}

function MyItems({ items }) {
  if (items.length === 0) {
    return (
      <EmptyState title="Nu ai pus inca nimic la licitatie">
        <Link to="/sell" className="font-semibold text-indigo-600 hover:text-indigo-500">
          Vinde primul obiect
        </Link>
      </EmptyState>
    )
  }
  return (
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      {items.map((item) => (
        <ItemCard
          key={item.id}
          item={item}
          extra={
            !item.active &&
            (item.winner ? (
              <Badge tone="green">Vandut lui {item.winner.username}</Badge>
            ) : (
              <Badge tone="gray">Nevandut</Badge>
            ))
          }
        />
      ))}
    </div>
  )
}

// Starea unei oferte, comparata cu situatia actuala a obiectului
function bidStatus(bid, userId) {
  const item = bid.item
  const isHighest = Number(bid.amount) === Number(item.currentPrice)
  if (item.active) return isHighest ? ['Conduci', 'green'] : ['Depasita', 'red']
  if (item.winner?.id === userId && isHighest) return ['Castigata', 'amber']
  return ['Pierduta', 'gray']
}

function MyBids({ bids, userId }) {
  if (bids.length === 0) {
    return (
      <EmptyState title="Nu ai plasat inca nicio oferta">
        <Link to="/" className="font-semibold text-indigo-600 hover:text-indigo-500">
          Vezi licitatiile active
        </Link>
      </EmptyState>
    )
  }
  return (
    <Card className="p-0">
      <ul className="divide-y divide-slate-100">
        {bids.map((bid) => {
          const [label, tone] = bidStatus(bid, userId)
          return (
            <li key={bid.id}>
              <Link
                to={`/items/${bid.item.id}`}
                className="flex items-center justify-between gap-4 px-6 py-4 transition hover:bg-slate-50"
              >
                <div className="min-w-0">
                  <p className="truncate font-medium text-slate-900">{bid.item.name}</p>
                  <p className="text-xs text-slate-500">{formatDate(bid.createdAt)}</p>
                </div>
                <div className="flex shrink-0 items-center gap-3">
                  <Badge tone={tone}>{label}</Badge>
                  <span className="font-semibold text-slate-900">{formatPrice(bid.amount)}</span>
                </div>
              </Link>
            </li>
          )
        })}
      </ul>
    </Card>
  )
}

function WonItems({ items }) {
  if (items.length === 0) {
    return <EmptyState title="Nu ai castigat inca nicio licitatie">Liciteaza si poate data viitoare e a ta!</EmptyState>
  }
  return (
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      {items.map((item) => (
        <ItemCard key={item.id} item={item} extra={
            <Badge tone="amber">
              <Icon name="trophy" size={14} /> Castigat
            </Badge>
          } />
      ))}
    </div>
  )
}

// Obiectele urmarite (lista vine din WatchlistProvider, ca sa se actualizeze imediat la ♥)
function WatchedItems({ items, loaded }) {
  if (!loaded) return <Loading />
  if (items.length === 0) {
    return (
      <EmptyState title="Nu urmaresti inca niciun obiect">
        Apasa pe ♥ la un obiect ca sa-l urmaresti: primesti o notificare cand cineva liciteaza.
      </EmptyState>
    )
  }
  return (
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      {items.map((item) => (
        <ItemCard key={item.id} item={item} />
      ))}
    </div>
  )
}
