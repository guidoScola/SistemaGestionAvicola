import { createContext } from 'react'
import type { AuthUser } from '../../shared/api/httpClient'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

export type AuthContextValue = {
  status: AuthStatus
  user: AuthUser | null
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
  hasPermission: (permission: string) => boolean
}

export const AuthContext = createContext<AuthContextValue | null>(null)
