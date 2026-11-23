import { Link } from 'react-router'

// Cadrul paginilor de login si inregistrare: formular in stanga, prezentare in dreapta
export default function AuthShell({ title, subtitle, children }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm">
          <Link to="/" className="mb-8 flex w-fit items-center gap-2 text-lg font-bold">
            <span className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-600 text-white">🔨</span>
            Licitatii
          </Link>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">{title}</h1>
          <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
          <div className="mt-8">{children}</div>
        </div>
      </div>

      <div className="relative hidden overflow-hidden bg-gradient-to-br from-indigo-600 via-indigo-700 to-violet-800 lg:block">
        <div className="absolute -top-24 -right-24 h-96 w-96 rounded-full bg-white/10 blur-3xl" />
        <div className="absolute -bottom-32 -left-16 h-96 w-96 rounded-full bg-violet-400/20 blur-3xl" />
        <div className="relative flex h-full flex-col justify-center px-16 text-white">
          <p className="text-4xl leading-tight font-bold">
            Vinde si cumpara
            <br />
            prin licitatii in timp real.
          </p>
          <ul className="mt-10 space-y-4 text-indigo-100">
            <li className="flex items-center gap-3">
              <span className="grid h-8 w-8 place-items-center rounded-lg bg-white/15">⏱</span>
              Licitatii cu durata limitata si cronometru
            </li>
            <li className="flex items-center gap-3">
              <span className="grid h-8 w-8 place-items-center rounded-lg bg-white/15">📈</span>
              Istoricul complet al ofertelor
            </li>
            <li className="flex items-center gap-3">
              <span className="grid h-8 w-8 place-items-center rounded-lg bg-white/15">🏆</span>
              Castigatorul este stabilit automat la final
            </li>
          </ul>
        </div>
      </div>
    </div>
  )
}
