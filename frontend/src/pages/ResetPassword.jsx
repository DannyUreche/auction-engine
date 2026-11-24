import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { api } from '../api.js'
import AuthShell from '../components/AuthShell.jsx'
import { Alert, Button, Field, Input } from '../components/ui.jsx'

// Pagina deschisa din emailul de resetare (/reset-password?token=...): alegerea parolei noi
export default function ResetPassword() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const [form, setForm] = useState({ password: '', confirm: '' })
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    if (form.password !== form.confirm) {
      setError('Cele doua parole nu coincid!')
      return
    }
    setSubmitting(true)
    try {
      const data = await api('/auth/reset-password', {
        method: 'POST',
        body: { token, password: form.password },
        auth: false,
      })
      setMessage(data.mesaj)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <AuthShell title="Parola noua" subtitle="Alege o parola noua pentru contul tau (minim 6 caractere).">
      {message ? (
        <div className="space-y-4">
          <Alert tone="success">{message}</Alert>
          <Link to="/login" className="block text-center font-semibold text-indigo-600 hover:text-indigo-500">
            Intra in cont →
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-5">
          <Alert>{error}</Alert>
          {!token && <Alert>Link-ul de resetare este invalid sau a expirat!</Alert>}
          <Field label="Parola noua">
            <Input type="password" value={form.password} onChange={update('password')} autoComplete="new-password" minLength={6} required />
          </Field>
          <Field label="Repeta parola">
            <Input type="password" value={form.confirm} onChange={update('confirm')} autoComplete="new-password" minLength={6} required />
          </Field>
          <Button type="submit" loading={submitting} disabled={!token} className="w-full">
            Schimba parola
          </Button>
        </form>
      )}
    </AuthShell>
  )
}
