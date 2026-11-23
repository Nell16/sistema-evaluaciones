import { renderToStaticMarkup } from 'react-dom/server';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ESCENARIOS_UI05 } from '../../../../dev/ui05-escenarios';
import { aErrorResultados } from '../../../resultados/errores';
import type { ClienteResultados, ResultadoResponse } from '../../../resultados/tipos';
import type { EstadoPeticion } from '../../../evaluacion/tipos';
import { servicioResultados } from '../../../../services/resultados';
import { crearClienteResultadosMock } from '../../../../services/resultados.mock';
import { EstadoPerfilBfa } from './EstadoPerfilBfa';

const mock = crearClienteResultadosMock({ retardoMs: 0 });

/** Las mismas fases de usePeticion (datos o error traducido), sin necesitar un navegador. */
async function consultar(
  id: number,
  cliente: ClienteResultados = mock,
): Promise<EstadoPeticion<ResultadoResponse>> {
  try {
    return { fase: 'exito', datos: await cliente.obtenerResultado(id) };
  } catch (error) {
    return { fase: 'error', error: aErrorResultados(error) };
  }
}

const html = (estado: EstadoPeticion<ResultadoResponse>, extra = {}) =>
  renderToStaticMarkup(<EstadoPerfilBfa estado={estado} {...extra} />);
const exito = (datos: ResultadoResponse): EstadoPeticion<ResultadoResponse> => ({
  fase: 'exito',
  datos,
});
const celdas = (m: string, clase: string) =>
  [...m.matchAll(new RegExp(`class="[^"]*${clase}[^"]*">([^<]*)<`, 'g'))].map((x) => x[1]);
const sinPerfil = (m: string) => {
  expect(m).not.toContain('perfil-tabla');
  expect(m).not.toContain('perfil-compuesto');
  expect(m).not.toContain('perfil-pd');
};
const escenario = (id: string) => ESCENARIOS_UI05.find((e) => e.id === id)!.resultado;

afterEach(() => vi.unstubAllGlobals());

describe('carga', () => {
  it.each(['inicial', 'cargando'] as const)('fase %s: indica que consulta', (fase) => {
    expect(html({ fase })).toContain('Consultando el perfil de resultados');
  });
});

describe('resultados disponibles y parciales', () => {
  it('organiza los 18 factores por área y compuestos tal como llegan', () => {
    const m = html(exito(escenario('completo')));
    for (const grupo of ['Verbal', 'Espacial', 'Razonamiento', 'Numérico', 'Resultados compuestos'])
      expect(m).toContain(grupo);
    expect(celdas(m, 'perfil-pd')).toHaveLength(18);
    expect(m).toContain('Perfil completo');
  });

  it('parcial: muestra solo lo recibido, sin valores ficticios', async () => {
    const m = html(await consultar(16));
    expect(m).toContain('Perfil parcial');
    expect(m).toContain('5 de 18 calculados');
    expect(celdas(m, 'perfil-pd').filter((t) => /^\d+$/.test(t))).toEqual([
      '20',
      '18',
      '38',
      '0',
      '12',
    ]);
    expect(celdas(m, 'perfil-pc').filter((t) => t.startsWith('Pc'))).toEqual([
      'Pc 50',
      'Pc 45',
      'Pc 50',
    ]);
    expect(m).toContain('Falta un componente: FV.');
    expect(m).toContain('Sin coincidencia en el baremo');
    expect(m).toContain('No recibido');
  });
});

describe('sin revisión publicada', () => {
  it.each([
    [17, 'Pendiente de validación'],
    [18, 'En cálculo'],
  ])('evaluación %i: muestra el estado y ningún valor', async (id, etiqueta) => {
    const m = html(await consultar(id));
    expect(m).toContain(etiqueta);
    expect(m).toContain('No hay una revisión publicada');
    sinPerfil(m);
  });

  it('DISPONIBLE sin revisión identificada: los factores internos no se muestran', async () => {
    const r = { ...(await mock.obtenerResultado(16)), calificacionId: null, revision: null };
    sinPerfil(html(exito(r)));
  });

  it('pendiente de validación con factores enviados: no se presentan como definitivos', async () => {
    const r = {
      ...(await mock.obtenerResultado(16)),
      estadoResultado: 'PENDIENTE_VALIDACION' as const,
    };
    sinPerfil(html(exito(r)));
  });

  it('distingue evaluación no finalizada de resultado pendiente de validación', async () => {
    const noFinalizada = html(await consultar(15));
    expect(noFinalizada).toContain('aún no ha finalizado');
    expect(noFinalizada).not.toContain('Pendiente de validación');
    expect(noFinalizada).not.toContain('ev-error');
    const pendiente = html(await consultar(17));
    expect(pendiente).toContain('Pendiente de validación');
    expect(pendiente).not.toContain('aún no ha finalizado');
  });
});

describe('errores y permisos', () => {
  it('sin permiso (403): lo informa, no muestra datos ni ofrece reintentar', async () => {
    const m = html(await consultar(403), { onReintentar: () => undefined });
    expect(m).toContain('Acceso denegado');
    expect(m).toContain('No tiene permiso para consultar los resultados de esta evaluación.');
    expect(m).not.toContain('Reintentar');
    sinPerfil(m);
  });

  it('con el cliente HTTP real: 403 y 401 del backend se informan sin datos', async () => {
    const responder = (status: number, codigo: string) =>
      vi.fn(async () => new Response(JSON.stringify({ status, codigo, mensaje: 'x' }), { status }));
    const fetchSimulado = responder(403, 'SIN_PERMISO');
    vi.stubGlobal('fetch', fetchSimulado);
    const m403 = html(await consultar(16, servicioResultados));
    const [ruta] = fetchSimulado.mock.calls[0] as unknown as [string];
    expect(ruta).toBe('/api/v1/evaluaciones/16/resultados');
    expect(m403).toContain('Acceso denegado');
    sinPerfil(m403);

    vi.stubGlobal('fetch', responder(401, 'NO_AUTENTICADO'));
    const m401 = html(await consultar(16, servicioResultados));
    expect(m401).toContain('Su sesión no es válida');
    sinPerfil(m401);
  });

  it('con el cliente HTTP real: un 200 muestra exactamente lo que devuelve E9', async () => {
    const r = await mock.obtenerResultado(16);
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response(JSON.stringify(r), { status: 200 })),
    );
    const m = html(await consultar(16, servicioResultados));
    expect(celdas(m, 'perfil-pd').filter((t) => /^\d+$/.test(t))).toEqual([
      '20',
      '18',
      '38',
      '0',
      '12',
    ]);
  });

  it('no existe (404) se informa', async () => {
    expect(html(await consultar(999))).toContain('No existe una evaluación con ese ID.');
  });

  it.each([500, 503])('fallo %i: se informa y se ofrece reintentar', async (id) => {
    const m = html(await consultar(id), { onReintentar: () => undefined });
    expect(m).toContain('Reintentar');
    sinPerfil(m);
  });

  it('avisa cuando los datos son simulados', async () => {
    expect(html(await consultar(16), { datosSimulados: true })).toContain('Datos simulados');
  });
});
