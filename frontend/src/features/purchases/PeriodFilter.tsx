import type { Period } from './purchasesApi'

type PeriodFilterProps = {
  period: Period
  onChange: (period: Period) => void
}

export function PeriodFilter({ period, onChange }: PeriodFilterProps) {
  return (
    <div className="filters">
      <label>
        Desde
        <input type="date" value={period.from} onChange={(e) => onChange({ ...period, from: e.target.value })} />
      </label>
      <label>
        Hasta
        <input type="date" value={period.to} onChange={(e) => onChange({ ...period, to: e.target.value })} />
      </label>
    </div>
  )
}
