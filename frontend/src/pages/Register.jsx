import { useState } from 'react'
import { Link, Navigate } from 'react-router'
import { useAuth } from '../auth.jsx'
import AuthShell from '../components/AuthShell.jsx'
import GoogleButton from '../components/GoogleButton.jsx'
import { Alert, Button, Field, Input } from '../components/ui.jsx'
import { useT } from '../i18n.jsx'

export default function Register() {
  const t = useT()
  const { user, register } = useAuth()
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [pending, setPending] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // Dupa inregistrare utilizatorul este logat automat si ajunge pe pagina principala
  if (user) return <Navigate to="/" replace />

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const result = await register(form.username, form.email, form.password)
      if (result?.verificationRequired) {
        setPending(result.mesaj)
        setSubmitting(false)
      }
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
    }
  }

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <AuthShell title={t('Creeaza un cont')} subtitle={t('Dureaza mai putin de un minut.')}>
      {pending && (
        <div className="mb-5">
          <Alert tone="success">{pending} {t('Deschide link-ul din email, apoi intra in cont.')}</Alert>
        </div>
      )}
      <form onSubmit={handleSubmit} className="space-y-5">
        <Alert>{error}</Alert>
        <Field label="Username" hint={t('Intre 3 si 50 de caractere')}>
          <Input value={form.username} onChange={update('username')} autoComplete="username" autoFocus required />
        </Field>
        <Field label="Email">
          <Input type="email" value={form.email} onChange={update('email')} autoComplete="email" required />
        </Field>
        <Field label={t('Parola')} hint={t('Minim 6 caractere')}>
          <Input
            type="password"
            value={form.password}
            onChange={update('password')}
            autoComplete="new-password"
            required
          />
        </Field>
        <Button type="submit" loading={submitting} className="w-full">
          {t('Creeaza contul')}
        </Button>
      </form>
      <GoogleButton />
      <p className="mt-6 text-center text-sm text-slate-500">
        {t('Ai deja cont?')}{' '}
        <Link to="/login" className="font-semibold text-indigo-600 hover:text-indigo-500">
          {t('Intra in cont')}
        </Link>
      </p>
    </AuthShell>
  )
}
