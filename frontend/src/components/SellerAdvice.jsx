import { useEffect, useState } from 'react'
import { api, formatPrice } from '../api.js'
import { Icon } from './icons.jsx'

const DAYS = ['', 'lunea', 'martea', 'miercurea', 'joia', 'vinerea', 'sambata', 'duminica']
const MONTHS = ['ian', 'feb', 'mar', 'apr', 'mai', 'iun', 'iul', 'aug', 'sep', 'oct', 'noi', 'dec']

// Sell form: what similar items sold for, the monthly median price of the category and tips drawn from the
// listings that sold (GET /api/items/price-advice). Loads after the name/category stop changing for a moment.
export default function SellerAdvice({ name, category, dutch, onUsePrices }) {
  const [advice, setAdvice] = useState(null)

  useEffect(() => {
    if (!category) {
      setAdvice(null)
      return
    }
    const id = setTimeout(() => {
      api(`/items/price-advice?category=${category}&name=${encodeURIComponent(name.trim())}`, { auth: false })
        .then(setAdvice)
        .catch(() => setAdvice(null))
    }, 600)
    return () => clearTimeout(id)
  }, [name, category])

  if (!advice) return null
  const { estimate, history } = advice
  const tips = buildTips(advice)
  const hasHistory = history.some((point) => point.sales > 0)
  if (!estimate && !hasHistory && tips.length === 0) return null

  return (
    <section className="animate-fade-up overflow-hidden rounded-2xl bg-gradient-to-br from-amber-50 to-indigo-50 ring-1 ring-amber-200">
      <div className="flex items-center gap-2 border-b border-amber-200/70 px-4 py-3">
        <Icon name="chart" size={22} />
        <p className="font-semibold text-slate-900">Istoric preturi si sfaturi</p>
        <span className="ml-auto text-xs text-slate-500">din vanzarile de pe site</span>
      </div>

      <div className="grid gap-5 p-4 sm:grid-cols-2">
        <div>
          <p className="text-[11px] font-semibold tracking-wide text-slate-500 uppercase">
            {estimate ? (estimate.basis === 'SIMILAR' ? `Obiecte asemanatoare (${estimate.sales} vanzari)` : `Toata categoria (${estimate.sales} vanzari)`) : 'Pret estimat'}
          </p>
          {estimate ? (
            <>
              <p className="mt-1 text-xl font-black text-slate-900">
                <span className="whitespace-nowrap">{formatPrice(estimate.low)}</span> – <span className="whitespace-nowrap">{formatPrice(estimate.high)}</span>
              </p>
              <p className="text-xs text-slate-500">pretul final obisnuit · median {formatPrice(estimate.median)}</p>
              {!dutch && (
                <button
                  type="button"
                  onClick={() => onUsePrices(advice.suggestedStart, advice.suggestedBuyNow)}
                  className="mt-3 inline-flex items-center gap-2 rounded-xl bg-white px-3 py-2 text-left text-xs font-semibold text-indigo-700 shadow-sm ring-1 ring-slate-300 transition hover:ring-indigo-400"
                >
                  <Icon name="sparkles" size={16} />
                  <span>
                    Foloseste pornire {formatPrice(advice.suggestedStart)}
                    <span className="block font-normal text-slate-500">si Cumpara acum {formatPrice(advice.suggestedBuyNow)}</span>
                  </span>
                </button>
              )}
              {estimate.recent.length > 0 && (
                <ul className="mt-3 space-y-1 text-xs">
                  {estimate.recent.slice(0, 3).map((sale) => (
                    <li key={sale.id} className="flex items-center justify-between gap-3">
                      <span className="truncate text-slate-600">{sale.name}</span>
                      <b className="shrink-0 text-slate-900 tabular-nums">{formatPrice(sale.price)}</b>
                    </li>
                  ))}
                </ul>
              )}
            </>
          ) : (
            <p className="mt-1 text-sm text-slate-500">Scrie numele obiectului ca sa vezi cat s-au vandut obiecte asemanatoare.</p>
          )}
        </div>

        {hasHistory && <MonthlyMedian history={history} />}
      </div>

      {tips.length > 0 && (
        <ul className="space-y-2 border-t border-amber-200/70 px-4 py-3">
          {tips.map(([icon, text]) => (
            <li key={text} className="flex items-start gap-2 text-sm text-slate-700">
              <span className="mt-0.5 shrink-0">
                <Icon name={icon} size={16} />
              </span>
              <span>{text}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

// Columns with the median sale price of each of the last 6 months (empty months stay as a thin line)
function MonthlyMedian({ history }) {
  const max = Math.max(1, ...history.map((point) => Number(point.median ?? 0)))
  return (
    <div>
      <p className="text-[11px] font-semibold tracking-wide text-slate-500 uppercase">Pretul median pe luni, in categorie</p>
      <div className="mt-3 flex h-28 items-end gap-2">
        {history.map((point) => {
          const month = MONTHS[Number(point.month.slice(5, 7)) - 1]
          const value = Number(point.median ?? 0)
          return (
            <div key={point.month} className="group relative flex flex-1 flex-col items-center justify-end gap-1">
              {point.median != null && (
                <span className="pointer-events-none absolute -top-6 rounded bg-slate-900 px-1.5 py-0.5 text-[10px] whitespace-nowrap text-white opacity-0 transition group-hover:opacity-100 stay-dark">
                  {formatPrice(point.median)} · {point.sales}
                </span>
              )}
              <span
                className={`w-full rounded-t-[4px] ${point.median == null ? 'bg-slate-200' : 'bg-indigo-500'}`}
                style={{ height: point.median == null ? 2 : `${Math.max(6, (value / max) * 80)}px` }}
                title={point.median == null ? `${month}: nicio vanzare` : `${month}: ${formatPrice(point.median)} (${point.sales} vanzari)`}
              />
              <span className="text-[10px] text-slate-500">{month}</span>
            </div>
          )
        })}
      </div>
    </div>
  )
}

function buildTips(advice) {
  const tips = []
  const { photos, lowStart } = advice
  if (photos.withPercent != null && photos.withoutPercent != null && photos.withPercent > photos.withoutPercent) {
    tips.push(['camera', `Anunturile cu 3 sau mai multe poze s-au vandut in ${photos.withPercent}% din cazuri, fata de ${photos.withoutPercent}% cu mai putine poze.`])
  }
  if (lowStart.withPercent != null && lowStart.withoutPercent != null && lowStart.withPercent > lowStart.withoutPercent) {
    tips.push(['coins', `Un pret de pornire sub pretul median atrage oferte: ${lowStart.withPercent}% vandute, fata de ${lowStart.withoutPercent}% cu pornire mare.`])
  }
  if (advice.bestDay != null && advice.bestHour != null) {
    tips.push(['timer', `In aceasta categorie se liciteaza cel mai mult ${DAYS[advice.bestDay]}, in jurul orei ${String(advice.bestHour).padStart(2, '0')}:00 - alege o durata care sa se termine atunci.`])
  }
  if (advice.lastHourPercent != null && advice.lastHourPercent >= 20) {
    tips.push(['hourglass', `${advice.lastHourPercent}% din oferte vin in ultima ora a licitatiei; ofertele de la final prelungesc licitatia cu 2 minute.`])
  }
  if (advice.avgBids != null) {
    tips.push(['gavel', `Un obiect vandut in aceasta categorie a primit in medie ${advice.avgBids} oferte.`])
  }
  return tips
}
