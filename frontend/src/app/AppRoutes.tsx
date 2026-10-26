import { Route, Routes } from 'react-router'
import { LoginPage } from '../features/auth/LoginPage'
import { ProtectedRoute } from '../features/auth/ProtectedRoute'
import { RolesPage } from '../features/roles/RolesPage'
import { SuppliersPage } from '../features/suppliers/SuppliersPage'
import { MyAccountPage } from '../features/users/MyAccountPage'
import { UsersPage } from '../features/users/UsersPage'
import { AppLayout } from './AppLayout'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<h1>Inicio</h1>} />
        <Route
          path="usuarios"
          element={
            <ProtectedRoute permission="USERS_VIEW">
              <UsersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="roles"
          element={
            <ProtectedRoute permission="ROLES_VIEW">
              <RolesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="proveedores"
          element={
            <ProtectedRoute permission="SUPPLIERS_VIEW">
              <SuppliersPage />
            </ProtectedRoute>
          }
        />
        <Route path="mi-cuenta" element={<MyAccountPage />} />
      </Route>
    </Routes>
  )
}
