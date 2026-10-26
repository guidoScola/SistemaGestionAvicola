import { useState, type FormEvent } from 'react'
import type { Role } from '../roles/rolesApi'
import type { CreateUserInput, User } from './usersApi'

type UserFormProps = {
  user: User | null
  roles: Role[]
  submitting: boolean
  onSubmit: (input: CreateUserInput) => void
  onCancel: () => void
}

/** Create form when `user` is null; edit form (name and roles only) otherwise. */
export function UserForm({ user, roles, submitting, onSubmit, onCancel }: UserFormProps) {
  const [fullName, setFullName] = useState(user?.fullName ?? '')
  const [username, setUsername] = useState(user?.username ?? '')
  const [password, setPassword] = useState('')
  const [roleIds, setRoleIds] = useState<string[]>(user?.roleIds ?? [])

  function toggleRole(id: string) {
    setRoleIds((current) => (current.includes(id) ? current.filter((r) => r !== id) : [...current, id]))
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSubmit({ username, fullName, password, roleIds })
  }

  return (
    <form className="card" onSubmit={handleSubmit}>
      <h2>{user ? `Editar ${user.username}` : 'Nuevo usuario'}</h2>
      <label>
        Nombre completo
        <input value={fullName} onChange={(e) => setFullName(e.target.value)} required />
      </label>
      {!user && (
        <>
          <label>
            Usuario
            <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="off" required />
          </label>
          <label>
            Contraseña
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="new-password"
              required
            />
          </label>
        </>
      )}
      <fieldset>
        <legend>Roles</legend>
        {roles.map((role) => (
          <label key={role.id} className="check">
            <input type="checkbox" checked={roleIds.includes(role.id)} onChange={() => toggleRole(role.id)} />
            {role.name}
          </label>
        ))}
      </fieldset>
      <div className="actions">
        <button type="submit" disabled={submitting}>
          Guardar
        </button>
        <button type="button" className="secondary" onClick={onCancel}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
