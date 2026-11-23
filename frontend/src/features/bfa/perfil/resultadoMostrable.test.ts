import { describe, expect, it } from 'vitest';
import { crearClienteResultadosMock } from '../../../services/resultados.mock';
import { puedeMostrarFactores, sinFactoresNoPublicados } from './resultadoMostrable';

const cliente = crearClienteResultadosMock({ retardoMs: 0 });

describe('resultadoMostrable', () => {
  it('muestra factores solo con estado DISPONIBLE, revisión publicada y factores recibidos', async () => {
    const r = await cliente.obtenerResultado(16);
    expect(puedeMostrarFactores(r)).toBe(true);
    expect(sinFactoresNoPublicados(r)).toBe(r); // sin copiar ni modificar
  });

  it.each([
    ['sin revisión (calificacionId nulo)', { calificacionId: null }],
    ['sin revisión (revision nula)', { revision: null }],
    ['PENDIENTE_VALIDACION', { estadoResultado: 'PENDIENTE_VALIDACION' as const }],
    ['CALCULANDO', { estadoResultado: 'CALCULANDO' as const }],
    ['sin factores', { factores: [] }],
  ])('descarta los factores: %s', async (_nombre, cambio) => {
    const r = { ...(await cliente.obtenerResultado(16)), ...cambio };
    expect(puedeMostrarFactores(r)).toBe(false);
    expect(sinFactoresNoPublicados(r).factores).toEqual([]);
  });

  it('no altera el resultado original al descartar factores', async () => {
    const r = { ...(await cliente.obtenerResultado(16)), calificacionId: null };
    const antes = r.factores.length;
    sinFactoresNoPublicados(r);
    expect(r.factores).toHaveLength(antes);
  });
});
