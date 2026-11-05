import { describe, expect, it, vi } from 'vitest';
import { crearSubpruebaApi } from './contrato';

/** Verifica el adaptador provisional: si DOC-05 cambia rutas o métodos, estas pruebas lo avisan. */
describe('crearSubpruebaApi (contrato provisional)', () => {
  const cliente = () => vi.fn().mockResolvedValue({});

  it('obtiene el estado de la subprueba', async () => {
    const http = cliente();
    await crearSubpruebaApi(http as never).obtener('s 1');
    expect(http).toHaveBeenCalledWith('/subpruebas/s%201');
  });

  it('inicia el tiempo oficial con POST', async () => {
    const http = cliente();
    await crearSubpruebaApi(http as never).iniciar('s1');
    expect(http).toHaveBeenCalledWith('/subpruebas/s1/inicio', { method: 'POST' });
  });

  it('guarda una respuesta con PUT y solo envía el valor', async () => {
    const http = cliente();
    await crearSubpruebaApi(http as never).guardarRespuesta('s1', {
      preguntaId: 'p1',
      valor: 'a',
    });
    expect(http).toHaveBeenCalledWith('/subpruebas/s1/respuestas/p1', {
      method: 'PUT',
      body: JSON.stringify({ valor: 'a' }),
    });
  });

  it('cierra la subprueba con POST', async () => {
    const http = cliente();
    await crearSubpruebaApi(http as never).cerrar('s1');
    expect(http).toHaveBeenCalledWith('/subpruebas/s1/cierre', { method: 'POST' });
  });
});
