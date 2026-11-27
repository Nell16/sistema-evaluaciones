import { afterEach, describe, expect, it, vi } from 'vitest';
import { aErrorMostrable } from '../features/evaluacion/mensajesError';
import { ApiError } from './http';
import { MENSAJES_BLOQUEO, servicioRevision } from './revision';

afterEach(() => vi.unstubAllGlobals());

function simular(cuerpo: unknown) {
  const f = vi.fn(async () => new Response(JSON.stringify(cuerpo), { status: 200 }));
  vi.stubGlobal('fetch', f);
  return f;
}
const llamada = (f: ReturnType<typeof simular>) =>
  f.mock.calls[0] as unknown as [string, RequestInit];

describe('cliente real de E10, E11 y E12', () => {
  it('E10: PUT a la revisión de la palabra', async () => {
    const f = simular({});
    await servicioRevision.revisarPalabra(15, 31, 7, { decision: 'VALIDA', motivo: 'ok' });
    const [ruta, op] = llamada(f);
    expect(ruta).toBe('/api/v1/evaluaciones/15/calificaciones/31/palabras/7/revision');
    expect(op.method).toBe('PUT');
    expect(JSON.parse(op.body as string)).toEqual({ decision: 'VALIDA', motivo: 'ok' });
  });

  it('E11: POST a recalculos', async () => {
    const f = simular({});
    await servicioRevision.recalcular(15, { motivo: 'm', idSolicitud: 'a' });
    const [ruta, op] = llamada(f);
    expect(ruta).toBe('/api/v1/evaluaciones/15/recalculos');
    expect(op.method).toBe('POST');
    expect(JSON.parse(op.body as string)).toEqual({ motivo: 'm', idSolicitud: 'a' });
  });

  it('E12: PUT a resultados/vigente', async () => {
    const f = simular({});
    const cuerpo = {
      calificacionId: 31,
      motivo: 'm',
      revisionVigenteEsperada: null,
      audiencia: ['SUJETO'],
    };
    await servicioRevision.publicar(15, cuerpo);
    const [ruta, op] = llamada(f);
    expect(ruta).toBe('/api/v1/evaluaciones/15/resultados/vigente');
    expect(op.method).toBe('PUT');
    expect(JSON.parse(op.body as string)).toEqual(cuerpo);
  });
});

/** Responde según la ruta: E9 (resultados) o E6 (preguntas de FV). */
function simularCarga(e6: { status: number; cuerpo: unknown }) {
  const f = vi.fn(async (ruta: string) =>
    ruta.endsWith('/resultados')
      ? Response.json({ calificacionId: 31, revision: 1 })
      : new Response(JSON.stringify(e6.cuerpo), { status: e6.status }),
  );
  vi.stubGlobal('fetch', f);
  return f;
}

async function codigoDe(promesa: Promise<unknown>) {
  const e = await promesa.then(
    () => null,
    (err: unknown) => err,
  );
  expect(e).toBeInstanceOf(ApiError);
  return (e as ApiError).error?.codigo;
}

describe('BFA-FE-31: carga y validaciones contra el contrato', () => {
  it('E6 cerrado (SUBPRUEBA_NO_ACTIVA) se informa como REVISION_SIN_PALABRAS', async () => {
    simularCarga({
      status: 409,
      cuerpo: {
        timestamp: '2026-11-27T15:00:00Z',
        status: 409,
        codigo: 'SUBPRUEBA_NO_ACTIVA',
        mensaje: 'La subprueba no está activa.',
        ruta: '/api/v1/evaluaciones/15/subpruebas/FV/preguntas',
      },
    });
    expect(await codigoDe(servicioRevision.cargar(15))).toBe('REVISION_SIN_PALABRAS');
  });

  it('el mensaje del bloqueo llega a la pantalla sin cambiar aErrorMostrable', async () => {
    simularCarga({
      status: 409,
      cuerpo: {
        status: 409,
        codigo: 'SUBPRUEBA_NO_ACTIVA',
        mensaje: 'x',
        ruta: 'x',
        timestamp: 'x',
      },
    });
    const e = await servicioRevision.cargar(15).catch((err: unknown) => err);
    expect(aErrorMostrable(e)).toMatchObject({
      tipo: 'conflicto',
      codigo: 'REVISION_SIN_PALABRAS',
      mensaje: MENSAJES_BLOQUEO.REVISION_SIN_PALABRAS,
    });
  });

  it('no usa ordenEntrada como respuestaId', async () => {
    const f = simularCarga({
      status: 200,
      cuerpo: { codigo: 'FV', palabras: [{ ordenEntrada: 1, texto: 'mesa' }] },
    });
    expect(await codigoDe(servicioRevision.cargar(15))).toBe('REVISION_SIN_IDENTIFICADORES');
    expect(f).toHaveBeenCalledTimes(2);
  });

  it('otros errores de E6 se propagan sin cambios', async () => {
    simularCarga({
      status: 404,
      cuerpo: {
        status: 404,
        codigo: 'SUBPRUEBA_NO_ENCONTRADA',
        mensaje: 'x',
        ruta: 'x',
        timestamp: 'x',
      },
    });
    expect(await codigoDe(servicioRevision.cargar(15))).toBe('SUBPRUEBA_NO_ENCONTRADA');
  });

  it('sin palabras devuelve la revisión de E9', async () => {
    simularCarga({ status: 200, cuerpo: { codigo: 'FV', palabras: [] } });
    await expect(servicioRevision.cargar(15)).resolves.toEqual({
      calificacionId: 31,
      revision: 1,
      revisionVigente: 1,
      palabras: [],
    });
  });

  it('E10 no llama a /calificaciones/0 cuando no hay calificación', async () => {
    const f = simular({});
    const r = servicioRevision.revisarPalabra(15, 0, 7, { decision: 'VALIDA', motivo: 'ok' });
    expect(await codigoDe(r)).toBe('REVISION_SIN_CALIFICACION');
    expect(f).not.toHaveBeenCalled();
  });

  it('E12 no publica sin calificación candidata', async () => {
    const f = simular({});
    const r = servicioRevision.publicar(15, {
      calificacionId: 0,
      motivo: 'm',
      revisionVigenteEsperada: null,
      audiencia: ['SUJETO'],
    });
    expect(await codigoDe(r)).toBe('REVISION_SIN_CALIFICACION');
    expect(f).not.toHaveBeenCalled();
  });
});
