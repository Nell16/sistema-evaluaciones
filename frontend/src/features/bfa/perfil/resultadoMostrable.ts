import { tieneRevisionPublicada } from '../../resultados/presentacion';
import type { ResultadoResponse } from '../../resultados/tipos';

/**
 * Regla de BFA-FE-08: los factores solo se presentan con una revisión publicada. Es decir,
 * estado DISPONIBLE, la revisión identificada por el backend (calificacionId y revision) y
 * factores recibidos. No calcula nada: solo decide qué parte de lo recibido se muestra.
 */
export const puedeMostrarFactores = (resultado: ResultadoResponse): boolean =>
  resultado.estadoResultado === 'DISPONIBLE' &&
  tieneRevisionPublicada(resultado) &&
  resultado.factores.length > 0;

/**
 * Sin revisión publicada se descartan los factores aunque el backend los envíe: valores internos
 * o pendientes nunca se presentan como definitivos (Documento general §4.4). No modifica ningún dato.
 */
export const sinFactoresNoPublicados = (resultado: ResultadoResponse): ResultadoResponse =>
  puedeMostrarFactores(resultado) ? resultado : { ...resultado, factores: [] };
