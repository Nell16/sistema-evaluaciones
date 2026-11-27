import { http } from './http';

/**
 * Contratos E5, E6 y E7 de DOC-05 (Documento BFA §5.3) para aplicar una subprueba.
 * Son comunes a todas las subpruebas: no contienen reglas de ningún test ni claves de corrección.
 */

/** Estados de una subprueba dentro de una evaluación (ENT-COM-06). */
export type EstadoSubprueba = 'PENDIENTE' | 'ACTIVA' | 'CERRADA';

/** E5 · 200 SubpruebaActivaResponse. */
export interface SubpruebaActivaResponse {
  codigo: string;
  estado: 'ACTIVA';
  inicio: string;
  finLimite: string;
  segundosRestantes: number;
  /** Solo aplica a FV. */
  letraInicial?: string | null;
}

export interface OpcionPregunta {
  letra: string;
  texto: string;
  recursoUrl?: string | null;
}

export interface PreguntaSubprueba {
  id: number;
  numero: number;
  enunciado: string;
  recursoUrl?: string | null;
  opciones: OpcionPregunta[];
  /** Letra ya marcada por la persona; `null` si aún no responde. Nunca es la clave correcta. */
  respuestaActual: string | null;
}

/** E6 · 200 PreguntasSubpruebaResponse. */
export interface PreguntasSubpruebaResponse {
  codigo: string;
  segundosRestantes: number;
  preguntas: PreguntaSubprueba[];
  /** Solo FV: palabras ya registradas con su orden. */
  palabras?: PalabraRegistrada[];
  /** Solo FV, si el servicio lo incluye al retomar. */
  letraInicial?: string | null;
}

export interface RespuestaSeleccion {
  preguntaId: number;
  opcion: string;
}

/** E6/E7 · Palabras guardadas por Fluidez Verbal, en el orden capturado. */
export interface PalabraRegistrada {
  ordenEntrada: number;
  texto: string;
}

/** E7 · El cuerpo depende del tipo de subprueba, según el contrato OpenAPI. */
export type RegistrarRespuestasRequest =
  { respuestas: RespuestaSeleccion[] } | { palabras: PalabraRegistrada[] };

/** E7 · 200 AvanceSubpruebaResponse. */
export interface AvanceSubpruebaResponse {
  codigo: string;
  registradas: number;
  totalRespondidas: number;
  segundosRestantes: number;
}

export interface OpcionRespuestaPractica {
  letra: string;
  texto: string;
}

export interface EjemploPractica {
  numero: number;
  enunciado: string;
  opciones: OpcionRespuestaPractica[];
  retroalimentacion: string;
}

/** E24 · Instrucciones y práctica previas a iniciar el tiempo oficial. */
export interface InstruccionesResponse {
  codigo: string;
  instrucciones: string;
  ejemplos: EjemploPractica[];
}

/** Puerto usado por las pantallas; el servicio real y el mock lo implementan. */
export interface ClienteSubpruebas {
  iniciar(evaluacionId: number, codigo: string): Promise<SubpruebaActivaResponse>;
  obtenerPreguntas(evaluacionId: number, codigo: string): Promise<PreguntasSubpruebaResponse>;
  registrarRespuestas(
    evaluacionId: number,
    codigo: string,
    respuestas: RespuestaSeleccion[],
  ): Promise<AvanceSubpruebaResponse>;
}

export interface ClienteInstrucciones {
  consultarInstrucciones(evaluacionId: number, codigo: string): Promise<InstruccionesResponse>;
}

export interface ClienteRegistroSubpruebas {
  registrarRespuestas(
    evaluacionId: number,
    codigo: string,
    cuerpo: RegistrarRespuestasRequest,
  ): Promise<AvanceSubpruebaResponse>;
}

/** Códigos de error de E5–E7 que la interfaz trata de forma específica. */
export const ERRORES_SUBPRUEBA = {
  noActiva: 'SUBPRUEBA_NO_ACTIVA',
  tiempoAgotado: 'TIEMPO_AGOTADO',
  cerrada: 'SUBPRUEBA_CERRADA',
  fueraDeOrden: 'SUBPRUEBA_FUERA_DE_ORDEN',
  hayActiva: 'HAY_SUBPRUEBA_ACTIVA',
  respuestaInvalida: 'RESPUESTA_INVALIDA',
  letraNoConfigurada: 'LETRA_NO_CONFIGURADA',
} as const;

const ruta = (evaluacionId: number, codigo: string, accion: string) =>
  `/evaluaciones/${evaluacionId}/subpruebas/${encodeURIComponent(codigo)}/${accion}`;

/** Servicio real sobre `services/http`. */
export function crearClienteSubpruebas(cliente: typeof http = http): ClienteSubpruebas {
  const registro = crearClienteRegistroSubpruebas(cliente);
  return {
    iniciar: (evaluacionId, codigo) =>
      cliente<SubpruebaActivaResponse>(ruta(evaluacionId, codigo, 'iniciar'), { method: 'POST' }),
    obtenerPreguntas: (evaluacionId, codigo) =>
      cliente<PreguntasSubpruebaResponse>(ruta(evaluacionId, codigo, 'preguntas')),
    registrarRespuestas: (evaluacionId, codigo, respuestas) =>
      registro.registrarRespuestas(evaluacionId, codigo, { respuestas }),
  };
}

/** E7 completo: selección o palabras de Fluidez Verbal según el contrato. */
export function crearClienteRegistroSubpruebas(
  cliente: typeof http = http,
): ClienteRegistroSubpruebas {
  return {
    registrarRespuestas: (evaluacionId, codigo, cuerpo) =>
      cliente<AvanceSubpruebaResponse>(ruta(evaluacionId, codigo, 'respuestas'), {
        method: 'PUT',
        body: JSON.stringify(cuerpo satisfies RegistrarRespuestasRequest),
      }),
  };
}

/** Cliente HTTP separado para E24; permite mantener las llamadas fuera de features/. */
export function crearClienteInstrucciones(cliente: typeof http = http): ClienteInstrucciones {
  return {
    consultarInstrucciones: (evaluacionId, codigo) =>
      cliente<InstruccionesResponse>(ruta(evaluacionId, codigo, 'instrucciones')),
  };
}
