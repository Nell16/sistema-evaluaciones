import type {
  ClienteEvaluacion,
  EvaluacionResponse,
  IniciarEvaluacionRequest,
  PeriodoResponse,
  SujetoResponse,
  TestDetalleResponse,
} from '../features/evaluacion/tipos';
import { http } from './http';

/**
 * Prepara el cuerpo de E3 tal como lo valida el backend (IniciarEvaluacionRequest):
 * - `subpruebas` vacía responde 400 (`@Size(min = 1)`): se omite y la aplicación es completa.
 * - `fluidez.letraInicial` debe ser una sola letra: se recorta y se pasa a mayúscula; si queda
 *   vacía se omite y el backend decide (LETRA_NO_ASIGNADA si no hay asignación previa).
 */
export function cuerpoInicio(solicitud: IniciarEvaluacionRequest): IniciarEvaluacionRequest {
  const { subpruebas, fluidez, ...resto } = solicitud;
  const codigos = subpruebas?.map((c) => c.trim()).filter(Boolean);
  const letra = fluidez?.letraInicial.trim().toUpperCase();
  return {
    ...resto,
    ...(codigos?.length ? { subpruebas: codigos } : {}),
    ...(letra ? { fluidez: { letraInicial: letra } } : {}),
  };
}

/**
 * Llamadas reales de GEN-02 a /api/v1 (DOC-05). Se usan con VITE_USAR_MOCKS=false.
 * Los errores llegan como ApiError (services/http.ts).
 * GEN-35-FE: E3 y E4 verificados contra docs/api/openapi.yaml y el backend de main.
 */
export const servicioEvaluacion: ClienteEvaluacion = {
  /** E2 — configuraciones de la edición publicada. */
  obtenerTest: (testId) => http<TestDetalleResponse>(`/tests/${testId}`),

  /** E21 — solo períodos que admiten nuevas asignaciones. */
  listarPeriodosDisponibles: () => http<PeriodoResponse[]>('/periodos?disponible=true'),

  /** E19 — datos autorizados del evaluado. */
  obtenerSujeto: (sujetoId) => http<SujetoResponse>(`/sujetos/${sujetoId}`),

  /** E3 — crea la evaluación EN_CURSO; responde 201 (con Location) y la misma forma que E4. */
  iniciarEvaluacion: (solicitud: IniciarEvaluacionRequest) =>
    http<EvaluacionResponse>('/evaluaciones', {
      method: 'POST',
      body: JSON.stringify(cuerpoInicio(solicitud)),
    }),

  /** E4 — estado actual para retomar sin reiniciar el reloj. */
  obtenerEvaluacion: (evaluacionId) => http<EvaluacionResponse>(`/evaluaciones/${evaluacionId}`),
};
