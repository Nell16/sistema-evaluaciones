// Verificación independiente de Testing (BFA-TEST-01) sobre el PR #45.
// Solo se ejecuta en una copia aparte del código; no forma parte del PR.
// Datos ficticios. Compara rutas, métodos, cuerpos y errores con docs/api/openapi.yaml.
import { afterEach, describe, expect, it, vi } from 'vitest';
import { bfaApiHttp } from './bfaApi';
import { ApiError } from '../../../services/http';

afterEach(() => vi.unstubAllGlobals());

type Llamada = { ruta: string; metodo: string; cuerpo: unknown; cabeceras: Record<string, string> };

function servidor(status = 200, cuerpo: unknown = {}, crudo?: string) {
  const llamadas: Llamada[] = [];
  vi.stubGlobal(
    'fetch',
    vi.fn(async (ruta: string, op: RequestInit = {}) => {
      llamadas.push({
        ruta,
        metodo: op.method ?? 'GET',
        cuerpo: op.body === undefined ? undefined : JSON.parse(op.body as string),
        cabeceras: (op.headers ?? {}) as Record<string, string>,
      });
      return new Response(crudo ?? JSON.stringify(cuerpo), { status });
    }),
  );
  return llamadas;
}

const errorContrato = (status: number, codigo: string) => ({
  timestamp: '2026-11-18T12:00:00Z',
  status,
  codigo,
  mensaje: `mensaje ficticio ${codigo}`,
  ruta: '/api/v1/evaluaciones/15',
});

describe('TC-BFA-FE01-003 a 012: rutas, métodos y cuerpos (E1–E8, E24)', () => {
  it('TC-003 E1 GET /api/v1/tests', async () => {
    const l = servidor(200, []);
    await bfaApiHttp.listarTests();
    expect(l[0]).toMatchObject({ ruta: '/api/v1/tests', metodo: 'GET', cuerpo: undefined });
  });
  it('TC-004 E2 GET /api/v1/tests/{testId}', async () => {
    const l = servidor();
    await bfaApiHttp.obtenerTest(1);
    expect(l[0]).toMatchObject({ ruta: '/api/v1/tests/1', metodo: 'GET', cuerpo: undefined });
  });
  it('TC-005 E3 POST /api/v1/evaluaciones con el cuerpo del contrato', async () => {
    const l = servidor(201, {});
    const cuerpo = {
      versionFormularioId: 3,
      sujetoId: 25,
      periodoId: 2,
      subpruebas: ['VOC1', 'FV'],
      fluidez: { letraInicial: 'M' },
    };
    await bfaApiHttp.iniciarEvaluacion(cuerpo);
    expect(l[0]).toMatchObject({ ruta: '/api/v1/evaluaciones', metodo: 'POST', cuerpo });
  });
  it('TC-006 E4 GET /api/v1/evaluaciones/{id}', async () => {
    const l = servidor();
    await bfaApiHttp.obtenerEvaluacion(15);
    expect(l[0]).toMatchObject({ ruta: '/api/v1/evaluaciones/15', metodo: 'GET', cuerpo: undefined });
  });
  it('TC-007 E24 GET …/subpruebas/{codigo}/instrucciones', async () => {
    const l = servidor();
    await bfaApiHttp.consultarInstrucciones(15, 'VOC1');
    expect(l[0]).toMatchObject({
      ruta: '/api/v1/evaluaciones/15/subpruebas/VOC1/instrucciones',
      metodo: 'GET',
      cuerpo: undefined,
    });
  });
  it('TC-008 E5 POST …/iniciar sin cuerpo', async () => {
    const l = servidor();
    await bfaApiHttp.iniciarSubprueba(15, 'VOC1');
    expect(l[0]).toMatchObject({
      ruta: '/api/v1/evaluaciones/15/subpruebas/VOC1/iniciar',
      metodo: 'POST',
      cuerpo: undefined,
    });
  });
  it('TC-009 E6 GET …/preguntas', async () => {
    const l = servidor();
    await bfaApiHttp.obtenerPreguntas(15, 'VOC1');
    expect(l[0]).toMatchObject({
      ruta: '/api/v1/evaluaciones/15/subpruebas/VOC1/preguntas',
      metodo: 'GET',
      cuerpo: undefined,
    });
  });
  it('TC-010 E7 selección: PUT con respuestas {preguntaId, opcion}', async () => {
    const l = servidor();
    const cuerpo = { respuestas: [{ preguntaId: 120, opcion: 'B' }, { preguntaId: 121, opcion: 'D' }] };
    await bfaApiHttp.registrarRespuestas(15, 'VOC1', cuerpo);
    expect(l[0]).toMatchObject({
      ruta: '/api/v1/evaluaciones/15/subpruebas/VOC1/respuestas',
      metodo: 'PUT',
      cuerpo,
    });
  });
  it('TC-011 E7 FV: PUT con palabras {ordenEntrada, texto}, sin preguntaId ni opcion', async () => {
    const l = servidor();
    const cuerpo = { palabras: [{ ordenEntrada: 1, texto: 'mesa' }, { ordenEntrada: 2, texto: 'montaña' }] };
    await bfaApiHttp.registrarRespuestas(15, 'FV', cuerpo);
    expect(l[0]).toMatchObject({ ruta: '/api/v1/evaluaciones/15/subpruebas/FV/respuestas', metodo: 'PUT' });
    expect(l[0].cuerpo).toEqual(cuerpo);
    expect(JSON.stringify(l[0].cuerpo)).not.toMatch(/preguntaId|opcion/);
  });
  it('TC-012 E8 POST …/finalizar sin cuerpo', async () => {
    const l = servidor();
    await bfaApiHttp.finalizarEvaluacion(15);
    expect(l[0]).toMatchObject({ ruta: '/api/v1/evaluaciones/15/finalizar', metodo: 'POST', cuerpo: undefined });
  });
});

