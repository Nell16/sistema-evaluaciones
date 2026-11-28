/**
 * Tipos correspondientes al contrato REST GEN-01.
 * Fuente: docs/api/openapi.yaml, PR #37.
 */

/**
 * Resumen de un test disponible en el catálogo.
 */
export interface TestResumenResponse {
  id?: number;
  codigo?: string;
  nombre?: string;
  numeroSubpruebas?: number;
}

/**
 * Configuración de una subprueba perteneciente a un test.
 */
export interface ConfiguracionSubprueba {
  codigo?: string;
  nombre?: string;
  orden?: number;
  /** Nulo si aún no se cargó (contrato: nullable). */
  factor?: string | null;
  /** Nulo si la subprueba no tiene límite (contrato: nullable). */
  tiempoLimiteSegundos?: number | null;
  tipoRespuesta?: 'SELECCION' | 'TEXTO_LIBRE';
  opciones?: number | null;
  numeroPreguntas?: number;
  maximoEntradas?: number | null;
  tienePractica?: boolean;
}

/**
 * Información detallada de un test y sus subpruebas.
 */
export interface TestDetalleResponse {
  id?: number;
  codigo?: string;
  nombre?: string;
  versionFormularioId?: number;
  subpruebas?: ConfiguracionSubprueba[];
}
