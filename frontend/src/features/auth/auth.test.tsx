import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AppRoutes } from '../../app/AppRoutes'
import { clearSession } from '../../shared/api/httpClient'
import { emptyResponse, jsonResponse, sessionBody } from '../../test/fetchMocks'
import { AuthProvider } from './AuthProvider'
import { Can } from './Can'

function renderApp(path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('authentication flow', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    clearSession()
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends anonymous users to the login page', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(401, {}))

    renderApp('/usuarios')

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
  })

  it('shows an error for wrong credentials', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(401, {}))
      .mockResolvedValueOnce(jsonResponse(401, { detail: 'Invalid credentials' }))
    renderApp('/login')

    await userEvent.type(await screen.findByLabelText('Usuario'), 'jperez')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'wrong-pass')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Usuario o contraseña incorrectos')
  })

  it('logs in and shows only the menu entries the user is allowed to see', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(401, {}))
      .mockResolvedValueOnce(jsonResponse(200, sessionBody('token-1', ['ROLES_VIEW'])))
    renderApp('/login')

    await userEvent.type(await screen.findByLabelText('Usuario'), 'jperez')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'secret123')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByText('Juan Perez')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Roles' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument()
    const [url, init] = fetchMock.mock.calls[1]
    expect(url).toBe('/api/auth/login')
    expect(JSON.parse(init.body)).toEqual({ username: 'jperez', password: 'secret123' })
  })

  it('restores the session from the refresh cookie on load', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, sessionBody('token-1', ['USERS_VIEW'])))

    renderApp('/')

    expect(await screen.findByText('Juan Perez')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Usuarios' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('blocks a route the user has no permission for', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, sessionBody('token-1', ['ROLES_VIEW'])))

    renderApp('/usuarios')

    expect(await screen.findByText('No tenés permiso para ver esta sección.')).toBeInTheDocument()
  })

  it('shows the login page when the backend is down on load', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))

    renderApp('/')

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
  })

  it('keeps the session and warns when logout cannot reach the server', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200, sessionBody('token-1')))
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
    renderApp('/')

    await userEvent.click(await screen.findByRole('button', { name: 'Cerrar sesión' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cerrar la sesión. Revisá la conexión e intentá de nuevo.',
    )
    expect(screen.getByText('Juan Perez')).toBeInTheDocument()
  })

  it('logs out and returns to the login page', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200, sessionBody('token-1')))
      .mockResolvedValueOnce(emptyResponse(204))
    renderApp('/')

    await userEvent.click(await screen.findByRole('button', { name: 'Cerrar sesión' }))

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(fetchMock.mock.calls[1][0]).toBe('/api/auth/logout')
  })
})

describe('Can', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    clearSession()
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue(jsonResponse(200, sessionBody('token-1', ['USERS_VIEW'])))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders children only when the user holds the permission', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <Can permission="USERS_VIEW">visible</Can>
          <Can permission="USERS_MANAGE">hidden</Can>
        </AuthProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText('visible')).toBeInTheDocument()
    expect(screen.queryByText('hidden')).not.toBeInTheDocument()
  })
})
