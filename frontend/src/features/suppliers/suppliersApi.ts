import { apiFetch } from '../../shared/api/httpClient'

export type VatCondition = 'RESPONSABLE_INSCRIPTO' | 'MONOTRIBUTISTA' | 'EXENTO' | 'NO_RESPONSABLE'

export const vatConditionLabels: Record<VatCondition, string> = {
  RESPONSABLE_INSCRIPTO: 'Responsable Inscripto',
  MONOTRIBUTISTA: 'Monotributista',
  EXENTO: 'Exento',
  NO_RESPONSABLE: 'No Responsable',
}

export type Supplier = {
  id: string
  businessName: string
  cuit: string
  vatCondition: VatCondition
  address?: string
  city?: string
  province?: string
  phone?: string
  email?: string
  active: boolean
}

export type SupplierInput = {
  businessName: string
  cuit: string
  vatCondition: VatCondition
  address: string
  city: string
  province: string
  phone: string
  email: string
}

export const suppliersApi = {
  list: () => apiFetch<Supplier[]>('/api/suppliers'),
  create: (input: SupplierInput) =>
    apiFetch<Supplier>('/api/suppliers', { method: 'POST', body: JSON.stringify(input) }),
  update: (id: string, input: SupplierInput) =>
    apiFetch<Supplier>(`/api/suppliers/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
  activate: (id: string) => apiFetch<void>(`/api/suppliers/${id}/activate`, { method: 'POST' }),
  deactivate: (id: string) => apiFetch<void>(`/api/suppliers/${id}/deactivate`, { method: 'POST' }),
}
