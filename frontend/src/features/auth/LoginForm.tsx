import { useState, type FormEvent } from 'react'

type LoginFormProps = {
  onSubmit: (username: string, password: string) => void
  submitting: boolean
  error: string | null
}

export function LoginForm({ onSubmit, submitting, error }: LoginFormProps) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSubmit(username, password)
  }

  return (
    <form className="card login-form" onSubmit={handleSubmit}>
      <h1>Iniciar sesión</h1>
      <label>
        Usuario
        <input
          name="username"
          autoComplete="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />
      </label>
      <label>
        Contraseña
        <input
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
      </label>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      <button type="submit" disabled={submitting}>
        {submitting ? 'Ingresando…' : 'Ingresar'}
      </button>
    </form>
  )
}
