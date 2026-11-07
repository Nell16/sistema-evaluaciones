import type { ReactNode } from 'react';

type HojaSimpleProps = {
  nombre: string;
  etiqueta: string;
  titulo: string;
  descripcion?: string;
  children?: ReactNode;
  /** Sin acción, la hoja no muestra pie (carga, cierre en curso). */
  accion?: { texto: string; alPulsar: () => void };
};

/** Hoja de test con el mismo marco que `TestScreen`, para pantallas que no son preguntas. */
export function HojaSimple({
  nombre,
  etiqueta,
  titulo,
  descripcion,
  children,
  accion,
}: HojaSimpleProps) {
  return (
    <section className="test-paper" aria-labelledby="hoja-simple-titulo" aria-busy={!accion}>
      <header className="test-header">
        <span className="test-name">{nombre}</span>
        <span className="test-timer">—</span>
      </header>
      <div className="test-content">
        <section className="phase-panel">
          <p className="phase-label">{etiqueta}</p>
          <h1 className="question-prompt" id="hoja-simple-titulo">
            {titulo}
          </h1>
          {descripcion && <p className="phase-description">{descripcion}</p>}
          {children}
        </section>
      </div>
      {accion && (
        <footer className="test-footer test-footer--solo">
          <button className="primary-action" type="button" onClick={accion.alPulsar}>
            {accion.texto}
          </button>
        </footer>
      )}
    </section>
  );
}
