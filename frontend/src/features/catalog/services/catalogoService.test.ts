import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, establecerToken } from '../../../services/http';
import { listarTests, obtenerTest } from './catalogoService';

afterEach(() => {
  establecerToken(null);
  vi.unstubAllGlobals();
});

/** Cuerpo de error común del backend con datos ficticios. */
function errorApi(status: number, codigo: string, ruta: string) {
  return Response.json(
    { timestamp: '2099-01-01T00:00:00Z', status, codigo, mensaje: 'Mensaje ficticio', ruta },
    { status },
  );
}

describe('servicio del catálogo (E1, E2)', () => {
  it('E1 consulta GET /api/v1/tests con el Bearer y devuelve los tests activos', async () => {
    const tests = [{ id: 1, codigo: 'BFA', nombre: 'Test ficticio', numeroSubpruebas: 13 }];
    establecerToken('token-ficticio');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json(tests)));
    await expect(listarTests()).resolves.toEqual(tests);
    expect(fetch).toHaveBeenCalledWith('/api/v1/tests', expect.any(Object));
    const opciones = vi.mocked(fetch).mock.calls[0][1];
    expect(opciones?.method ?? 'GET').toBe('GET');
    expect(new Headers(opciones?.headers).get('Authorization')).toBe('Bearer token-ficticio');
  });

  it('E1 acepta la lista vacía cuando no hay tests activos', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json([])));
    await expect(listarTests()).resolves.toEqual([]);
  });

  it.each([
    [401, 'NO_AUTENTICADO'],
    [403, 'SIN_PERMISO'],
  ])('E1 rechaza con %i %s según el contrato', async (status, codigo) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(errorApi(status, codigo, '/api/v1/tests')));
    const promesa = listarTests();
    await expect(promesa).rejects.toBeInstanceOf(ApiError);
    await expect(promesa).rejects.toMatchObject({ status, error: { codigo } });
  });

  it('E2 consulta GET /api/v1/tests/{testId} y conserva los campos nulos del contrato', async () => {
    const detalle = {
      id: 1,
      codigo: 'BFA',
      nombre: 'Test ficticio',
      versionFormularioId: 10,
      subpruebas: [
        {
          codigo: 'S1',
          nombre: 'Subprueba ficticia',
          orden: 1,
          factor: null,
          tiempoLimiteSegundos: null,
          tipoRespuesta: 'TEXTO_LIBRE',
          opciones: null,
          numeroPreguntas: 0,
          maximoEntradas: 75,
          tienePractica: false,
        },
      ],
    };
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json(detalle)));
    await expect(obtenerTest(1)).resolves.toEqual(detalle);
    expect(fetch).toHaveBeenCalledWith('/api/v1/tests/1', expect.any(Object));
  });

  it('E2 informa TEST_NO_ENCONTRADO (404)', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(errorApi(404, 'TEST_NO_ENCONTRADO', '/api/v1/tests/999')),
    );
    await expect(obtenerTest(999)).rejects.toMatchObject({
      status: 404,
      error: { codigo: 'TEST_NO_ENCONTRADO' },
    });
  });
});
