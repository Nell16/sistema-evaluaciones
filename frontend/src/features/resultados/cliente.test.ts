import { afterEach, describe, expect, it, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllEnvs();
  vi.resetModules();
});

describe('selección del cliente de resultados', () => {
  it.each([
    [undefined, false],
    ['false', false],
    ['true', true],
  ])('con VITE_USAR_MOCKS=%s, usaMocksResultados es %s', async (valor, esperado) => {
    vi.stubEnv('VITE_USAR_MOCKS', valor);
    vi.resetModules();
    const { usaMocksResultados, clienteResultados } = await import('./cliente');
    const { servicioResultados } = await import('../../services/resultados');

    expect(usaMocksResultados).toBe(esperado);
    if (!esperado) expect(clienteResultados).toBe(servicioResultados);
    else expect(clienteResultados).not.toBe(servicioResultados);
  });
});
