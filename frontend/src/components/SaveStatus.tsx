/**
 * `idle`: todavía no hay respuesta que guardar. `pending`: respuesta escrita pero aún sin
 * confirmar por el servidor. `saved`: confirmada por el servidor.
 */
export type SaveState = 'idle' | 'pending' | 'saved' | 'saving' | 'error';

type SaveStatusProps = {
  state: SaveState;
};

const saveStateLabels: Record<SaveState, string> = {
  idle: 'Sin respuesta',
  pending: 'Sin guardar',
  saved: 'Guardado',
  saving: 'Guardando',
  error: 'No se pudo guardar',
};

const saveStateIcons: Record<SaveState, string> = {
  idle: '–',
  pending: '○',
  saved: '✓',
  saving: '···',
  error: '!',
};

export function SaveStatus({ state }: SaveStatusProps) {
  return (
    <p className={`save-status save-status--${state}`} role="status" aria-live="polite">
      <span className="status-icon" aria-hidden="true">
        {saveStateIcons[state]}
      </span>
      {saveStateLabels[state]}
    </p>
  );
}
