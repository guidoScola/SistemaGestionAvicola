import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { onSessionChange, refreshSession, type AuthUser } from '../../shared/api/httpClient'
import { AuthContext, type AuthStatus } from './AuthContext'
import * as authApi from './authApi'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<AuthUser | null>(null)

  useEffect(() => {
    onSessionChange((session) => {
      setUser(session?.user ?? null)
      setStatus(session ? 'authenticated' : 'anonymous')
    })
    void refreshSession()
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    await authApi.login(username, password)
  }, [])

  const logout = useCallback(async () => {
    await authApi.logout()
  }, [])

  const hasPermission = useCallback(
    (permission: string) => user?.permissions.includes(permission) ?? false,
    [user],
  )

  const value = useMemo(
    () => ({ status, user, login, logout, hasPermission }),
    [status, user, login, logout, hasPermission],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
