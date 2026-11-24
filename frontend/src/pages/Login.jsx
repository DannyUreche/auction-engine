import { useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { useAuth } from '../auth.jsx'
import { api } from '../api.js'
import AuthShell from '../components/AuthShell.jsx'
import GoogleButton from '../components/GoogleButton.jsx'
import { Alert, Button, Field, Input } from '../components/ui.jsx'
import { useT } from '../i18n.jsx'

export default function Login() {
  const t = useT()
  const { user, login } = useAuth()
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
      await login(form.username, form.password)
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
    }
  }

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

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
