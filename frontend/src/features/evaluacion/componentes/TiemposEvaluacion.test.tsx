import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import type { TestDetalleResponse } from '../tipos';
import { FormularioInicio } from './FormularioInicio';

const test: TestDetalleResponse = {
  id: 1,
  codigo: 'DEMO',
  nombre: 'Test ficticio',
  versionFormularioId: 1,
  subpruebas: [
    {
      codigo: 'S1',
      nombre: 'Primera',
      orden: 1,
      factor: null,
      tiempoLimiteSegundos: 300,
      tipoRespuesta: 'SELECCION',
      opciones: 4,
      numeroPreguntas: 1,
      maximoEntradas: null,
      tienePractica: false,
    },
    {
      codigo: 'S2',
      nombre: 'Segunda',
      orden: 2,
      factor: null,
      tiempoLimiteSegundos: null,
      tipoRespuesta: 'SELECCION',
      opciones: 4,
      numeroPreguntas: 1,
      maximoEntradas: null,
      tienePractica: false,
    },
  ],
};

describe('tiempos de inicio de evaluación', () => {
  it('muestra el tiempo desconocido sin inventar un límite ni un total', () => {
    const html = renderToStaticMarkup(
      <FormularioInicio test={test} periodos={[]} onIniciada={() => {}} />,
    );
    expect(html).toContain('5:00');
    expect(html).not.toContain('0:00');
    expect(html.match(/No definido/g)).toHaveLength(2);
  });
});
