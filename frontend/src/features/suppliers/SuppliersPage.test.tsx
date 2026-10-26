import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearSession } from '../../shared/api/httpClient'
import { callsTo, created, mockApi, noContent, ok, problem, renderApp } from '../../test/renderApp'

const suppliers = [
  {
    id: 's-1',
    businessName: 'Granja Don Pedro SA',
    cuit: '30-71234567-1',
    vatCondition: 'RESPONSABLE_INSCRIPTO',
    city: 'Chascomús',
    province: 'Buenos Aires',
    phone: '02241-123456',
    active: true,
  },
  { id: 's-2', businessName: 'Forrajera Sur', cuit: '20-12345678-6', vatCondition: 'MONOTRIBUTISTA', active: false },
]

function row(name: string) {
  return screen.getByRole('row', { name: new RegExp(name) })
}

describe('SuppliersPage', () => {
  beforeEach(() => clearSession())
  afterEach(() => vi.unstubAllGlobals())

  it('is reachable from the menu only with SUPPLIERS_VIEW', async () => {
    mockApi(['USERS_VIEW'], {})

    renderApp('/proveedores')

    expect(await screen.findByText('No tenés permiso para ver esta sección.')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Proveedores' })).not.toBeInTheDocument()
  })

  it('lists suppliers with CUIT, VAT condition and status', async () => {
    mockApi(['SUPPLIERS_VIEW'], { 'GET /api/suppliers': ok(suppliers) })

    renderApp('/proveedores')

    expect(await screen.findByRole('cell', { name: 'Granja Don Pedro SA' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Proveedores' })).toBeInTheDocument()
    expect(within(row('Granja Don Pedro')).getByText('30-71234567-1')).toBeInTheDocument()
    expect(within(row('Granja Don Pedro')).getByText('Responsable Inscripto')).toBeInTheDocument()
    expect(within(row('Granja Don Pedro')).getByText('Chascomús, Buenos Aires')).toBeInTheDocument()
    expect(within(row('Forrajera Sur')).getByText('Inactivo')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Nuevo proveedor' })).not.toBeInTheDocument()
  })

  it('filters by business name or CUIT', async () => {
    mockApi(['SUPPLIERS_VIEW'], { 'GET /api/suppliers': ok(suppliers) })
    renderApp('/proveedores')
    await screen.findByRole('cell', { name: 'Granja Don Pedro SA' })

    await userEvent.type(screen.getByLabelText('Buscar'), '20-1234')

    expect(screen.queryByRole('cell', { name: 'Granja Don Pedro SA' })).not.toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'Forrajera Sur' })).toBeInTheDocument()
  })

  it('creates a supplier', async () => {
    const fetchMock = mockApi(['SUPPLIERS_VIEW', 'SUPPLIERS_MANAGE'], {
      'GET /api/suppliers': ok(suppliers),
      'POST /api/suppliers': created({ ...suppliers[0], id: 's-9' }),
    })
    renderApp('/proveedores')

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo proveedor' }))
    await userEvent.type(screen.getByLabelText('Razón social'), 'Avícola del Sur')
    await userEvent.type(screen.getByLabelText('CUIT'), '30-50000000-3')
    await userEvent.selectOptions(screen.getByLabelText('Condición frente al IVA'), 'Exento')
    await userEvent.type(screen.getByLabelText('Localidad'), 'Dolores')
    await userEvent.type(screen.getByLabelText('Email'), 'compras@avisur.com')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Proveedor guardado.')).toBeInTheDocument()
    expect(callsTo(fetchMock, 'POST', '/api/suppliers')).toEqual([
      {
        businessName: 'Avícola del Sur',
        cuit: '30-50000000-3',
        vatCondition: 'EXENTO',
        address: '',
        city: 'Dolores',
        province: '',
        phone: '',
        email: 'compras@avisur.com',
      },
    ])
  })

  it('shows an invalid CUIT error in Spanish', async () => {
    mockApi(['SUPPLIERS_VIEW', 'SUPPLIERS_MANAGE'], {
      'GET /api/suppliers': ok(suppliers),
      'POST /api/suppliers': problem(400, 'Invalid CUIT'),
    })
    renderApp('/proveedores')

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo proveedor' }))
    await userEvent.type(screen.getByLabelText('Razón social'), 'Avícola del Sur')
    await userEvent.type(screen.getByLabelText('CUIT'), '30-50000000-4')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El CUIT no es válido.')
  })

  it('edits a supplier keeping the loaded values', async () => {
    const fetchMock = mockApi(['SUPPLIERS_VIEW', 'SUPPLIERS_MANAGE'], {
      'GET /api/suppliers': ok(suppliers),
      'PUT /api/suppliers/s-1': ok(suppliers[0]),
    })
    renderApp('/proveedores')

    await userEvent.click(
      await within(await screen.findByRole('row', { name: /Granja Don Pedro/ })).findByRole('button', {
        name: 'Editar',
      }),
    )
    await userEvent.clear(screen.getByLabelText('Teléfono'))
    await userEvent.type(screen.getByLabelText('Teléfono'), '02241-999999')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Proveedor guardado.')).toBeInTheDocument()
    expect(callsTo(fetchMock, 'PUT', '/api/suppliers/s-1')).toEqual([
      {
        businessName: 'Granja Don Pedro SA',
        cuit: '30-71234567-1',
        vatCondition: 'RESPONSABLE_INSCRIPTO',
        address: '',
        city: 'Chascomús',
        province: 'Buenos Aires',
        phone: '02241-999999',
        email: '',
      },
    ])
  })

  it('deactivates and reactivates suppliers', async () => {
    const fetchMock = mockApi(['SUPPLIERS_VIEW', 'SUPPLIERS_MANAGE'], {
      'GET /api/suppliers': ok(suppliers),
      'POST /api/suppliers/s-1/deactivate': noContent(),
      'POST /api/suppliers/s-2/activate': noContent(),
    })
    renderApp('/proveedores')

    await userEvent.click(
      await within(await screen.findByRole('row', { name: /Granja Don Pedro/ })).findByRole('button', {
        name: 'Dar de baja',
      }),
    )
    await userEvent.click(within(row('Forrajera Sur')).getByRole('button', { name: 'Reactivar' }))

    expect(await screen.findByText('Forrajera Sur fue reactivado.')).toBeInTheDocument()
    expect(callsTo(fetchMock, 'POST', '/api/suppliers/s-1/deactivate')).toHaveLength(1)
    expect(callsTo(fetchMock, 'POST', '/api/suppliers/s-2/activate')).toHaveLength(1)
  })
})
