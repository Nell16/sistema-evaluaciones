import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter, Route, Routes } from 'react-router';
import { describe, expect, it } from 'vitest';
import { rutasBfa } from '../rutas';
import { PerfilBfaPagina } from './PerfilBfaPagina';

const pagina = (ruta: string) =>
  renderToStaticMarkup(
    <MemoryRouter initialEntries={[ruta]}>
      <Routes>
        <Route path="bfa/evaluaciones/:evaluacionId/perfil" element={<PerfilBfaPagina />} />
      </Routes>
    </MemoryRouter>,
  );

describe('PerfilBfaPagina', () => {
  it('con un ID válido muestra el título y empieza a consultar', () => {
    const m = pagina('/bfa/evaluaciones/16/perfil');
    expect(m).toContain('Resultados BFA');
    expect(m).toContain('Consultando el perfil de resultados');
  });

  it.each(['abc', '0', '-5', '1.5'])('con el ID «%s» avisa y no consulta', (id) => {
    const m = pagina(`/bfa/evaluaciones/${id}/perfil`);
    expect(m).toContain('debe ser un número entero positivo');
    expect(m).not.toContain('Consultando');
  });

  it('la ruta del perfil está registrada en el módulo BFA', () => {
    expect(rutasBfa.map((r) => r.path)).toContain('bfa/evaluaciones/:evaluacionId/perfil');
  });
});
