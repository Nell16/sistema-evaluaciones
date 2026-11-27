import { afterEach, describe, expect, it, vi } from 'vitest';
import { servicioGestion } from './gestion';
import { establecerToken } from './http';

const persona = {
  identificadorInstitucional: 'DEMO +/ñ',
  nombres: 'Persona',
  apellidos: 'Ficticia',
};
const periodo = {
  codigo: 'DEMO-01',
  denominacion: 'Período ficticio',
  inicio: '2026-11-01',
  fin: '2026-12-31',
};

function responder(status: number, cuerpo: unknown) {
  const fetch = vi.fn().mockResolvedValue(Response.json(cuerpo, { status }));
  vi.stubGlobal('fetch', fetch);
  return fetch;
}

afterEach(() => {
  establecerToken(null);
  vi.unstubAllGlobals();
});

describe('gestión con la API real', () => {
  it.each([
    ['/sujetos', persona, { id: 25, ...persona }, () => servicioGestion.registrarSujeto(persona)],
    [
      '/periodos',
      periodo,
      { id: 3, ...periodo, disponible: true },
      () => servicioGestion.crearPeriodo(periodo),
    ],
  ] as const)(
    'envía POST %s con la sesión y devuelve el recurso creado (201)',
    async (ruta, cuerpo, creado, enviar) => {
      establecerToken('sesion-de-prueba');
      const fetch = responder(201, creado);
      await expect(enviar()).resolves.toEqual(creado);
      expect(fetch).toHaveBeenCalledWith(
        '/api/v1' + ruta,
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(cuerpo),
        }),
      );
      expect(fetch.mock.calls[0][1].headers.get('Authorization')).toBe('Bearer sesion-de-prueba');
    },
  );

  it('E23 codifica el identificador como un único parámetro y conserva el resultado vacío', async () => {
    const fetch = responder(200, []);
    await expect(servicioGestion.buscarSujeto(persona.identificadorInstitucional)).resolves.toEqual(
      [],
    );
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/sujetos?identificadorInstitucional=DEMO%20%2B%2F%C3%B1',
      expect.anything(),
    );
  });

  it('E21 incluye también períodos no disponibles para nuevas asignaciones', async () => {
    const lista = [{ id: 3, ...periodo, disponible: false }];
    const fetch = responder(200, lista);
    await expect(servicioGestion.listarPeriodos()).resolves.toEqual(lista);
    expect(fetch).toHaveBeenCalledWith('/api/v1/periodos', expect.anything());
  });

  it.each([
    [409, 'SUJETO_DUPLICADO', () => servicioGestion.registrarSujeto(persona)],
    [409, 'PERIODO_DUPLICADO', () => servicioGestion.crearPeriodo(periodo)],
    [422, 'PERIODO_INVALIDO', () => servicioGestion.crearPeriodo(periodo)],
    [403, 'SIN_PERMISO', () => servicioGestion.listarPeriodos()],
  ] as const)('conserva el error HTTP %i %s para la pantalla', async (status, codigo, llamar) => {
    responder(status, { status, codigo, mensaje: 'Solicitud rechazada.' });
    await expect(llamar()).rejects.toMatchObject({ status, error: { codigo } });
  });
});
