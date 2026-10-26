import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearSession } from '../../shared/api/httpClient'
import { jsonResponse } from '../../test/fetchMocks'
import { callsTo, created, mockApi, noContent, ok, problem, renderApp } from '../../test/renderApp'

const roles = [
  { id: 'r-1', name: 'Vendedor', permissionCodes: ['ROLES_VIEW'] },
  { id: 'r-2', name: 'Contador', permissionCodes: ['USERS_VIEW'] },
]
const users = [
  { id: 'u-root', username: 'admin', fullName: 'Administrador', active: true, superAdmin: true, roleIds: [] },
  { id: 'u-2', username: 'mgomez', fullName: 'Maria Gomez', active: true, superAdmin: false, roleIds: ['r-1'] },
  { id: 'u-3', username: 'jlopez', fullName: 'Juan Lopez', active: false, superAdmin: false, roleIds: [] },
]

function row(name: string) {
  return screen.getByRole('row', { name: new RegExp(name) })
}

describe('UsersPage', () => {
  beforeEach(() => clearSession())
  afterEach(() => vi.unstubAllGlobals())

  it('lists users with their roles, status and the super admin badge', async () => {
    mockApi(['USERS_VIEW'], { 'GET /api/users': ok(users), 'GET /api/roles': ok(roles) })

    renderApp('/usuarios')

    expect(await screen.findByRole('cell', { name: 'Maria Gomez' })).toBeInTheDocument()
    expect(within(row('Maria Gomez')).getByText('Vendedor')).toBeInTheDocument()
    expect(within(row('Juan Lopez')).getByText('Inactivo')).toBeInTheDocument()
    expect(within(row('Administrador')).getByText('Super admin')).toBeInTheDocument()
  })

  it('hides management actions without USERS_MANAGE', async () => {
    mockApi(['USERS_VIEW'], { 'GET /api/users': ok(users), 'GET /api/roles': ok(roles) })

    renderApp('/usuarios')

    await screen.findByRole('cell', { name: 'Maria Gomez' })
    expect(screen.queryByRole('button', { name: 'Nuevo usuario' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument()
  })

  it('creates a user with the selected roles', async () => {
    const fetchMock = mockApi(['USERS_VIEW', 'USERS_MANAGE'], {
      'GET /api/users': ok(users),
      'GET /api/roles': ok(roles),
      'POST /api/users': created({ ...users[1], id: 'u-9', username: 'nuevo' }),
    })
    renderApp('/usuarios')

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo usuario' }))
    await userEvent.type(screen.getByLabelText('Nombre completo'), 'Nuevo Usuario')
    await userEvent.type(screen.getByLabelText('Usuario'), 'nuevo')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'secret123')
    await userEvent.click(screen.getByRole('checkbox', { name: 'Contador' }))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Usuario guardado.')).toBeInTheDocument()
    expect(callsTo(fetchMock, 'POST', '/api/users')).toEqual([
      { username: 'nuevo', fullName: 'Nuevo Usuario', password: 'secret123', roleIds: ['r-2'] },
    ])
  })

  it('shows backend rejections in Spanish', async () => {
    mockApi(['USERS_VIEW', 'USERS_MANAGE'], {
      'GET /api/users': ok(users),
      'GET /api/roles': ok(roles),
      'PUT /api/users/u-2': problem(403, 'You can only assign roles whose permissions you already hold'),
    })
    renderApp('/usuarios')

    await userEvent.click(await within(await screen.findByRole('row', { name: /Maria Gomez/ })).findByRole('button', { name: 'Editar' }))
    await userEvent.click(screen.getByRole('checkbox', { name: 'Contador' }))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Solo podés asignar roles cuyos permisos ya tenés.',
    )
  })

  it('does not offer actions on the super admin to non super admins', async () => {
    mockApi(['USERS_VIEW', 'USERS_MANAGE'], { 'GET /api/users': ok(users), 'GET /api/roles': ok(roles) })

    renderApp('/usuarios')

    await screen.findByRole('cell', { name: 'Maria Gomez' })
    expect(within(row('Administrador')).queryByRole('button')).not.toBeInTheDocument()
  })

  it('deactivates, activates and resets the password of a user', async () => {
    const fetchMock = mockApi(['USERS_VIEW', 'USERS_MANAGE'], {
      'GET /api/users': ok(users),
      'GET /api/roles': ok(roles),
      'POST /api/users/u-2/deactivate': noContent(),
      'POST /api/users/u-3/activate': noContent(),
      'PUT /api/users/u-2/password': noContent(),
    })
    renderApp('/usuarios')

    await userEvent.click(await within(await screen.findByRole('row', { name: /Maria Gomez/ })).findByRole('button', { name: 'Desactivar' }))
    await userEvent.click(within(row('Juan Lopez')).getByRole('button', { name: 'Activar' }))
    await userEvent.click(within(row('Maria Gomez')).getByRole('button', { name: 'Resetear contraseña' }))
    await userEvent.type(screen.getByLabelText('Nueva contraseña'), 'brand-new-1')
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar' }))

    expect(await screen.findByText('Contraseña reseteada. Se cerraron sus sesiones.')).toBeInTheDocument()
    expect(callsTo(fetchMock, 'POST', '/api/users/u-2/deactivate')).toHaveLength(1)
    expect(callsTo(fetchMock, 'POST', '/api/users/u-3/activate')).toHaveLength(1)
    expect(callsTo(fetchMock, 'PUT', '/api/users/u-2/password')).toEqual([{ newPassword: 'brand-new-1' }])
  })
})

describe('MyAccountPage', () => {
  beforeEach(() => clearSession())
  afterEach(() => vi.unstubAllGlobals())

  it('changes the own password', async () => {
    const fetchMock = mockApi([], { 'PUT /api/users/me/password': noContent() })
    renderApp('/mi-cuenta')

    await userEvent.type(await screen.findByLabelText('Contraseña actual'), 'secret123')
    await userEvent.type(screen.getByLabelText('Nueva contraseña'), 'brand-new-1')
    await userEvent.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))

    expect(await screen.findByText('Contraseña actualizada.')).toBeInTheDocument()
    expect(callsTo(fetchMock, 'PUT', '/api/users/me/password')).toEqual([
      { currentPassword: 'secret123', newPassword: 'brand-new-1' },
    ])
  })

  it('reports a wrong current password', async () => {
    mockApi([], { 'PUT /api/users/me/password': () => jsonResponse(400, { detail: 'Current password is incorrect' }) })
    renderApp('/mi-cuenta')

    await userEvent.type(await screen.findByLabelText('Contraseña actual'), 'wrong-pass')
    await userEvent.type(screen.getByLabelText('Nueva contraseña'), 'brand-new-1')
    await userEvent.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La contraseña actual no es correcta.')
  })
})
