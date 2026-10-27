import { useQuery } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { toUserMessage } from '../../shared/api/errorMessages'
import { Feedback } from '../../shared/ui/Feedback'
import { suppliersApi } from '../suppliers/suppliersApi'
import { money } from './format'
import {
  discriminatesVat,
  voucherTypeLabels,
  type Amounts,
  type VoucherInput,
  type VoucherType,
} from './purchasesApi'

type VoucherFormProps = {
  submitting: boolean
  onSubmit: (input: VoucherInput) => void
  onCancel: () => void
}

type AmountField = keyof Amounts

const vatFields: Array<{ net: AmountField; vat: AmountField; rate: number; label: string }> = [
  { net: 'netTaxed21', vat: 'vat21', rate: 0.21, label: '21%' },
  { net: 'netTaxed105', vat: 'vat105', rate: 0.105, label: '10,5%' },
  { net: 'netTaxed27', vat: 'vat27', rate: 0.27, label: '27%' },
]

const otherFields: Array<{ field: AmountField; label: string }> = [
  { field: 'nonTaxed', label: 'No gravado' },
  { field: 'exempt', label: 'Exento' },
  { field: 'vatPerceptions', label: 'Percepciones de IVA' },
  { field: 'grossIncomePerceptions', label: 'Percepciones de Ingresos Brutos' },
  { field: 'otherTaxes', label: 'Otros impuestos' },
]

const allFields: AmountField[] = [...vatFields.flatMap((f) => [f.net, f.vat]), ...otherFields.map((f) => f.field)]

const emptyAmounts = Object.fromEntries(allFields.map((field) => [field, ''])) as Record<AmountField, string>

function toNumber(value: string) {
  const parsed = Number(value)
  return value.trim() === '' || Number.isNaN(parsed) ? 0 : parsed
}

function round2(value: number) {
  return Math.round(value * 100) / 100
}

export function VoucherForm({ submitting, onSubmit, onCancel }: VoucherFormProps) {
  const suppliers = useQuery({ queryKey: ['suppliers'], queryFn: suppliersApi.list })
  const [supplierId, setSupplierId] = useState('')
  const [type, setType] = useState<VoucherType>('FACTURA_A')
  const [issueDate, setIssueDate] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [pointOfSale, setPointOfSale] = useState('')
  const [number, setNumber] = useState('')
  const [amounts, setAmounts] = useState(emptyAmounts)

  const withVat = discriminatesVat(type)
  const visibleFields = withVat ? allFields : otherFields.map((f) => f.field)
  const total = round2(visibleFields.reduce((sum, field) => sum + toNumber(amounts[field]), 0))

  function setAmount(field: AmountField, value: string) {
    setAmounts((current) => ({ ...current, [field]: value }))
  }

  function setNet(net: AmountField, vat: AmountField, rate: number, value: string) {
    setAmounts((current) => ({
      ...current,
      [net]: value,
      [vat]: value.trim() === '' ? '' : String(round2(toNumber(value) * rate)),
    }))
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const sent = Object.fromEntries(
      allFields.map((field) => [field, visibleFields.includes(field) ? toNumber(amounts[field]) : 0]),
    ) as Amounts
    onSubmit({
      supplierId,
      type,
      issueDate,
      dueDate: dueDate || null,
      pointOfSale: Number(pointOfSale),
      number: Number(number),
      amounts: sent,
    })
  }

  if (suppliers.isPending) {
    return <p className="status">Cargando proveedores…</p>
  }
  if (suppliers.isError) {
    return <Feedback error={toUserMessage(suppliers.error)} success={null} />
  }

  const amountInput = (field: AmountField, label: string, onChange: (value: string) => void) => (
    <label key={field}>
      {label}
      <input
        type="number"
        min="0"
        step="0.01"
        inputMode="decimal"
        value={amounts[field]}
        onChange={(e) => onChange(e.target.value)}
      />
    </label>
  )

  return (
    <form className="card" onSubmit={handleSubmit}>
      <h2>Nuevo comprobante</h2>
      <label>
        Proveedor
        <select value={supplierId} onChange={(e) => setSupplierId(e.target.value)} required>
          <option value="">Elegí un proveedor</option>
          {suppliers.data
            .filter((s) => s.active)
            .map((s) => (
              <option key={s.id} value={s.id}>
                {s.businessName} ({s.cuit})
              </option>
            ))}
        </select>
      </label>
      <label>
        Tipo
        <select value={type} onChange={(e) => setType(e.target.value as VoucherType)}>
          {Object.entries(voucherTypeLabels).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <label>
        Fecha
        <input type="date" value={issueDate} onChange={(e) => setIssueDate(e.target.value)} required />
      </label>
      <label>
        Vencimiento
        <input type="date" value={dueDate} min={issueDate} onChange={(e) => setDueDate(e.target.value)} />
      </label>
      <label>
        Punto de venta
        <input
          type="number"
          min="1"
          max="99999"
          value={pointOfSale}
          onChange={(e) => setPointOfSale(e.target.value)}
          required
        />
      </label>
      <label>
        Número
        <input
          type="number"
          min="1"
          max="99999999"
          value={number}
          onChange={(e) => setNumber(e.target.value)}
          required
        />
      </label>
      {withVat &&
        vatFields.flatMap(({ net, vat, rate, label }) => [
          amountInput(net, `Neto gravado ${label}`, (value) => setNet(net, vat, rate, value)),
          amountInput(vat, `IVA ${label}`, (value) => setAmount(vat, value)),
        ])}
      {otherFields.map(({ field, label }) => amountInput(field, label, (value) => setAmount(field, value)))}
      <p className="total">Total: {money(total)}</p>
      <div className="actions">
        <button type="submit" disabled={submitting}>
          Guardar
        </button>
        <button type="button" className="secondary" onClick={onCancel}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
