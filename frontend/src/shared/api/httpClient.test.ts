import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, clearSession, refreshSession, setSession } from './httpClient'
import { jsonResponse, sessionBody } from '../../test/fetchMocks'

describe('httpClient', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    clearSession()
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends the access token as a bearer header', async () => {
    setSession(sessionBody('token-1'))
    fetchMock.mockResolvedValueOnce(jsonResponse(200, [{ id: 1 }]))

    await apiFetch('/api/users')

    const [, init] = fetchMock.mock.calls[0]
    expect(new Headers(init.headers).get('Authorization')).toBe('Bearer token-1')
  })

  it('runs a single refresh request for concurrent callers', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, sessionBody('fresh')))

    const [a, b] = await Promise.all([refreshSession(), refreshSession()])

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(a?.accessToken).toBe('fresh')
    expect(b?.accessToken).toBe('fresh')
  })

  it('returns null when the refresh cookie is not valid', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(401, { detail: 'Invalid credentials' }))

    expect(await refreshSession()).toBeNull()
  })

  it('returns null when the backend cannot be reached', async () => {
    setSession(sessionBody('token-1'))
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))

    expect(await refreshSession()).toBeNull()
  })

  it('refreshes once and retries when the access token expired', async () => {
    setSession(sessionBody('expired'))
    fetchMock
      .mockResolvedValueOnce(jsonResponse(401, {}))
      .mockResolvedValueOnce(jsonResponse(200, sessionBody('renewed')))
      .mockResolvedValueOnce(jsonResponse(200, { ok: true }))

    const result = await apiFetch<{ ok: boolean }>('/api/users')

    expect(result).toEqual({ ok: true })
    const [, retryInit] = fetchMock.mock.calls[2]
    expect(new Headers(retryInit.headers).get('Authorization')).toBe('Bearer renewed')
  })

  it('throws an ApiError with the problem detail', async () => {
    setSession(sessionBody('token-1'))
    fetchMock.mockResolvedValueOnce(jsonResponse(409, { detail: 'Username already exists: jperez' }))

    await expect(apiFetch('/api/users', { method: 'POST' })).rejects.toEqual(
      new ApiError(409, 'Username already exists: jperez'),
    )
  })
})
