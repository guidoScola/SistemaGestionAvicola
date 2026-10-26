import type { Role } from '../roles/rolesApi'
import type { User } from './usersApi'

type UsersTableProps = {
  users: User[]
  roles: Role[]
  canManage: (user: User) => boolean
  onEdit: (user: User) => void
  onToggleActive: (user: User) => void
  onResetPassword: (user: User) => void
}

export function UsersTable({ users, roles, canManage, onEdit, onToggleActive, onResetPassword }: UsersTableProps) {
  const roleName = (id: string) => roles.find((r) => r.id === id)?.name ?? '—'

  return (
    <table>
      <thead>
        <tr>
          <th>Nombre</th>
          <th>Usuario</th>
          <th>Roles</th>
          <th>Estado</th>
          <th>
            <span className="sr-only">Acciones</span>
          </th>
        </tr>
      </thead>
      <tbody>
        {users.map((user) => (
          <tr key={user.id}>
            <td>{user.fullName}</td>
            <td>{user.username}</td>
            <td>
              {user.superAdmin ? (
                <span className="badge">Super admin</span>
              ) : (
                user.roleIds.map((id) => (
                  <span key={id} className="tag">
                    {roleName(id)}
                  </span>
                ))
              )}
            </td>
            <td>{user.active ? 'Activo' : 'Inactivo'}</td>
            <td className="actions">
              {canManage(user) && (
                <>
                  <button type="button" className="secondary" onClick={() => onEdit(user)}>
                    Editar
                  </button>
                  <button type="button" className="secondary" onClick={() => onToggleActive(user)}>
                    {user.active ? 'Desactivar' : 'Activar'}
                  </button>
                  <button type="button" className="secondary" onClick={() => onResetPassword(user)}>
                    Resetear contraseña
                  </button>
                </>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
