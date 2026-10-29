import { useState, type FormEvent } from 'react'
import { permissionLabel, type Permission, type Role, type RoleInput } from './rolesApi'

type RoleFormProps = {
  role: Role | null
  permissions: Permission[]
  submitting: boolean
  onSubmit: (input: RoleInput) => void
  onCancel: () => void
}

export function RoleForm({ role, permissions, submitting, onSubmit, onCancel }: RoleFormProps) {
  const [name, setName] = useState(role?.name ?? '')
  const [codes, setCodes] = useState<string[]>(role?.permissionCodes ?? [])

  function toggle(code: string) {
    setCodes((current) => (current.includes(code) ? current.filter((c) => c !== code) : [...current, code]))
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSubmit({ name, permissionCodes: codes })
  }

  return (
    <form className="card" onSubmit={handleSubmit}>
      <h2>{role ? `Editar ${role.name}` : 'Nuevo rol'}</h2>
      <label>
        Nombre
        <input value={name} onChange={(e) => setName(e.target.value)} required />
      </label>
      <fieldset>
        <legend>Permisos</legend>
        {permissions.map((permission) => (
          <label key={permission.code} className="check">
            <input
              type="checkbox"
              checked={codes.includes(permission.code)}
              onChange={() => toggle(permission.code)}
            />
            {permissionLabel(permission.code, permissions)}
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
