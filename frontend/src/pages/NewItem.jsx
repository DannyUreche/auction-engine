import { useState } from 'react'
import { useNavigate } from 'react-router'
import { api, MAX_IMAGES, uploadItemImage } from '../api.js'
import { useCategories } from '../categories.js'
import CategoryPicker from '../components/CategoryPicker.jsx'
import { Icon } from '../components/icons.jsx'
import AiAssistant from '../components/AiAssistant.jsx'
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
    auctionType: 'ENGLISH',
    floorPrice: '',
    priceDrop: '',
    scheduled: false,
    startTime: '',
  })
  const dutch = form.auctionType === 'DUTCH'
  const [images, setImages] = useState([])
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    if (!form.category) {
      setError('Alege o categorie pentru obiect!')
      return
    }
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
          buyNowPrice: !dutch && form.buyNowPrice ? Number(form.buyNowPrice) : null,
          durationInMinutes: Number(form.durationInMinutes),
          auctionType: form.auctionType,
          floorPrice: dutch ? Number(form.floorPrice) : null,
          priceDrop: dutch ? Number(form.priceDrop) : null,
          // datetime-local is local time; the server works in UTC (LocalDateTime without zone)
          startTime: form.scheduled && form.startTime ? new Date(form.startTime).toISOString().slice(0, 19) : null,
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

          <AiAssistant
            image={images[0]}
            onSuggestion={(suggestion) =>
              setForm((current) => ({
                ...current,
                name: suggestion.name,
                description: suggestion.description,
                category: suggestion.category,
                startPrice: String(suggestion.startPrice),
              }))
            }
          />

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

          <div>
            <span className="mb-1.5 block text-sm font-medium text-slate-700">Categorie</span>
            <CategoryPicker categories={categories} value={form.category} onChange={(category) => setForm({ ...form, category })} />
          </div>

          <div>
            <span className="mb-1.5 block text-sm font-medium text-slate-700">Tip licitatie</span>
            <div className="grid gap-2 sm:grid-cols-2">
              {[
                ['ENGLISH', 'coins', 'Clasica', 'Ofertele cresc pretul; castiga oferta cea mai mare.'],
                ['DUTCH', 'trendDown', 'Olandeza', 'Pretul scade in fiecare minut; primul care cumpara castiga.'],
              ].map(([type, icon, title, text]) => (
                <button
                  key={type}
                  type="button"
                  aria-pressed={form.auctionType === type}
                  onClick={() => setForm({ ...form, auctionType: type })}
                  className={`flex items-start gap-3 rounded-xl p-3 text-left transition ${
                    form.auctionType === type ? 'bg-indigo-50 ring-2 ring-indigo-500' : 'bg-white ring-1 ring-slate-200 hover:ring-indigo-300'
                  }`}
                >
                  <Icon name={icon} size={26} />
                  <span>
                    <span className="block text-sm font-semibold text-slate-900">{title}</span>
                    <span className="block text-xs text-slate-500">{text}</span>
                  </span>
                </button>
              ))}
            </div>
          </div>

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

          {!dutch && (
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
          )}

          {dutch && (
            <div className="grid gap-6 sm:grid-cols-2">
              <Field label="Pret minim (RON)" hint="Pretul nu scade sub aceasta valoare.">
                <Input type="number" min="0.01" step="0.01" value={form.floorPrice} onChange={update('floorPrice')} placeholder="ex: 40" required />
              </Field>
              <Field label="Scadere pe minut (RON)" hint="Cu cat scade pretul in fiecare minut.">
                <Input type="number" min="0.01" step="0.01" value={form.priceDrop} onChange={update('priceDrop')} placeholder="ex: 5" required />
              </Field>
            </div>
          )}

          <div className="rounded-xl bg-slate-50 p-4 ring-1 ring-slate-200">
            <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
              <input
                type="checkbox"
                checked={form.scheduled}
                onChange={(e) => setForm({ ...form, scheduled: e.target.checked })}
                className="h-4 w-4 accent-indigo-600"
              />
              <Icon name="timer" size={18} /> Programeaza inceputul licitatiei
            </label>
            {form.scheduled && (
              <div className="mt-3">
                <Input type="datetime-local" value={form.startTime} onChange={update('startTime')} required />
                <p className="mt-1 text-xs text-slate-500">
                  Pana atunci obiectul apare cu „Incepe in...”, iar cine il urmareste primeste o notificare la start.
                </p>
              </div>
            )}
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
