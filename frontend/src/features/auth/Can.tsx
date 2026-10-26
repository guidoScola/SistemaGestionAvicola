import type { ReactNode } from 'react'
import { useAuth } from './useAuth'

type CanProps = {
  permission: string
  children: ReactNode
  fallback?: ReactNode
}

/** Renders its children only when the signed-in user holds the permission. */
export function Can({ permission, children, fallback = null }: CanProps) {
  const { hasPermission } = useAuth()
  return hasPermission(permission) ? children : fallback
}
