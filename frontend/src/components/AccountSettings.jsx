import { useState } from 'react'
import { api, clearToken } from '../api.js'
import { Icon } from './icons.jsx'
import { Alert, Button, Card, Input } from './ui.jsx'

// "Account settings" tab: download all your data (JSON) or delete the account
export default function AccountSettings({ user }) {
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [confirm, setConfirm] = useState('')

  async function download() {
    setBusy('export')
    setError('')
    try {
      const data = await api('/users/me/export')
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `datele-mele-${user.username}.json`
      link.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy('')
    }
  }

  async function remove(event) {
    event.preventDefault()
    if (!window.confirm('Sigur stergi contul? Actiunea nu poate fi anulata.')) return
    setBusy('delete')
    setError('')
    try {
      await api('/users/me', { method: 'DELETE', body: { confirm } })
      clearToken()
      window.location.assign('/')
    } catch (err) {
      setError(err.message)
      setBusy('')
    }
  }

  return (
    <div className="space-y-6">
      <Alert>{error}</Alert>
      <Card>
        <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-900">
          <Icon name="download" size={22} /> Descarca datele tale
        </h2>
        <p className="mt-1 text-sm text-slate-600">
          Un fisier JSON cu tot ce stim despre tine: contul, obiectele, ofertele, mesajele, recenziile, portofelul si
          notificarile.
        </p>
        <Button variant="secondary" className="mt-4" loading={busy === 'export'} onClick={download}>
          Descarca datele mele
        </Button>
      </Card>

      <Card>
        <h2 className="flex items-center gap-2 text-lg font-semibold text-red-700">
          <Icon name="flag" size={22} /> Sterge contul
        </h2>
        <p className="mt-1 text-sm text-slate-600">
          Emailul, parola, notificarile, lista de urmarire si licitarile automate se sterg. Ofertele, vanzarile si recenziile
          raman pentru ceilalti participanti, dar fara numele tau (apar ca „sters_…”). Nu poti sterge contul cat timp conduci o
          licitatie sau ai obiecte la vanzare cu oferte; obiectele tale fara oferte se sterg.
        </p>
        <form onSubmit={remove} className="mt-4 flex flex-col gap-3 sm:flex-row">
          <Input
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            placeholder={`Scrie „${user.username}” ca sa confirmi`}
            aria-label="Confirmare: numele tau de utilizator"
          />
          <Button type="submit" variant="danger" className="shrink-0" loading={busy === 'delete'} disabled={confirm !== user.username}>
            Sterge contul definitiv
          </Button>
        </form>
      </Card>
    </div>
  )
}
