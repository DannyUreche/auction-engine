import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { api } from '../api.js'
import AuthShell from '../components/AuthShell.jsx'
import { Alert, Loading } from '../components/ui.jsx'

// Opened from the confirmation email (/verify-email?token=...): activates the account
export default function VerifyEmail() {
  const [params] = useSearchParams()
  const [result, setResult] = useState(null)
  const sent = useRef(false)

  useEffect(() => {
    if (sent.current) return // React StrictMode runs effects twice; the link works only once
    sent.current = true
    api('/auth/verify-email', { method: 'POST', body: { token: params.get('token') ?? '' }, auth: false })
      .then((data) => setResult({ ok: true, text: data.mesaj }))
      .catch((err) => setResult({ ok: false, text: err.message }))
  }, [params])

  return (
    <AuthShell title="Confirmarea emailului" subtitle="Activam contul tau.">
      {!result ? (
        <Loading />
      ) : (
        <div className="space-y-4">
          <Alert tone={result.ok ? 'success' : 'error'}>{result.text}</Alert>
          <Link to="/login" className="block text-center font-semibold text-indigo-600 hover:text-indigo-500">
            Intra in cont →
          </Link>
        </div>
      )}
    </AuthShell>
  )
}
