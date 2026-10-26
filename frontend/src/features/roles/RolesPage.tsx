import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { toUserMessage } from '../../shared/api/errorMessages'
import { Feedback } from '../../shared/ui/Feedback'
import { Can } from '../auth/Can'
import { RoleForm } from './RoleForm'
import { permissionLabel, rolesApi, type Role, type RoleInput } from './rolesApi'

type Panel = { kind: 'none' } | { kind: 'create' } | { kind: 'edit'; role: Role }

export function RolesPage() {
  const queryClient = useQueryClient()
  const roles = useQuery({ queryKey: ['roles'], queryFn: rolesApi.list })
  const permissions = useQuery({ queryKey: ['permissions'], queryFn: rolesApi.permissions })
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
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['roles'] }),
  })

  function run(task: () => Promise<unknown>, message: string) {
    action.mutate(task, {
      onSuccess: () => {
        setSuccess(message)
        setPanel({ kind: 'none' })
      },
    })
  }

  function save(input: RoleInput) {
    run(
      () => (panel.kind === 'edit' ? rolesApi.update(panel.role.id, input) : rolesApi.create(input)),
      'Rol guardado.',
    )
  }

  if (roles.isPending || permissions.isPending) {
    return <p className="status">Cargando…</p>
  }
  if (roles.isError || permissions.isError) {
    return <Feedback error={toUserMessage(roles.error ?? permissions.error)} success={null} />
  }

  return (
    <section>
      <div className="page-header">
        <h1>Roles</h1>
        <Can permission="ROLES_MANAGE">
          {panel.kind === 'none' && (
            <button type="button" onClick={() => setPanel({ kind: 'create' })}>
              Nuevo rol
            </button>
          )}
        </Can>
      </div>
      <Feedback error={error} success={success} />
      {panel.kind !== 'none' && (
        <RoleForm
          key={panel.kind === 'edit' ? panel.role.id : 'new'}
          role={panel.kind === 'edit' ? panel.role : null}
          permissions={permissions.data}
          submitting={action.isPending}
          onSubmit={save}
          onCancel={() => setPanel({ kind: 'none' })}
        />
      )}
      <table>
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Permisos</th>
            <th>
              <span className="sr-only">Acciones</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {roles.data.map((role) => (
            <tr key={role.id}>
              <td>{role.name}</td>
              <td>
                {role.permissionCodes.map((code) => (
                  <span key={code} className="tag">
                    {permissionLabel(code, permissions.data)}
                  </span>
                ))}
              </td>
              <td className="actions">
                <Can permission="ROLES_MANAGE">
                  <button type="button" className="secondary" onClick={() => setPanel({ kind: 'edit', role })}>
                    Editar
                  </button>
                  <button
                    type="button"
                    className="secondary"
                    onClick={() => run(() => rolesApi.remove(role.id), 'Rol eliminado.')}
                  >
                    Eliminar
                  </button>
                </Can>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
