import { useRef, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { useAuth } from '../auth.jsx'
import { api } from '../api.js'
import AuthShell from '../components/AuthShell.jsx'
import GoogleButton from '../components/GoogleButton.jsx'
import { Icon } from '../components/icons.jsx'
import { Alert, Button, Field, Input } from '../components/ui.jsx'
import { useT } from '../i18n.jsx'

export default function Login() {
  const t = useT()
  const { user, login, loginTwoFactor } = useAuth()
  const [ticket, setTicket] = useState(null)
  const location = useLocation()
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // Dupa login (sau daca e deja logat) il trimitem pe pagina pe care voia sa o vada
  if (user) return <Navigate to={location.state?.from || '/'} replace />

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const result = await login(form.username, form.password)
      if (result.twoFactorTicket) {
        setTicket(result.twoFactorTicket)
        setSubmitting(false)
      }
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
    }
  }

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  if (ticket) {
    return (
      <AuthShell title={t('Verificare in doi pasi')} subtitle={t('Deschide aplicatia de autentificare si scrie codul de 6 cifre.')}>
        <TwoFactorStep
          onSubmit={(code) => loginTwoFactor(ticket, code)}
          onBack={() => {
            setTicket(null)
            setForm({ ...form, password: '' })
          }}
        />
      </AuthShell>
    )
  }

  return (
    <AuthShell title={t('Bine ai revenit')} subtitle={t('Intra in cont ca sa licitezi si sa vinzi obiecte.')}>
      <form onSubmit={handleSubmit} className="space-y-5">
        <Alert>{error}</Alert>
        <Field label="Username">
          <Input value={form.username} onChange={update('username')} autoComplete="username" autoFocus required />
        </Field>
        <Field label={t('Parola')}>
          <Input
            type="password"
            value={form.password}
            onChange={update('password')}
            autoComplete="current-password"
            required
          />
        </Field>
        <div className="-mt-2 text-right">
          <Link to="/forgot-password" className="text-sm font-medium text-indigo-600 hover:text-indigo-500">
            {t('Ai uitat parola?')}
          </Link>
        </div>
        <Button type="submit" loading={submitting} className="w-full">
          {t('Intra in cont')}
        </Button>
      </form>
      {error.startsWith('Confirma adresa de email') && <ResendVerification />}
      <GoogleButton />
      <p className="mt-6 text-center text-sm text-slate-500">
        {t('Nu ai cont?')}{' '}
        <Link to="/register" className="font-semibold text-indigo-600 hover:text-indigo-500">
          {t('Creeaza unul')}
        </Link>
      </p>
    </AuthShell>
  )
}

// "Did not get the email?": sends a new confirmation link
function ResendVerification() {
  const t = useT()
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  async function send(event) {
    event.preventDefault()
    const data = await api('/auth/resend-verification', { method: 'POST', body: { email }, auth: false }).catch((err) => ({ mesaj: err.message }))
    setMessage(data.mesaj)
  }
  if (message) {
    return (
      <div className="mt-4">
        <Alert tone="success">{message}</Alert>
      </div>
    )
  }
  return (
    <form onSubmit={send} className="mt-4 flex gap-2">
      <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder={t('Emailul contului')} required />
      <Button type="submit" variant="secondary" className="shrink-0">
        {t('Retrimite link-ul')}
      </Button>
    </form>
  )
}

// Second login step: six separate digit boxes (paste works too); "use a backup code" switches to a text field
function TwoFactorStep({ onSubmit, onBack }) {
  const t = useT()
  const [digits, setDigits] = useState(['', '', '', '', '', ''])
  const [backup, setBackup] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const boxes = useRef([])

  async function submit(code) {
    setBusy(true)
    setError('')
    try {
      await onSubmit(code)
    } catch (err) {
      setError(err.message)
      setDigits(['', '', '', '', '', ''])
      boxes.current[0]?.focus()
      setBusy(false)
    }
  }

  function type(index, value) {
    const clean = value.replace(/\D/g, '')
    if (clean.length > 1) {
      // Pasted the whole code
      const next = clean.slice(0, 6).split('')
      while (next.length < 6) next.push('')
      setDigits(next)
      if (clean.length >= 6) submit(clean.slice(0, 6))
      return
    }
    const next = [...digits]
    next[index] = clean
    setDigits(next)
    if (clean && index < 5) boxes.current[index + 1]?.focus()
    if (next.every(Boolean)) submit(next.join(''))
  }

  if (backup !== null) {
    return (
      <form
        onSubmit={(event) => {
          event.preventDefault()
          submit(backup)
        }}
        className="space-y-4"
      >
        <Alert>{error}</Alert>
        <Field label={t('Cod de rezerva')}>
          <Input value={backup} onChange={(e) => setBackup(e.target.value)} placeholder="abcde-fghjk" autoFocus required />
        </Field>
        <Button type="submit" loading={busy} className="w-full">
          {t('Intra in cont')}
        </Button>
        <button type="button" onClick={() => setBackup(null)} className="w-full text-sm font-semibold text-indigo-600 hover:text-indigo-500">
          {t('Foloseste codul din aplicatie')}
        </button>
      </form>
    )
  }

  return (
    <div className="space-y-5">
      <div className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-gradient-to-br from-indigo-500 to-violet-600 shadow-lg shadow-indigo-500/30">
        <Icon name="shield" size={34} mono className="text-white" />
      </div>
      <Alert>{error}</Alert>
      <div className="flex justify-center gap-2">
        {digits.map((digit, index) => (
          <input
            key={index}
            ref={(el) => {
              boxes.current[index] = el
            }}
            value={digit}
            onChange={(e) => type(index, e.target.value)}
            onKeyDown={(e) => e.key === 'Backspace' && !digit && index > 0 && boxes.current[index - 1]?.focus()}
            inputMode="numeric"
            autoComplete={index === 0 ? 'one-time-code' : 'off'}
            autoFocus={index === 0}
            disabled={busy}
            aria-label={`${t('Cifra')} ${index + 1}`}
            className={`h-14 w-12 rounded-xl border-0 bg-white text-center text-2xl font-bold text-slate-900 shadow-sm ring-1 transition ring-inset focus:ring-2 focus:ring-indigo-600 focus:outline-none ${
              digit ? 'ring-indigo-400' : 'ring-slate-300'
            } ${index === 2 ? 'mr-3' : ''}`}
          />
        ))}
      </div>
      <div className="flex items-center justify-between text-sm">
        <button type="button" onClick={onBack} className="font-medium text-slate-500 hover:text-slate-700">
          ← {t('Inapoi')}
        </button>
        <button type="button" onClick={() => setBackup('')} className="font-semibold text-indigo-600 hover:text-indigo-500">
          {t('Foloseste un cod de rezerva')}
        </button>
      </div>
    </div>
  )
}
