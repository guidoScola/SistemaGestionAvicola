import type { Period } from './purchasesApi'

const currency = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

export function money(value: number) {
  return currency.format(value)
}

/** ISO date (yyyy-mm-dd) to dd/mm/yyyy. */
export function date(iso: string | undefined) {
  if (!iso) {
    return ''
  }
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

function iso(value: Date) {
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${value.getFullYear()}-${month}-${day}`
}

export function currentMonth(): Period {
  const today = new Date()
  return {
    from: iso(new Date(today.getFullYear(), today.getMonth(), 1)),
    to: iso(new Date(today.getFullYear(), today.getMonth() + 1, 0)),
  }
}

/** Both dates are filled and in order; incomplete input while typing does not trigger a request. */
export function isComplete(period: Period) {
  return period.from.length === 10 && period.to.length === 10 && period.from <= period.to
}

export function yearToDate(): Period {
  const today = new Date()
  return { from: iso(new Date(today.getFullYear(), 0, 1)), to: currentMonth().to }
}
