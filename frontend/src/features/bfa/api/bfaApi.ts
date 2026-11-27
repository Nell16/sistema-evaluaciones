import { http } from '../../../services/http';
import {
  crearClienteInstrucciones,
  crearClienteRegistroSubpruebas,
  crearClienteSubpruebas,
} from '../../../services/subpruebas';
import type {
  AvanceSubpruebaResponse,
  EvaluacionResponse,
  IniciarEvaluacionRequest,
  InstruccionesResponse,
  PreguntasSubpruebaResponse,
  RegistrarRespuestasRequest,
  SubpruebaActivaResponse,
  TestDetalle,
  TestResumen,
} from './tipos';

/** Todo lo que el flujo BFA necesita del backend. El resto del módulo solo conoce esta interfaz. */
export interface BfaApi {
  /** `true` en la implementación simulada, para avisarlo en pantalla. */
  readonly simulada?: boolean;
  listarTests(): Promise<TestResumen[]>;
  obtenerTest(testId: number): Promise<TestDetalle>;
  iniciarEvaluacion(solicitud: IniciarEvaluacionRequest): Promise<EvaluacionResponse>;
  obtenerEvaluacion(evaluacionId: number): Promise<EvaluacionResponse>;
  /** E24: instrucciones y ejemplos de práctica; no consume tiempo. */
  consultarInstrucciones(evaluacionId: number, codigo: string): Promise<InstruccionesResponse>;
  iniciarSubprueba(evaluacionId: number, codigo: string): Promise<SubpruebaActivaResponse>;
  obtenerPreguntas(evaluacionId: number, codigo: string): Promise<PreguntasSubpruebaResponse>;
  registrarRespuestas(
    evaluacionId: number,
    codigo: string,
    cuerpo: RegistrarRespuestasRequest,
  ): Promise<AvanceSubpruebaResponse>;
  finalizarEvaluacion(evaluacionId: number): Promise<EvaluacionResponse>;
}

const clienteSubpruebas = crearClienteSubpruebas();
const clienteInstrucciones = crearClienteInstrucciones();
const clienteRegistroSubpruebas = crearClienteRegistroSubpruebas();

/** Implementación real: usa el cliente HTTP común (`services/http.ts`). */
export const bfaApiHttp: BfaApi = {
  listarTests: () => http('/tests'),
  obtenerTest: (testId) => http(`/tests/${testId}`),
  iniciarEvaluacion: (solicitud) =>
    http('/evaluaciones', { method: 'POST', body: JSON.stringify(solicitud) }),
  obtenerEvaluacion: (id) => http(`/evaluaciones/${id}`),
  consultarInstrucciones: clienteInstrucciones.consultarInstrucciones,
  iniciarSubprueba: clienteSubpruebas.iniciar,
  obtenerPreguntas: clienteSubpruebas.obtenerPreguntas,
  registrarRespuestas: (id, codigo, cuerpo) =>
    clienteRegistroSubpruebas.registrarRespuestas(id, codigo, cuerpo),
  finalizarEvaluacion: (id) => http(`/evaluaciones/${id}/finalizar`, { method: 'POST' }),
};
