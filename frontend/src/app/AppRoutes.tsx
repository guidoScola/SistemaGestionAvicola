import { Route, Routes } from 'react-router'
import { LoginPage } from '../features/auth/LoginPage'
import { ProtectedRoute } from '../features/auth/ProtectedRoute'
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
              <h1>Usuarios</h1>
            </ProtectedRoute>
          }
        />
        <Route
          path="roles"
          element={
            <ProtectedRoute permission="ROLES_VIEW">
              <h1>Roles</h1>
            </ProtectedRoute>
          }
        />
      </Route>
    </Routes>
  )
}
