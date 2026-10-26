import { useState } from 'react'
import { Navigate, useLocation } from 'react-router'
import { ApiError } from '../../shared/api/httpClient'
import { LoginForm } from './LoginForm'
import { useAuth } from './useAuth'

export function LoginPage() {
  const { status, login } = useAuth()
  const location = useLocation()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (status === 'authenticated') {
    const from = (location.state as { from?: string } | null)?.from ?? '/'
    return <Navigate to={from} replace />
  }

  async function handleSubmit(username: string, password: string) {
    setSubmitting(true)
    setError(null)
    try {
      await login(username, password)
    } catch (e) {
      setError(
        e instanceof ApiError && e.status === 401
          ? 'Usuario o contraseña incorrectos'
          : 'No se pudo iniciar sesión. Intentá de nuevo.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <LoginForm onSubmit={handleSubmit} submitting={submitting} error={error} />
    </main>
  )
}
