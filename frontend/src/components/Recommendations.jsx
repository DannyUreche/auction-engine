import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Icon } from './icons.jsx'
import ItemCard from './ItemCard.jsx'
import { useT } from '../i18n.jsx'

// "You might like": active items picked from the categories the user bids on or watches (logged-in users only)
export default function Recommendations() {
  const t = useT()
  const { user } = useAuth()
  const [items, setItems] = useState([])

  useEffect(() => {
    if (!user) {
      setItems([])
      return
    }
    api('/items/recommended')
      .then(setItems)
      .catch(() => setItems([]))
  }, [user])

  if (!user || items.length === 0) return null
  return (
    <section className="mt-10">
      <h2 className="flex items-center gap-2 text-xl font-bold text-slate-900">
        <Icon name="sparkles" size={22} /> {t('Te-ar putea interesa')}
      </h2>
      <p className="mt-1 text-sm text-slate-500">{t('Alese dupa categoriile in care ai licitat sau pe care le urmaresti.')}</p>
      <div className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((item, index) => (
          <ItemCard key={item.id} item={item} index={index} />
        ))}
      </div>
    </section>
  )
}
