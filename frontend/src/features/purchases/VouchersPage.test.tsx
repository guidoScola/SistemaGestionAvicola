import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearSession } from '../../shared/api/httpClient'
import { callsTo, created, mockApi, ok, problem, renderApp } from '../../test/renderApp'

const OCTOBER = '/api/purchases/vouchers?from=2026-10-01&to=2026-10-31'

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

const voucher = {
  id: 'v-1',
  supplierId: 's-1',
  supplierName: 'Granja Don Pedro SA',
  supplierCuit: '30-71234567-1',
  type: 'FACTURA_A',
  issueDate: '2026-10-05',
  dueDate: '2026-11-04',
  pointOfSale: 3,
  number: 1234,
  formattedNumber: '00003-00001234',
  amounts: { ...zero, netTaxed21: 1000, vat21: 210 },
  total: 1210,
}

const suppliers = [
  { id: 's-1', businessName: 'Granja Don Pedro SA', cuit: '30-71234567-1', vatCondition: 'RESPONSABLE_INSCRIPTO', active: true },
  { id: 's-2', businessName: 'Forrajera Sur', cuit: '20-12345678-6', vatCondition: 'MONOTRIBUTISTA', active: true },
  { id: 's-3', businessName: 'Proveedor Viejo', cuit: '20-11111111-2', vatCondition: 'EXENTO', active: false },
]

describe('VouchersPage', () => {
  beforeEach(() => {
    clearSession()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 9, 15, 12))
  })
  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('is reachable from the menu only with PURCHASES_VIEW', async () => {
    mockApi(['SUPPLIERS_VIEW'], { 'GET /api/suppliers': ok(suppliers) })

    renderApp('/compras/comprobantes')

    expect(await screen.findByText('No tenés permiso para ver esta sección.')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Comprobantes' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'IVA Compras' })).not.toBeInTheDocument()
  })

  it('lists the vouchers of the current month', async () => {
    mockApi(['PURCHASES_VIEW'], { [`GET ${OCTOBER}`]: ok([voucher]) })

    renderApp('/compras/comprobantes')

    const row = await screen.findByRole('row', { name: /Granja Don Pedro SA/ })
    expect(within(row).getByText('05/10/2026')).toBeInTheDocument()
    expect(within(row).getByText('Factura A')).toBeInTheDocument()
    expect(within(row).getByText('00003-00001234')).toBeInTheDocument()
    expect(within(row).getByText(/1\.210,00/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Comprobantes' })).toBeInTheDocument()
    expect(screen.getByLabelText('Desde')).toHaveValue('2026-10-01')
    expect(screen.getByLabelText('Hasta')).toHaveValue('2026-10-31')
    expect(screen.queryByRole('button', { name: 'Nuevo comprobante' })).not.toBeInTheDocument()
  })

  it('reloads when the period changes', async () => {
    const fetchMock = mockApi(['PURCHASES_VIEW'], {
      [`GET ${OCTOBER}`]: ok([voucher]),
      'GET /api/purchases/vouchers?from=2026-09-01&to=2026-10-31': ok([]),
    })
    renderApp('/compras/comprobantes')
    await screen.findByRole('row', { name: /Granja Don Pedro SA/ })

    await userEvent.clear(screen.getByLabelText('Desde'))
    await userEvent.type(screen.getByLabelText('Desde'), '2026-09-01')

    expect(await screen.findByText('No hay comprobantes en el período.')).toBeInTheDocument()
    expect(callsTo(fetchMock, 'GET', '/api/purchases/vouchers?from=2026-09-01&to=2026-10-31')).toHaveLength(1)
  })

  it('registers an A invoice suggesting the VAT and showing the total', async () => {
    const fetchMock = mockApi(['PURCHASES_VIEW', 'PURCHASES_MANAGE', 'SUPPLIERS_VIEW'], {
      [`GET ${OCTOBER}`]: ok([]),
      'GET /api/suppliers': ok(suppliers),
      'POST /api/purchases/vouchers': created(voucher),
    })
    renderApp('/compras/comprobantes')

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo comprobante' }))
    const supplier = await screen.findByLabelText('Proveedor')
    expect(within(supplier).queryByRole('option', { name: /Proveedor Viejo/ })).not.toBeInTheDocument()
    await userEvent.selectOptions(supplier, 'Granja Don Pedro SA (30-71234567-1)')
    await userEvent.type(screen.getByLabelText('Fecha'), '2026-10-05')
    await userEvent.type(screen.getByLabelText('Vencimiento'), '2026-11-04')
    await userEvent.type(screen.getByLabelText('Punto de venta'), '3')
    await userEvent.type(screen.getByLabelText('Número'), '1234')
    await userEvent.type(screen.getByLabelText('Neto gravado 21%'), '1000')
    expect(screen.getByLabelText('IVA 21%')).toHaveValue(210)
    await userEvent.type(screen.getByLabelText('Percepciones de IVA'), '30.5')
    expect(screen.getByText(/Total:/)).toHaveTextContent('1.240,50')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Comprobante registrado.')).toBeInTheDocument()
    expect(callsTo(fetchMock, 'POST', '/api/purchases/vouchers')).toEqual([
      {
        supplierId: 's-1',
        type: 'FACTURA_A',
        issueDate: '2026-10-05',
        dueDate: '2026-11-04',
        pointOfSale: 3,
        number: 1234,
        amounts: { ...zero, netTaxed21: 1000, vat21: 210, vatPerceptions: 30.5 },
      },
    ])
  })

  it('hides VAT fields for B and C vouchers', async () => {
    mockApi(['PURCHASES_VIEW', 'PURCHASES_MANAGE', 'SUPPLIERS_VIEW'], {
      [`GET ${OCTOBER}`]: ok([]),
      'GET /api/suppliers': ok(suppliers),
    })
    renderApp('/compras/comprobantes')

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo comprobante' }))
    await screen.findByLabelText('Proveedor')
    await userEvent.selectOptions(screen.getByLabelText('Tipo'), 'Factura C')

    expect(screen.queryByLabelText('Neto gravado 21%')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('IVA 21%')).not.toBeInTheDocument()
    expect(screen.getByLabelText('No gravado')).toBeInTheDocument()
    expect(screen.getByLabelText('Exento')).toBeInTheDocument()
  })

  it('shows validation and duplicate errors in Spanish', async () => {
    mockApi(['PURCHASES_VIEW', 'PURCHASES_MANAGE', 'SUPPLIERS_VIEW'], {
      [`GET ${OCTOBER}`]: ok([]),
      'GET /api/suppliers': ok(suppliers),
      'POST /api/purchases/vouchers': problem(409, 'Voucher FACTURA_A 00003-00001234 is already registered for this supplier'),
    })
    renderApp('/compras/comprobantes')

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo comprobante' }))
    await userEvent.selectOptions(await screen.findByLabelText('Proveedor'), 'Granja Don Pedro SA (30-71234567-1)')
    await userEvent.type(screen.getByLabelText('Fecha'), '2026-10-05')
    await userEvent.type(screen.getByLabelText('Punto de venta'), '3')
    await userEvent.type(screen.getByLabelText('Número'), '1234')
    await userEvent.type(screen.getByLabelText('No gravado'), '100')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ese comprobante ya está cargado para este proveedor.')
  })
})
