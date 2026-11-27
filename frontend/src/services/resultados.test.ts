import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from './http';
import { servicioResultados } from './resultados';

afterEach(() => vi.unstubAllGlobals());

describe('servicioResultados (llamada real)', () => {
  it('E9 hace GET /api/v1/evaluaciones/{id}/resultados y devuelve la respuesta sin cambios', async () => {
    const cuerpo = { evaluacionId: 16, estadoResultado: 'DISPONIBLE', factores: [] };
    const fetchSimulado = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify(cuerpo), { status: 200 }));
    vi.stubGlobal('fetch', fetchSimulado);
    await expect(servicioResultados.obtenerResultado(16)).resolves.toEqual(cuerpo);
    expect(fetchSimulado).toHaveBeenCalledWith(
      '/api/v1/evaluaciones/16/resultados',
      expect.anything(),
    );
  });

  it('E9 soporta CALCULANDO con resultadoId nulo y PENDIENTE_VALIDACION sin revisión', async () => {
    const calculando = {
      evaluacionId: 18,
      resultadoId: null,
      estadoResultado: 'CALCULANDO',
      versionFormularioId: 10,
      calificacionId: null,
      revision: null,
      versionReglas: null,
      idClave: null,
      idBaremo: null,
      factores: [],
    };
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(JSON.stringify(calculando), { status: 200 })),
    );
    await expect(servicioResultados.obtenerResultado(18)).resolves.toEqual(calculando);

    const pendiente = {
      ...calculando,
      evaluacionId: 17,
      resultadoId: 8,
      estadoResultado: 'PENDIENTE_VALIDACION',
    };
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(JSON.stringify(pendiente), { status: 200 })),
    );
    await expect(servicioResultados.obtenerResultado(17)).resolves.toEqual(pendiente);
  });

  it.each([
    [403, 'SIN_PERMISO'],
    [404, 'EVALUACION_NO_ENCONTRADA'],
    [409, 'EVALUACION_NO_FINALIZADA'],
  ])('propaga el error %i %s del backend como ApiError', async (status, codigo) => {
    const cuerpo = { status, codigo, mensaje: 'x', ruta: '', timestamp: '' };
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(JSON.stringify(cuerpo), { status })),
    );
    await expect(servicioResultados.obtenerResultado(16)).rejects.toMatchObject({
      status,
      error: expect.objectContaining({ codigo }),
    });
    await expect(servicioResultados.obtenerResultado(16)).rejects.toBeInstanceOf(ApiError);
  });
});
