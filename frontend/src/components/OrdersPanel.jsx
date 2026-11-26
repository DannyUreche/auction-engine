import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, formatDate, formatPrice } from '../api.js'
import ItemImage from './ItemImage.jsx'
import { Alert, Loading } from './ui.jsx'

export const ORDER_STATUS = {
  PAID: { label: 'Asteapta expedierea', tone: 'bg-amber-50 text-amber-800 ring-amber-200' },
  SHIPPED: { label: 'Expediata', tone: 'bg-indigo-50 text-indigo-800 ring-indigo-200' },
  DISPUTED: { label: 'In disputa', tone: 'bg-rose-50 text-rose-800 ring-rose-200' },
  COMPLETED: { label: 'Finalizata', tone: 'bg-emerald-50 text-emerald-800 ring-emerald-200' },
  REFUNDED: { label: 'Rambursata', tone: 'bg-slate-100 text-slate-700 ring-slate-200' },
}

// Account tab "Comenzi": what I bought and what I sold, with the step each order is at
export default function OrdersPanel() {
  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/orders/mine')
      .then(setOrders)
      .catch((err) => setError(err.message))
  }, [])

  if (error) return <Alert>{error}</Alert>
  if (!orders) return <Loading />

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <OrderList title="Cumparaturi" orders={orders.purchases} empty="Inca nu ai castigat nicio licitatie." todo={(o) => o.status === 'SHIPPED' && 'Confirma primirea'} />
      <OrderList title="Vanzari" orders={orders.sales} empty="Inca nu ai vandut nimic." todo={(o) => o.status === 'PAID' && `Expediaza pana la ${formatDate(o.shipBy)}`} />
    </div>
  )
}

export function OrderList({ title, orders, empty, todo, action }) {
  return (
    <section>
      <h3 className="font-semibold text-slate-900">
        {title} <span className="text-sm font-normal text-slate-500">({orders.length})</span>
      </h3>
      {orders.length === 0 ? (
        <p className="mt-3 rounded-2xl bg-white/85 p-6 text-center text-sm text-slate-500 ring-1 ring-slate-200/80">{empty}</p>
      ) : (
        <ul className="mt-3 space-y-2">
          {orders.map((order, index) => {
            const status = ORDER_STATUS[order.status]
            const next = todo?.(order)
            return (
              <li key={order.id} className="animate-fade-up" style={{ animationDelay: `${index * 50}ms` }}>
                <div className="flex items-center gap-3 rounded-2xl bg-white/85 p-3 shadow-sm ring-1 ring-slate-200/80">
                  <Link to={`/items/${order.itemId}`} className="shrink-0">
                    <ItemImage item={{ id: order.itemId, name: order.itemName, imageUrl: order.itemImageUrl }} className="h-14 w-14 rounded-xl" />
                  </Link>
                  <div className="min-w-0 flex-1">
                    <Link to={`/items/${order.itemId}`} className="block truncate font-medium text-slate-900 hover:text-indigo-700">
                      {order.itemName}
                    </Link>
                    <p className="text-xs text-slate-500">
                      #{order.id} · {formatPrice(order.amount)} · {order.role === 'SELLER' ? `catre ${order.buyer}` : `de la ${order.seller}`} · {formatDate(order.createdAt)}
                    </p>
                    {next && <p className="mt-0.5 text-xs font-semibold text-indigo-700">→ {next}</p>}
                    {action?.(order)}
                  </div>
                  <span className={`shrink-0 rounded-full px-2.5 py-1 text-[11px] font-semibold ring-1 ${status.tone}`}>{status.label}</span>
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
