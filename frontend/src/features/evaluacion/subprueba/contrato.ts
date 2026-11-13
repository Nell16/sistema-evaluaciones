import { http } from '../../../services/http';

/**
 * CONTRATO PROVISIONAL de GEN-03-FE.
 *
 * DOC-05 (endpoints E5, E6 y E7) sigue "In Review" en ClickUp, así que las rutas, los campos y el
 * reparto de operaciones de este archivo son SUPUESTOS. Es el único lugar que los conoce: al
 * aprobarse DOC-05 solo hay que ajustar este archivo; los componentes y el hook dependen de la
 * interfaz `SubpruebaApi`, no de las rutas.
 *
 * La lógica general no incorpora reglas de calificación (ni de BFA ni de otros tests): solo
 * presenta preguntas y registra respuestas.
 */

export type TipoRespuesta = 'seleccion' | 'texto';

/** `secuencial`: no se puede volver atrás. `libre`: se puede retroceder dentro de la subprueba. */
export type ReglaNavegacion = 'secuencial' | 'libre';

export type FaseSubprueba = 'instrucciones' | 'practica' | 'activa' | 'cerrada';

export interface OpcionPregunta {
  id: string;
  texto: string;
}

export interface PreguntaSubprueba {
  id: string;
  enunciado: string;
  tipo: TipoRespuesta;
  opciones?: OpcionPregunta[];
  /** Recurso visual opcional; si existe debe traer `recursoDescripcion` para el texto alternativo. */
  recursoUrl?: string;
  recursoDescripcion?: string;
  longitudMaxima?: number;
}

export interface RespuestaGuardada {
  preguntaId: string;
  valor: string;
}

export interface EstadoSubprueba {
  subpruebaId: string;
  nombre: string;
  fase: FaseSubprueba;
  instrucciones: string;
  practica?: string;
  preguntas: PreguntaSubprueba[];
  /** Respuestas ya confirmadas por el servidor: permiten recuperar el avance al recargar. */
  respuestas: RespuestaGuardada[];
  /** Tiempo oficial restante según el servidor; `null` si todavía no corre. */
  tiempoRestanteSegundos: number | null;
  navegacion: ReglaNavegacion;
}

export interface ResultadoGuardado {
  fase: FaseSubprueba;
  tiempoRestanteSegundos: number | null;
}

/** Operaciones que necesita la interfaz. Los componentes solo dependen de esta interfaz. */
export interface SubpruebaApi {
  /** Estado completo de la subprueba (supuesto: E5). */
  obtener(subpruebaId: string): Promise<EstadoSubprueba>;
  /** Pasa de instrucciones/práctica al tiempo oficial. El servidor controla el reloj. */
  iniciar(subpruebaId: string): Promise<EstadoSubprueba>;
  /** Registra una respuesta (supuesto: E6). Idempotente por pregunta. */
  guardarRespuesta(subpruebaId: string, respuesta: RespuestaGuardada): Promise<ResultadoGuardado>;
  /** Cierra la subprueba (supuesto: E7). También se usa al agotarse el tiempo. */
  cerrar(subpruebaId: string): Promise<EstadoSubprueba>;
}

/** Adaptador HTTP sobre `services/http`. Rutas PROVISIONALES: confirmar contra DOC-05. */
export function crearSubpruebaApi(cliente: typeof http = http): SubpruebaApi {
  const base = (id: string) => `/subpruebas/${encodeURIComponent(id)}`;

  return {
    obtener: (id) => cliente<EstadoSubprueba>(base(id)),
    iniciar: (id) => cliente<EstadoSubprueba>(`${base(id)}/inicio`, { method: 'POST' }),
    guardarRespuesta: (id, respuesta) =>
      cliente<ResultadoGuardado>(
        `${base(id)}/respuestas/${encodeURIComponent(respuesta.preguntaId)}`,
        {
          method: 'PUT',
          body: JSON.stringify({ valor: respuesta.valor }),
        },
      ),
    cerrar: (id) => cliente<EstadoSubprueba>(`${base(id)}/cierre`, { method: 'POST' }),
  };
}
