import { CategoryIcon } from './icons.jsx'

// Category choice as a grid of buttons with icons (a native <select> cannot show icons)
export default function CategoryPicker({ categories, value, onChange }) {
  return (
    <div role="radiogroup" aria-label="Categorie" className="grid grid-cols-3 gap-2 sm:grid-cols-5">
      {categories.map((c) => {
        const selected = c.code === value
        return (
          <button
            key={c.code}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(c.code)}
            className={`flex flex-col items-center gap-1.5 rounded-xl px-2 py-3 text-center text-xs font-medium transition ${
              selected
                ? 'bg-indigo-50 text-indigo-700 ring-2 ring-indigo-500'
                : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:-translate-y-0.5 hover:ring-indigo-300'
            }`}
          >
            <CategoryIcon category={c} size={30} />
            <span className="leading-tight">{c.label}</span>
          </button>
        )
      })}
    </div>
  )
}
