import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it, vi } from 'vitest';
import { BfaApiSimulada } from '../api/bfaApiSimulada';
import { presentacionDe, presentacionPorSubprueba } from '../familias';
import { ControladorBfa } from '../flujo/controlador';
import { VistaFlujo } from './VistaFlujo';

// Consulta C-1 de GEN-03: qué subpruebas restringen volver a una pregunta anterior. Mientras siga
// abierta, el Documento BFA §5.2 («puede permitir volver») no restringe ninguna, y el README lo dice.

async function enEjecucion(codigo: string) {
  const api = new BfaApiSimulada();
  const { id } = await api.iniciarEvaluacion({
    versionFormularioId: 10,
    sujetoId: 25,
    periodoId: 3,
    subpruebas: [codigo],
  });
  const c = new ControladorBfa(api, id);
  await c.cargar();
  c.continuar(); // instrucciones → práctica
  c.continuar(); // práctica → inicia el tiempo oficial
  await vi.waitFor(() => expect(c.obtenerEstado().fase).toBe('ejecucion'));
  return c;
}

const pantalla = (c: ControladorBfa) =>
  renderToStaticMarkup(
    createElement(VistaFlujo, {
      estado: c.obtenerEstado(),
      controlador: c,
      alSalir: () => undefined,
    }),
  );

describe('Volver atrás (C-1)', () => {
  it('todas las subpruebas lo permiten mientras C-1 siga abierta', () => {
    for (const codigo of Object.keys(presentacionPorSubprueba)) {
      expect(presentacionDe(codigo).volverAtras, codigo).toBe(true);
    }
  });

  it.each(['VOC1', 'R1', 'N1-A', 'N1-B'])(
    '%s: «Anterior» no aparece en la primera pregunta y sí desde la segunda',
    async (codigo) => {
      const c = await enEjecucion(codigo);
      expect(pantalla(c)).not.toContain('Anterior');
      c.siguiente();
      const marcado = pantalla(c);
      expect(marcado).toContain('Anterior');
      expect(marcado.match(/primary-action/g)).toHaveLength(1); // un solo botón oscuro
    },
  );
});
