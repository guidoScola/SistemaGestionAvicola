import { apiFetch } from '../../shared/api/httpClient'

export type User = {
  id: string
  username: string
  fullName: string
  active: boolean
  superAdmin: boolean
  roleIds: string[]
}

export type CreateUserInput = {
  username: string
  fullName: string
  password: string
  roleIds: string[]
}

export type UpdateUserInput = {
  fullName: string
  roleIds: string[]
}

export const usersApi = {
  list: () => apiFetch<User[]>('/api/users'),
  create: (input: CreateUserInput) => apiFetch<User>('/api/users', { method: 'POST', body: JSON.stringify(input) }),
  update: (id: string, input: UpdateUserInput) =>
    apiFetch<User>(`/api/users/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
  activate: (id: string) => apiFetch<void>(`/api/users/${id}/activate`, { method: 'POST' }),
  deactivate: (id: string) => apiFetch<void>(`/api/users/${id}/deactivate`, { method: 'POST' }),
  resetPassword: (id: string, newPassword: string) =>
    apiFetch<void>(`/api/users/${id}/password`, { method: 'PUT', body: JSON.stringify({ newPassword }) }),
  changeOwnPassword: (currentPassword: string, newPassword: string) =>
    apiFetch<void>('/api/users/me/password', {
      method: 'PUT',
      body: JSON.stringify({ currentPassword, newPassword }),
    }),
}
