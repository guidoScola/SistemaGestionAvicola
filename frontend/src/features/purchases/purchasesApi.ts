import { apiFetch } from '../../shared/api/httpClient'
import type { VatCondition } from '../suppliers/suppliersApi'

export type VoucherType =
  | 'FACTURA_A'
  | 'FACTURA_B'
  | 'FACTURA_C'
  | 'NOTA_DEBITO_A'
  | 'NOTA_DEBITO_B'
  | 'NOTA_DEBITO_C'
  | 'NOTA_CREDITO_A'
  | 'NOTA_CREDITO_B'
  | 'NOTA_CREDITO_C'

export const voucherTypeLabels: Record<VoucherType, string> = {
  FACTURA_A: 'Factura A',
  FACTURA_B: 'Factura B',
  FACTURA_C: 'Factura C',
  NOTA_DEBITO_A: 'Nota de Débito A',
  NOTA_DEBITO_B: 'Nota de Débito B',
  NOTA_DEBITO_C: 'Nota de Débito C',
  NOTA_CREDITO_A: 'Nota de Crédito A',
  NOTA_CREDITO_B: 'Nota de Crédito B',
  NOTA_CREDITO_C: 'Nota de Crédito C',
}

/** Only A vouchers show VAT separately. */
export function discriminatesVat(type: VoucherType) {
  return type.endsWith('_A')
}

export type Amounts = {
  netTaxed21: number
  netTaxed105: number
  netTaxed27: number
  vat21: number
  vat105: number
  vat27: number
  nonTaxed: number
  exempt: number
  vatPerceptions: number
  grossIncomePerceptions: number
  otherTaxes: number
}

export type Voucher = {
  id: string
  supplierId: string
  supplierName: string
  supplierCuit: string
  type: VoucherType
  issueDate: string
  dueDate?: string
  pointOfSale: number
  number: number
  formattedNumber: string
  amounts: Amounts
  total: number
}

export type VoucherInput = {
  supplierId: string
  type: VoucherType
  issueDate: string
  dueDate: string | null
  pointOfSale: number
  number: number
  amounts: Amounts
}

export type VatLedgerRow = {
  voucherId: string
  issueDate: string
  type: VoucherType
  formattedNumber: string
  supplierName: string
  supplierCuit: string
  vatCondition: VatCondition
  amounts: Amounts
  total: number
}

export type VatLedger = {
  from: string
  to: string
  rows: VatLedgerRow[]
  totals: Amounts
  total: number
}

export type AccountMovement = {
  voucherId: string
  issueDate: string
  dueDate?: string
  type: VoucherType
  formattedNumber: string
  debit: number
  credit: number
  balance: number
}

export type SupplierAccount = {
  supplierId: string
  supplierName: string
  supplierCuit: string
  from?: string
  to?: string
  openingBalance: number
  movements: AccountMovement[]
  closingBalance: number
}

export type Period = { from: string; to: string }

const range = ({ from, to }: Period) => `from=${from}&to=${to}`

export const purchasesApi = {
  listVouchers: (period: Period) => apiFetch<Voucher[]>(`/api/purchases/vouchers?${range(period)}`),
  registerVoucher: (input: VoucherInput) =>
    apiFetch<Voucher>('/api/purchases/vouchers', { method: 'POST', body: JSON.stringify(input) }),
  vatLedger: (period: Period) => apiFetch<VatLedger>(`/api/purchases/vat-ledger?${range(period)}`),
  supplierAccount: (supplierId: string, period: Period) =>
    apiFetch<SupplierAccount>(`/api/purchases/suppliers/${supplierId}/account?${range(period)}`),
}
