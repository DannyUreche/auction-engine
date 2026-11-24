import { useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api.js'
import AuthShell from '../components/AuthShell.jsx'
import { Alert, Button, Field, Input } from '../components/ui.jsx'

// "Am uitat parola": cere un link de resetare pe email
export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const data = await api('/auth/forgot-password', { method: 'POST', body: { email }, auth: false })
      setMessage(data.mesaj)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthShell title="Ai uitat parola?" subtitle="Iti trimitem pe email un link cu care iti alegi o parola noua.">
      {message ? (
        <Alert tone="success">
          {message} Link-ul este valabil 30 de minute. Verifica si folderul Spam.
        </Alert>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-5">
          <Alert>{error}</Alert>
          <Field label="Emailul contului">
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" autoFocus required />
          </Field>
          <Button type="submit" loading={submitting} className="w-full">
            Trimite link-ul
          </Button>
        </form>
      )}
      <p className="mt-6 text-center text-sm text-slate-500">
        Ti-ai amintit?{' '}
        <Link to="/login" className="font-semibold text-indigo-600 hover:text-indigo-500">
          Intra in cont
        </Link>
      </p>
    </AuthShell>
  )
}
