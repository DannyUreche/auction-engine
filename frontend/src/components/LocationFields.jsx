import { Icon } from './icons.jsx'
import { COUNTIES } from '../counties.js'
import { Field, Select } from './ui.jsx'

export const DELIVERY = [
  { code: 'BOTH', label: 'Curier sau predare personala', short: 'Curier / personal' },
  { code: 'COURIER', label: 'Doar prin curier', short: 'Curier' },
  { code: 'PICKUP', label: 'Doar predare personala', short: 'Personal' },
]

export const deliveryLabel = (code) => DELIVERY.find((d) => d.code === code)?.label ?? DELIVERY[0].label


// County and how the item reaches the buyer; used by the sell and edit forms
export default function LocationFields({ county, delivery, onChange }) {
  return (
    <div className="grid gap-6 sm:grid-cols-2">
      <Field label="Judet (optional)" hint="Cumparatorii pot filtra licitatiile dupa judet.">
        <Select value={county} onChange={(e) => onChange({ county: e.target.value })}>
          <option value="">Alege judetul</option>
          {COUNTIES.map((name) => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
        </Select>
      </Field>
      <div>
        <span className="mb-1.5 block text-sm font-medium text-slate-700">Livrare</span>
        <div className="flex flex-wrap gap-2">
          {DELIVERY.map((option) => (
            <button
              key={option.code}
              type="button"
              onClick={() => onChange({ delivery: option.code })}
              className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-2 text-sm font-medium transition ${
                delivery === option.code
                  ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/25'
                  : 'bg-slate-50 text-slate-600 ring-1 ring-slate-200 hover:text-indigo-700 hover:ring-indigo-300'
              }`}
            >
              <Icon name={option.code === 'PICKUP' ? 'pin' : 'box'} size={16} mono={delivery === option.code} /> {option.short}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
