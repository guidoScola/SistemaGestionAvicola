import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { vi } from 'vitest'
import { AppRoutes } from '../app/AppRoutes'
import { AuthProvider } from '../features/auth/AuthProvider'
import { emptyResponse, jsonResponse, sessionBody } from './fetchMocks'

type Handler = (body: unknown, url: string) => Response
export type Routes = Record<string, Handler>

/**
 * Installs a fetch mock keyed by "METHOD /path". The refresh endpoint returns a session with the given permissions.
 * Unmatched requests fail the test with a 500 so missing routes are obvious.
 */
export function mockApi(permissions: string[], routes: Routes) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
    const url = String(input)
    const key = `${init.method ?? 'GET'} ${url}`
    if (key === 'POST /api/auth/refresh') {
      return jsonResponse(200, sessionBody('token-1', permissions))
    }
    const handler = routes[key]
    if (!handler) {
      return jsonResponse(500, { detail: `Unmocked request: ${key}` })
    }
    return handler(init.body ? JSON.parse(String(init.body)) : undefined, url)
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

export function callsTo(fetchMock: ReturnType<typeof mockApi>, method: string, url: string) {
  return fetchMock.mock.calls
    .filter(([u, init]) => String(u) === url && ((init as RequestInit | undefined)?.method ?? 'GET') === method)
    .map(([, init]) => {
      const body = (init as RequestInit | undefined)?.body
      return body ? JSON.parse(String(body)) : undefined
    })
}

export function renderApp(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

export const ok = (body: unknown) => () => jsonResponse(200, body)
export const created = (body: unknown) => () => jsonResponse(201, body)
export const noContent = () => () => emptyResponse(204)
export const problem = (status: number, detail: string) => () => jsonResponse(status, { detail })
