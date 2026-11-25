import { useState } from 'react';
import type { TestOption } from '../../components/AnswerOption';
import { TestScreen } from '../../components/TestScreen';
import {
  LAMINAS,
  OPCIONES_POR_REACTIVO,
  POSICIONES_POR_LAMINA,
  TOTAL_REACTIVOS,
  laminaYPosicion,
} from './estructura';
import './weill.css';

const NOMBRE_TEST = 'Test de Weill · prototipo';

type Pantalla = 'inicio' | 'instrucciones' | 'reactivo' | 'cierre';

const OPCIONES: TestOption[] = Array.from({ length: OPCIONES_POR_REACTIVO }, (_, indice) => ({
  id: String.fromCharCode(97 + indice),
  label: `Opción ${indice + 1}`,
}));

/** W02 · RF-003, RF-004. Datos ficticios: la versión conectada los toma de la API. */
function InicioAplicacion({ onIniciar }: { onIniciar: () => void }) {
  const [sujeto, setSujeto] = useState('');
  const [periodo, setPeriodo] = useState('');
  const [escolaridad, setEscolaridad] = useState('');
  const completo = [sujeto, periodo, escolaridad].every((valor) => valor.trim() !== '');

  return (
    <section className="test-paper" aria-labelledby="weill-inicio-titulo">
      <header className="test-header">
        <span className="test-name">{NOMBRE_TEST}</span>
      </header>
      <div className="test-content">
        <p className="phase-label">Inicio de aplicación</p>
        <h1 className="question-prompt" id="weill-inicio-titulo">
          Datos de la aplicación
        </h1>
        <p className="phase-description">
          Use datos ficticios. En la versión conectada se eligen de la API: sujeto registrado,
          periodo activo y catálogo de escolaridad (por confirmar).
        </p>
        <form
          className="weill-form"
          onSubmit={(evento) => {
            evento.preventDefault();
            if (completo) onIniciar();
          }}
        >
          <label className="weill-field">
            Sujeto evaluado
            <input value={sujeto} onChange={(evento) => setSujeto(evento.target.value)} />
          </label>
          <label className="weill-field">
            Periodo activo
            <input value={periodo} onChange={(evento) => setPeriodo(evento.target.value)} />
          </label>
          <label className="weill-field">
            Escolaridad
            <input value={escolaridad} onChange={(evento) => setEscolaridad(evento.target.value)} />
          </label>
          <p className="weill-grupo">
            Grupo de referencia: indeterminado (pendiente de Psicología, H-06).
          </p>
          <button className="primary-action" type="submit" disabled={!completo}>
            Iniciar aplicación
          </button>
        </form>
      </div>
    </section>
  );
}

/**
 * Prototipo navegable del flujo de Weill (DOC-10): inicio, instrucciones, reactivos y cierre.
 * No llama a la API ni guarda nada: las selecciones viven en memoria. No supone tiempo
 * límite, pausa, retroceso ni tratamiento de omitidos, que siguen abiertos (H-01, H-08, H-09).
 */
export function WeillPrototipo() {
  const [pantalla, setPantalla] = useState<Pantalla>('inicio');
  const [orden, setOrden] = useState(1);
  const [selecciones, setSelecciones] = useState<Record<number, string>>({});

  const reiniciar = () => {
    setOrden(1);
    setSelecciones({});
    setPantalla('inicio');
  };

  if (pantalla === 'inicio') {
    return <InicioAplicacion onIniciar={() => setPantalla('instrucciones')} />;
  }

  if (pantalla === 'instrucciones') {
    return (
      <TestScreen
        testName={NOMBRE_TEST}
        questionNumber={0}
        totalQuestions={TOTAL_REACTIVOS}
        timeRemainingSeconds={null}
        prompt=""
        selectedOption={null}
        saveState="saved"
        phase="instructions"
        phaseTitle="Instrucciones"
        phaseDescription="[Texto oficial pendiente de Psicología (H-10). No se redacta ni se supone aquí.]"
        primaryActionLabel="Comenzar"
        onNext={() => setPantalla('reactivo')}
      />
    );
  }

  if (pantalla === 'cierre') {
    const respondidos = Object.keys(selecciones).length;
    return (
      <TestScreen
        testName={NOMBRE_TEST}
        questionNumber={TOTAL_REACTIVOS}
        totalQuestions={TOTAL_REACTIVOS}
        timeRemainingSeconds={null}
        prompt=""
        selectedOption={null}
        saveState="saved"
        phase="closed"
        phaseTitle="Aplicación finalizada"
        phaseDescription={`Prototipo sin conexión: ${respondidos} de ${TOTAL_REACTIVOS} reactivos con selección. El cierre real, el tratamiento de omitidos y lo que ve el evaluado dependen de decisiones abiertas (DEC-07, DEC-09, DEC-15).`}
        primaryActionLabel="Volver al inicio"
        onNext={reiniciar}
      />
    );
  }

  const { lamina, posicion } = laminaYPosicion(orden);
  const esUltimo = orden === TOTAL_REACTIVOS;

  return (
    <TestScreen
      key={orden}
      testName={NOMBRE_TEST}
      questionNumber={orden}
      totalQuestions={TOTAL_REACTIVOS}
      questionLabel={`Lámina ${lamina} de ${LAMINAS} · posición ${posicion} de ${POSICIONES_POR_LAMINA}`}
      timeRemainingSeconds={null}
      prompt="Seleccione una opción"
      questionResource={
        <div className="weill-recurso">
          [Recurso visual del reactivo: referencia autorizada, no incluida en el prototipo]
        </div>
      }
      options={OPCIONES}
      optionsLayout="compact"
      selectedOption={selecciones[orden] ?? null}
      saveState="saved"
      onSelect={(opcion) => setSelecciones((previas) => ({ ...previas, [orden]: opcion }))}
      primaryActionLabel={esUltimo ? 'Finalizar' : 'Siguiente'}
      onNext={() => {
        if (esUltimo) {
          setPantalla('cierre');
        } else {
          setOrden(orden + 1);
        }
      }}
    />
  );
}
