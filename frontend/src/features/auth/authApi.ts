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

export async function logout(): Promise<void> {
  try {
    await fetch('/api/auth/logout', { method: 'POST' })
  } finally {
    clearSession()
  }
}
