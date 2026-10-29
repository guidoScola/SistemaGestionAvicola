import type { Session } from '../shared/api/httpClient'

export function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

export function emptyResponse(status: number): Response {
  return new Response(null, { status })
}

export function sessionBody(accessToken: string, permissions: string[] = []): Session {
  return {
    accessToken,
    expiresAt: '2026-10-26T12:15:00Z',
    user: { id: 'u-1', username: 'jperez', fullName: 'Juan Perez', permissions },
  }
}
