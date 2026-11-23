import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, formatDate, formatPrice } from '../api.js'
import ItemCard from '../components/ItemCard.jsx'
import { Alert, Badge, Card, EmptyState, Loading } from '../components/ui.jsx'

// Profilul public al unui vanzator: obiectele lui la licitatie si cele vandute (fara email)
export default function Profile() {
  const { username } = useParams()
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api(`/users/${encodeURIComponent(username)}/profile`, { auth: false })
      .then((data) => {
        setProfile(data)
        setError('')
      })
      .catch((err) => setError(err.message))
  }, [username])

  if (error) {
    return (
      <div>
        <Alert>{error}</Alert>
        <Link to="/" className="mt-4 inline-block text-sm font-medium text-indigo-600 hover:text-indigo-500">
          ← Inapoi la licitatii
        </Link>
      </div>
    )
  }
  if (!profile || profile.username !== username) return <Loading />

  const soldValue = profile.soldItems.reduce((sum, item) => sum + Number(item.currentPrice), 0)

  return (
    <div>
      <div className="flex items-center gap-4">
        <span className="grid h-16 w-16 shrink-0 place-items-center rounded-2xl bg-indigo-600 text-2xl font-bold text-white">
          {profile.username[0]?.toUpperCase()}
        </span>
        <div className="min-w-0">
          <h1 className="truncate text-2xl font-bold tracking-tight text-slate-900">{profile.username}</h1>
          <p className="text-sm text-slate-500">Membru din {formatDate(profile.createdAt)}</p>
        </div>
      </div>

      <div className="mt-6 grid grid-cols-3 gap-3 sm:max-w-xl">
        <Stat label="La licitatie acum" value={profile.activeItems.length} />
        <Stat label="Obiecte vandute" value={profile.soldItems.length} />
        <Stat label="Valoare vanduta" value={formatPrice(soldValue)} />
      </div>

      <h2 className="mt-10 text-lg font-semibold text-slate-900">La licitatie acum</h2>
      <div className="mt-4">
        {profile.activeItems.length === 0 ? (
          <EmptyState title="Nicio licitatie activa">Revino mai tarziu pentru obiecte noi.</EmptyState>
        ) : (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {profile.activeItems.map((item) => (
              <ItemCard key={item.id} item={item} />
            ))}
          </div>
        )}
      </div>

      {profile.soldItems.length > 0 && (
        <>
          <h2 className="mt-10 text-lg font-semibold text-slate-900">Vandute</h2>
          <div className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {profile.soldItems.map((item) => (
              <ItemCard key={item.id} item={item} extra={<Badge tone="green">Vandut lui {item.winner.username}</Badge>} />
            ))}
          </div>
        </>
      )}
    </div>
  )
}

function Stat({ label, value }) {
  return (
    <Card className="p-4">
      <p className="text-xs font-medium text-slate-400">{label}</p>
      <p className="mt-1 truncate text-lg font-bold text-slate-900">{value}</p>
    </Card>
  )
}
