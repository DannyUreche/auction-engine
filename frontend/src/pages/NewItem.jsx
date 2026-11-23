import { useState } from 'react'
import { useNavigate } from 'react-router'
import { api, MAX_IMAGES, uploadItemImage } from '../api.js'
import { useCategories } from '../categories.js'
import ImagePicker from '../components/ImagePicker.jsx'
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
  const categories = useCategories()
  const [form, setForm] = useState({
    name: '',
    description: '',
    category: '',
    startPrice: '',
    buyNowPrice: '',
    durationInMinutes: '60',
  })
  const [images, setImages] = useState([])
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    let item
    try {
      item = await api('/items', {
        method: 'POST',
        body: {
          name: form.name,
          description: form.description,
          category: form.category,
          startPrice: Number(form.startPrice),
          // Optional: gol = fara "Cumpara acum"
          buyNowPrice: form.buyNowPrice ? Number(form.buyNowPrice) : null,
          durationInMinutes: Number(form.durationInMinutes),
        },
      })
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
      return
    }

    // Obiectul exista deja: pozele se incarca pe rand, in ordinea aleasa (prima = poza principala).
    // Cele care nu se incarca pot fi adaugate din nou de pe pagina obiectului.
    let failed = 0
    let lastError = ''
    for (const file of images) {
      try {
        await uploadItemImage(item.id, file)
      } catch (err) {
        failed++
        lastError = err.message
      }
    }
    if (failed > 0) {
      const what = failed === 1 ? 'o poza nu a putut fi incarcata' : `${failed} poze nu au putut fi incarcate`
      navigate(`/items/${item.id}`, { state: { warning: `Licitatia a pornit, dar ${what}: ${lastError}` } })
      return
    }
    navigate(`/items/${item.id}`)
  }

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="text-3xl font-bold tracking-tight text-slate-900">Vinde un obiect</h1>
      <p className="mt-2 text-slate-500">Completeaza detaliile, iar licitatia porneste imediat.</p>

      <Card className="mt-8">
        <form onSubmit={handleSubmit} className="space-y-6">
          <Alert>{error}</Alert>

          <div>
            <span className="mb-1.5 block text-sm font-medium text-slate-700">
              Poze (optionale, maxim {MAX_IMAGES})
            </span>
            <ImagePicker files={images} onChange={setImages} onError={setError} />
          </div>

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

          <Field label="Categorie">
            <Select value={form.category} onChange={update('category')} required>
              <option value="" disabled>
                Alege categoria...
              </option>
              {categories.map((c) => (
                <option key={c.code} value={c.code}>
                  {c.icon} {c.label}
                </option>
              ))}
            </Select>
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

          <Field
            label="Pret &quot;Cumpara acum&quot; (RON, optional)"
            hint="Cine plateste acest pret castiga imediat. Trebuie sa fie mai mare decat pretul de pornire. Lasa gol daca vrei doar licitatie."
          >
            <Input
              type="number"
              min="0.01"
              step="0.01"
              value={form.buyNowPrice}
              onChange={update('buyNowPrice')}
              placeholder="ex: 500"
            />
          </Field>

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
