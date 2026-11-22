import { Ref } from './referencia.model';
import { Reserva } from './reserva.model';

export const METODOS_PAGO = ['Efectivo', 'Tarjeta de Crédito', 'Transferencia'] as const;
export const ESTADOS_PAGO = ['PENDIENTE', 'COMPLETADO', 'RECHAZADO'] as const;

export interface Pago {
  id?: number;
  reserva: Reserva;
  monto: number;
  metodoPago: string;
  estado: string;
  referencia?: string | null;
  transaccionId?: string | null;
}

// Respuesta de POST /api/cliente/pagos/wompi: el link firmado de la página de pago de Wompi
export interface CheckoutRespuesta {
  url: string;
  referencia: string;
}

export interface PagoDatos {
  reserva: Ref;
  monto: number;
  metodoPago: string;
  estado: string;
}
