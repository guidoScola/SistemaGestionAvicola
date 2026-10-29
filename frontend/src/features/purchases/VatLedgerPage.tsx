import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { toUserMessage } from '../../shared/api/errorMessages'
import { Feedback } from '../../shared/ui/Feedback'
import { currentMonth, date, isComplete, money } from './format'
import { PeriodFilter } from './PeriodFilter'
import { purchasesApi, voucherTypeLabels, type Amounts } from './purchasesApi'

const columns: Array<{ field: keyof Amounts; label: string }> = [
  { field: 'netTaxed21', label: 'Neto 21%' },
  { field: 'netTaxed105', label: 'Neto 10,5%' },
  { field: 'netTaxed27', label: 'Neto 27%' },
  { field: 'vat21', label: 'IVA 21%' },
  { field: 'vat105', label: 'IVA 10,5%' },
  { field: 'vat27', label: 'IVA 27%' },
  { field: 'nonTaxed', label: 'No gravado' },
  { field: 'exempt', label: 'Exento' },
  { field: 'vatPerceptions', label: 'Perc. IVA' },
  { field: 'grossIncomePerceptions', label: 'Perc. IIBB' },
  { field: 'otherTaxes', label: 'Otros imp.' },
]

export function VatLedgerPage() {
  const [period, setPeriod] = useState(currentMonth)
  const ledger = useQuery({
    queryKey: ['vat-ledger', period],
    queryFn: () => purchasesApi.vatLedger(period),
    enabled: isComplete(period),
  })

  return (
    <section>
      <div className="page-header">
        <h1>Subdiario de IVA Compras</h1>
      </div>
      <PeriodFilter period={period} onChange={setPeriod} />
      {ledger.isError && <Feedback error={toUserMessage(ledger.error)} success={null} />}
      {ledger.isPending && isComplete(period) && <p className="status">Cargando…</p>}
      {ledger.data && (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Comprobante</th>
                <th>Número</th>
                <th>Proveedor</th>
                <th>CUIT</th>
                {columns.map((c) => (
                  <th key={c.field} className="number">
                    {c.label}
                  </th>
                ))}
                <th className="number">Total</th>
              </tr>
            </thead>
            <tbody>
              {ledger.data.rows.map((row) => (
                <tr key={row.voucherId}>
                  <td>{date(row.issueDate)}</td>
                  <td>{voucherTypeLabels[row.type]}</td>
                  <td>{row.formattedNumber}</td>
                  <td>{row.supplierName}</td>
                  <td>{row.supplierCuit}</td>
                  {columns.map((c) => (
                    <td key={c.field} className="number">
                      {money(row.amounts[c.field])}
                    </td>
                  ))}
                  <td className="number">{money(row.total)}</td>
                </tr>
              ))}
              {ledger.data.rows.length === 0 && (
                <tr>
                  <td colSpan={columns.length + 6} className="status">
                    No hay comprobantes en el período.
                  </td>
                </tr>
              )}
            </tbody>
            <tfoot>
              <tr>
                <th colSpan={5}>Totales</th>
                {columns.map((c) => (
                  <td key={c.field} className="number">
                    {money(ledger.data.totals[c.field])}
                  </td>
                ))}
                <td className="number">{money(ledger.data.total)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      )}
    </section>
  )
}
