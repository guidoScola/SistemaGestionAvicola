import { ApiError } from './httpClient'

// Backend problem details are in English; the UI shows them in Spanish.
const exact: Record<string, string> = {
  'Invalid CUIT': 'El CUIT no es válido.',
  'Invalid email': 'El email no es válido.',
  'Business name is required': 'La razón social es obligatoria.',
  'VAT condition is required': 'La condición frente al IVA es obligatoria.',
  'Unknown supplier': 'El proveedor no existe.',
  'Supplier is inactive': 'El proveedor está dado de baja.',
  'Only A vouchers discriminate VAT': 'Solo los comprobantes A discriminan IVA.',
  'VAT 21% does not match its net taxed amount': 'El IVA 21% no coincide con su neto gravado.',
  'VAT 10.5% does not match its net taxed amount': 'El IVA 10,5% no coincide con su neto gravado.',
  'VAT 27% does not match its net taxed amount': 'El IVA 27% no coincide con su neto gravado.',
  'VAT 21% requires a net taxed amount': 'El IVA 21% requiere un neto gravado.',
  'VAT 10.5% requires a net taxed amount': 'El IVA 10,5% requiere un neto gravado.',
  'VAT 27% requires a net taxed amount': 'El IVA 27% requiere un neto gravado.',
  'Amounts must not be negative': 'Los importes no pueden ser negativos.',
  'Amounts must have at most 2 decimals': 'Los importes pueden tener hasta 2 decimales.',
  'Amounts must have at most 13 integer digits': 'Los importes pueden tener hasta 13 dígitos enteros.',
  'Voucher total must be greater than zero': 'El total del comprobante debe ser mayor a cero.',
  'Point of sale must be between 1 and 99999': 'El punto de venta debe estar entre 1 y 99999.',
  'Number must be between 1 and 99999999': 'El número debe estar entre 1 y 99999999.',
  'Due date must not be before the issue date': 'El vencimiento no puede ser anterior a la fecha del comprobante.',
  'Start date must not be after end date': 'La fecha desde no puede ser posterior a la fecha hasta.',
  'Date range is required': 'Indicá el rango de fechas.',
  'Password must be at least 8 characters': 'La contraseña debe tener al menos 8 caracteres.',
  'Current password is incorrect': 'La contraseña actual no es correcta.',
  'One or more roles do not exist': 'Alguno de los roles seleccionados no existe.',
  'One or more permissions do not exist': 'Alguno de los permisos seleccionados no existe.',
  'Role is assigned to users and cannot be deleted': 'El rol está asignado a usuarios y no se puede eliminar.',
  'You can only assign roles whose permissions you already hold': 'Solo podés asignar roles cuyos permisos ya tenés.',
  'You can only grant permissions you already hold': 'Solo podés otorgar permisos que ya tenés.',
  'Only a super administrator can manage a super administrator':
    'Solo un super administrador puede modificar a otro super administrador.',
  'You cannot reset the password of a user with permissions you do not hold':
    'No podés resetear la contraseña de un usuario con permisos que vos no tenés.',
  'The last active super administrator cannot be deactivated':
    'No se puede desactivar al último super administrador activo.',
  'Full name is required and must be at most 100 characters':
    'El nombre completo es obligatorio y puede tener hasta 100 caracteres.',
  'Role name is required and must be at most 50 characters':
    'El nombre del rol es obligatorio y puede tener hasta 50 caracteres.',
  'Username must be 3-50 characters: letters, digits, dot, underscore or hyphen':
    'El usuario debe tener entre 3 y 50 caracteres: letras, números, punto, guion o guion bajo.',
}

const prefixes: Array<[string, string]> = [
  ['Username already exists', 'Ese nombre de usuario ya existe.'],
  ['Role name already exists', 'Ya existe un rol con ese nombre.'],
  ['A supplier with CUIT', 'Ya existe un proveedor con ese CUIT.'],
]

const suffixes: Array<[string, string]> = [
  ['is already registered for this supplier', 'Ese comprobante ya está cargado para este proveedor.'],
]

export function toUserMessage(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return 'No se pudo conectar con el servidor.'
  }
  const translated =
    exact[error.message] ??
    prefixes.find(([prefix]) => error.message.startsWith(prefix))?.[1] ??
    suffixes.find(([suffix]) => error.message.endsWith(suffix))?.[1]
  if (translated) {
    return translated
  }
  if (error.status === 403) {
    return 'No tenés permiso para realizar esta acción.'
  }
  if (error.status === 404) {
    return 'El registro ya no existe.'
  }
  return 'No se pudo completar la operación.'
}
