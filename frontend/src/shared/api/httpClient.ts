export type AuthUser = {
  id: string
  username: string
  fullName: string
  permissions: string[]
}

export type Session = {
  accessToken: string
  expiresAt: string
  user: AuthUser
}

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

// The access token lives only in memory; the refresh token is an httpOnly cookie the browser sends itself.
let accessToken: string | null = null
let refreshInFlight: Promise<Session | null> | null = null
let sessionListener: (session: Session | null) => void = () => {}

export function onSessionChange(listener: (session: Session | null) => void) {
  sessionListener = listener
}

export function setSession(session: Session) {
  accessToken = session.accessToken
  sessionListener(session)
}

export function clearSession() {
  accessToken = null
  refreshInFlight = null
  sessionListener(null)
}

/**
 * Exchanges the refresh cookie for a new session. Concurrent callers share one request: the backend rotates
 * the refresh token and treats a second use of the old one as theft, revoking every session of the user.
 */
export function refreshSession(): Promise<Session | null> {
  if (!refreshInFlight) {
    refreshInFlight = fetch('/api/auth/refresh', { method: 'POST' })
      .then(async (response) => {
        if (!response.ok) {
          clearSession()
          return null
        }
        const session = (await response.json()) as Session
        setSession(session)
        return session
      })
      .finally(() => {
        refreshInFlight = null
      })
  }
  return refreshInFlight
}

async function toApiError(response: Response): Promise<ApiError> {
  let detail = response.statusText
  try {
    const body = await response.json()
    if (body && typeof body.detail === 'string') {
      detail = body.detail
    }
  } catch {
    // Body is empty or not JSON: keep the status text.
  }
  return new ApiError(response.status, detail)
}

function withAuth(init: RequestInit): RequestInit {
  const headers = new Headers(init.headers)
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  return { ...init, headers }
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response = await fetch(path, withAuth(init))
  if (response.status === 401 && accessToken) {
    const renewed = await refreshSession()
    if (renewed) {
      response = await fetch(path, withAuth(init))
    }
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}
