import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { toUserMessage } from '../../shared/api/errorMessages'
import { Feedback } from '../../shared/ui/Feedback'
import { useAuth } from '../auth/useAuth'
import { rolesApi } from '../roles/rolesApi'
import { ResetPasswordForm } from './ResetPasswordForm'
import { UserForm } from './UserForm'
import { UsersTable } from './UsersTable'
import { usersApi, type CreateUserInput, type User } from './usersApi'

type Panel = { kind: 'none' } | { kind: 'create' } | { kind: 'edit'; user: User } | { kind: 'reset'; user: User }

export function UsersPage() {
  const { user: me, hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const users = useQuery({ queryKey: ['users'], queryFn: usersApi.list })
  const roles = useQuery({ queryKey: ['roles'], queryFn: rolesApi.list })
  const [panel, setPanel] = useState<Panel>({ kind: 'none' })
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const action = useMutation({
    mutationFn: (run: () => Promise<unknown>) => run(),
    onMutate: () => {
      setError(null)
      setSuccess(null)
    },
    onError: (e) => setError(toUserMessage(e)),
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  })

  function run(task: () => Promise<unknown>, message: string) {
    action.mutate(task, {
      onSuccess: () => {
        setSuccess(message)
        setPanel({ kind: 'none' })
      },
    })
  }

  function save(input: CreateUserInput) {
    if (panel.kind === 'edit') {
      run(() => usersApi.update(panel.user.id, { fullName: input.fullName, roleIds: input.roleIds }), 'Usuario guardado.')
    } else {
      run(() => usersApi.create(input), 'Usuario guardado.')
    }
  }

  const manage = hasPermission('USERS_MANAGE')
  // The backend only lets a super admin manage a super admin; do not offer what will be rejected.
  const canManage = (target: User) => manage && (!target.superAdmin || target.id === me?.id)

  if (users.isPending || roles.isPending) {
    return <p className="status">Cargando…</p>
  }
  if (users.isError || roles.isError) {
    return <Feedback error={toUserMessage(users.error ?? roles.error)} success={null} />
  }

  return (
    <section>
      <div className="page-header">
        <h1>Usuarios</h1>
        {manage && panel.kind === 'none' && (
          <button type="button" onClick={() => setPanel({ kind: 'create' })}>
            Nuevo usuario
          </button>
        )}
      </div>
      <Feedback error={error} success={success} />
      {(panel.kind === 'create' || panel.kind === 'edit') && (
        <UserForm
          key={panel.kind === 'edit' ? panel.user.id : 'new'}
          user={panel.kind === 'edit' ? panel.user : null}
          roles={roles.data}
          submitting={action.isPending}
          onSubmit={save}
          onCancel={() => setPanel({ kind: 'none' })}
        />
      )}
      {panel.kind === 'reset' && (
        <ResetPasswordForm
          username={panel.user.username}
          submitting={action.isPending}
          onSubmit={(newPassword) =>
            run(
              () => usersApi.resetPassword(panel.user.id, newPassword),
              'Contraseña reseteada. Se cerraron sus sesiones.',
            )
          }
          onCancel={() => setPanel({ kind: 'none' })}
        />
      )}
      <UsersTable
        users={users.data}
        roles={roles.data}
        canManage={canManage}
        onEdit={(user) => setPanel({ kind: 'edit', user })}
        onToggleActive={(user) =>
          user.active
            ? run(() => usersApi.deactivate(user.id), `${user.fullName} fue desactivado.`)
            : run(() => usersApi.activate(user.id), `${user.fullName} fue activado.`)
        }
        onResetPassword={(user) => setPanel({ kind: 'reset', user })}
      />
    </section>
  )
}
