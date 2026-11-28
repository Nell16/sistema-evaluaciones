import type { BorradorRevision, ClienteRevision } from '../features/bfa/revision/tipos';
import type { PreguntasSubpruebaResponse } from '../features/bfa/api/tipos';
import type { ResultadoResponse } from '../features/resultados/tipos';
import { ApiError, http } from './http';

const json = (cuerpo: unknown): RequestInit => ({ body: JSON.stringify(cuerpo) });

/** Códigos propios del frontend para los casos en que la revisión real no se puede hacer todavía. */
export type CodigoBloqueoRevision =
  'REVISION_SIN_PALABRAS' | 'REVISION_SIN_IDENTIFICADORES' | 'REVISION_SIN_CALIFICACION';

export const MENSAJES_BLOQUEO: Record<CodigoBloqueoRevision, string> = {
  REVISION_SIN_PALABRAS:
    'No se pueden cargar las palabras de Fluidez Verbal: E6 solo responde al evaluado y mientras la subprueba está activa, y la revisión la hace el psicólogo con la evaluación finalizada. Falta acordar el endpoint con BFA-BE-31.',
  REVISION_SIN_IDENTIFICADORES:
    'Las palabras llegan sin el identificador guardado de cada respuesta que exige E10; no se registran decisiones para no afectar respuestas equivocadas.',
  REVISION_SIN_CALIFICACION:
    'Todavía no hay una calificación candidata. Cree primero una nueva revisión (recálculo).',
};

/**
 * Error con la forma del backend (§5.2.2) para que la pantalla lo muestre con su mensaje
 * sin cambiar `aErrorMostrable`: un código desconocido se muestra con `error.mensaje`.
 */
function bloqueo(codigo: CodigoBloqueoRevision, ruta: string): ApiError {
  return new ApiError(409, {
    timestamp: new Date().toISOString(),
    status: 409,
    codigo,
    mensaje: MENSAJES_BLOQUEO[codigo],
    ruta: `/api/v1${ruta}`,
  });
}

const esIdValido = (id: number | null | undefined): id is number =>
  typeof id === 'number' && Number.isInteger(id) && id > 0;

/** Llamadas reales de BFA-FE-09 a /api/v1 (DOC-05). Se usan con VITE_USAR_MOCKS=false. */
export const servicioRevision: ClienteRevision = {
  // No hay un endpoint de listado para revisar; se arma con E9 (revisión vigente) y E6 (palabras).
  // Contrato verificado (BFA-FE-31): E9 exige la evaluación finalizada (409 EVALUACION_NO_FINALIZADA),
  // pero E6 no sirve para revisar: responde 403 SIN_PERMISO al psicólogo (exige ITEMS_LEER, que
  // solo tiene SUJETO) y 409 SUBPRUEBA_NO_ACTIVA si la evaluación no está EN_CURSO. Además PalabraFV
  // no trae el respuestaId que E10 necesita. Hasta que BFA-BE-31 lo resuelva, la carga se detiene con
  // un mensaje claro en lugar de inventar identificadores a partir de ordenEntrada.
  // E9 va primero: si quien revisa no tiene acceso a la evaluación, ese error se muestra tal cual.
  async cargar(evaluacionId): Promise<BorradorRevision> {
    const rutaFv = `/evaluaciones/${evaluacionId}/subpruebas/FV/preguntas`;
    const resultado = await http<ResultadoResponse>(`/evaluaciones/${evaluacionId}/resultados`);
    const fv = await http<PreguntasSubpruebaResponse>(rutaFv).catch((e: unknown) => {
      const codigo = e instanceof ApiError ? e.error?.codigo : undefined;
      if (codigo === 'SUBPRUEBA_NO_ACTIVA' || codigo === 'SIN_PERMISO') {
        throw bloqueo('REVISION_SIN_PALABRAS', rutaFv);
      }
      throw e;
    });
    if (fv.palabras?.length) throw bloqueo('REVISION_SIN_IDENTIFICADORES', rutaFv);
    return {
      calificacionId: resultado.calificacionId,
      revision: resultado.revision,
      revisionVigente: resultado.revision,
      palabras: [],
    };
  },
  /** E10 */
  async revisarPalabra(evaluacionId, calificacionId, respuestaId, cuerpo) {
    const ruta = `/evaluaciones/${evaluacionId}/calificaciones/${calificacionId}/palabras/${respuestaId}/revision`;
    if (!esIdValido(calificacionId)) throw bloqueo('REVISION_SIN_CALIFICACION', ruta);
    if (!esIdValido(respuestaId)) throw bloqueo('REVISION_SIN_IDENTIFICADORES', ruta);
    return http(ruta, { method: 'PUT', ...json(cuerpo) });
  },
  /** E11 */
  recalcular: (evaluacionId, cuerpo) =>
    http(`/evaluaciones/${evaluacionId}/recalculos`, { method: 'POST', ...json(cuerpo) }),
  /** E12 */
  async publicar(evaluacionId, cuerpo) {
    const ruta = `/evaluaciones/${evaluacionId}/resultados/vigente`;
    if (!esIdValido(cuerpo.calificacionId)) throw bloqueo('REVISION_SIN_CALIFICACION', ruta);
    return http(ruta, { method: 'PUT', ...json(cuerpo) });
  },
};
