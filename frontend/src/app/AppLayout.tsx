import { NavLink, Outlet } from 'react-router'
import { Can } from '../features/auth/Can'
import { useAuth } from '../features/auth/useAuth'

export function AppLayout() {
  const { user, logout } = useAuth()

  return (
    <div className="layout">
      <header className="topbar">
        <strong>Sistema de Gestión Avícola</strong>
        <nav>
          <NavLink to="/">Inicio</NavLink>
          <Can permission="USERS_VIEW">
            <NavLink to="/usuarios">Usuarios</NavLink>
          </Can>
          <Can permission="ROLES_VIEW">
            <NavLink to="/roles">Roles</NavLink>
          </Can>
        </nav>
        <NavLink to="/mi-cuenta" className="user">
          {user?.fullName}
        </NavLink>
        <button type="button" onClick={() => void logout()}>
          Cerrar sesión
        </button>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
