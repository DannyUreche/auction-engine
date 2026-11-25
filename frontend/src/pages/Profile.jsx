import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, formatDate, formatPrice } from '../api.js'
import Badges from '../components/Badges.jsx'
import CollectionCard from '../components/CollectionCard.jsx'
import FollowButton from '../components/FollowButton.jsx'
import ItemCard from '../components/ItemCard.jsx'
import { ReviewCard, Stars } from '../components/Reviews.jsx'
import { Alert, Badge, Card, EmptyState, Loading } from '../components/ui.jsx'

// Profilul public al unui vanzator: obiectele lui la licitatie si cele vandute (fara email)
export default function Profile() {
  const { username } = useParams()
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')
  const [followersOverride, setFollowers] = useState(null)
  const [collections, setCollections] = useState([])

  useEffect(() => {
    api(`/users/${encodeURIComponent(username)}/collections`, { auth: false })
      .then(setCollections)
      .catch(() => setCollections([]))
  }, [username])

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

  const followers = followersOverride ?? profile.followers
  const soldValue = profile.soldItems.reduce((sum, item) => sum + Number(item.currentPrice), 0)

  return (
    <div>
      {/* Cover with the avatar overlapping it, follower count and the follow button */}
      <section className="overflow-hidden rounded-3xl bg-white/85 shadow-xl ring-1 shadow-indigo-500/5 ring-slate-200/80">
        <div className="stay-dark relative h-36 overflow-hidden bg-slate-950 sm:h-44">
          <div aria-hidden="true" className="absolute inset-0">
            <div className="absolute -top-20 -left-10 h-64 w-64 animate-float rounded-full bg-indigo-600/50 blur-3xl" />
            <div className="absolute -right-10 -bottom-24 h-72 w-72 animate-float-slow rounded-full bg-fuchsia-600/40 blur-3xl" />
            <div className="absolute top-6 left-1/2 h-40 w-40 animate-float rounded-full bg-sky-500/30 blur-3xl [animation-delay:-5s]" />
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_1px_1px,rgb(255_255_255/0.08)_1px,transparent_0)] bg-[size:20px_20px]" />
          </div>
        </div>
        <div className="relative flex flex-wrap items-end justify-between gap-4 px-6 pt-4 pb-6 sm:px-8">
          {/* Only the avatar overlaps the cover; the text stays on the light part */}
          <span className="absolute -top-12 left-6 grid h-24 w-24 place-items-center rounded-3xl bg-gradient-to-br from-indigo-500 via-violet-500 to-fuchsia-500 text-4xl font-black text-white shadow-xl ring-4 ring-white sm:left-8">
            {profile.username[0]?.toUpperCase()}
          </span>
          <div className="flex items-end gap-4 pt-12 sm:pt-0 sm:pl-28">
            <div className="min-w-0 pb-1">
              <h1 className="truncate text-2xl font-bold tracking-tight text-slate-900">{profile.username}</h1>
              <p className="text-sm text-slate-500">
                Membru din {formatDate(profile.createdAt)} · <b className="text-slate-800">{followers}</b> {followers === 1 ? 'urmaritor' : 'urmaritori'}
              </p>
              {profile.rating.count > 0 ? (
                <p className="mt-1 flex items-center gap-2 text-sm text-slate-600">
                  <Stars value={profile.rating.average} />
                  <span className="font-semibold text-slate-900">{profile.rating.average.toFixed(1)}</span>
                  <span>
                    din {profile.rating.count} {profile.rating.count === 1 ? 'recenzie' : 'recenzii'}
                  </span>
                </p>
              ) : (
                <p className="mt-1 text-sm text-slate-400">Inca nu are recenzii</p>
              )}
            </div>
          </div>
          <FollowButton username={profile.username} onChange={setFollowers} />
        </div>
      </section>

      <div className="mt-6 grid grid-cols-3 gap-3 sm:max-w-xl">
        <Stat label="La licitatie acum" value={profile.activeItems.length} />
        <Stat label="Obiecte vandute" value={profile.soldItems.length} />
        <Stat label="Valoare vanduta" value={formatPrice(soldValue)} />
      </div>

      {profile.badges?.length > 0 && (
        <>
          <h2 className="mt-10 text-lg font-semibold text-slate-900">Insigne</h2>
          <div className="mt-4">
            <Badges badges={profile.badges} />
          </div>
        </>
      )}

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

      {collections.length > 0 && (
        <>
          <h2 className="mt-10 text-lg font-semibold text-slate-900">Colectii</h2>
          <div className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {collections.map((list, index) => (
              <CollectionCard key={list.id} list={list} index={index} />
            ))}
          </div>
        </>
      )}

      {profile.reviews.length > 0 && (
        <>
          <h2 className="mt-10 text-lg font-semibold text-slate-900">Recenzii primite</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            {profile.reviews.map((review) => (
              <ReviewCard key={review.id} review={review} showItem />
            ))}
          </div>
        </>
      )}

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
