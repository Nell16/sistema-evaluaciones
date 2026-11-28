import { afterEach, describe, expect, it, vi } from 'vitest';
import { cuerpoInicio, servicioEvaluacion } from './evaluacion';

const responder = (status: number, cuerpo: unknown) => {
  const simulado = vi.fn().mockResolvedValue(new Response(JSON.stringify(cuerpo), { status }));
  vi.stubGlobal('fetch', simulado);
  return simulado;
};

afterEach(() => vi.unstubAllGlobals());

describe('servicioEvaluacion (llamadas reales)', () => {
  it('E3 hace POST /api/v1/evaluaciones con el cuerpo JSON', async () => {
    const fetchSimulado = responder(201, { id: 15 });
    const solicitud = { versionFormularioId: 10, sujetoId: 25, periodoId: 3 };
    await expect(servicioEvaluacion.iniciarEvaluacion(solicitud)).resolves.toEqual({ id: 15 });
    expect(fetchSimulado).toHaveBeenCalledWith(
      '/api/v1/evaluaciones',
      expect.objectContaining({ method: 'POST', body: JSON.stringify(solicitud) }),
    );
  });

  it.each([
    ['E4', () => servicioEvaluacion.obtenerEvaluacion(15), '/api/v1/evaluaciones/15'],
    ['E2', () => servicioEvaluacion.obtenerTest(1), '/api/v1/tests/1'],
    ['E19', () => servicioEvaluacion.obtenerSujeto(25), '/api/v1/sujetos/25'],
    [
      'E21',
      () => servicioEvaluacion.listarPeriodosDisponibles(),
      '/api/v1/periodos?disponible=true',
    ],
  ])('%s llama a la ruta de DOC-05', async (_nombre, llamar, ruta) => {
    const fetchSimulado = responder(200, {});
    await llamar();
    expect(fetchSimulado).toHaveBeenCalledWith(ruta, expect.anything());
  });

  it('E3 sin subpruebas es una aplicación completa: no envía la propiedad', async () => {
    const fetchSimulado = responder(201, { id: 15 });
    await servicioEvaluacion.iniciarEvaluacion({
      versionFormularioId: 10,
      sujetoId: 25,
      periodoId: 3,
    });
    const [, opciones] = fetchSimulado.mock.calls[0] as [string, RequestInit];
    expect(JSON.parse(opciones.body as string)).not.toHaveProperty('subpruebas');
  });

  it.each([[[]], [[' ', '']]])(
    'E3 no convierte una selección parcial inválida (%j) en aplicación completa',
    async (subpruebas) => {
      const fetchSimulado = responder(400, {
        timestamp: 't',
        status: 400,
        codigo: 'VALIDACION_FALLIDA',
        mensaje: 'm',
        ruta: '/api/v1/evaluaciones',
      });
      await expect(
        servicioEvaluacion.iniciarEvaluacion({
          versionFormularioId: 10,
          sujetoId: 25,
          periodoId: 3,
          subpruebas,
        }),
      ).rejects.toMatchObject({ status: 400 });
      const [, opciones] = fetchSimulado.mock.calls[0] as [string, RequestInit];
      expect(JSON.parse(opciones.body as string).subpruebas).toEqual(subpruebas);
    },
  );

  it('E3 envía la letra de FV recortada y en mayúscula, y no oculta una letra en blanco', () => {
    const base = { versionFormularioId: 10, sujetoId: 25, periodoId: 3 };
    expect(cuerpoInicio({ ...base, subpruebas: ['FV'], fluidez: { letraInicial: ' m ' } })).toEqual(
      { ...base, subpruebas: ['FV'], fluidez: { letraInicial: 'M' } },
    );
    expect(cuerpoInicio({ ...base, fluidez: { letraInicial: '  ' } })).toEqual({
      ...base,
      fluidez: { letraInicial: '  ' },
    });
  });

  it('E4 entrega la forma del contrato (cerradaEn y subpruebaActiva)', async () => {
    const cuerpo = {
      id: 15,
      testId: 1,
      versionFormularioId: 10,
      sujetoId: 25,
      periodoId: 3,
      estado: 'EN_CURSO',
      fechaInicio: '2026-10-30T15:30:00Z',
      fechaFin: null,
      subpruebaActiva: {
        codigo: 'VOC2',
        finLimite: '2026-10-30T15:42:10Z',
        segundosRestantes: 214,
      },
      subpruebas: [
        { codigo: 'VOC1', estado: 'CERRADA', cerradaEn: '2026-10-30T15:38:00Z' },
        { codigo: 'VOC2', estado: 'ACTIVA', cerradaEn: null },
      ],
    };
    responder(200, cuerpo);
    await expect(servicioEvaluacion.obtenerEvaluacion(15)).resolves.toEqual(cuerpo);
  });

  it.each([
    [409, 'EVALUACION_EN_CURSO'],
    [409, 'LETRA_NO_ASIGNADA'],
    [409, 'VERSION_NO_PUBLICADA'],
    [422, 'VERSION_INCOMPATIBLE'],
    [422, 'SUBPRUEBA_NO_INCLUIDA'],
    [404, 'PERIODO_NO_ENCONTRADO'],
  ])('E3 propaga %i %s con su código', async (status, codigo) => {
    responder(status, {
      timestamp: 't',
      status,
      codigo,
      mensaje: 'm',
      ruta: '/api/v1/evaluaciones',
    });
    await expect(
      servicioEvaluacion.iniciarEvaluacion({ versionFormularioId: 10, sujetoId: 25, periodoId: 3 }),
    ).rejects.toMatchObject({ status, error: { codigo } });
  });

  it('propaga el error del backend con su código', async () => {
    responder(404, {
      timestamp: 't',
      status: 404,
      codigo: 'EVALUACION_NO_ENCONTRADA',
      mensaje: 'No existe.',
      ruta: '/api/v1/evaluaciones/9',
    });
    await expect(servicioEvaluacion.obtenerEvaluacion(9)).rejects.toMatchObject({
      status: 404,
      error: { codigo: 'EVALUACION_NO_ENCONTRADA' },
    });
  });
});
