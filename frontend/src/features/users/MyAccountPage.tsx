import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { toUserMessage } from '../../shared/api/errorMessages'
import { Feedback } from '../../shared/ui/Feedback'
import { usersApi } from './usersApi'

export function MyAccountPage() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const change = useMutation({
    mutationFn: () => usersApi.changeOwnPassword(currentPassword, newPassword),
    onSuccess: () => {
      setCurrentPassword('')
      setNewPassword('')
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    change.mutate()
  }

  return (
    <section>
      <h1>Mi cuenta</h1>
      <form className="card" onSubmit={handleSubmit}>
        <h2>Cambiar contraseña</h2>
        <label>
          Contraseña actual
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
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
        <Feedback
          error={change.isError ? toUserMessage(change.error) : null}
          success={change.isSuccess ? 'Contraseña actualizada.' : null}
        />
        <button type="submit" disabled={change.isPending}>
          Cambiar contraseña
        </button>
      </form>
    </section>
  )
}
