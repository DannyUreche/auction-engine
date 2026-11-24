import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, formatDate, formatPrice } from '../api.js'
import AdminCharts from '../components/AdminCharts.jsx'
import { Icon } from '../components/icons.jsx'
import ItemImage from '../components/ItemImage.jsx'
import { Alert, Badge, Button, Card, Loading } from '../components/ui.jsx'
import { useAuctionEvents } from '../events.jsx'

const TABS = [
  { key: 'items', label: 'Obiecte' },
  { key: 'users', label: 'Utilizatori' },
  { key: 'reports', label: 'Raportari' },
]

// Pagina de administrare (doar pentru conturile din ADMIN_USERNAMES)
export default function Admin() {
  const [tab, setTab] = useState('items')
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const load = useCallback(async () => {
    try {
      const [stats, items, users, reports] = await Promise.all([
        api('/admin/stats'),
        api('/admin/items'),
        api('/admin/users'),
        api('/admin/reports'),
      ])
      setData({ stats, items, users, reports })
      setError('')
    } catch (err) {
      setError(err.message)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  // Orice oferta noua / licitatie inchisa / obiect sters actualizeaza tabelele
  useAuctionEvents(load)

  async function closeNow(item) {
    if (!window.confirm(`Inchizi acum licitatia pentru "${item.name}"? Castiga oferta cea mai mare de acum.`)) return
    try {
      const updated = await api(`/admin/items/${item.id}/close`, { method: 'POST' })
      setMessage(
        updated.winner
          ? `Licitatia "${item.name}" a fost inchisa. Castigator: ${updated.winner}.`
          : `Licitatia "${item.name}" a fost inchisa fara oferte.`,
      )
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function deleteItem(item) {
    if (!window.confirm(`Stergi definitiv "${item.name}" impreuna cu cele ${item.bidsCount} oferte? Nu se poate anula.`)) return
    try {
      await api(`/admin/items/${item.id}`, { method: 'DELETE' })
      setMessage(`Obiectul "${item.name}" a fost sters.`)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function dismissReport(report) {
    try {
      await api(`/admin/reports/${report.id}/dismiss`, { method: 'POST' })
      setMessage(`Raportarea pentru "${report.itemName}" a fost respinsa.`)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function toggleBlock(user) {
    const action = user.blocked ? 'unblock' : 'block'
    if (!user.blocked && !window.confirm(`Blochezi contul "${user.username}"? Nu va mai putea intra in cont.`)) return
    try {
      await api(`/admin/users/${user.id}/${action}`, { method: 'POST' })
      setMessage(user.blocked ? `Contul "${user.username}" a fost deblocat.` : `Contul "${user.username}" a fost blocat.`)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  if (!data && !error) return <Loading />

  return (
    <div>
      <div className="flex items-center gap-3">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">Administrare</h1>
        <Badge tone="indigo">Admin</Badge>
      </div>
      <p className="mt-2 text-slate-500">Statistici, toate obiectele si toti utilizatorii platformei.</p>

      <DemoDataPanel onChange={load} />
      <AdminCharts refreshKey={data} />

      <div className="mt-6 space-y-3">
        <Alert>{error}</Alert>
        <Alert tone="success">{message}</Alert>
      </div>

      {data && (
        <>
          <div className="mt-6 grid grid-cols-2 gap-4 lg:grid-cols-6">
            <Stat label="Utilizatori" value={data.stats.users} />
            <Stat label="Obiecte" value={data.stats.items} />
            <Stat label="Licitatii active" value={data.stats.activeItems} />
            <Stat label="Oferte" value={data.stats.bids} />
            <Stat label="Obiecte vandute" value={data.stats.soldItems} />
            <Stat label="Valoare vanduta" value={formatPrice(data.stats.soldValue)} />
          </div>

          <div className="mt-8 flex gap-1 rounded-xl bg-slate-100 p-1 sm:inline-flex">
            {TABS.map((t) => (
              <button
                key={t.key}
                onClick={() => setTab(t.key)}
                className={`flex-1 rounded-lg px-4 py-2 text-sm font-medium transition sm:flex-none ${
                  tab === t.key ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {t.label}
                <span className="ml-1.5 text-slate-400">{data[t.key].length}</span>
              </button>
            ))}
          </div>

          <Card className="mt-6 overflow-x-auto p-0">
            {tab === 'items' && <ItemsTable items={data.items} onClose={closeNow} onDelete={deleteItem} />}
            {tab === 'users' && <UsersTable users={data.users} onToggleBlock={toggleBlock} />}
            {tab === 'reports' && (
              <ReportsTable
                reports={data.reports}
                onDismiss={dismissReport}
                onDeleteItem={(report) => deleteItem({ id: report.itemId, name: report.itemName, bidsCount: 'toate' })}
              />
            )}
          </Card>
        </>
      )}
    </div>
  )
}

function Stat({ label, value }) {
  return (
    <div className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200">
      <p className="text-xs font-medium tracking-wide text-slate-400 uppercase">{label}</p>
      <p className="mt-1 text-xl font-bold [overflow-wrap:anywhere] text-slate-900 xl:text-2xl">{value}</p>
    </div>
  )
}

const th = 'px-3 py-3 text-left text-xs font-semibold tracking-wide whitespace-nowrap text-slate-500 uppercase'
const td = 'px-3 py-3 text-sm whitespace-nowrap text-slate-700'

function ItemsTable({ items, onClose, onDelete }) {
  if (items.length === 0) return <p className="p-6 text-sm text-slate-500">Nu exista obiecte.</p>
  return (
    <table className="min-w-full divide-y divide-slate-100">
      <thead className="bg-slate-50">
        <tr>
          <th className={th}>Obiect</th>
          <th className={th}>Vanzator</th>
          <th className={th}>Pret</th>
          <th className={th}>Oferte</th>
          <th className={th}>Stare</th>
          <th className={th}>Final</th>
          <th className={th}></th>
        </tr>
      </thead>
      <tbody className="divide-y divide-slate-100">
        {items.map((item) => (
          <tr key={item.id} className="hover:bg-slate-50">
            <td className={td}>
              <Link to={`/items/${item.id}`} className="flex items-center gap-3 font-medium text-slate-900 hover:text-indigo-600">
                <ItemImage item={item} small className="h-10 w-10 shrink-0 rounded-lg" />
                <span className="max-w-44 truncate" title={item.name}>{item.name}</span>
              </Link>
            </td>
            <td className={td}>{item.seller}</td>
            <td className={`${td} font-semibold text-slate-900`}>{formatPrice(item.currentPrice)}</td>
            <td className={td}>{item.bidsCount}</td>
            <td className={td}>
              {item.active ? (
                <Badge tone="green">Activa</Badge>
              ) : item.winner ? (
                <Badge tone="amber">
                    <Icon name="trophy" size={14} /> {item.winner}
                  </Badge>
              ) : (
                <Badge tone="gray">Fara oferte</Badge>
              )}
            </td>
            <td className={`${td} text-xs text-slate-500`}>{formatDate(item.endTime)}</td>
            <td className={`${td} text-right`}>
              <div className="flex justify-end gap-2">
                {item.active && (
                  <button
                    onClick={() => onClose(item)}
                    className="rounded-lg px-3 py-1.5 text-xs font-semibold text-amber-700 ring-1 ring-amber-200 hover:bg-amber-50"
                  >
                    Inchide
                  </button>
                )}
                <button
                  onClick={() => onDelete(item)}
                  className="rounded-lg px-3 py-1.5 text-xs font-semibold text-red-600 ring-1 ring-red-200 hover:bg-red-50"
                >
                  Sterge
                </button>
              </div>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function UsersTable({ users, onToggleBlock }) {
  return (
    <table className="min-w-full divide-y divide-slate-100">
      <thead className="bg-slate-50">
        <tr>
          <th className={th}>Id</th>
          <th className={th}>Username</th>
          <th className={th}>Email</th>
          <th className={th}>Inscris</th>
          <th className={th}>Obiecte</th>
          <th className={th}>Oferte</th>
          <th className={th} />
        </tr>
      </thead>
      <tbody className="divide-y divide-slate-100">
        {users.map((user) => (
          <tr key={user.id} className="hover:bg-slate-50">
            <td className={`${td} text-slate-400`}>{user.id}</td>
            <td className={`${td} font-medium text-slate-900`}>
              {user.username} {user.admin && <Badge tone="indigo">Admin</Badge>}{' '}
              {user.blocked && <Badge tone="red">Blocat</Badge>}
            </td>
            <td className={td}>{user.email}</td>
            <td className={`${td} text-slate-500`}>{formatDate(user.createdAt)}</td>
            <td className={td}>{user.itemsCount}</td>
            <td className={td}>{user.bidsCount}</td>
            <td className={`${td} text-right`}>
              {!user.admin && (
                <button
                  type="button"
                  onClick={() => onToggleBlock(user)}
                  className={`rounded-lg px-2.5 py-1 text-xs font-semibold ring-1 ${
                    user.blocked ? 'text-emerald-700 ring-emerald-200 hover:bg-emerald-50' : 'text-red-600 ring-red-200 hover:bg-red-50'
                  }`}
                >
                  {user.blocked ? 'Deblocheaza' : 'Blocheaza'}
                </button>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

const REASONS = {
  FRAUD: 'Posibila inselatorie',
  PROHIBITED: 'Obiect interzis',
  WRONG_CATEGORY: 'Categorie gresita',
  OFFENSIVE: 'Continut ofensator',
  OTHER: 'Altceva',
}

// Open reports: look at the item, then dismiss the report or delete the item
function ReportsTable({ reports, onDismiss, onDeleteItem }) {
  if (reports.length === 0) return <p className="px-6 py-10 text-center text-sm text-slate-500">Nicio raportare deschisa.</p>
  return (
    <ul className="divide-y divide-slate-100">
      {reports.map((report) => (
        <li key={report.id} className="flex flex-wrap items-center justify-between gap-3 px-6 py-4">
          <div className="min-w-0">
            <p className="flex items-center gap-2 font-medium text-slate-900">
              <Icon name="flag" size={18} />
              <Link to={`/items/${report.itemId}`} className="truncate hover:underline">
                {report.itemName}
              </Link>
              <Badge tone="red">{REASONS[report.reason] ?? report.reason}</Badge>
            </p>
            <p className="mt-0.5 text-sm text-slate-500">
              de la {report.reporter} · {formatDate(report.createdAt)}
              {report.details && <> · „{report.details}”</>}
            </p>
          </div>
          <div className="flex gap-2">
            <button type="button" onClick={() => onDismiss(report)} className="rounded-lg px-2.5 py-1 text-xs font-semibold text-slate-600 ring-1 ring-slate-200 hover:bg-slate-50">
              Respinge
            </button>
            <button type="button" onClick={() => onDeleteItem(report)} className="rounded-lg px-2.5 py-1 text-xs font-semibold text-red-600 ring-1 ring-red-200 hover:bg-red-50">
              Sterge obiectul
            </button>
          </div>
        </li>
      ))}
    </ul>
  )
}

// Date demo pentru prezentari: umple site-ul cu obiecte, poze si oferte realiste (si le sterge la loc)
function DemoDataPanel({ onChange }) {
  const [exists, setExists] = useState(null)
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState('')
  const [failure, setFailure] = useState('')

  useEffect(() => {
    api('/admin/demo-data')
      .then((data) => setExists(data.exists))
      .catch(() => setExists(null))
  }, [])

  async function run(method) {
    if (method === 'DELETE' && !window.confirm('Stergi toate obiectele si utilizatorii demo?')) return
    setBusy(true)
    setFailure('')
    setResult('')
    try {
      const data = await api('/admin/demo-data', { method })
      setExists(data.exists)
      setResult(data.exists ? `Gata: ${data.items} obiecte demo create.` : `Gata: ${data.items} obiecte demo sterse.`)
      onChange()
    } catch (err) {
      setFailure(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (exists === null) return null
  return (
    <div className="mt-6 flex flex-col gap-3 rounded-2xl bg-gradient-to-r from-indigo-50 to-fuchsia-50 p-5 ring-1 ring-indigo-100 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="flex items-center gap-2 font-semibold text-slate-900">
          <Icon name="clapper" size={22} /> Date demo pentru prezentare
        </p>
        <p className="mt-0.5 text-sm text-slate-600">
          Peste 100 de obiecte (cate 10+ in fiecare categorie), cu poze, descrieri, oferte de-a lungul a 2 zile, licitatii
          olandeze si programate si cateva vanzari. Utilizatorii au numele
          „demo_...” si nu se poate intra in contul lor.
        </p>
        {result && <p className="mt-1 text-sm font-medium text-emerald-700">{result}</p>}
        {failure && <p className="mt-1 text-sm font-medium text-red-600">{failure}</p>}
      </div>
      {exists ? (
        <Button variant="secondary" loading={busy} onClick={() => run('DELETE')} className="shrink-0">
          Sterge datele demo
        </Button>
      ) : (
        <Button loading={busy} onClick={() => run('POST')} className="shrink-0">
          Genereaza date demo
        </Button>
      )}
    </div>
  )
}
