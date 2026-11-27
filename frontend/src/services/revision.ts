import type { BorradorRevision, ClienteRevision } from '../features/bfa/revision/tipos';
import type { PreguntasSubpruebaResponse } from '../features/bfa/api/tipos';
import type { ResultadoResponse } from '../features/resultados/tipos';
import { http } from './http';

const json = (cuerpo: unknown): RequestInit => ({ body: JSON.stringify(cuerpo) });

export class RevisionSinIdentificadoresError extends Error {}

/** Llamadas reales de BFA-FE-09 a /api/v1 (DOC-05). Se usan con VITE_USAR_MOCKS=false. */
export const servicioRevision: ClienteRevision = {
  async cargar(evaluacionId): Promise<BorradorRevision> {
    const [resultado, fv] = await Promise.all([
      http<ResultadoResponse>(`/evaluaciones/${evaluacionId}/resultados`),
      http<PreguntasSubpruebaResponse>(`/evaluaciones/${evaluacionId}/subpruebas/FV/preguntas`),
    ]);
    // E6 devuelve el orden, pero E10 requiere el identificador persistido.
    if (fv.palabras?.length) throw new RevisionSinIdentificadoresError();
    return {
      calificacionId: resultado.calificacionId,
      revision: resultado.revision,
      revisionVigente: resultado.revision,
      palabras: [],
    };
  },
  /** E10 */
  revisarPalabra: (evaluacionId, calificacionId, respuestaId, cuerpo) =>
    http(
      `/evaluaciones/${evaluacionId}/calificaciones/${calificacionId}/palabras/${respuestaId}/revision`,
      { method: 'PUT', ...json(cuerpo) },
    ),
  /** E11 */
  recalcular: (evaluacionId, cuerpo) =>
    http(`/evaluaciones/${evaluacionId}/recalculos`, { method: 'POST', ...json(cuerpo) }),
  /** E12 */
  publicar: (evaluacionId, cuerpo) =>
    http(`/evaluaciones/${evaluacionId}/resultados/vigente`, { method: 'PUT', ...json(cuerpo) }),
};
