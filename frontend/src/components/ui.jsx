// Componente mici de interfata, refolosite in toate paginile

const buttonStyles = {
  primary:
    'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/40 hover:brightness-110 active:scale-[0.98]',
  secondary: 'bg-white text-slate-700 ring-1 ring-slate-300 hover:bg-slate-50 active:scale-[0.98]',
  ghost: 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
}

export function Button({ variant = 'primary', className = '', loading, children, ...props }) {
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600
        disabled:cursor-not-allowed disabled:opacity-50 ${buttonStyles[variant]} ${className}`}
      disabled={loading || props.disabled}
      {...props}
    >
      {loading && <Spinner small />}
      {children}
    </button>
  )
}

export function Field({ label, hint, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-slate-700">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-slate-500">{hint}</span>}
    </label>
  )
}

const inputClass =
  'block w-full rounded-lg border-0 bg-white px-3.5 py-2.5 text-slate-900 ring-1 ring-slate-300 ring-inset ' +
  'placeholder:text-slate-400 focus:ring-2 focus:ring-indigo-600 focus:outline-none sm:text-sm'

export function Input(props) {
  return <input className={inputClass} {...props} />
}

export function Textarea(props) {
  return <textarea className={inputClass} rows={4} {...props} />
}

export function Select({ children, ...props }) {
  return (
    <select className={inputClass} {...props}>
      {children}
    </select>
  )
}

export function Alert({ children, tone = 'error' }) {
  if (!children) return null
  const tones = {
    error: 'bg-red-50 text-red-700 ring-red-200',
    success: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  }
  return <div className={`rounded-lg px-4 py-3 text-sm ring-1 ${tones[tone]}`}>{children}</div>
}

export function Badge({ children, tone = 'gray' }) {
  const tones = {
    gray: 'bg-slate-100 text-slate-700',
    green: 'bg-emerald-100 text-emerald-800',
    amber: 'bg-amber-100 text-amber-800',
    red: 'bg-red-100 text-red-700',
    indigo: 'bg-indigo-100 text-indigo-700',
  }
  return (
    <span
      className={`inline-flex shrink-0 items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap ${tones[tone]}`}
    >
      {children}
    </span>
  )
}

export function Spinner({ small }) {
  return (
    <span
      className={`inline-block animate-spin rounded-full border-2 border-current border-t-transparent ${
        small ? 'h-4 w-4' : 'h-8 w-8 text-indigo-600'
      }`}
      aria-label="Se incarca"
    />
  )
}

export function Loading() {
  return (
    <div className="flex justify-center py-20">
      <Spinner />
    </div>
  )
}

// Locul unui card cat timp se incarca lista (in loc de spinner)
export function CardSkeleton() {
  return (
    <div className="overflow-hidden rounded-2xl bg-white/85 ring-1 ring-slate-200/80">
      <div className="skeleton aspect-[4/3]" />
      <div className="space-y-3 p-5">
        <div className="skeleton h-3 w-20 rounded" />
        <div className="skeleton h-4 w-3/4 rounded" />
        <div className="skeleton h-3 w-full rounded" />
        <div className="skeleton mt-4 h-6 w-28 rounded" />
      </div>
    </div>
  )
}

export function EmptyState({ title, children }) {
  return (
    <div className="rounded-2xl border-2 border-dashed border-slate-200 px-6 py-14 text-center">
      <p className="font-semibold text-slate-700">{title}</p>
      {children && <div className="mt-2 text-sm text-slate-500">{children}</div>}
    </div>
  )
}

export function Card({ className = '', children }) {
  return (
    <div className={`rounded-2xl bg-white/85 p-6 shadow-sm ring-1 ring-slate-200/80 backdrop-blur ${className}`}>{children}</div>
  )
}
