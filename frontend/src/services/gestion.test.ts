import { afterEach, describe, expect, it, vi } from 'vitest';
import { servicioGestion } from './gestion';

afterEach(() => vi.unstubAllGlobals());

describe('cliente real de E22', () => {
  it('consulta las evaluaciones sin parámetros si no hay filtros', async () => {
    const fetch = vi.fn(async () => Response.json([]));
    vi.stubGlobal('fetch', fetch);

    await expect(servicioGestion.listarEvaluaciones()).resolves.toEqual([]);
    expect(fetch).toHaveBeenCalledWith('/api/v1/evaluaciones', expect.anything());
  });

  it('envía los filtros del contrato y omite los campos ajenos y ausentes', async () => {
    const fetch = vi.fn(async () => Response.json([]));
    vi.stubGlobal('fetch', fetch);
    const filtro = { sujetoId: 25, periodoId: 3, estado: 'FINALIZADA' as const, pagina: 1 };

    await servicioGestion.listarEvaluaciones(filtro);

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/evaluaciones?sujetoId=25&periodoId=3&estado=FINALIZADA',
      expect.anything(),
    );
  });
});
