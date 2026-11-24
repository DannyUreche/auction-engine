import { useState } from 'react'
import { Icon } from './icons.jsx'

// Butonul soare / luna: comuta intre modul luminos si cel intunecat si tine minte alegerea.
// Clasa .dark pe <html> activeaza culorile din dark.css (vezi si scriptul din index.html).
export default function ThemeToggle() {
  const [dark, setDark] = useState(() => document.documentElement.classList.contains('dark'))

  function toggle() {
    const next = !dark
    document.documentElement.classList.toggle('dark', next)
    localStorage.setItem('theme', next ? 'dark' : 'light')
    setDark(next)
  }

  return (
    <button
      type="button"
      onClick={toggle}
      aria-label={dark ? 'Treci pe modul luminos' : 'Treci pe modul intunecat'}
      title={dark ? 'Mod luminos' : 'Mod intunecat'}
      className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-white/80 text-slate-600 ring-1 ring-slate-200 transition hover:text-indigo-600 hover:ring-indigo-300"
    >
      <span key={String(dark)} className="grid animate-fade-in place-items-center">
        <Icon name={dark ? 'sun' : 'moon'} size={19} />
      </span>
    </button>
  )
}
