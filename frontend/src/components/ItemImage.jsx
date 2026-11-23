// Imaginea unui obiect sau, daca nu are, un fundal colorat cu initiala numelui
const PLACEHOLDER_COLORS = [
  'from-indigo-100 to-violet-200 text-indigo-500',
  'from-amber-100 to-orange-200 text-amber-600',
  'from-emerald-100 to-teal-200 text-emerald-600',
  'from-sky-100 to-blue-200 text-sky-600',
  'from-rose-100 to-pink-200 text-rose-500',
]

// small: pentru miniaturi (ex: tabelul din pagina de administrare)
export default function ItemImage({ item, className = '', small = false }) {
  if (item.imageUrl) {
    return <img src={item.imageUrl} alt={item.name} loading="lazy" className={`object-cover ${className}`} />
  }
  const color = PLACEHOLDER_COLORS[item.id % PLACEHOLDER_COLORS.length]
  return (
    <div className={`grid place-items-center bg-gradient-to-br ${color} ${className}`}>
      <span className={`font-bold opacity-60 ${small ? 'text-base' : 'text-5xl'}`}>{item.name?.[0]?.toUpperCase()}</span>
    </div>
  )
}