describe('TC-BFA-FE01-013 y 014: formato de la solicitud', () => {
  it('TC-013 el código de subprueba se codifica en la ruta', async () => {
    const l = servidor();
    await bfaApiHttp.obtenerPreguntas(15, 'N1 A/B');
    expect(l[0].ruta).toBe('/api/v1/evaluaciones/15/subpruebas/N1%20A%2FB/preguntas');
  });
  it('TC-014 envía Content-Type application/json', async () => {
    const l = servidor();
    await bfaApiHttp.registrarRespuestas(15, 'VOC1', { respuestas: [{ preguntaId: 1, opcion: 'A' }] });
    expect(l[0].cabeceras['Content-Type']).toBe('application/json');
  });
});

describe('TC-BFA-FE01-015 a 018: errores (§5.2.2)', () => {
  for (const [status, codigo] of [
    [400, 'SOLICITUD_INVALIDA'],
    [401, 'NO_AUTENTICADO'],
    [403, 'SIN_PERMISO'],
    [404, 'EVALUACION_NO_ENCONTRADA'],
    [409, 'HAY_SUBPRUEBA_ACTIVA'],
    [422, 'OPCION_NO_PERTENECE'],
    [500, 'ERROR_INTERNO'],
  ] as const) {
    it(`TC-015 ${status} ${codigo}: rechaza con ApiError y conserva status y codigo`, async () => {
      servidor(status, errorContrato(status, codigo));
      const p = bfaApiHttp.obtenerEvaluacion(15);
      await expect(p).rejects.toBeInstanceOf(ApiError);
      await p.catch((e: ApiError) => {
        expect(e.status).toBe(status);
        expect(e.error?.codigo).toBe(codigo);
      });
    });
  }
  it('TC-016 error sin cuerpo JSON: ApiError con el status y mensaje genérico', async () => {
    servidor(503, undefined, '<html>Servicio no disponible</html>');
    await bfaApiHttp.obtenerTest(1).catch((e: ApiError) => {
      expect(e).toBeInstanceOf(ApiError);
      expect(e.status).toBe(503);
      expect(e.message).toBe('HTTP 503');
    });
    expect.assertions(3);
  });
  it('TC-017 fallo de conexión: la promesa se rechaza (no se da por guardado)', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Promise.reject(new TypeError('Failed to fetch'))));
    await expect(
      bfaApiHttp.registrarRespuestas(15, 'VOC1', { respuestas: [{ preguntaId: 1, opcion: 'A' }] }),
    ).rejects.toThrow('Failed to fetch');
  });
  it('TC-018 respuesta correcta: devuelve el cuerpo tal cual', async () => {
    const avance = { codigo: 'VOC1', registradas: 1, totalRespondidas: 5, segundosRestantes: 240 };
    servidor(200, avance);
    await expect(
      bfaApiHttp.registrarRespuestas(15, 'VOC1', { respuestas: [{ preguntaId: 1, opcion: 'A' }] }),
    ).resolves.toEqual(avance);
  });
});

describe('TC-BFA-FE01-019: autenticación (observación)', () => {
  it('TC-019 hoy no envía Authorization: Bearer (pendiente de GEN-06)', async () => {
    const l = servidor();
    await bfaApiHttp.obtenerEvaluacion(15);
    expect(l[0].cabeceras.Authorization).toBeUndefined();
  });
});
