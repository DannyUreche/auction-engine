import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, formatPrice, parseDate } from '../api.js'
import { useAuth } from '../auth.jsx'
import { useNow } from '../components/Countdown.jsx'
import { Icon } from '../components/icons.jsx'
import { Alert, Card, EmptyState, Loading } from '../components/ui.jsx'
import { useT } from '../i18n.jsx'

// Monthly leaderboard: a podium for the top 3, the rest of the top 10, and the user's monthly challenges
const MONTHS = ['ianuarie', 'februarie', 'martie', 'aprilie', 'mai', 'iunie', 'iulie', 'august', 'septembrie', 'octombrie', 'noiembrie', 'decembrie']

const MEDALS = [
  { ring: 'from-amber-200 via-amber-400 to-orange-500', block: 'from-amber-300 to-amber-500', height: 'h-40', text: 'text-amber-950' },
  { ring: 'from-slate-100 via-slate-300 to-slate-400', block: 'from-slate-200 to-slate-400', height: 'h-28', text: 'text-slate-800' },
  { ring: 'from-orange-200 via-orange-400 to-orange-700', block: 'from-orange-300 to-orange-600', height: 'h-20', text: 'text-orange-950' },
]

export default function Leaderboard() {
  const t = useT()
  const { user } = useAuth()
  const [board, setBoard] = useState(null)
  const [mine, setMine] = useState(null)
  const [tab, setTab] = useState('bidders')
  const [error, setError] = useState('')

  useEffect(() => {
    api('/leaderboard', { auth: false })
      .then(setBoard)
      .catch((err) => setError(err.message))
  }, [])

  useEffect(() => {
    if (!user) return
    api('/leaderboard/me')
      .then(setMine)
      .catch(() => {})
  }, [user])

  if (error) return <Alert>{error}</Alert>
  if (!board) return <Loading />

  const month = parseDate(board.monthStart)
  const rows =
    tab === 'bidders'
      ? board.bidders.map((r) => ({ username: r.username, main: `${r.bids} ${t('oferte')}`, sub: `${r.items} ${t('obiecte')} · max ${formatPrice(r.highestBid)}`, value: r.bids }))
      : board.sellers.map((r) => ({ username: r.username, main: formatPrice(r.value), sub: `${r.sold} ${r.sold === 1 ? t('vanzare') : t('vanzari')}`, value: Number(r.value) }))
  const max = Math.max(1, ...rows.map((r) => r.value))

  return (
    <div className="space-y-8">
      <Banner monthName={`${MONTHS[month.getUTCMonth()]} ${month.getUTCFullYear()}`} monthEnd={board.monthEnd} />

      <div className="grid gap-8 lg:grid-cols-[1.4fr_1fr]">
        <section>
          <div className="inline-flex rounded-xl bg-slate-100 p-1">
            {[
              ['bidders', t('Licitatori'), 'gavel'],
              ['sellers', t('Vanzatori'), 'tagCheck'],
            ].map(([key, label, icon]) => (
              <button
                key={key}
                onClick={() => setTab(key)}
                className={`inline-flex items-center gap-1.5 rounded-lg px-4 py-2 text-sm font-semibold transition ${
                  tab === key ? 'bg-white text-indigo-700 shadow-sm ring-1 ring-indigo-300' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Icon name={icon} size={16} /> {label}
              </button>
            ))}
          </div>

          {rows.length === 0 ? (
            <div className="mt-6">
              <EmptyState title={tab === 'bidders' ? t('Nicio oferta luna aceasta') : t('Nicio vanzare luna aceasta')}>
                {t('Fii primul in clasament!')}
              </EmptyState>
            </div>
          ) : (
            <>
              <Podium key={tab} rows={rows.slice(0, 3)} />
              <ol className="mt-6 space-y-2">
                {rows.slice(3).map((row, index) => (
                  <li key={row.username} className="animate-fade-up" style={{ animationDelay: `${400 + index * 60}ms` }}>
                    <Link
                      to={`/users/${encodeURIComponent(row.username)}`}
                      className={`flex items-center gap-4 rounded-2xl bg-white/85 px-4 py-3 shadow-sm ring-1 transition hover:-translate-y-0.5 hover:shadow-md ${
                        row.username === user?.username ? 'ring-2 ring-indigo-400' : 'ring-slate-200/80'
                      }`}
                    >
                      <span className="w-6 text-center text-lg font-black text-slate-400 tabular-nums">{index + 4}</span>
                      <Avatar name={row.username} className="h-9 w-9 text-sm" />
                      <span className="min-w-0 flex-1">
                        <span className="block truncate font-semibold text-slate-900">{row.username}</span>
                        <span className="block text-xs text-slate-500">{row.sub}</span>
                      </span>
                      <span className="hidden h-1.5 w-28 overflow-hidden rounded-full bg-slate-200 sm:block">
                        <span className="block h-full rounded-full bg-indigo-500" style={{ width: `${(row.value / max) * 100}%` }} />
                      </span>
                      <span className="w-24 text-right font-bold text-slate-900 tabular-nums">{row.main}</span>
                    </Link>
                  </li>
                ))}
              </ol>
            </>
          )}
        </section>

        <Challenges user={user} mine={mine} />
      </div>
    </div>
  )
}

function Banner({ monthName, monthEnd }) {
  const t = useT()
  const now = useNow()
  const left = Math.max(0, parseDate(monthEnd).getTime() - now)
  const days = Math.floor(left / 86400000)
  const hours = Math.floor((left % 86400000) / 3600000)
  return (
    <section className="stay-dark relative overflow-hidden rounded-3xl bg-slate-950 px-6 py-8 text-white shadow-2xl shadow-indigo-950/20 ring-1 ring-white/10 sm:px-10">
      <div aria-hidden="true" className="pointer-events-none absolute inset-0">
        <div className="absolute -top-24 -left-10 h-72 w-72 animate-float rounded-full bg-indigo-600/40 blur-3xl" />
        <div className="absolute -right-16 -bottom-28 h-80 w-80 animate-float-slow rounded-full bg-amber-500/25 blur-3xl" />
      </div>
      <div className="relative flex flex-wrap items-center justify-between gap-6">
        <div>
          <p className="text-xs font-semibold tracking-[0.2em] text-amber-200/80 uppercase">{t('Clasamentul lunii')}</p>
          <h1 className="mt-1 text-3xl font-black tracking-tight capitalize sm:text-4xl">{monthName}</h1>
          <p className="mt-2 max-w-lg text-sm text-indigo-100/80">
            {t('Cei mai activi licitatori si cei mai buni vanzatori. Completeaza provocarile lunii si primesti insigne pe profil.')}
          </p>
        </div>
        <div className="flex items-center gap-5">
          <div className="text-right">
            <p className="text-xs text-white/60">{t('Clasamentul se reia in')}</p>
            <p className="text-2xl font-bold tabular-nums">
              {days}
              {t('z')} {hours}h
            </p>
          </div>
          <span className="leaderboard-trophy grid h-20 w-20 place-items-center rounded-3xl bg-gradient-to-br from-amber-300 to-orange-500 shadow-xl shadow-amber-500/40">
            <Icon name="trophy" size={44} mono className="text-amber-950" />
          </span>
        </div>
      </div>
    </section>
  )
}

// 2nd, 1st, 3rd: blocks grow one after another, the winner gets a crown
function Podium({ rows }) {
  const order = [1, 0, 2].filter((index) => rows[index])
  return (
    <div className="mt-8 grid grid-cols-3 items-end gap-3 sm:gap-5">
      {order.map((index) => {
        const row = rows[index]
        const medal = MEDALS[index]
        return (
          <Link key={row.username} to={`/users/${encodeURIComponent(row.username)}`} className="group flex flex-col items-center text-center">
            <div className="relative animate-fade-up" style={{ animationDelay: `${index === 0 ? 650 : index === 1 ? 350 : 500}ms` }}>
              {index === 0 && (
                <span className="podium-crown absolute -top-7 left-1/2 -translate-x-1/2">
                  <Icon name="trophy" size={28} />
                </span>
              )}
              <span className={`block rounded-full bg-gradient-to-br p-1 shadow-lg ${medal.ring} ${index === 0 ? 'shadow-amber-400/50' : ''}`}>
                <Avatar name={row.username} className={index === 0 ? 'h-20 w-20 text-2xl' : 'h-16 w-16 text-xl'} />
              </span>
            </div>
            <p className="mt-3 w-full truncate font-bold text-slate-900 group-hover:text-indigo-600">{row.username}</p>
            <p className="text-sm font-semibold text-indigo-600">{row.main}</p>
            <p className="hidden text-xs text-slate-500 sm:block">{row.sub}</p>
            <div
              className={`podium-rise stay-dark mt-3 w-full origin-bottom rounded-t-2xl bg-gradient-to-b shadow-inner ${medal.block} ${medal.height}`}
              style={{ animationDelay: `${index === 0 ? 300 : index === 1 ? 0 : 150}ms` }}
            >
              <span className={`block pt-3 text-4xl font-black ${medal.text}`}>{index + 1}</span>
            </div>
          </Link>
        )
      })}
    </div>
  )
}

function Avatar({ name, className = '' }) {
  return (
    <span className={`grid place-items-center rounded-full bg-gradient-to-br from-indigo-500 to-fuchsia-500 font-bold text-white ${className}`}>
      {name[0]?.toUpperCase()}
    </span>
  )
}

function Challenges({ user, mine }) {
  const t = useT()
  if (!user) {
    return (
      <Card className="self-start">
        <h2 className="flex items-center gap-2 text-lg font-bold text-slate-900">
          <Icon name="sparkles" size={22} /> {t('Provocarile lunii')}
        </h2>
        <p className="mt-2 text-sm text-slate-600">{t('Intra in cont ca sa vezi provocarile tale si cat mai ai pana la insigne.')}</p>
        <Link to="/login" state={{ from: '/clasament' }} className="mt-4 inline-block font-semibold text-indigo-600 hover:text-indigo-500">
          {t('Intra in cont')} →
        </Link>
      </Card>
    )
  }
  if (!mine) return <Loading />
  const all = mine.completed === mine.challenges.length
  return (
    <Card className="self-start">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="flex items-center gap-2 text-lg font-bold text-slate-900">
            <Icon name="sparkles" size={22} /> {t('Provocarile tale')}
          </h2>
          <p className="mt-0.5 text-sm text-slate-500">
            {mine.completed} {t('din')} {mine.challenges.length} {t('completate')}
            {mine.bidderRank && ` · ${t('locul')} #${mine.bidderRank} ${t('la licitatori')}`}
          </p>
        </div>
        <ProgressRing value={mine.completed} total={mine.challenges.length} size={52} />
      </div>
      {all && (
        <p className="mt-4 rounded-xl bg-amber-50 px-3 py-2 text-sm font-semibold text-amber-800 ring-1 ring-amber-200">
          <Icon name="trophy" size={16} /> {t('Ai castigat insigna „Campionul provocarilor”!')}
        </p>
      )}
      <ul className="mt-5 space-y-3">
        {mine.challenges.map((challenge, index) => {
          const done = challenge.progress >= challenge.target
          return (
            <li
              key={challenge.code}
              className={`flex animate-fade-up items-center gap-3 rounded-2xl p-3 ring-1 transition ${
                done ? 'bg-emerald-50 ring-emerald-200' : 'bg-white ring-slate-200'
              }`}
              style={{ animationDelay: `${index * 70}ms` }}
            >
              <ProgressRing value={challenge.progress} total={challenge.target} size={44} icon={challenge.icon} done={done} />
              <span className="min-w-0 flex-1">
                <span className="block font-semibold text-slate-900">{t(challenge.title)}</span>
                <span className="block text-xs text-slate-500">{t(challenge.description)}</span>
              </span>
              <span className={`text-sm font-bold tabular-nums ${done ? 'text-emerald-700' : 'text-slate-600'}`}>
                {challenge.progress}/{challenge.target}
              </span>
            </li>
          )
        })}
      </ul>
    </Card>
  )
}

// Circular progress; fills up with an animation when it appears
function ProgressRing({ value, total, size, icon, done = false }) {
  const radius = size / 2 - 4
  const circumference = 2 * Math.PI * radius
  const share = Math.min(1, value / total)
  return (
    <span className="relative grid shrink-0 place-items-center" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="var(--color-slate-200)" strokeWidth="4" />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={done ? 'var(--color-emerald-500)' : 'var(--color-indigo-500)'}
          strokeWidth="4"
          strokeLinecap="round"
          strokeDasharray={circumference}
          className="progress-ring"
          style={{ '--ring-from': circumference, '--ring-to': circumference * (1 - share), strokeDashoffset: circumference * (1 - share) }}
        />
      </svg>
      <span className="absolute inset-0 grid place-items-center">
        {icon ? <Icon name={done ? 'tagCheck' : icon} size={size * 0.45} /> : <span className="text-xs font-bold text-slate-700">{Math.round(share * 100)}%</span>}
      </span>
    </span>
  )
}
