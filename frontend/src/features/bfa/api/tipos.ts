// Contratos REST usados por el flujo BFA (DOC-05 · Documento BFA §5.3, E1–E8).
// Los comentarios «SUPUESTO» marcan lo que el documento no fija y hay que confirmar.

export type EstadoSubprueba = 'PENDIENTE' | 'ACTIVA' | 'CERRADA';
export type TipoRespuesta = 'SELECCION' | 'TEXTO_LIBRE';

/** E1 */
export interface TestResumen {
  id: number;
  codigo: string;
  nombre: string;
  numeroSubpruebas: number;
}

/** Configuración de una subprueba dentro de E2. El backend manda el orden y el tiempo. */
export interface SubpruebaConfig {
  codigo: string;
  nombre: string;
  orden: number;
  factor: string;
  tiempoLimiteSegundos: number;
  tipoRespuesta: TipoRespuesta;
  opciones: number | null;
  numeroPreguntas: number;
  maximoEntradas: number | null;
  tienePractica: boolean;
}

/** E2 */
export interface TestDetalle {
  id: number;
  codigo: string;
  nombre: string;
  subpruebas: SubpruebaConfig[];
}

/** E3 */
export interface IniciarEvaluacionRequest {
  testId?: number;
  versionFormularioId: number;
  sujetoId: number;
  periodoId: number;
  subpruebas?: string[];
  fluidez?: { letraInicial: string };
}

export interface SubpruebaEstado {
  codigo: string;
  estado: EstadoSubprueba;
}

/** E3 (201) y E4 (200). */
export interface EvaluacionResponse {
  id: number;
  testId: number;
  sujetoId: number;
  periodoId: number;
  // SUPUESTO: el documento solo muestra EN_CURSO y FINALIZADA.
  estado: 'EN_CURSO' | 'FINALIZADA';
  fechaInicio: string;
  fechaFin: string | null;
  subpruebaActiva: { codigo: string; finLimite: string; segundosRestantes: number } | null;
  /** Solo las subpruebas incluidas, en el orden oficial de aplicación. */
  subpruebas: SubpruebaEstado[];
  /** E8 */
  resultadoId?: number;
  estadoResultado?: string;
}

/** E5 */
export interface SubpruebaActivaResponse {
  codigo: string;
  estado: 'ACTIVA';
  inicio: string;
  finLimite: string;
  segundosRestantes: number;
  /** Solo FV. */
  letraInicial?: string;
}

export interface OpcionPregunta {
  letra: string;
  texto: string;
  /** Recurso versionado cuando la alternativa es una figura. */
  recursoUrl?: string | null;
}

export interface Pregunta {
  id: number;
  numero: number;
  enunciado: string;
  /** SUPUESTO: el documento dice que «el enunciado o la opción» incluyen recursoUrl. */
  recursoUrl?: string | null;
  opciones: OpcionPregunta[];
  respuestaActual: string | null;
}

export interface PalabraRegistrada {
  orden: number;
  texto: string;
}

/** E6 */
export interface PreguntasSubpruebaResponse {
  codigo: string;
  segundosRestantes: number;
  preguntas: Pregunta[];
  // SUPUESTO: el documento dice que FV devuelve «las palabras ya registradas con su orden»
  // y que la letra se fija en E5, pero E4 no la trae: se necesita al retomar FV tras una caída.
  palabras?: PalabraRegistrada[];
  letraInicial?: string | null;
}

/** E7 */
export type RegistrarRespuestasRequest =
  { respuestas: { preguntaId: number; opcion: string }[] } | { palabras: PalabraRegistrada[] };

export interface AvanceSubpruebaResponse {
  codigo: string;
  registradas: number;
  totalRespondidas: number;
  segundosRestantes: number;
}
