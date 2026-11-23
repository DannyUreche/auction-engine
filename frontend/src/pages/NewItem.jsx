import { useState } from 'react'
import { useNavigate } from 'react-router'
import { api } from '../api.js'
import { Alert, Button, Card, Field, Input, Select, Textarea } from '../components/ui.jsx'

const DURATIONS = [
  { minutes: 2, label: '2 minute (pentru test)' },
  { minutes: 15, label: '15 minute' },
  { minutes: 60, label: '1 ora' },
  { minutes: 360, label: '6 ore' },
  { minutes: 1440, label: '1 zi' },
  { minutes: 4320, label: '3 zile' },
  { minutes: 10080, label: '7 zile' },
]

export default function NewItem() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', description: '', startPrice: '', durationInMinutes: '60' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const item = await api('/items', {
        method: 'POST',
        body: {
          name: form.name,
          description: form.description,
          startPrice: Number(form.startPrice),
          durationInMinutes: Number(form.durationInMinutes),
        },
      })
      navigate(`/items/${item.id}`)
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
    }
  }

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="text-3xl font-bold tracking-tight text-slate-900">Vinde un obiect</h1>
      <p className="mt-2 text-slate-500">Completeaza detaliile, iar licitatia porneste imediat.</p>

      <Card className="mt-8">
        <form onSubmit={handleSubmit} className="space-y-6">
          <Alert>{error}</Alert>

          <Field label="Nume" hint="Maxim 100 de caractere">
            <Input
              value={form.name}
              onChange={update('name')}
              placeholder="ex: Ceas de buzunar vechi"
              maxLength={100}
              required
            />
          </Field>

          <Field label="Descriere">
            <Textarea
              value={form.description}
              onChange={update('description')}
              placeholder="Starea obiectului, detalii importante..."
            />
          </Field>

          <div className="grid gap-6 sm:grid-cols-2">
            <Field label="Pret de pornire (RON)">
              <Input
                type="number"
                min="0.01"
                step="0.01"
                value={form.startPrice}
                onChange={update('startPrice')}
                placeholder="100"
                required
              />
            </Field>
            <Field label="Durata licitatiei">
              <Select value={form.durationInMinutes} onChange={update('durationInMinutes')}>
                {DURATIONS.map((d) => (
                  <option key={d.minutes} value={d.minutes}>
                    {d.label}
                  </option>
                ))}
              </Select>
            </Field>
          </div>

          <div className="flex justify-end gap-3 border-t border-slate-100 pt-6">
            <Button type="button" variant="secondary" onClick={() => navigate(-1)}>
              Renunta
            </Button>
            <Button type="submit" loading={submitting}>
              Porneste licitatia
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
