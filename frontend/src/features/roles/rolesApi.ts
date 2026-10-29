import { apiFetch } from '../../shared/api/httpClient'

export type Role = {
  id: string
  name: string
  permissionCodes: string[]
}

export type Permission = {
  code: string
  description: string
}

export type RoleInput = {
  name: string
  permissionCodes: string[]
}

const labels: Record<string, string> = {
  USERS_VIEW: 'Ver usuarios',
  USERS_MANAGE: 'Gestionar usuarios',
  ROLES_VIEW: 'Ver roles y permisos',
  ROLES_MANAGE: 'Gestionar roles',
}

/** Spanish label for a permission code; falls back to the backend description for codes added later. */
export function permissionLabel(code: string, permissions: Permission[] = []): string {
  return labels[code] ?? permissions.find((p) => p.code === code)?.description ?? code
}

export const rolesApi = {
  list: () => apiFetch<Role[]>('/api/roles'),
  permissions: () => apiFetch<Permission[]>('/api/permissions'),
  create: (input: RoleInput) => apiFetch<Role>('/api/roles', { method: 'POST', body: JSON.stringify(input) }),
  update: (id: string, input: RoleInput) =>
    apiFetch<Role>(`/api/roles/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
  remove: (id: string) => apiFetch<void>(`/api/roles/${id}`, { method: 'DELETE' }),
}
