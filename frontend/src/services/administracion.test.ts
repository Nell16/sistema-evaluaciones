import { afterEach, describe, expect, it, vi } from 'vitest';
import { servicioAdministracion } from './administracion';

const responder = (status: number, cuerpo: unknown) => {
  const simulado = vi.fn().mockResolvedValue(new Response(JSON.stringify(cuerpo), { status }));
  vi.stubGlobal('fetch', simulado);
  return simulado;
};

afterEach(() => vi.unstubAllGlobals());

const datosBorrador = { codigoVersion: 'demo-v2', fuenteRef: 'referencia-ficticia' };

describe('servicioAdministracion (llamadas reales)', () => {
  it.each([
    [
      'E25',
      () => servicioAdministracion.crearVersion(1, datosBorrador),
      'POST',
      '/api/v1/tests/1/versiones',
    ],
    [
      'E26',
      () => servicioAdministracion.guardarConfiguracion(11, 'VOC1', {}),
      'PUT',
      '/api/v1/versiones/11/configuraciones/VOC1',
    ],
    [
      'E27',
      () =>
        servicioAdministracion.agregarPregunta(11, 'VOC1', {
          numeroVisible: 1,
          ordenPresentacion: 1,
          enunciado: 'x',
        }),
      'POST',
      '/api/v1/versiones/11/configuraciones/VOC1/preguntas',
    ],
    [
      'E28',
      () => servicioAdministracion.guardarOpciones(11, 120, { opciones: [] }),
      'PUT',
      '/api/v1/versiones/11/preguntas/120/opciones',
    ],
    [
      'E29',
      () => servicioAdministracion.cargarClave(11, { entradas: [] }),
      'PUT',
      '/api/v1/versiones/11/clave',
    ],
    [
      'E30',
      () =>
        servicioAdministracion.cargarBaremo({
          nombre: 'B',
          version: '1',
          anioNorma: 2000,
          rangos: [],
        }),
      'POST',
      '/api/v1/baremos',
    ],
    ['E31', () => servicioAdministracion.publicar(11), 'POST', '/api/v1/versiones/11/publicar'],
  ])('%s usa método y ruta de DOC-05', async (_nombre, llamar, metodo, ruta) => {
    const fetchSimulado = responder(200, {});
    await llamar();
    expect(fetchSimulado).toHaveBeenCalledWith(ruta, expect.objectContaining({ method: metodo }));
  });

  it('E25 envía el código y la referencia de la fuente al crear el borrador', async () => {
    const fetchSimulado = responder(201, { id: 11, testId: 1, estado: 'BORRADOR' });
    await expect(servicioAdministracion.crearVersion(1, datosBorrador)).resolves.toEqual({
      id: 11,
      testId: 1,
      estado: 'BORRADOR',
    });
    expect(fetchSimulado).toHaveBeenCalledWith(
      '/api/v1/tests/1/versiones',
      expect.objectContaining({ method: 'POST', body: JSON.stringify(datosBorrador) }),
    );
  });

  it('E31 va sin cuerpo; E26 envía el JSON', async () => {
    const fetchSimulado = responder(200, {});
    await servicioAdministracion.publicar(11);
    expect(fetchSimulado.mock.calls[0]?.[1]).not.toHaveProperty('body');
    await servicioAdministracion.guardarConfiguracion(11, 'VOC1', { orden: 1 });
    expect(fetchSimulado.mock.calls[1]?.[1]).toHaveProperty('body', '{"orden":1}');
  });

  it('propaga el error del backend con su código', async () => {
    responder(422, { status: 422, codigo: 'VERSION_INCOMPLETA', mensaje: 'Falta clave.' });
    await expect(servicioAdministracion.publicar(11)).rejects.toMatchObject({
      status: 422,
      error: { codigo: 'VERSION_INCOMPLETA' },
    });
  });

  it.each([{}, { recursoRef: 'demo/imagen-ficticia.svg' }])(
    'E27 conserva el cuerpo enviado y lee la pregunta creada (201) con %o',
    async (imagen) => {
      const solicitud = {
        numeroVisible: 2,
        ordenPresentacion: 3,
        enunciado: 'Pregunta de prueba',
        ...imagen,
      };
      const creada = { ...solicitud, id: 121, recursoRef: imagen.recursoRef ?? null };
      const fetchSimulado = responder(201, creada);
      await expect(servicioAdministracion.agregarPregunta(11, 'VOC1', solicitud)).resolves.toEqual(
        creada,
      );
      expect(fetchSimulado).toHaveBeenCalledWith(
        '/api/v1/versiones/11/configuraciones/VOC1/preguntas',
        expect.objectContaining({ method: 'POST', body: JSON.stringify(solicitud) }),
      );
    },
  );

  it('E27 conserva el código de error si el número u orden ya está en uso', async () => {
    responder(422, { codigo: 'PREGUNTA_DUPLICADA', mensaje: 'Pregunta duplicada.' });
    await expect(
      servicioAdministracion.agregarPregunta(11, 'VOC1', {
        numeroVisible: 2,
        ordenPresentacion: 3,
        enunciado: 'Pregunta de prueba',
      }),
    ).rejects.toMatchObject({ status: 422, error: { codigo: 'PREGUNTA_DUPLICADA' } });
  });
});
