// Contratos REST usados por el flujo BFA (DOC-05: E1–E8 y E24). Verificados contra
// docs/api/openapi.yaml del PR #37 (aún sin fusionar). «SUPUESTO» marca lo que el contrato no fija.

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
  /** Edición publicada que se aplicará; E3 la exige. */
  versionFormularioId: number;
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
  estado: 'EN_CURSO' | 'FINALIZADA';
  /** Agregado a E4 el 2026-11-08. */
  versionFormularioId?: number;
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
  /** Solo si la subprueba usa imágenes. */
  recursoUrl?: string | null;
  opciones: OpcionPregunta[];
  respuestaActual: string | null;
}

export interface PalabraRegistrada {
  /** Posición de la palabra (E6 y E7 de FV; antes `orden`, renombrado el 2026-11-08). */
  ordenEntrada: number;
  texto: string;
}

/** E6 */
export interface PreguntasSubpruebaResponse {
  codigo: string;
  segundosRestantes: number;
  preguntas: Pregunta[];
  /** Solo FV: palabras ya registradas con su orden. */
  palabras?: PalabraRegistrada[];
  // SUPUESTO: el contrato no devuelve la letra en E4 ni en E6, pero hace falta para retomar FV tras
  // una caída. Mientras tanto se usa un respaldo local; conviene pedir que E6 la incluya.
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

export interface OpcionRespuesta {
  letra: string;
  texto: string;
}

export interface EjemploPractica {
  numero: number;
  enunciado: string;
  opciones: OpcionRespuesta[];
  retroalimentacion: string;
}

/** E24: instrucciones y ejemplos de práctica, antes de E5 y sin consumir tiempo. */
export interface InstruccionesResponse {
  codigo: string;
  instrucciones: string;
  ejemplos: EjemploPractica[];
}
