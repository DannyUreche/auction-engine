import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Icon } from './icons.jsx'

// "Urmareste vanzatorul": follow / unfollow with a little pop; compact = small pill for the item page.
// onChange(followers) lets the profile update its follower count.
export default function FollowButton({ username, compact = false, onChange }) {
  const { user } = useAuth()
  const [state, setState] = useState(null) // { following, followers }
  const [busy, setBusy] = useState(false)
  const [pop, setPop] = useState(0)

  useEffect(() => {
    if (!user || user.username === username) return
    api(`/users/${encodeURIComponent(username)}/follow`)
      .then(setState)
      .catch(() => {})
  }, [user, username])

  if (user?.username === username) return null
  if (!user) {
    return compact ? null : (
      <Link
        to="/login"
        className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-indigo-500/30"
      >
        <Icon name="bell" size={18} mono /> Urmareste
      </Link>
    )
  }
  if (!state) return null

  async function toggle() {
    setBusy(true)
    try {
      const next = await api(`/users/${encodeURIComponent(username)}/follow`, { method: state.following ? 'DELETE' : 'POST' })
      setState(next)
      setPop((value) => value + 1)
      onChange?.(next.followers)
    } finally {
      setBusy(false)
    }
  }

  if (compact) {
    return (
      <button
        onClick={toggle}
        disabled={busy}
        className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-semibold ring-1 transition ${
          state.following ? 'bg-indigo-50 text-indigo-700 ring-indigo-200' : 'bg-white text-slate-700 ring-slate-300 hover:text-indigo-700 hover:ring-indigo-400'
        }`}
      >
        <Icon name={state.following ? 'tagCheck' : 'bell'} size={13} /> {state.following ? 'Urmaresti vanzatorul' : 'Urmareste vanzatorul'}
      </button>
    )
  }

  return (
    <button
      onClick={toggle}
      disabled={busy}
      key={pop}
      className={`group inline-flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-semibold transition ${pop ? 'follow-pop' : ''} ${
        state.following
          ? 'bg-white text-slate-800 shadow-sm ring-1 ring-slate-300 hover:text-rose-600 hover:ring-rose-300'
          : 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-lg shadow-indigo-500/30 hover:-translate-y-0.5 hover:shadow-xl'
      }`}
    >
      <Icon name={state.following ? 'tagCheck' : 'bell'} size={18} mono={!state.following} />
      {state.following ? (
        <>
          <span className="group-hover:hidden">Urmaresti</span>
          <span className="hidden group-hover:inline">Nu mai urmari</span>
        </>
      ) : (
        'Urmareste'
      )}
    </button>
  )
}
