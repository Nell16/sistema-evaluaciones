/// <reference types="vite/client" />
import { servicioResultados } from '../../services/resultados';
import { crearClienteResultadosMock } from '../../services/resultados.mock';
import type { ClienteResultados } from './tipos';

/**
 * Punto único donde se elige la implementación de E9. Igual que en GEN-02: con
 * VITE_USAR_MOCKS=true en frontend/.env.local se usan datos simulados.
 */
export const usaMocksResultados = import.meta.env.VITE_USAR_MOCKS === 'true';

export const clienteResultados: ClienteResultados = usaMocksResultados
  ? crearClienteResultadosMock()
  : servicioResultados;
