import { Link, NavLink, Outlet, useLocation } from 'react-router'
import { useAuth } from '../auth.jsx'
import { useAuctionEvents } from '../events.jsx'
import { formatPrice } from '../api.js'
import { useWallet } from '../wallet.js'
import { Icon } from './icons.jsx'
import Backdrop from './Backdrop.jsx'
import InstallButton from './InstallButton.jsx'
import NotificationBell from './NotificationBell.jsx'
import ThemeToggle from './ThemeToggle.jsx'
import { LanguageToggle, useT } from '../i18n.jsx'

function NavItem({ to, children }) {
  return (
    <NavLink
      to={to}
      end
      className={({ isActive }) =>
        `relative rounded-lg px-3 py-2 text-sm font-medium whitespace-nowrap transition ${
          isActive
            ? 'bg-white text-indigo-700 shadow-sm ring-1 ring-slate-200'
            : 'text-slate-600 hover:bg-white/70 hover:text-slate-900'
        }`
      }
    >
      {children}
    </NavLink>
  )
}

// compact: pe telefon se vede doar iconita (textul apare de la ecrane mai late)
export function Logo({ dark = false, compact = false }) {
  return (
    <span className={`flex items-center gap-2.5 text-lg font-bold tracking-tight ${dark ? 'text-white' : 'text-slate-900'}`}>
      <span className="grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 via-violet-500 to-fuchsia-500 text-white shadow-lg shadow-indigo-500/30">
        <Icon name="gavel" size={20} mono />
      </span>
      <span className={`whitespace-nowrap ${compact ? 'hidden sm:inline' : ''}`}>
        Licitatii<span className="text-indigo-500">.live</span>
      </span>
    </span>
  )
}

