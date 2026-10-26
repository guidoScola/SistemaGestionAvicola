import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from './useAuth'

type ProtectedRouteProps = {
  children: ReactNode
  permission?: string
}

export function ProtectedRoute({ children, permission }: ProtectedRouteProps) {
  const { status, hasPermission } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return <p className="status">Cargando…</p>
  }
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (permission && !hasPermission(permission)) {
    return <p className="status">No tenés permiso para ver esta sección.</p>
  }
  return children
}
