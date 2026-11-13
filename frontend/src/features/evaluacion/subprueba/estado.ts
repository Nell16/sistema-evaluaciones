import type { EstadoSubprueba, FaseSubprueba, PreguntaSubprueba } from './contrato';

/**
 * Estado de la subprueba en pantalla. Es lógica pura (sin React ni red) para poder probarla sin
 * navegador. Distingue una respuesta `pendiente` (escrita en local, aún sin confirmar) de una
 * `confirmada` (el servidor ya la almacenó).
 */

export type EstadoGuardado = 'pendiente' | 'guardando' | 'confirmada' | 'error';

export interface RespuestaLocal {
  valor: string;
  estado: EstadoGuardado;
  /** Se incrementa con cada edición: una confirmación vieja no puede pisar una edición nueva. */
  version: number;
}

export interface EstadoPantalla {
  carga: 'cargando' | 'lista' | 'error';
  mensajeError?: string;
  subprueba?: EstadoSubprueba;
  fase: FaseSubprueba;
  indice: number;
  respuestas: Record<string, RespuestaLocal>;
  /** Instante (ms desde epoch) en que vence el tiempo oficial, o `null` si no corre. */
  limite: number | null;
  /** `true` si la subprueba se cerró por agotarse el tiempo y no por acción de la persona. */
  cerradaPorTiempo: boolean;
}

export type Accion =
  | { tipo: 'carga-iniciada' }
  | { tipo: 'carga-fallida'; mensaje: string }
  | { tipo: 'cargada'; subprueba: EstadoSubprueba; ahora: number }
  | { tipo: 'editada'; preguntaId: string; valor: string }
  | { tipo: 'guardando'; preguntaId: string; version: number }
  | {
      tipo: 'confirmada';
      preguntaId: string;
      version: number;
      fase: FaseSubprueba;
      tiempoRestanteSegundos: number | null;
      ahora: number;
    }
  | { tipo: 'guardado-fallido'; preguntaId: string; version: number }
  | { tipo: 'indice'; indice: number }
  /** Paso puramente visual de instrucciones a práctica: no consume tiempo oficial. */
  | { tipo: 'ir-a-practica' }
  | { tipo: 'tiempo-agotado' };

export const estadoInicial: EstadoPantalla = {
  carga: 'cargando',
  fase: 'instrucciones',
  indice: 0,
  respuestas: {},
  limite: null,
  cerradaPorTiempo: false,
};

export function calcularLimite(tiempoRestanteSegundos: number | null, ahora: number) {
  return tiempoRestanteSegundos === null ? null : ahora + tiempoRestanteSegundos * 1000;
}

/** Segundos que faltan según el reloj local, redondeando hacia arriba y sin bajar de cero. */
export function segundosRestantes(limite: number | null, ahora: number) {
  return limite === null ? null : Math.max(0, Math.ceil((limite - ahora) / 1000));
}

/** Al recuperar el avance se retoma en la primera pregunta sin respuesta confirmada. */
function indiceDeRetoma(subprueba: EstadoSubprueba, respuestas: Record<string, RespuestaLocal>) {
  const total = subprueba.preguntas.length;
  if (total === 0) return 0;
  const primeraSinResponder = subprueba.preguntas.findIndex((pregunta) => !respuestas[pregunta.id]);
  return primeraSinResponder === -1 ? total - 1 : primeraSinResponder;
}

