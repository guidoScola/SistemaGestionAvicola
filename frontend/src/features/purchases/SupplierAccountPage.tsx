import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { toUserMessage } from '../../shared/api/errorMessages'
import { Feedback } from '../../shared/ui/Feedback'
import { suppliersApi } from '../suppliers/suppliersApi'
import { date, isComplete, money, yearToDate } from './format'
import { PeriodFilter } from './PeriodFilter'
import { purchasesApi, voucherTypeLabels } from './purchasesApi'

export function SupplierAccountPage() {
  const suppliers = useQuery({ queryKey: ['suppliers'], queryFn: suppliersApi.list })
  const [supplierId, setSupplierId] = useState('')
  const [period, setPeriod] = useState(yearToDate)
  const account = useQuery({
    queryKey: ['supplier-account', supplierId, period],
    queryFn: () => purchasesApi.supplierAccount(supplierId, period),
    enabled: supplierId !== '' && isComplete(period),
  })

  if (suppliers.isPending) {
    return <p className="status">Cargando…</p>
  }
  if (suppliers.isError) {
    return <Feedback error={toUserMessage(suppliers.error)} success={null} />
  }

  return (
    <section>
      <div className="page-header">
        <h1>Cuenta corriente de proveedores</h1>
      </div>
      <div className="filters">
        <label>
          Proveedor
          <select value={supplierId} onChange={(e) => setSupplierId(e.target.value)}>
            <option value="">Elegí un proveedor</option>
            {suppliers.data.map((s) => (
              <option key={s.id} value={s.id}>
                {s.businessName} ({s.cuit}){s.active ? '' : ' - inactivo'}
              </option>
            ))}
          </select>
        </label>
      </div>
      <PeriodFilter period={period} onChange={setPeriod} />
      {supplierId === '' && <p className="status">Elegí un proveedor para ver su cuenta corriente.</p>}
      {account.isError && <Feedback error={toUserMessage(account.error)} success={null} />}
      {account.isPending && supplierId !== '' && isComplete(period) && <p className="status">Cargando…</p>}
      {account.data && (
        <>
          <p className="balance">Saldo inicial: {money(account.data.openingBalance)}</p>
          <table>
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Comprobante</th>
                <th>Número</th>
                <th>Vencimiento</th>
                <th className="number">Debe</th>
                <th className="number">Haber</th>
                <th className="number">Saldo</th>
              </tr>
            </thead>
            <tbody>
              {account.data.movements.map((m) => (
                <tr key={m.voucherId}>
                  <td>{date(m.issueDate)}</td>
                  <td>{voucherTypeLabels[m.type]}</td>
                  <td>{m.formattedNumber}</td>
                  <td>{date(m.dueDate)}</td>
                  <td className="number">{m.debit ? money(m.debit) : ''}</td>
                  <td className="number">{m.credit ? money(m.credit) : ''}</td>
                  <td className="number">{money(m.balance)}</td>
                </tr>
              ))}
              {account.data.movements.length === 0 && (
                <tr>
                  <td colSpan={7} className="status">
                    No hay movimientos en el período.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          <p className="balance">Saldo final: {money(account.data.closingBalance)}</p>
        </>
      )}
    </section>
  )
}
