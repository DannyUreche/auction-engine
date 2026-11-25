import { useEffect, useState } from 'react'
import { api, formatPrice } from '../api.js'
import { findCategory, useCategories } from '../categories.js'
import { CategoryIcon, Icon } from './icons.jsx'
import { Link } from 'react-router'
import FollowButton from './FollowButton.jsx'
import { Alert, Card, EmptyState, Loading } from './ui.jsx'

// Account tab: the user's saved searches. New ones are added from the filter panel on the home page.
export default function AlertsPanel() {
  const categories = useCategories()
  const [alerts, setAlerts] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/alerts')
      .then(setAlerts)
      .catch((err) => setError(err.message))
  }, [])

  async function remove(id) {
    try {
      await api(`/alerts/${id}`, { method: 'DELETE' })
      setAlerts((current) => current.filter((alert) => alert.id !== id))
    } catch (err) {
      setError(err.message)
    }
  }

  if (error) return <Alert>{error}</Alert>
  if (!alerts) return <Loading />
  return (
    <div className="space-y-4">
      <FollowedSellers />
      <p className="text-sm text-slate-600">
        Primesti o notificare (si push, daca le-ai activat) cand apare o licitatie noua care se potriveste. Adauga alerte din
        panoul de filtre de pe pagina principala, cu butonul „Salveaza ca alerta”. Maxim 10.
      </p>
      {alerts.length === 0 ? (
        <EmptyState title="Nicio alerta salvata">Cauta ceva pe pagina principala si salveaza cautarea ca alerta.</EmptyState>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2">
          {alerts.map((alert) => {
            const category = alert.category ? findCategory(categories, alert.category) : null
            return (
              <Card key={alert.id} className="flex items-start justify-between gap-3 !p-4">
                <div className="min-w-0 space-y-1 text-sm">
                  <p className="flex items-center gap-2 font-semibold text-slate-900">
                    <Icon name="bell" size={18} /> {alert.query ? `„${alert.query}”` : 'Orice obiect'}
                  </p>
                  {category && (
                    <p className="flex items-center gap-1.5 text-slate-600">
                      <CategoryIcon category={category} size={16} /> {category.label}
                    </p>
                  )}
                  {(alert.minPrice != null || alert.maxPrice != null) && (
                    <p className="text-slate-600">
                      {alert.minPrice != null && `de la ${formatPrice(alert.minPrice)} `}
                      {alert.maxPrice != null && `pana la ${formatPrice(alert.maxPrice)}`}
                    </p>
                  )}
                </div>
                <button onClick={() => remove(alert.id)} className="shrink-0 text-sm font-semibold text-red-600 hover:text-red-500">
                  Sterge
                </button>
              </Card>
            )
          })}
        </div>
      )}
    </div>
  )
}

// Sellers the user follows: a notification arrives when they list something new
function FollowedSellers() {
  const [sellers, setSellers] = useState(null)
  useEffect(() => {
    api('/users/me/following')
      .then(setSellers)
      .catch(() => setSellers([]))
  }, [])
  if (!sellers || sellers.length === 0) return null
  return (
    <Card>
      <h3 className="flex items-center gap-2 font-semibold text-slate-900">
        <Icon name="bell" size={20} /> Vanzatori urmariti
      </h3>
      <p className="mt-0.5 text-sm text-slate-500">Primesti o notificare cand pun ceva nou la licitatie.</p>
      <ul className="mt-3 divide-y divide-slate-100">
        {sellers.map((seller) => (
          <li key={seller.username} className="flex items-center justify-between gap-3 py-2.5">
            <Link to={`/users/${encodeURIComponent(seller.username)}`} className="flex min-w-0 items-center gap-3">
              <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-gradient-to-br from-indigo-500 to-fuchsia-500 text-sm font-bold text-white">
                {seller.username[0]?.toUpperCase()}
              </span>
              <span className="min-w-0">
                <span className="block truncate font-medium text-slate-900 hover:text-indigo-600">{seller.username}</span>
                <span className="block text-xs text-slate-500">
                  {seller.activeItems} la licitatie · {seller.followers} urmaritori
                </span>
              </span>
            </Link>
            <FollowButton username={seller.username} compact />
          </li>
        ))}
      </ul>
    </Card>
  )
}
