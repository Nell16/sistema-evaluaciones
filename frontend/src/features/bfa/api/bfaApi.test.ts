import { afterEach, describe, expect, it, vi } from 'vitest';
import { bfaApiHttp } from './bfaApi';

afterEach(() => vi.unstubAllGlobals());

function simularServidor(cuerpoRespuesta: unknown) {
  const fetchSimulado = vi.fn(
    async () => new Response(JSON.stringify(cuerpoRespuesta), { status: 200 }),
  );
  vi.stubGlobal('fetch', fetchSimulado);
  return fetchSimulado;
}

describe('E7 de Fluidez Verbal (contrato del 2026-11-08, BUG-03)', () => {
  it('envía PUT con palabras y ordenEntrada, sin pregunta ni opción', async () => {
    const fetchSimulado = simularServidor({
      codigo: 'FV',
      registradas: 2,
      totalRespondidas: 2,
      segundosRestantes: 290,
    });

    await bfaApiHttp.registrarRespuestas(15, 'FV', {
      palabras: [
        { ordenEntrada: 1, texto: 'mesa' },
        { ordenEntrada: 2, texto: 'montaña' },
      ],
    });

    const [ruta, opciones] = fetchSimulado.mock.calls[0] as unknown as [string, RequestInit];
    expect(ruta).toBe('/api/v1/evaluaciones/15/subpruebas/FV/respuestas');
    expect(opciones.method).toBe('PUT');
    expect(JSON.parse(opciones.body as string)).toEqual({
      palabras: [
        { ordenEntrada: 1, texto: 'mesa' },
        { ordenEntrada: 2, texto: 'montaña' },
      ],
    });
  });

  it('E5 y E6 usan las rutas del contrato', async () => {
    const fetchSimulado = simularServidor({});
    await bfaApiHttp.iniciarSubprueba(15, 'FV');
    await bfaApiHttp.obtenerPreguntas(15, 'FV');
    const rutas = fetchSimulado.mock.calls.map((llamada) => {
      const [ruta, opciones] = llamada as unknown as [string, RequestInit | undefined];
      return `${opciones?.method ?? 'GET'} ${ruta}`;
    });
    expect(rutas).toEqual([
      'POST /api/v1/evaluaciones/15/subpruebas/FV/iniciar',
      'GET /api/v1/evaluaciones/15/subpruebas/FV/preguntas',
    ]);
  });
});
