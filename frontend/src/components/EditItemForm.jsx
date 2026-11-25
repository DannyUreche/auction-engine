import { useState } from 'react'
import { api } from '../api.js'
import { useCategories } from '../categories.js'
import CategoryPicker from './CategoryPicker.jsx'
import { Alert, Button, Card, Field, Input, Textarea } from './ui.jsx'
import LocationFields from './LocationFields.jsx'

// Formularul de editare al vanzatorului (doar cat timp licitatia nu are oferte; serverul verifica si el).
// Data de incheiere nu se schimba; pozele se modifica direct din galerie.
export default function EditItemForm({ item, onSaved, onCancel }) {
  const categories = useCategories()
  const [form, setForm] = useState({
    name: item.name,
    description: item.description ?? '',
    category: item.category,
    startPrice: String(item.startPrice),
    buyNowPrice: item.buyNowPrice != null ? String(item.buyNowPrice) : '',
    city: item.city ?? '',
    delivery: item.delivery ?? 'BOTH',
  })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      const updated = await api(`/items/${item.id}`, {
        method: 'PUT',
        body: {
          name: form.name,
          description: form.description,
          category: form.category,
          startPrice: Number(form.startPrice),
          buyNowPrice: form.buyNowPrice ? Number(form.buyNowPrice) : null,
          city: form.city,
          delivery: form.delivery,
        },
      })
      onSaved(updated)
    } catch (err) {
      setError(err.message)
      setSaving(false)
    }
  }

  return (
    <Card>
      <h2 className="text-lg font-semibold text-slate-900">Editeaza obiectul</h2>
      <p className="mt-1 text-sm text-slate-500">
        Poti modifica obiectul doar pana la prima oferta. Data de incheiere ramane aceeasi.
      </p>

      <form onSubmit={handleSubmit} className="mt-6 space-y-5">
        <Alert>{error}</Alert>

        <Field label="Nume">
          <Input value={form.name} onChange={update('name')} maxLength={100} required />
        </Field>

        <Field label="Descriere">
          <Textarea value={form.description} onChange={update('description')} />
        </Field>

        <div>
          <span className="mb-1.5 block text-sm font-medium text-slate-700">Categorie</span>
          <CategoryPicker categories={categories} value={form.category} onChange={(category) => setForm({ ...form, category })} />
        </div>

        <div className="grid gap-5 sm:grid-cols-2">
          <Field label="Pret de pornire (RON)">
            <Input type="number" min="0.01" step="0.01" value={form.startPrice} onChange={update('startPrice')} required />
          </Field>
          <Field label="Pret &quot;Cumpara acum&quot; (optional)">
            <Input
              type="number"
              min="0.01"
              step="0.01"
              value={form.buyNowPrice}
              onChange={update('buyNowPrice')}
              placeholder="gol = doar licitatie"
            />
          </Field>
        </div>

        <LocationFields city={form.city} delivery={form.delivery} onChange={(change) => setForm((current) => ({ ...current, ...change }))} />

        <div className="flex justify-end gap-3 border-t border-slate-100 pt-5">
          <Button type="button" variant="secondary" onClick={onCancel}>
            Renunta
          </Button>
          <Button type="submit" loading={saving}>
            Salveaza
          </Button>
        </div>
      </form>
    </Card>
  )
}
