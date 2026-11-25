import { describe, expect, it } from 'vitest';
import type { IniciarEvaluacionRequest } from '../features/evaluacion/tipos';
import { crearClienteMock } from './evaluacion.mock';

const valida: IniciarEvaluacionRequest = {
  testId: 1,
  versionFormularioId: 10,
  sujetoId: 26,
  periodoId: 4,
  subpruebas: ['CV', 'VOC1'],
};

const cliente = () => crearClienteMock({ retardoMs: 0 });

describe('mock de evaluación (DOC-05)', () => {
  it('E2 devuelve las 13 subpruebas del BFA en orden', async () => {
    const test = await cliente().obtenerTest(1);
    expect(test.subpruebas).toHaveLength(13);
    expect(test.subpruebas.map((s) => s.orden)).toEqual([...Array(13).keys()].map((i) => i + 1));
    expect(test.subpruebas.reduce((t, s) => t + s.tiempoLimiteSegundos, 0)).toBe(5070); // 84:30
  });

  it('E21 solo lista períodos disponibles', async () => {
    const periodos = await cliente().listarPeriodosDisponibles();
    expect(periodos.every((p) => p.disponible)).toBe(true);
  });

  it('E3 crea una evaluación EN_CURSO con las subpruebas en orden oficial', async () => {
    const creada = await cliente().iniciarEvaluacion(valida);
    expect(creada).toMatchObject({ estado: 'EN_CURSO', sujetoId: 26, subpruebaActiva: null });
    expect(creada.subpruebas).toEqual([
      { codigo: 'VOC1', estado: 'PENDIENTE' },
      { codigo: 'CV', estado: 'PENDIENTE' },
    ]);
  });

  it('E4 recupera la evaluación creada sin cambiarla', async () => {
    const c = cliente();
    const creada = await c.iniciarEvaluacion(valida);
    await expect(c.obtenerEvaluacion(creada.id)).resolves.toEqual(creada);
  });

  it('E3 rechaza campos obligatorios ausentes con detalles', async () => {
    const fallo = cliente().iniciarEvaluacion({ ...valida, sujetoId: 0, periodoId: 0 });
    await expect(fallo).rejects.toMatchObject({
      status: 400,
      error: {
        codigo: 'SOLICITUD_INVALIDA',
        detalles: [
          { campo: 'sujetoId', mensaje: 'es obligatorio' },
          { campo: 'periodoId', mensaje: 'es obligatorio' },
        ],
      },
    });
  });

  it.each([
    [{ sujetoId: 999 }, 404, 'SUJETO_NO_ENCONTRADO'],
    [{ periodoId: 99 }, 404, 'PERIODO_NO_ENCONTRADO'],
    [{ versionFormularioId: 11 }, 409, 'VERSION_NO_PUBLICADA'],
    [{ testId: 2 }, 422, 'VERSION_INCOMPATIBLE'],
    [{ subpruebas: ['X'] }, 422, 'SUBPRUEBA_NO_INCLUIDA'],
    [{ sujetoId: 409 }, 409, 'EVALUACION_EN_CURSO'],
    [{ sujetoId: 403 }, 403, 'SIN_PERMISO'],
    [{ sujetoId: 500 }, 500, 'ERROR_INTERNO'],
  ])('E3 con %o responde %i %s', async (cambio, status, codigo) => {
    await expect(cliente().iniciarEvaluacion({ ...valida, ...cambio })).rejects.toMatchObject({
      status,
      error: { codigo },
    });
  });

  it('E3 exige letra inicial si la aplicación incluye FV', async () => {
    const completa: IniciarEvaluacionRequest = { ...valida, subpruebas: undefined };
    await expect(cliente().iniciarEvaluacion(completa)).rejects.toMatchObject({
      status: 409,
      error: { codigo: 'LETRA_NO_ASIGNADA' },
    });
    await expect(
      cliente().iniciarEvaluacion({ ...completa, fluidez: { letraInicial: 'M' } }),
    ).resolves.toMatchObject({ estado: 'EN_CURSO' });
  });

  it('E3 no permite dos evaluaciones abiertas del mismo sujeto, test y período', async () => {
    const c = cliente();
    await c.iniciarEvaluacion(valida);
    await expect(c.iniciarEvaluacion(valida)).rejects.toMatchObject({
      status: 409,
      error: { codigo: 'EVALUACION_EN_CURSO' },
    });
  });

  it('E4 devuelve el estado del ejemplo de DOC-05 y los errores 403/404', async () => {
    const c = cliente();
    await expect(c.obtenerEvaluacion(15)).resolves.toMatchObject({
      estado: 'EN_CURSO',
      subpruebaActiva: { codigo: 'VOC2', segundosRestantes: 214 },
    });
    await expect(c.obtenerEvaluacion(403)).rejects.toMatchObject({ status: 403 });
    await expect(c.obtenerEvaluacion(77)).rejects.toMatchObject({
      status: 404,
      error: { codigo: 'EVALUACION_NO_ENCONTRADA' },
    });
  });

  describe('E8 finalizar', () => {
    const despuesDelLimite = () => new Date('2026-10-31T10:00:00Z');
    const antesDelLimite = () => new Date('2026-10-31T09:10:00Z');

    it('cierra la ACTIVA vencida con cerradaEn = finLimite y deja el resultado en validación', async () => {
      const c = crearClienteMock({ retardoMs: 0, ahora: despuesDelLimite });
      const cierre = await c.finalizarEvaluacion(17);
      expect(cierre).toMatchObject({
        estado: 'FINALIZADA',
        fechaFin: '2026-10-31T10:00:00.000Z',
        subpruebaActiva: null,
        estadoResultado: 'PENDIENTE_VALIDACION',
      });
      expect(cierre.resultadoId).toEqual(expect.any(Number));
      expect(cierre.subpruebas).toEqual([
        { codigo: 'VOC1', estado: 'CERRADA', cerradaEn: '2026-10-31T09:05:00Z' },
        { codigo: 'CV', estado: 'CERRADA', cerradaEn: '2026-10-31T09:20:00Z' },
      ]);
      await expect(c.obtenerEvaluacion(17)).resolves.toEqual(cierre);
    });

    it('repetirla devuelve el mismo cierre sin otro cálculo', async () => {
      const c = crearClienteMock({ retardoMs: 0, ahora: despuesDelLimite });
      const primero = await c.finalizarEvaluacion(17);
      await expect(c.finalizarEvaluacion(17)).resolves.toEqual(primero);
    });

    it('responde 409 SUBPRUEBAS_PENDIENTES si hay PENDIENTE o ACTIVA sin vencer', async () => {
      await expect(cliente().finalizarEvaluacion(15)).rejects.toMatchObject({
        status: 409,
        error: { codigo: 'SUBPRUEBAS_PENDIENTES' },
      });
      const c = crearClienteMock({ retardoMs: 0, ahora: antesDelLimite });
      await expect(c.finalizarEvaluacion(17)).rejects.toMatchObject({
        status: 409,
        error: { codigo: 'SUBPRUEBAS_PENDIENTES' },
      });
    });

    it('responde 404 y 403 como el resto de endpoints', async () => {
      await expect(cliente().finalizarEvaluacion(77)).rejects.toMatchObject({
        status: 404,
        error: { codigo: 'EVALUACION_NO_ENCONTRADA' },
      });
      await expect(cliente().finalizarEvaluacion(403)).rejects.toMatchObject({ status: 403 });
    });
  });

  it('el ID 503 simula un fallo de conexión como fetch (TypeError)', async () => {
    await expect(cliente().obtenerSujeto(503)).rejects.toBeInstanceOf(TypeError);
  });
});
