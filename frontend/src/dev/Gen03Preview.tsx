import { useMemo, useState } from 'react';
import type {
  EstadoSubprueba,
  FaseSubprueba,
  ReglaNavegacion,
  SubpruebaApi,
} from '../features/evaluacion/subprueba/contrato';
import { SubpruebaRunner } from '../features/evaluacion/subprueba/SubpruebaRunner';

/**
 * Vista previa de GEN-03-FE con un backend simulado en memoria (contenido demostrativo).
 *
 * Parámetros opcionales de la URL:
 *  - `?tiempo=45`    segundos de tiempo oficial (por defecto 120).
 *  - `?nav=libre`    permite retroceder (por defecto `secuencial`).
 *  - `?fallo=1`      el guardado de respuestas siempre falla (para ver el error).
 */

const LATENCIA_MS = 350;

type OpcionesDemo = { tiempo: number; navegacion: ReglaNavegacion; fallaGuardado: boolean };

function leerOpciones(): OpcionesDemo {
  const parametros = new URLSearchParams(window.location.search);
  const tiempo = Number(parametros.get('tiempo'));
  return {
    tiempo: Number.isFinite(tiempo) && tiempo > 0 ? tiempo : 120,
    navegacion: parametros.get('nav') === 'libre' ? 'libre' : 'secuencial',
    fallaGuardado: parametros.get('fallo') === '1',
  };
}

function crearApiDemo({ tiempo, navegacion, fallaGuardado }: OpcionesDemo): SubpruebaApi {
  let fase: FaseSubprueba = 'instrucciones';
  let limite: number | null = null;
  const respuestas = new Map<string, string>();

  const esperar = () => new Promise((resolver) => setTimeout(resolver, LATENCIA_MS));
  const restante = () =>
    limite === null ? null : Math.max(0, Math.ceil((limite - Date.now()) / 1000));

  const estado = (): EstadoSubprueba => ({
    subpruebaId: 'demo',
    nombre: 'Subprueba de muestra',
    fase,
    instrucciones: 'Estas son instrucciones de ejemplo. Lee con atención antes de continuar.',
    practica: 'Esta es una práctica de ejemplo: no consume tiempo oficial.',
    navegacion,
    tiempoRestanteSegundos: restante(),
    respuestas: [...respuestas].map(([preguntaId, valor]) => ({ preguntaId, valor })),
    preguntas: [
      {
        id: 'p1',
        enunciado: 'Pregunta de selección de ejemplo',
        tipo: 'seleccion',
        opciones: [
          { id: 'a', texto: 'Opción de ejemplo A' },
          { id: 'b', texto: 'Opción de ejemplo B' },
          { id: 'c', texto: 'Opción de ejemplo C' },
        ],
      },
      {
        id: 'p2',
        enunciado: 'Otra pregunta de selección de ejemplo',
        tipo: 'seleccion',
        opciones: [
          { id: 'a', texto: 'Opción de ejemplo A' },
          { id: 'b', texto: 'Opción de ejemplo B' },
        ],
      },
      {
        id: 'p3',
        enunciado: 'Pregunta de texto libre de ejemplo',
        tipo: 'texto',
        longitudMaxima: 200,
      },
    ],
  });

  return {
    obtener: async () => {
      await esperar();
      return estado();
    },
    iniciar: async () => {
      await esperar();
      fase = 'activa';
      limite = Date.now() + tiempo * 1000;
      return estado();
    },
    guardarRespuesta: async (_id, respuesta) => {
      await esperar();
      if (fallaGuardado) throw new Error('Fallo simulado');
      if (restante() === 0) fase = 'cerrada';
      else respuestas.set(respuesta.preguntaId, respuesta.valor);
      return { fase, tiempoRestanteSegundos: restante() };
    },
    cerrar: async () => {
      await esperar();
      fase = 'cerrada';
      limite = null;
      return estado();
    },
  };
}

export function Gen03Preview() {
  const [opciones] = useState(leerOpciones);
  const api = useMemo(() => crearApiDemo(opciones), [opciones]);
  const [reinicios, setReinicios] = useState(0);

  return (
    <main className="app-shell">
      <section className="preview-toolbar" aria-labelledby="preview-gen03-title">
        <div className="preview-toolbar-heading">
          <p className="preview-eyebrow">GEN-03-FE · herramienta de preview</p>
          <h1 id="preview-gen03-title">Presentación y captura de respuestas</h1>
          <p>
            Backend simulado en memoria con contenido de ejemplo. Parámetros en la URL:{' '}
            <code>?tiempo=45</code>, <code>?nav=libre</code>, <code>?fallo=1</code>.
          </p>
        </div>
      </section>

      <div className="test-stage">
        <SubpruebaRunner
          key={reinicios}
          subpruebaId="demo"
          api={api}
          onCerrada={() => setReinicios((actual) => actual + 1)}
        />
      </div>
    </main>
  );
}
