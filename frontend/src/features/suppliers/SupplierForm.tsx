import { useState, type FormEvent } from 'react'
import { vatConditionLabels, type Supplier, type SupplierInput, type VatCondition } from './suppliersApi'

type SupplierFormProps = {
  supplier: Supplier | null
  submitting: boolean
  onSubmit: (input: SupplierInput) => void
  onCancel: () => void
}

function initialValues(supplier: Supplier | null): SupplierInput {
  return {
    businessName: supplier?.businessName ?? '',
    cuit: supplier?.cuit ?? '',
    vatCondition: supplier?.vatCondition ?? 'RESPONSABLE_INSCRIPTO',
    address: supplier?.address ?? '',
    city: supplier?.city ?? '',
    province: supplier?.province ?? '',
    phone: supplier?.phone ?? '',
    email: supplier?.email ?? '',
  }
}

export function SupplierForm({ supplier, submitting, onSubmit, onCancel }: SupplierFormProps) {
  const [values, setValues] = useState<SupplierInput>(() => initialValues(supplier))

  function set<K extends keyof SupplierInput>(field: K, value: SupplierInput[K]) {
    setValues((current) => ({ ...current, [field]: value }))
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSubmit(values)
  }

  return (
    <form className="card" onSubmit={handleSubmit}>
      <h2>{supplier ? `Editar ${supplier.businessName}` : 'Nuevo proveedor'}</h2>
      <label>
        Razón social
        <input value={values.businessName} onChange={(e) => set('businessName', e.target.value)} required />
      </label>
      <label>
        CUIT
        <input
          value={values.cuit}
          onChange={(e) => set('cuit', e.target.value)}
          placeholder="30-12345678-9"
          inputMode="numeric"
          required
        />
      </label>
      <label>
        Condición frente al IVA
        <select value={values.vatCondition} onChange={(e) => set('vatCondition', e.target.value as VatCondition)}>
          {Object.entries(vatConditionLabels).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <label>
        Domicilio
        <input value={values.address} onChange={(e) => set('address', e.target.value)} />
      </label>
      <label>
        Localidad
        <input value={values.city} onChange={(e) => set('city', e.target.value)} />
      </label>
      <label>
        Provincia
        <input value={values.province} onChange={(e) => set('province', e.target.value)} />
      </label>
      <label>
        Teléfono
        <input value={values.phone} onChange={(e) => set('phone', e.target.value)} />
      </label>
      <label>
        Email
        <input type="email" value={values.email} onChange={(e) => set('email', e.target.value)} />
      </label>
      <div className="actions">
        <button type="submit" disabled={submitting}>
          Guardar
        </button>
        <button type="button" className="secondary" onClick={onCancel}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
