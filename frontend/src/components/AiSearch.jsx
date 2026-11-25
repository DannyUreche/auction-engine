import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { useT } from '../i18n.jsx'
import { Icon } from './icons.jsx'

const EXAMPLES = ['un ceas vechi sub 500 lei', 'telefon cu Cumpara acum, cele mai ieftine', 'bicicleta in Cluj pana in 1500 lei', 'tablouri care se termina azi']

// "Cauta cu AI": the visitor describes what they want in plain words, Claude (POST /api/ai/search) picks the
// filters and the home page applies them. Hidden when the server has no API key (GET /api/ai/config);
// visitors without an account see an invitation to log in, because each search costs money.
export default function AiSearch({ onApply }) {
  const { user } = useAuth()
  const t = useT()
  const [enabled, setEnabled] = useState(false)
  const [open, setOpen] = useState(false)
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [answer, setAnswer] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/ai/config', { auth: false })
      .then((config) => setEnabled(config.enabled))
      .catch(() => setEnabled(false))
  }, [])

  if (!enabled) return null

  async function search(query = text) {
    if (query.trim().length < 3) return
    setText(query)
    setBusy(true)
    setError('')
    try {
      const filters = await api('/ai/search', { method: 'POST', body: { query } })
      setAnswer(filters)
      onApply(filters)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="group mt-3 flex w-full items-center gap-3 rounded-2xl bg-gradient-to-r from-violet-50 via-fuchsia-50 to-indigo-50 px-4 py-2.5 text-left text-sm ring-1 ring-violet-200 transition hover:ring-violet-400"
      >
        <span className="ai-orb grid h-8 w-8 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-violet-500 to-fuchsia-500 text-white shadow-md shadow-violet-500/30">
          <Icon name="sparkles" size={18} mono />
        </span>
        <span className="min-w-0 flex-1">
          <b className="text-violet-800">{t('Cauta cu AI')}</b>
          <span className="block truncate text-slate-500">{t('Spune in cuvintele tale ce cauti, iar asistentul alege filtrele.')}</span>
        </span>
        <span className="text-violet-600 transition group-hover:translate-x-1">→</span>
      </button>
    )
  }

  return (
    <div className="ai-border mt-3 rounded-2xl p-[2px]">
      <div className="rounded-[14px] bg-white p-4">
        <div className="flex items-center justify-between gap-3">
          <p className="flex items-center gap-2 text-sm font-bold text-violet-800">
            <Icon name="sparkles" size={18} /> {t('Cauta cu AI')}
          </p>
          <button type="button" onClick={() => setOpen(false)} className="text-xs font-semibold text-slate-500 hover:text-slate-700">
            {t('Inchide')}
          </button>
        </div>

        {user ? (
          <>
            <form
              onSubmit={(e) => {
                e.preventDefault()
                search()
              }}
              className="mt-3 flex flex-col gap-2 sm:flex-row"
            >
              <input
                value={text}
                onChange={(e) => setText(e.target.value)}
                maxLength={200}
                autoFocus
                placeholder={t('ex: un ceas vechi sub 500 lei in Cluj')}
                className="block w-full rounded-xl border-0 bg-white px-4 py-3 text-slate-900 shadow-sm ring-1 ring-slate-300 placeholder:text-slate-400 focus:ring-2 focus:ring-violet-500 focus:outline-none"
              />
              <button
                type="submit"
                disabled={busy || text.trim().length < 3}
                className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-3 text-sm font-semibold text-white shadow-md shadow-violet-500/25 transition hover:brightness-110 disabled:opacity-50"
              >
                {busy ? (
                  <>
                    <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white" /> {t('Ma gandesc...')}
                  </>
                ) : (
                  <>
                    <Icon name="search" size={16} mono /> {t('Cauta')}
                  </>
                )}
              </button>
            </form>

            {!answer && !busy && (
              <div className="mt-3 flex flex-wrap gap-2">
                {EXAMPLES.map((example) => (
                  <button
                    key={example}
                    type="button"
                    onClick={() => search(example)}
                    className="rounded-full bg-violet-50 px-3 py-1 text-xs font-medium text-violet-700 ring-1 ring-violet-200 transition hover:bg-violet-100"
                  >
                    „{example}”
                  </button>
                ))}
              </div>
            )}

            {error && <p className="mt-3 rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700 ring-1 ring-rose-200">{error}</p>}
            {answer && !error && (
              <p className="animate-fade-up mt-3 flex items-start gap-2 rounded-xl bg-violet-50 px-3 py-2 text-sm text-violet-900 ring-1 ring-violet-200">
                <Icon name="robot" size={18} />
                <span>
                  {answer.explanation || t('Am aplicat filtrele.')}{' '}
                  <span className="text-violet-600">{t('Poti ajusta filtrele de mai jos.')}</span>
                </span>
              </p>
            )}
          </>
        ) : (
          <p className="mt-3 text-sm text-slate-600">
            <Link to="/login" state={{ from: '/' }} className="font-semibold text-violet-700 hover:underline">
              {t('Intra in cont')}
            </Link>{' '}
            {t('ca sa folosesti cautarea cu AI.')}
          </p>
        )}
      </div>
    </div>
  )
}
