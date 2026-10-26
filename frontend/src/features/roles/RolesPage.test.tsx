import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearSession } from '../../shared/api/httpClient'
import { callsTo, created, mockApi, noContent, ok, problem, renderApp } from '../../test/renderApp'

const permissions = [
  { code: 'ROLES_MANAGE', description: 'Create, edit and delete roles' },
  { code: 'ROLES_VIEW', description: 'View roles and permissions' },
  { code: 'USERS_VIEW', description: 'View users' },
]
const roles = [
  { id: 'r-1', name: 'Vendedor', permissionCodes: ['ROLES_VIEW'] },
  { id: 'r-2', name: 'Contador', permissionCodes: ['USERS_VIEW'] },
]

function row(name: string) {
  return screen.getByRole('row', { name: new RegExp(name) })
}

describe('RolesPage', () => {
  beforeEach(() => clearSession())
  afterEach(() => vi.unstubAllGlobals())

  it('lists roles with readable permission names', async () => {
    mockApi(['ROLES_VIEW'], { 'GET /api/roles': ok(roles), 'GET /api/permissions': ok(permissions) })

    renderApp('/roles')

    expect(await screen.findByRole('cell', { name: 'Vendedor' })).toBeInTheDocument()
    expect(within(row('Vendedor')).getByText('Ver roles y permisos')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Nuevo rol' })).not.toBeInTheDocument()
  })

  it('creates a role with the checked permissions', async () => {
    const fetchMock = mockApi(['ROLES_VIEW', 'ROLES_MANAGE'], {
      'GET /api/roles': ok(roles),
      'GET /api/permissions': ok(permissions),
      'POST /api/roles': created({ id: 'r-3', name: 'Auditor', permissionCodes: ['USERS_VIEW'] }),
    })
    renderApp('/roles')

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo rol' }))
    await userEvent.type(screen.getByLabelText('Nombre'), 'Auditor')
    await userEvent.click(screen.getByRole('checkbox', { name: 'Ver usuarios' }))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Rol guardado.')).toBeInTheDocument()
    expect(callsTo(fetchMock, 'POST', '/api/roles')).toEqual([{ name: 'Auditor', permissionCodes: ['USERS_VIEW'] }])
  })

  it('edits an existing role', async () => {
    const fetchMock = mockApi(['ROLES_VIEW', 'ROLES_MANAGE'], {
      'GET /api/roles': ok(roles),
      'GET /api/permissions': ok(permissions),
      'PUT /api/roles/r-1': ok({ id: 'r-1', name: 'Ventas', permissionCodes: ['ROLES_VIEW'] }),
    })
    renderApp('/roles')

    await userEvent.click(await within(await screen.findByRole('row', { name: /Vendedor/ })).findByRole('button', { name: 'Editar' }))
    const name = screen.getByLabelText('Nombre')
    await userEvent.clear(name)
    await userEvent.type(name, 'Ventas')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Rol guardado.')).toBeInTheDocument()
    expect(callsTo(fetchMock, 'PUT', '/api/roles/r-1')).toEqual([{ name: 'Ventas', permissionCodes: ['ROLES_VIEW'] }])
  })

  it('explains why a role in use cannot be deleted', async () => {
    mockApi(['ROLES_VIEW', 'ROLES_MANAGE'], {
      'GET /api/roles': ok(roles),
      'GET /api/permissions': ok(permissions),
      'DELETE /api/roles/r-1': problem(409, 'Role is assigned to users and cannot be deleted'),
    })
    renderApp('/roles')

    await userEvent.click(await within(await screen.findByRole('row', { name: /Vendedor/ })).findByRole('button', { name: 'Eliminar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El rol está asignado a usuarios y no se puede eliminar.',
    )
  })

  it('deletes an unused role', async () => {
    const fetchMock = mockApi(['ROLES_VIEW', 'ROLES_MANAGE'], {
      'GET /api/roles': ok(roles),
      'GET /api/permissions': ok(permissions),
      'DELETE /api/roles/r-2': noContent(),
    })
    renderApp('/roles')

    await userEvent.click(await within(await screen.findByRole('row', { name: /Contador/ })).findByRole('button', { name: 'Eliminar' }))

    expect(await screen.findByText('Rol eliminado.')).toBeInTheDocument()
    expect(callsTo(fetchMock, 'DELETE', '/api/roles/r-2')).toHaveLength(1)
  })
})
