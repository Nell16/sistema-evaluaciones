/// <reference types="vite/client" />
import { servicioEvaluacion } from '../../services/evaluacion';
import { crearClienteMock } from '../../services/evaluacion.mock';
import type { ClienteEvaluacion } from './tipos';

/**
 * Punto único donde se elige la implementación de la API de evaluación.
 * Para usar datos simulados, configura VITE_USAR_MOCKS=true.
 */
export const usaMocks = import.meta.env.VITE_USAR_MOCKS === 'true';

export const clienteEvaluacion: ClienteEvaluacion = usaMocks
  ? crearClienteMock()
  : servicioEvaluacion;
