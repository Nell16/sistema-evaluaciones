import { afterEach, describe, expect, it, vi } from 'vitest';
import { clienteAcceso } from './cliente';
import { establecerToken } from '../../services/http';

afterEach(() => {
  establecerToken(null);
  vi.unstubAllGlobals();
});

describe('cliente de acceso', () => {
  it('E14 envía correo y contraseña y devuelve el token opaco con rol y permisos', async () => {
    const sesion = {
      token: 'token-ficticio',
      expiraEn: '2099-01-01T00:00:00Z',
      rolActivo: 'SUJETO',
      permisos: ['PRACTICA_REALIZAR'],
    };
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json(sesion)));
    await expect(
      clienteAcceso.iniciarSesion({
        correo: 'prueba@example.invalid',
        contrasena: 'entrada-ficticia',
      }),
    ).resolves.toEqual(sesion);
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/auth/sesion',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ correo: 'prueba@example.invalid', contrasena: 'entrada-ficticia' }),
      }),
    );
  });

  it('E15 revoca el Bearer actual y acepta la respuesta vacía', async () => {
    establecerToken('token-ficticio');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })));
    await expect(clienteAcceso.cerrarSesion()).resolves.toBeUndefined();
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/auth/sesion',
      expect.objectContaining({ method: 'DELETE' }),
    );
    expect(new Headers(vi.mocked(fetch).mock.calls[0][1]?.headers).get('Authorization')).toBe(
      'Bearer token-ficticio',
    );
  });
});
