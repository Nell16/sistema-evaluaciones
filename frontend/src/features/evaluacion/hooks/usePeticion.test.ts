import { beforeEach, describe, expect, it, vi } from 'vitest';

const { guardarEstado } = vi.hoisted(() => ({ guardarEstado: vi.fn() }));
vi.mock('react', () => ({
  useState: () => [{ fase: 'inicial' }, guardarEstado],
  useRef: (valor: number) => ({ current: valor }),
  useCallback: <T>(funcion: T) => funcion,
}));

import { usePeticion } from './usePeticion';

function diferida<T>() {
  let resolver!: (valor: T) => void;
  const promesa = new Promise<T>((resolve) => {
    resolver = resolve;
  });
  return { promesa, resolver };
}

beforeEach(() => guardarEstado.mockClear());

describe('usePeticion', () => {
  it('descarta el retorno de una respuesta anterior que llega después', async () => {
    const primera = diferida<string>();
    const segunda = diferida<string>();
    const operacion = vi
      .fn()
      .mockReturnValueOnce(primera.promesa)
      .mockReturnValueOnce(segunda.promesa);
    const peticion = usePeticion(operacion);
    const respuestaAnterior = peticion.ejecutar();
    const respuestaActual = peticion.ejecutar();

    segunda.resolver('actual');
    await expect(respuestaActual).resolves.toBe('actual');
    primera.resolver('anterior');
    await expect(respuestaAnterior).resolves.toBeNull();
    expect(guardarEstado).toHaveBeenLastCalledWith({ fase: 'exito', datos: 'actual' });
  });

  it('descarta el retorno de una solicitud reiniciada mientras estaba pendiente', async () => {
    const pendiente = diferida<string>();
    const peticion = usePeticion(() => pendiente.promesa);
    const respuesta = peticion.ejecutar();
    peticion.reiniciar();
    pendiente.resolver('obsoleto');

    await expect(respuesta).resolves.toBeNull();
    expect(guardarEstado).toHaveBeenLastCalledWith({ fase: 'inicial' });
  });
});
