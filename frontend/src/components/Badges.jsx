import { Icon } from './icons.jsx'

// Icon per reputation badge (codes from BadgeService.Badge)
const ICONS = {
  TRUSTED_SELLER: 'shield',
  ACTIVE_SELLER: 'tagCheck',
  COLLECTOR: 'trophy',
  PASSIONATE_BIDDER: 'gavel',
  FIRST_BIDDER: 'bolt',
  VETERAN: 'star',
  TOP_BIDDER: 'trophy',
  CHALLENGER: 'sparkles',
}

// Reputation badges; compact = small pills (item page), otherwise cards with the description (profile)
export default function Badges({ badges, compact = false }) {
  if (!badges?.length) return null
  if (compact) {
    return (
      <span className="flex flex-wrap gap-1.5">
        {badges.map((badge) => (
          <span
            key={badge.code}
            title={badge.description}
            className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2 py-0.5 text-[11px] font-semibold text-amber-800 ring-1 ring-amber-200"
          >
            <Icon name={ICONS[badge.code] ?? 'star'} size={13} /> {badge.label}
          </span>
        ))}
      </span>
    )
  }
  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {badges.map((badge) => (
        <div key={badge.code} className="flex items-center gap-3 rounded-2xl bg-white/85 p-4 shadow-sm ring-1 ring-amber-200/80">
          <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-amber-100 to-orange-100">
            <Icon name={ICONS[badge.code] ?? 'star'} size={26} />
          </span>
          <span>
            <span className="block font-semibold text-slate-900">{badge.label}</span>
            <span className="block text-xs text-slate-500">{badge.description}</span>
          </span>
        </div>
      ))}
    </div>
  )
}
