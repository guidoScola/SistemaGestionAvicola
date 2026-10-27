import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { toUserMessage } from '../../shared/api/errorMessages'
import { Feedback } from '../../shared/ui/Feedback'
import { Can } from '../auth/Can'
import { currentMonth, date, isComplete, money } from './format'
import { PeriodFilter } from './PeriodFilter'
import { purchasesApi, voucherTypeLabels, type VoucherInput } from './purchasesApi'
import { VoucherForm } from './VoucherForm'

export function VouchersPage() {
  const queryClient = useQueryClient()
  const [period, setPeriod] = useState(currentMonth)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const vouchers = useQuery({
    queryKey: ['vouchers', period],
    queryFn: () => purchasesApi.listVouchers(period),
    enabled: isComplete(period),
  })

  const register = useMutation({
    mutationFn: (input: VoucherInput) => purchasesApi.registerVoucher(input),
    onMutate: () => {
      setError(null)
      setSuccess(null)
    },
    onError: (e) => setError(toUserMessage(e)),
    onSuccess: () => {
      setSuccess('Comprobante registrado.')
      setCreating(false)
      return queryClient.invalidateQueries({ queryKey: ['vouchers'] })
    },
  })

  return (
    <section>
      <div className="page-header">
        <h1>Comprobantes de proveedores</h1>
        <Can permission="PURCHASES_MANAGE">
          {!creating && (
            <button type="button" onClick={() => setCreating(true)}>
              Nuevo comprobante
            </button>
          )}
        </Can>
      </div>
      <Feedback error={error} success={success} />
      {creating && (
        <VoucherForm
          submitting={register.isPending}
          onSubmit={(input) => register.mutate(input)}
          onCancel={() => setCreating(false)}
        />
      )}
      <PeriodFilter period={period} onChange={setPeriod} />
      {vouchers.isError && <Feedback error={toUserMessage(vouchers.error)} success={null} />}
      {vouchers.isPending && isComplete(period) && <p className="status">Cargando…</p>}
      {vouchers.data && (
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Comprobante</th>
              <th>Número</th>
              <th>Proveedor</th>
              <th>CUIT</th>
              <th>Vencimiento</th>
              <th className="number">Total</th>
            </tr>
          </thead>
          <tbody>
            {vouchers.data.map((v) => (
              <tr key={v.id}>
                <td>{date(v.issueDate)}</td>
                <td>{voucherTypeLabels[v.type]}</td>
                <td>{v.formattedNumber}</td>
                <td>{v.supplierName}</td>
                <td>{v.supplierCuit}</td>
                <td>{date(v.dueDate)}</td>
                <td className="number">{money(v.total)}</td>
              </tr>
            ))}
            {vouchers.data.length === 0 && (
              <tr>
                <td colSpan={7} className="status">
                  No hay comprobantes en el período.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </section>
  )
}
