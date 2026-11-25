import { useEffect, useState } from 'react'
import QRCode from 'qrcode'
import { api } from '../api.js'
import { Icon } from './icons.jsx'
import { Alert, Button, Card, Input } from './ui.jsx'

// "Autentificare in doi pasi" in the account settings: status, a 3-step setup (scan the QR code, type a code,
// save the backup codes) and turning it off (password + code)
export default function TwoFactorCard() {
  const [status, setStatus] = useState(null)
  const [setup, setSetup] = useState(null) // { secret, qr }
  const [backupCodes, setBackupCodes] = useState(null)
  const [code, setCode] = useState('')
  const [disabling, setDisabling] = useState(false)
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = () => api('/users/me/2fa').then(setStatus).catch(() => {})
  useEffect(() => {
    load()
  }, [])

  async function run(action) {
    setBusy(true)
    setError('')
    try {
      await action()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const start = () =>
    run(async () => {
      const data = await api('/users/me/2fa/setup', { method: 'POST' })
      const qr = await QRCode.toDataURL(data.otpauthUri, { width: 220, margin: 1, color: { dark: '#1e1b4b', light: '#ffffff' } })
      setSetup({ secret: data.secret, qr })
      setCode('')
    })

  const confirm = (event) => {
    event.preventDefault()
    run(async () => {
      const data = await api('/users/me/2fa/enable', { method: 'POST', body: { code } })
      setBackupCodes(data.backupCodes)
      setSetup(null)
      await load()
    })
  }

  const disable = (event) => {
    event.preventDefault()
    run(async () => {
      await api('/users/me/2fa/disable', { method: 'POST', body: { password, code } })
      setDisabling(false)
      setPassword('')
      setCode('')
      await load()
    })
  }

  if (!status) return null

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-900">
            <Icon name="shield" size={22} /> Autentificare in doi pasi
          </h2>
          <p className="mt-1 max-w-xl text-sm text-slate-600">
            Pe langa parola, la intrarea in cont iti cerem un cod de 6 cifre din aplicatia de pe telefon (Google Authenticator,
            Microsoft Authenticator, Authy). Chiar daca cineva iti afla parola, nu poate intra fara telefonul tau.
          </p>
        </div>
        <span
          className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-semibold ring-1 ${
            status.enabled ? 'bg-emerald-50 text-emerald-800 ring-emerald-200' : 'bg-slate-100 text-slate-600 ring-slate-200'
          }`}
        >
          <span className={`h-2 w-2 rounded-full ${status.enabled ? 'bg-emerald-500' : 'bg-slate-400'}`} />
          {status.enabled ? 'Activa' : 'Inactiva'}
        </span>
      </div>

      <div className="mt-4">
        <Alert>{error}</Alert>
      </div>

      {backupCodes && (
        <div className="mt-4 animate-fade-up rounded-2xl bg-violet-50 p-5 ring-1 ring-violet-200">
          <p className="font-semibold text-violet-900">Gata! Salveaza codurile de rezerva</p>
          <p className="mt-1 text-sm text-violet-800">
            Daca pierzi telefonul, poti intra cu unul dintre ele (fiecare merge o singura data). Nu le mai afisam.
          </p>
          <div className="mt-3 grid grid-cols-2 gap-2 font-mono text-sm sm:grid-cols-4">
            {backupCodes.map((backup) => (
              <span key={backup} className="rounded-lg bg-white px-3 py-2 text-center font-semibold text-slate-800 ring-1 ring-violet-200">
                {backup}
              </span>
            ))}
          </div>
          <div className="mt-3 flex gap-2">
            <Button variant="secondary" onClick={() => navigator.clipboard?.writeText(backupCodes.join('\n'))}>
              Copiaza codurile
            </Button>
            <Button onClick={() => setBackupCodes(null)}>Le-am salvat</Button>
          </div>
        </div>
      )}

      {!status.enabled && !setup && !backupCodes && (
        <Button onClick={start} loading={busy} className="mt-2">
          Activeaza
        </Button>
      )}

      {setup && (
        <ol className="mt-4 grid animate-fade-up gap-5 md:grid-cols-[auto_1fr]">
          <li className="flex flex-col items-center gap-2">
            <span className="rounded-2xl bg-white p-3 shadow-lg ring-1 ring-slate-200">
              <img src={setup.qr} alt="Cod QR pentru aplicatia de autentificare" className="h-44 w-44" />
            </span>
            <span className="text-xs text-slate-500">sau scrie manual:</span>
            <code className="max-w-56 rounded-lg bg-slate-100 px-2 py-1 text-center text-xs break-all text-slate-700">{setup.secret}</code>
          </li>
          <li>
            <Steps />
            <form onSubmit={confirm} className="mt-4 flex max-w-sm gap-2">
              <Input
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                inputMode="numeric"
                autoComplete="one-time-code"
                placeholder="Codul de 6 cifre"
                className="text-center !text-lg font-bold tracking-[0.3em] placeholder:text-sm placeholder:font-normal placeholder:tracking-normal"
                required
              />
              <Button type="submit" loading={busy} disabled={code.length !== 6} className="shrink-0">
                Confirma
              </Button>
            </form>
            <button onClick={() => setSetup(null)} className="mt-2 text-sm text-slate-500 hover:text-slate-700">
              Renunta
            </button>
          </li>
        </ol>
      )}

      {status.enabled && !backupCodes && (
        <div className="mt-2">
          <p className="text-sm text-slate-600">
            Mai ai <b>{status.backupCodesLeft}</b> coduri de rezerva nefolosite.
          </p>
          {disabling ? (
            <form onSubmit={disable} className="mt-3 grid max-w-md gap-2 sm:grid-cols-[1fr_1fr_auto]">
              <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Parola" autoComplete="current-password" required />
              <Input value={code} onChange={(e) => setCode(e.target.value)} placeholder="Cod sau cod de rezerva" required />
              <Button type="submit" variant="danger" loading={busy}>
                Dezactiveaza
              </Button>
            </form>
          ) : (
            <Button variant="secondary" className="mt-3" onClick={() => setDisabling(true)}>
              Dezactiveaza
            </Button>
          )}
        </div>
      )}
    </Card>
  )
}

function Steps() {
  const steps = [
    'Instaleaza o aplicatie de autentificare pe telefon (Google Authenticator, Microsoft Authenticator sau Authy).',
    'In aplicatie, adauga un cont nou si scaneaza codul QR.',
    'Scrie mai jos codul de 6 cifre afisat de aplicatie.',
  ]
  return (
    <ul className="space-y-3">
      {steps.map((text, index) => (
        <li key={text} className="flex gap-3 text-sm text-slate-700">
          <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-indigo-600 text-xs font-bold text-white">{index + 1}</span>
          <span className="pt-1">{text}</span>
        </li>
      ))}
    </ul>
  )
}
