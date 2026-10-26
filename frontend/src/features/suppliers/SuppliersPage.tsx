import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { toUserMessage } from '../../shared/api/errorMessages'
import { Feedback } from '../../shared/ui/Feedback'
import { Can } from '../auth/Can'
import { SupplierForm } from './SupplierForm'
import { suppliersApi, vatConditionLabels, type Supplier, type SupplierInput } from './suppliersApi'

type Panel = { kind: 'none' } | { kind: 'create' } | { kind: 'edit'; supplier: Supplier }

function matches(supplier: Supplier, term: string) {
  const needle = term.trim().toLowerCase()
  if (!needle) {
    return true
  }
  const digits = needle.replace(/\D/g, '')
  return (
    supplier.businessName.toLowerCase().includes(needle) ||
    (digits.length > 0 && supplier.cuit.replace(/\D/g, '').includes(digits))
  )
}

function location(supplier: Supplier) {
  return [supplier.city, supplier.province].filter(Boolean).join(', ')
}

export function SuppliersPage() {
  const queryClient = useQueryClient()
  const suppliers = useQuery({ queryKey: ['suppliers'], queryFn: suppliersApi.list })
  const [panel, setPanel] = useState<Panel>({ kind: 'none' })
  const [search, setSearch] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const action = useMutation({
    mutationFn: (run: () => Promise<unknown>) => run(),
    onMutate: () => {
      setError(null)
      setSuccess(null)
    },
    onError: (e) => setError(toUserMessage(e)),
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['suppliers'] }),
  })

  function run(task: () => Promise<unknown>, message: string) {
    action.mutate(task, {
      onSuccess: () => {
        setSuccess(message)
        setPanel({ kind: 'none' })
      },
    })
  }

  function save(input: SupplierInput) {
    run(
      () => (panel.kind === 'edit' ? suppliersApi.update(panel.supplier.id, input) : suppliersApi.create(input)),
      'Proveedor guardado.',
    )
  }

  const visible = useMemo(() => (suppliers.data ?? []).filter((s) => matches(s, search)), [suppliers.data, search])

  if (suppliers.isPending) {
    return <p className="status">Cargando…</p>
  }
  if (suppliers.isError) {
    return <Feedback error={toUserMessage(suppliers.error)} success={null} />
  }

  return (
    <section>
      <div className="page-header">
        <h1>Proveedores</h1>
        <Can permission="SUPPLIERS_MANAGE">
          {panel.kind === 'none' && (
            <button type="button" onClick={() => setPanel({ kind: 'create' })}>
              Nuevo proveedor
            </button>
          )}
        </Can>
      </div>
      <Feedback error={error} success={success} />
      {panel.kind !== 'none' && (
        <SupplierForm
          key={panel.kind === 'edit' ? panel.supplier.id : 'new'}
          supplier={panel.kind === 'edit' ? panel.supplier : null}
          submitting={action.isPending}
          onSubmit={save}
          onCancel={() => setPanel({ kind: 'none' })}
        />
      )}
      <label className="search">
        Buscar
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Razón social o CUIT"
        />
      </label>
      <table>
        <thead>
          <tr>
            <th>Razón social</th>
            <th>CUIT</th>
            <th>Condición IVA</th>
            <th>Localidad</th>
            <th>Teléfono</th>
            <th>Estado</th>
            <th>
              <span className="sr-only">Acciones</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {visible.map((supplier) => (
            <tr key={supplier.id}>
              <td>{supplier.businessName}</td>
              <td>{supplier.cuit}</td>
              <td>{vatConditionLabels[supplier.vatCondition]}</td>
              <td>{location(supplier)}</td>
              <td>{supplier.phone}</td>
              <td>{supplier.active ? 'Activo' : 'Inactivo'}</td>
              <td className="actions">
                <Can permission="SUPPLIERS_MANAGE">
                  <button type="button" className="secondary" onClick={() => setPanel({ kind: 'edit', supplier })}>
                    Editar
                  </button>
                  {supplier.active ? (
                    <button
                      type="button"
                      className="secondary"
                      onClick={() =>
                        run(() => suppliersApi.deactivate(supplier.id), `${supplier.businessName} fue dado de baja.`)
                      }
                    >
                      Dar de baja
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="secondary"
                      onClick={() =>
                        run(() => suppliersApi.activate(supplier.id), `${supplier.businessName} fue reactivado.`)
                      }
                    >
                      Reactivar
                    </button>
                  )}
                </Can>
              </td>
            </tr>
          ))}
          {visible.length === 0 && (
            <tr>
              <td colSpan={7} className="status">
                No hay proveedores para mostrar.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </section>
  )
}
