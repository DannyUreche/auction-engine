import { useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { useAuth } from '../auth.jsx'
import AuthShell from '../components/AuthShell.jsx'
import { Alert, Button, Field, Input } from '../components/ui.jsx'

export default function Login() {
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
    <AuthShell title="Bine ai revenit" subtitle="Intra in cont ca sa licitezi si sa vinzi obiecte.">
      <form onSubmit={handleSubmit} className="space-y-5">
        <Alert>{error}</Alert>
        <Field label="Username">
          <Input value={form.username} onChange={update('username')} autoComplete="username" autoFocus required />
        </Field>
        <Field label="Parola">
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
            Ai uitat parola?
          </Link>
        </div>
        <Button type="submit" loading={submitting} className="w-full">
          Intra in cont
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">
        Nu ai cont?{' '}
        <Link to="/register" className="font-semibold text-indigo-600 hover:text-indigo-500">
          Creeaza unul
        </Link>
      </p>
    </AuthShell>
  )
}
