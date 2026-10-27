import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearSession } from '../../shared/api/httpClient'
import { mockApi, ok, renderApp } from '../../test/renderApp'

const zero = {
  netTaxed21: 0,
  netTaxed105: 0,
  netTaxed27: 0,
  vat21: 0,
  vat105: 0,
  vat27: 0,
  nonTaxed: 0,
  exempt: 0,
  vatPerceptions: 0,
  grossIncomePerceptions: 0,
  otherTaxes: 0,
}

const ledger = {
  from: '2026-10-01',
  to: '2026-10-31',
  rows: [
    {
      voucherId: 'v-1',
      issueDate: '2026-10-05',
      type: 'FACTURA_A',
      formattedNumber: '00001-00000002',
      supplierName: 'Granja Don Pedro SA',
      supplierCuit: '30-71234567-1',
      vatCondition: 'RESPONSABLE_INSCRIPTO',
      amounts: { ...zero, netTaxed21: 500, vat21: 105 },
      total: 605,
    },
    {
      voucherId: 'v-2',
      issueDate: '2026-10-07',
      type: 'NOTA_CREDITO_A',
      formattedNumber: '00001-00000001',
      supplierName: 'Granja Don Pedro SA',
      supplierCuit: '30-71234567-1',
      vatCondition: 'RESPONSABLE_INSCRIPTO',
      amounts: { ...zero, netTaxed21: -100, vat21: -21 },
      total: -121,
    },
  ],
  totals: { ...zero, netTaxed21: 400, vat21: 84 },
  total: 484,
}

const suppliers = [
  { id: 's-1', businessName: 'Granja Don Pedro SA', cuit: '30-71234567-1', vatCondition: 'RESPONSABLE_INSCRIPTO', active: true },
  { id: 's-3', businessName: 'Proveedor Viejo', cuit: '20-11111111-2', vatCondition: 'EXENTO', active: false },
]

const account = {
  supplierId: 's-1',
  supplierName: 'Granja Don Pedro SA',
  supplierCuit: '30-71234567-1',
  from: '2026-01-01',
  to: '2026-10-31',
  openingBalance: 1210,
  movements: [
    {
      voucherId: 'v-1',
      issueDate: '2026-10-05',
      dueDate: '2026-11-04',
      type: 'FACTURA_A',
      formattedNumber: '00001-00000002',
      debit: 0,
      credit: 605,
      balance: 1815,
    },
    {
      voucherId: 'v-2',
      issueDate: '2026-10-07',
      type: 'NOTA_CREDITO_A',
      formattedNumber: '00001-00000001',
      debit: 121,
      credit: 0,
      balance: 1694,
    },
  ],
  closingBalance: 1694,
}

describe('purchase reports', () => {
  beforeEach(() => {
    clearSession()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 9, 15, 12))
  })
  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('shows the VAT purchases ledger of the month with signed rows and totals', async () => {
    mockApi(['PURCHASES_VIEW'], {
      'GET /api/purchases/vat-ledger?from=2026-10-01&to=2026-10-31': ok(ledger),
    })

    renderApp('/compras/iva-compras')

    expect(await screen.findByRole('heading', { name: 'Subdiario de IVA Compras' })).toBeInTheDocument()
    const creditNote = await screen.findByRole('row', { name: /00001-00000001/ })
    expect(within(creditNote).getByText('Nota de Crédito A')).toBeInTheDocument()
    expect(within(creditNote).getByText('30-71234567-1')).toBeInTheDocument()
    expect(within(creditNote).getAllByText(/-\s?\$\s?121,00|\$\s?-121,00/)).not.toHaveLength(0)
    const totals = screen.getByRole('row', { name: /Totales/ })
    expect(within(totals).getByText(/^\$\s400,00$/)).toBeInTheDocument()
    expect(within(totals).getByText(/^\$\s84,00$/)).toBeInTheDocument()
    expect(within(totals).getByText(/^\$\s484,00$/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'IVA Compras' })).toBeInTheDocument()
  })

  it('shows the current account of the selected supplier from the start of the year', async () => {
    mockApi(['PURCHASES_VIEW', 'SUPPLIERS_VIEW'], {
      'GET /api/suppliers': ok(suppliers),
      'GET /api/purchases/suppliers/s-1/account?from=2026-01-01&to=2026-10-31': ok(account),
    })
    renderApp('/compras/cuenta-corriente')

    const supplier = await screen.findByLabelText('Proveedor')
    expect(within(supplier).getByRole('option', { name: /Proveedor Viejo/ })).toBeInTheDocument()
    expect(screen.getByText('Elegí un proveedor para ver su cuenta corriente.')).toBeInTheDocument()
    await userEvent.selectOptions(supplier, 'Granja Don Pedro SA (30-71234567-1)')

    expect(await screen.findByText(/Saldo inicial/)).toHaveTextContent('1.210,00')
    const invoice = screen.getByRole('row', { name: /00001-00000002/ })
    expect(within(invoice).getByText('04/11/2026')).toBeInTheDocument()
    expect(within(invoice).getByText(/605,00/)).toBeInTheDocument()
    expect(within(invoice).getByText(/1\.815,00/)).toBeInTheDocument()
    expect(screen.getByText(/Saldo final/)).toHaveTextContent('1.694,00')
    expect(screen.getByRole('link', { name: 'Cuenta corriente' })).toBeInTheDocument()
  })
})