export function reducir(estado: EstadoPantalla, accion: Accion): EstadoPantalla {
  switch (accion.tipo) {
    case 'carga-iniciada':
      return { ...estadoInicial };

    case 'carga-fallida':
      return { ...estado, carga: 'error', mensajeError: accion.mensaje };

    case 'cargada': {
      const respuestas: Record<string, RespuestaLocal> = {};
      for (const guardada of accion.subprueba.respuestas) {
        respuestas[guardada.preguntaId] = {
          valor: guardada.valor,
          estado: 'confirmada',
          version: 0,
        };
      }
      const fase = accion.subprueba.fase;
      return {
        carga: 'lista',
        subprueba: accion.subprueba,
        fase,
        indice: fase === 'activa' ? indiceDeRetoma(accion.subprueba, respuestas) : 0,
        respuestas,
        limite:
          fase === 'activa'
            ? calcularLimite(accion.subprueba.tiempoRestanteSegundos, accion.ahora)
            : null,
        cerradaPorTiempo: false,
      };
    }

    case 'editada': {
      if (estado.fase !== 'activa') return estado;
      const previa = estado.respuestas[accion.preguntaId];
      return {
        ...estado,
        respuestas: {
          ...estado.respuestas,
          [accion.preguntaId]: {
            valor: accion.valor,
            estado: 'pendiente',
            version: (previa?.version ?? 0) + 1,
          },
        },
      };
    }

    case 'guardando': {
      const actual = estado.respuestas[accion.preguntaId];
      if (!actual || actual.version !== accion.version) return estado;
      return {
        ...estado,
        respuestas: {
          ...estado.respuestas,
          [accion.preguntaId]: { ...actual, estado: 'guardando' },
        },
      };
    }

    case 'confirmada': {
      const actual = estado.respuestas[accion.preguntaId];
      const cerrada = accion.fase === 'cerrada';
      // La hora del servidor manda: cada confirmación vuelve a sincronizar el reloj local.
      const siguiente: EstadoPantalla = {
        ...estado,
        fase: cerrada ? 'cerrada' : estado.fase,
        limite: cerrada ? null : calcularLimite(accion.tiempoRestanteSegundos, accion.ahora),
      };
      // Si la persona editó de nuevo mientras se guardaba, la respuesta sigue pendiente.
      if (!actual || actual.version !== accion.version) return siguiente;
      return {
        ...siguiente,
        respuestas: {
          ...estado.respuestas,
          [accion.preguntaId]: { ...actual, estado: 'confirmada' },
        },
      };
    }

    case 'guardado-fallido': {
      const actual = estado.respuestas[accion.preguntaId];
      if (!actual || actual.version !== accion.version) return estado;
      return {
        ...estado,
        respuestas: { ...estado.respuestas, [accion.preguntaId]: { ...actual, estado: 'error' } },
      };
    }

    case 'indice': {
      if (!estado.subprueba) return estado;
      const ultimo = estado.subprueba.preguntas.length - 1;
      return { ...estado, indice: Math.min(Math.max(0, accion.indice), Math.max(0, ultimo)) };
    }

    case 'ir-a-practica':
      return estado.fase === 'instrucciones' && estado.subprueba?.practica
        ? { ...estado, fase: 'practica' }
        : estado;

    case 'tiempo-agotado':
      if (estado.fase === 'cerrada') return estado;
      return { ...estado, fase: 'cerrada', limite: null, cerradaPorTiempo: true };
  }
}

export function preguntaActual(estado: EstadoPantalla): PreguntaSubprueba | undefined {
  return estado.subprueba?.preguntas[estado.indice];
}

export function respuestaActual(estado: EstadoPantalla): RespuestaLocal | undefined {
  const pregunta = preguntaActual(estado);
  return pregunta ? estado.respuestas[pregunta.id] : undefined;
}

/** Solo se puede retroceder si la regla de la subprueba lo permite y la subprueba sigue abierta. */
export function puedeRetroceder(estado: EstadoPantalla) {
  return estado.fase === 'activa' && estado.subprueba?.navegacion === 'libre' && estado.indice > 0;
}

export function esUltimaPregunta(estado: EstadoPantalla) {
  const total = estado.subprueba?.preguntas.length ?? 0;
  return total > 0 && estado.indice === total - 1;
}

/** `true` si todavía hay respuestas escritas que el servidor no ha confirmado. */
export function hayRespuestasSinConfirmar(estado: EstadoPantalla) {
  return Object.values(estado.respuestas).some((respuesta) => respuesta.estado !== 'confirmada');
}
