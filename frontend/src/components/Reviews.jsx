import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, formatDate } from '../api.js'
import { Alert, Button, Textarea } from './ui.jsx'

// Stele afisate (rating 0-5, accepta si zecimale pentru medie)
export function Stars({ value, size = 'text-base' }) {
  return (
    <span className={`inline-flex tracking-tight ${size}`} aria-label={`${value} din 5 stele`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <span key={n} className={n <= Math.round(value) ? 'text-amber-400' : 'text-slate-300'}>
          ★
        </span>
      ))}
    </span>
  )
}

// O recenzie (pe profil sau pe pagina obiectului)
export function ReviewCard({ review, showItem = false }) {
  return (
    <div className="rounded-xl bg-white/80 p-4 ring-1 ring-slate-200">
      <div className="flex items-center justify-between gap-3">
        <Stars value={review.rating} />
        <span className="text-xs text-slate-400">{formatDate(review.createdAt)}</span>
      </div>
      {review.comment && <p className="mt-2 text-sm text-slate-700">„{review.comment}”</p>}
      <p className="mt-2 text-xs text-slate-500">
        de la{' '}
        <Link to={`/users/${encodeURIComponent(review.author)}`} className="font-medium text-indigo-600 hover:underline">
          {review.author}
        </Link>{' '}
        ({review.role})
        {showItem && (
          <>
            {' · '}
            <Link to={`/items/${review.itemId}`} className="hover:underline">
              {review.itemName}
            </Link>
          </>
        )}
      </p>
    </div>
  )
}

// Dupa vanzare: formularul de recenzie (pentru vanzator / castigator) si recenziile deja lasate
export function ItemReviews({ item, user }) {
  const [reviews, setReviews] = useState(null)
  const [rating, setRating] = useState(0)
  const [hover, setHover] = useState(0)
  const [comment, setComment] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api(`/items/${item.id}/reviews`, { auth: false })
      .then(setReviews)
      .catch(() => setReviews([]))
  }, [item.id])

  if (!reviews) return null
  const involved = user && (user.id === item.seller?.id || user.id === item.winner?.id)
  const alreadyReviewed = reviews.some((r) => r.author === user?.username)
  if (reviews.length === 0 && !involved) return null

  async function submit(event) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      const created = await api(`/items/${item.id}/reviews`, { method: 'POST', body: { rating, comment } })
      setReviews([...reviews, created])
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-3">
      <h2 className="text-lg font-semibold text-slate-900">Recenzii</h2>
      {involved && !alreadyReviewed && (
        <form onSubmit={submit} className="space-y-3 rounded-xl bg-amber-50/70 p-4 ring-1 ring-amber-200">
          <p className="text-sm font-medium text-slate-800">
            Cum a fost {user.id === item.seller?.id ? 'cumparatorul' : 'vanzatorul'}? Lasa o nota:
          </p>
          <div className="flex gap-1 text-3xl" onMouseLeave={() => setHover(0)}>
            {[1, 2, 3, 4, 5].map((n) => (
              <button
                key={n}
                type="button"
                onClick={() => setRating(n)}
                onMouseEnter={() => setHover(n)}
                aria-label={`${n} stele`}
                className={`transition hover:scale-110 ${n <= (hover || rating) ? 'text-amber-400' : 'text-slate-300'}`}
              >
                ★
              </button>
            ))}
          </div>
          <Textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            maxLength={500}
            placeholder="Un comentariu scurt (optional)"
          />
          <Alert>{error}</Alert>
          <Button type="submit" loading={saving} disabled={!rating}>
            Trimite recenzia
          </Button>
        </form>
      )}
      {reviews.map((review) => (
        <ReviewCard key={review.id} review={review} />
      ))}
    </div>
  )
}
