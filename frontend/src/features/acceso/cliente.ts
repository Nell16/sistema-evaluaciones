import { crearClienteAccesoMock } from '../../services/acceso.mock';
import { http } from '../../services/http';
import type { ClienteAcceso, CuentaResponse, SesionResponse } from './tipos';

/** E13–E17 con sesiones Bearer del backend. */
const clienteAccesoReal: ClienteAcceso = {
  registrar: (solicitud) =>
    http<CuentaResponse>('/auth/registro', {
      method: 'POST',
      body: JSON.stringify(solicitud),
    }),
  iniciarSesion: (solicitud) =>
    http<SesionResponse>('/auth/sesion', {
      method: 'POST',
      body: JSON.stringify(solicitud),
    }),
  cerrarSesion: () => http<void>('/auth/sesion', { method: 'DELETE' }),
  pedirRecuperacion: (solicitud) =>
    http<void>('/auth/recuperacion', {
      method: 'POST',
      body: JSON.stringify(solicitud),
    }),
  confirmarRecuperacion: (solicitud) =>
    http<void>('/auth/recuperacion/confirmar', {
      method: 'POST',
      body: JSON.stringify(solicitud),
    }),
};

export const clienteAcceso: ClienteAcceso =
  import.meta.env.VITE_USAR_MOCKS === 'true' ? crearClienteAccesoMock() : clienteAccesoReal;
