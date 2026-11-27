import { afterEach, describe, expect, it, vi } from 'vitest';
import { RevisionSinIdentificadoresError, servicioRevision } from './revision';

afterEach(() => vi.unstubAllGlobals());

function simular(cuerpo: unknown) {
  const f = vi.fn(async () => new Response(JSON.stringify(cuerpo), { status: 200 }));
  vi.stubGlobal('fetch', f);
  return f;
}
const llamada = (f: ReturnType<typeof simular>) =>
  f.mock.calls[0] as unknown as [string, RequestInit];

describe('cliente real de E10, E11 y E12', () => {
  it('no usa el orden de entrada como identificador de respuesta', async () => {
    const fetch = vi.fn(async (ruta: string) =>
      Response.json(
        ruta.endsWith('/resultados')
          ? { calificacionId: 31, revision: 1 }
          : { palabras: [{ ordenEntrada: 1, texto: 'mesa' }] },
      ),
    );
    vi.stubGlobal('fetch', fetch);

    await expect(servicioRevision.cargar(15)).rejects.toBeInstanceOf(
      RevisionSinIdentificadoresError,
    );
    expect(fetch).toHaveBeenCalledTimes(2);
  });

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
