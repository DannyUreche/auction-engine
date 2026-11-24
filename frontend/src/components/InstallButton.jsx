import { useInstallPrompt } from '../pwa.js'
import { Icon } from './icons.jsx'
import { useT } from '../i18n.jsx'

// "Install app" button; shown only when the browser offers installation (Chrome, Edge, Android)
export default function InstallButton() {
  const t = useT()
  const [canInstall, install] = useInstallPrompt()
  if (!canInstall) return null
  return (
    <button
      type="button"
      onClick={install}
      title={t('Instaleaza aplicatia')}
      aria-label={t('Instaleaza aplicatia')}
      className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-white/80 text-slate-600 ring-1 ring-slate-200 transition hover:text-indigo-600 hover:ring-indigo-300"
    >
      <Icon name="download" size={19} />
    </button>
  )
}
