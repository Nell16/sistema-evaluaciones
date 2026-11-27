/// <reference types="vite/client" />
import { servicioAdministracion } from '../../services/administracion';
import { crearAdministracionMock } from '../../services/administracion.mock';
import type { ClienteAdministracion } from './tipos';

/** Elige mock o API real; con el backend integrado, la API real es el valor por defecto. */
export const clienteAdministracion: ClienteAdministracion =
  import.meta.env.VITE_USAR_MOCKS === 'true' ? crearAdministracionMock() : servicioAdministracion;
