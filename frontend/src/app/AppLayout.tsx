import { useState } from 'react'
import { NavLink, Outlet } from 'react-router'
import { Can } from '../features/auth/Can'
import { useAuth } from '../features/auth/useAuth'

export function AppLayout() {
  const { user, logout } = useAuth()
  const [logoutError, setLogoutError] = useState<string | null>(null)

  async function handleLogout() {
    setLogoutError(null)
    try {
      await logout()
    } catch {
      setLogoutError('No se pudo cerrar la sesión. Revisá la conexión e intentá de nuevo.')
    }
  }

  return (
    <div className="layout">
      <header className="topbar">
        <strong>Sistema de Gestión Avícola</strong>
        <nav>
          <NavLink to="/">Inicio</NavLink>
          <Can permission="SUPPLIERS_VIEW">
            <NavLink to="/proveedores">Proveedores</NavLink>
          </Can>
          <Can permission="PURCHASES_VIEW">
            <NavLink to="/compras/comprobantes">Comprobantes</NavLink>
            <NavLink to="/compras/iva-compras">IVA Compras</NavLink>
            <Can permission="SUPPLIERS_VIEW">
              <NavLink to="/compras/cuenta-corriente">Cuenta corriente</NavLink>
            </Can>
          </Can>
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
        <button type="button" onClick={() => void handleLogout()}>
          Cerrar sesión
        </button>
      </header>
      {logoutError && (
        <p role="alert" className="error content">
          {logoutError}
        </p>
      )}
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
