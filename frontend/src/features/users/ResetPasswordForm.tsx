import { useState, type FormEvent } from 'react'

type ResetPasswordFormProps = {
  username: string
  submitting: boolean
  onSubmit: (newPassword: string) => void
  onCancel: () => void
}

export function ResetPasswordForm({ username, submitting, onSubmit, onCancel }: ResetPasswordFormProps) {
  const [newPassword, setNewPassword] = useState('')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSubmit(newPassword)
  }

  return (
    <form className="card" onSubmit={handleSubmit}>
      <h2>Resetear contraseña de {username}</h2>
      <label>
        Nueva contraseña
        <input
          type="password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          autoComplete="new-password"
          required
        />
      </label>
      <div className="actions">
        <button type="submit" disabled={submitting}>
          Confirmar
        </button>
        <button type="button" className="secondary" onClick={onCancel}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
