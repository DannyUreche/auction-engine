import { Link } from 'react-router'
import Backdrop from './Backdrop.jsx'
import { Icon } from './icons.jsx'
import ThemeToggle from './ThemeToggle.jsx'
import { Logo } from './Layout.jsx'
import { useT } from '../i18n.jsx'

// Cadrul paginilor de login si inregistrare: formular in stanga, prezentare in dreapta
export default function AuthShell({ title, subtitle, children }) {
  const t = useT()
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative flex items-center justify-center px-6 py-12">
        <Backdrop />
        <div className="absolute top-5 right-5">
          <ThemeToggle />
        </div>
        <div className="w-full max-w-sm">
          <Link to="/" className="mb-8 flex w-fit items-center gap-2 text-lg font-bold">
            <Logo />
          </Link>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">{title}</h1>
          <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
          <div className="mt-8">{children}</div>
        </div>
      </div>

      <div className="stay-dark relative hidden overflow-hidden bg-slate-950 lg:block">
        {/* Fundal animat */}
        <div aria-hidden="true" className="pointer-events-none absolute inset-0">
          <div className="absolute -top-32 -right-24 h-[30rem] w-[30rem] animate-float rounded-full bg-indigo-600/40 blur-3xl" />
          <div className="absolute -bottom-40 -left-20 h-[28rem] w-[28rem] animate-float-slow rounded-full bg-fuchsia-600/30 blur-3xl" />
          <div className="absolute inset-0 bg-[linear-gradient(to_right,rgb(255_255_255/0.04)_1px,transparent_1px),linear-gradient(to_bottom,rgb(255_255_255/0.04)_1px,transparent_1px)] bg-[size:40px_40px]" />
        </div>

        <div className="relative flex h-full flex-col justify-center px-16 text-white">
          <p className="text-4xl leading-tight font-bold tracking-tight">
            {t('Vinde si cumpara')}
            <br />
            {t('prin licitatii')} <span className="text-gradient">{t('in timp real')}</span>.
          </p>

          {/* Exemple de activitate care plutesc (doar decor) */}
          <div aria-hidden="true" className="relative mt-12 h-56">
            <DemoBid className="top-0 left-0 animate-float" user="ana" amount="1.250 RON" item="Ceas Omega vintage" />
            <DemoBid className="top-20 left-40 animate-float-slow" user="mihai" amount="380 RON" item="Casti wireless" highlight />
            <DemoBid className="top-40 left-8 animate-float [animation-delay:-7s]" user="ioana" amount="95 RON" item="Set carti rare" />
          </div>

          <ul className="mt-4 space-y-3 text-sm text-indigo-100/80">
            <li className="flex items-center gap-2.5">
              <Icon name="timer" size={20} /> Cronometru live si prelungire automata in ultimele minute
            </li>
            <li className="flex items-center gap-2.5">
              <Icon name="chart" size={20} /> Grafic cu evolutia pretului si istoricul ofertelor
            </li>
            <li className="flex items-center gap-2.5">
              <Icon name="trophy" size={20} /> Castigatorul este stabilit automat la final
            </li>
          </ul>
        </div>
      </div>
    </div>
  )
}

function DemoBid({ className, user, amount, item, highlight }) {
  return (
    <div
      className={`absolute flex w-64 items-center gap-3 rounded-2xl px-4 py-3 ring-1 backdrop-blur ${
        highlight ? 'bg-white/15 ring-white/25' : 'bg-white/[0.07] ring-white/10'
      } ${className}`}
    >
      <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-gradient-to-br from-indigo-400 to-fuchsia-500 text-sm font-bold">
        {user[0].toUpperCase()}
      </span>
      <span className="min-w-0 text-sm">
        <span className="block truncate">
          <b>{user}</b> a oferit <b className="text-emerald-300">{amount}</b>
        </span>
        <span className="block truncate text-xs text-indigo-200/70">{item}</span>
      </span>
    </div>
  )
}
