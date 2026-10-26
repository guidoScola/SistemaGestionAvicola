import { ApiError, clearSession, setSession, type Session } from '../../shared/api/httpClient'

export async function login(username: string, password: string): Promise<Session> {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (!response.ok) {
    throw new ApiError(response.status, 'Login failed')
  }
  const session = (await response.json()) as Session
  setSession(session)
  return session
}

/**
 * Ends the session on the server first. If the server cannot be reached the local session is kept, because the
 * refresh cookie would still be valid and silently sign the user back in on the next page load.
 */
export async function logout(): Promise<void> {
  const response = await fetch('/api/auth/logout', { method: 'POST' })
  if (!response.ok) {
    throw new ApiError(response.status, 'Logout failed')
  }
  clearSession()
}