// Bara de sus + continutul paginii curente, peste fundalul animat
export default function Layout() {
  const { user, loading, logout } = useAuth()
  const location = useLocation()
  // Conexiunea live e una singura pe pagina (vezi events.jsx); aici doar ii afisam starea
  const live = useAuctionEvents(() => {})
  const t = useT()

  function handleLogout() {
    logout()
    // Reincarcare completa pe pagina principala: altfel o pagina protejata (ex: Contul meu)
    // apuca sa redirectioneze la login inainte de navigare, iar starea aplicatiei e oricum golita
    window.location.assign('/')
  }

  return (
    <div className="flex min-h-screen flex-col">
      <Backdrop />

      <header className="sticky top-0 z-20 border-b border-white/60 bg-white/60 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-3 px-4 sm:gap-6 sm:px-6">
          <Link to="/" aria-label="Pagina principala">
            <Logo compact />
          </Link>

          <nav className="hidden items-center gap-1 sm:flex">
            <NavItem to="/">{t('Licitatii active')}</NavItem>
            <NavItem to="/sell">{t('Vinde un obiect')}</NavItem>
            <NavItem to="/account">{t('Contul meu')}</NavItem>
            <NavItem to="/despre">{t('Despre')}</NavItem>
            {user?.admin && <NavItem to="/admin">Admin</NavItem>}
          </nav>

          <div className="ml-auto flex items-center gap-2 sm:gap-3">
            <LanguageToggle />
            <span className="hidden sm:contents">
              <InstallButton />
            </span>
            <ThemeToggle />
            <NotificationBell />
            <span
              title={t(live ? 'Conectat: preturile se actualizeaza instant' : 'Se reconecteaza...')}
              className="hidden items-center gap-1.5 text-xs font-medium text-slate-500 2xl:flex"
            >
              <span className={`h-2 w-2 rounded-full ${live ? 'animate-pulse bg-emerald-500' : 'bg-slate-300'}`} />
              {live ? 'Live' : t('Offline')}
            </span>

            {user ? (
              <>
                <WalletChip user={user} />
                <Link
                  to={`/users/${encodeURIComponent(user.username)}`}
                  className="hidden items-center gap-2 rounded-full bg-white/80 p-1 text-sm ring-1 ring-slate-200 transition hover:ring-indigo-300 md:flex 2xl:pr-3"
                  title={`${t('Profilul tau public')}: ${user.username}`}
                  aria-label={`${t('Profilul tau public')}: ${user.username}`}
                >
                  <span className="grid h-7 w-7 place-items-center rounded-full bg-gradient-to-br from-indigo-500 to-fuchsia-500 text-xs font-bold text-white">
                    {user.username[0]?.toUpperCase()}
                  </span>
                  <span className="hidden max-w-32 truncate font-semibold text-slate-800 2xl:inline">{user.username}</span>
                </Link>
                <button
                  onClick={handleLogout}
                  className="rounded-lg px-3 py-2 text-sm font-medium whitespace-nowrap text-slate-600 hover:bg-white/70 hover:text-slate-900"
                >
                  {t('Iesire')}
                </button>
              </>
            ) : (
              !loading && (
                <>
                  {/* Dupa login, vizitatorul revine pe pagina pe care era */}
                  <Link
                    to="/login"
                    state={{ from: location.pathname }}
                    className="rounded-lg px-2 py-2 text-sm font-medium whitespace-nowrap text-slate-600 hover:bg-white/70 hover:text-slate-900 sm:px-3"
                  >
                    {t('Intra in cont')}
                  </Link>
                  <Link
                    to="/register"
                    className="rounded-lg bg-gradient-to-r from-indigo-600 to-violet-600 px-3 py-2 text-sm whitespace-nowrap font-semibold text-white shadow-md shadow-indigo-500/25 transition hover:shadow-lg hover:shadow-indigo-500/40"
                  >
                    {t('Creeaza cont')}
                  </Link>
                </>
              )
            )}
          </div>
        </div>

        {/* Navigare pe telefon */}
        <nav className="flex gap-1 overflow-x-auto border-t border-white/60 px-4 py-2 sm:hidden">
          <NavItem to="/">{t('Active')}</NavItem>
          <NavItem to="/sell">{t('Vinde')}</NavItem>
          <NavItem to="/account">{t('Contul meu')}</NavItem>
          <NavItem to="/despre">{t('Despre')}</NavItem>
          {user?.admin && <NavItem to="/admin">Admin</NavItem>}
          <span className="ml-auto">
            <InstallButton />
          </span>
        </nav>
      </header>

      {/* key: la schimbarea paginii, continutul apare cu o animatie scurta */}
      <main key={location.pathname} className="mx-auto w-full max-w-6xl flex-1 animate-fade-in px-4 py-8 sm:px-6">
        <Outlet />
      </main>

      <footer className="border-t border-slate-200/70 bg-white/50 backdrop-blur">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-8 sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <div>
            <Logo />
            <p className="mt-2 text-xs text-slate-500">
              {t('Licitatii in timp real · oferte instant · castigator stabilit automat ·')}{' '}
              <Link to="/despre" className="font-medium text-indigo-600 hover:underline">
                {t('Despre proiect')}
              </Link>
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {['Spring Boot 4', 'React 19', 'PostgreSQL', 'Server-Sent Events', 'JWT'].map((tech) => (
              <span key={tech} className="rounded-full bg-white px-2.5 py-1 text-[11px] font-medium text-slate-500 ring-1 ring-slate-200">
                {tech}
              </span>
            ))}
          </div>
        </div>
      </footer>
    </div>
  )
}

// Money available for bids, in the header (opens the wallet tab of the account page)
function WalletChip({ user }) {
  const { wallet } = useWallet(user)
  const t = useT()
  if (!wallet) return null
  return (
    <Link
      to="/account?tab=wallet"
      title={t('Portofelul tau (disponibil pentru oferte)')}
      className="hidden items-center gap-1.5 rounded-full bg-white/80 py-1 pr-3 pl-1.5 text-sm font-semibold text-slate-800 ring-1 ring-slate-200 transition hover:ring-indigo-300 lg:flex"
    >
      <Icon name="wallet" size={20} />
      {formatPrice(wallet.available)}
    </Link>
  )
}
