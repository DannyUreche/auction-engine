import { Link, NavLink, Outlet, useLocation } from 'react-router'
import { useAuth } from '../auth.jsx'

function NavItem({ to, children }) {
  return (
    <NavLink
      to={to}
      end
      className={({ isActive }) =>
        `rounded-lg px-3 py-2 text-sm font-medium transition ${
          isActive ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
        }`
      }
    >
      {children}
    </NavLink>
  )
}

// Bara de sus + continutul paginii curente
export default function Layout() {
  const { user, loading, logout } = useAuth()
  const location = useLocation()

  function handleLogout() {
    logout()
    // Reincarcare completa pe pagina principala: altfel o pagina protejata (ex: Contul meu)
    // apuca sa redirectioneze la login inainte de navigare, iar starea aplicatiei e oricum golita
    window.location.assign('/')
  }

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-10 border-b border-slate-200 bg-white/80 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-4 sm:px-6">
          <Link to="/" className="flex items-center gap-2 text-lg font-bold text-slate-900">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-indigo-600 text-white">🔨</span>
            Licitatii
          </Link>

          <nav className="hidden items-center gap-1 sm:flex">
            <NavItem to="/">Licitatii active</NavItem>
            <NavItem to="/sell">Vinde un obiect</NavItem>
            <NavItem to="/account">Contul meu</NavItem>
          </nav>

          <div className="ml-auto flex items-center gap-3">
            {user ? (
              <>
                <span className="hidden text-sm text-slate-600 md:inline">
                  Salut, <span className="font-semibold text-slate-900">{user.username}</span>
                </span>
                <button
                  onClick={handleLogout}
                  className="rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                >
                  Iesire
                </button>
              </>
            ) : (
              !loading && (
                <>
                  {/* Dupa login, vizitatorul revine pe pagina pe care era */}
                  <Link
                    to="/login"
                    state={{ from: location.pathname }}
                    className="rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                  >
                    Intra in cont
                  </Link>
                  <Link
                    to="/register"
                    className="rounded-lg bg-indigo-600 px-3.5 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500"
                  >
                    Creeaza cont
                  </Link>
                </>
              )
            )}
          </div>
        </div>

        {/* Navigare pe telefon */}
        <nav className="flex gap-1 overflow-x-auto border-t border-slate-100 px-4 py-2 sm:hidden">
          <NavItem to="/">Active</NavItem>
          <NavItem to="/sell">Vinde</NavItem>
          <NavItem to="/account">Contul meu</NavItem>
        </nav>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">
        <Outlet />
      </main>

      <footer className="border-t border-slate-200 py-6 text-center text-xs text-slate-400">
        Auction Engine · Spring Boot + React
      </footer>
    </div>
  )
}
