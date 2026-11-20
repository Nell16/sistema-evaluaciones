import { TestNotice, type TestNoticeData } from '../../../components/TestNotice';
import type { SaveState } from '../../../components/SaveStatus';
import { TestScreen, type TestPhase } from '../../../components/TestScreen';
import { crearSubpruebaApi, type FaseSubprueba, type SubpruebaApi } from './contrato';
import {
  preguntaActual,
  puedeRetroceder,
  respuestaActual,
  esUltimaPregunta,
  type EstadoGuardado,
} from './estado';
import { useSubprueba } from './useSubprueba';

type SubpruebaRunnerProps = {
  subpruebaId: string;
  /** Permite sustituir el acceso al backend (pruebas, vista previa). */
  api?: SubpruebaApi;
  /** Se llama al pulsar "Continuar" cuando la subprueba ya está cerrada. */
  onCerrada?: () => void;
};

const apiPorDefecto = crearSubpruebaApi();

/** Segundos restantes por debajo de los cuales se avisa a la persona. */
const UMBRAL_AVISO_SEGUNDOS = 60;

const fases: Record<FaseSubprueba, TestPhase> = {
  instrucciones: 'instructions',
  practica: 'practice',
  activa: 'active',
  cerrada: 'closed',
};

const estadosDeGuardado: Record<EstadoGuardado, SaveState> = {
  pendiente: 'pending',
  guardando: 'saving',
  confirmada: 'saved',
  error: 'error',
};

/**
 * Presenta una subprueba (instrucciones, práctica, preguntas con temporizador) y registra las
 * respuestas. Es independiente del test: no conoce reglas de calificación, solo la configuración
 * que entrega el backend.
 */
export function SubpruebaRunner({
  subpruebaId,
  api = apiPorDefecto,
  onCerrada,
}: SubpruebaRunnerProps) {
  const subprueba = useSubprueba(subpruebaId, api);
  const { estado, aviso, segundosRestantes } = subprueba;

  if (estado.carga === 'cargando') {
    return (
      <p className="test-loading" role="status">
        Cargando subprueba…
      </p>
    );
  }

  if (estado.carga === 'error' || !estado.subprueba) {
    return (
      <section className="test-paper" aria-label="Error al cargar la subprueba">
        <div className="test-content">
          <TestNotice
            kind="error"
            message={estado.mensajeError ?? 'No se pudo cargar la subprueba.'}
          />
        </div>
        <div className="test-retry">
          <button className="primary-action" type="button" onClick={() => void subprueba.cargar()}>
            Reintentar
          </button>
        </div>
      </section>
    );
  }

  const { fase } = estado;
  const config = estado.subprueba;
  const pregunta = preguntaActual(estado);
  const respuesta = respuestaActual(estado);
  const activa = fase === 'activa';

  const notice: TestNoticeData | undefined = aviso
    ? { kind: 'error', message: aviso }
    : activa && segundosRestantes !== null && segundosRestantes <= UMBRAL_AVISO_SEGUNDOS
      ? { kind: 'warning', message: 'Queda menos de un minuto de tiempo oficial.' }
      : fase === 'cerrada' && estado.cerradaPorTiempo
        ? {
            kind: 'warning',
            message: 'El tiempo terminó. Las respuestas confirmadas quedaron guardadas.',
          }
        : undefined;

  const etiquetaPrincipal =
    fase === 'instrucciones'
      ? config.practica
        ? 'Continuar'
        : 'Comenzar'
      : fase === 'practica'
        ? 'Comenzar'
        : fase === 'activa'
          ? esUltimaPregunta(estado)
            ? 'Terminar'
            : 'Siguiente'
          : 'Continuar';

  const alAvanzar = () => {
    if (fase === 'activa') return void subprueba.siguiente();
    if (fase === 'cerrada') return onCerrada?.();
    return void subprueba.pasarAPracticaOIniciar();
  };

  return (
    <TestScreen
      testName={config.nombre}
      questionNumber={estado.indice + 1}
      totalQuestions={config.preguntas.length}
      timeRemainingSeconds={activa ? segundosRestantes : null}
      prompt={pregunta?.enunciado ?? ''}
      options={pregunta?.opciones?.map((opcion) => ({ id: opcion.id, label: opcion.texto }))}
      responseType={pregunta?.tipo === 'texto' ? 'text' : 'selection'}
      selectedOption={pregunta?.tipo === 'seleccion' ? (respuesta?.valor ?? null) : null}
      textResponse={pregunta?.tipo === 'texto' ? (respuesta?.valor ?? '') : ''}
      textMaxLength={pregunta?.longitudMaxima}
      saveState={respuesta ? estadosDeGuardado[respuesta.estado] : 'idle'}
      notice={notice}
      phase={fases[fase]}
      showSaveStatus={activa}
      phaseTitle={
        fase === 'instrucciones'
          ? 'Instrucciones'
          : fase === 'practica'
            ? 'Práctica'
            : 'Subprueba cerrada'
      }
      phaseDescription={
        fase === 'instrucciones'
          ? config.instrucciones
          : fase === 'practica'
            ? config.practica
            : 'Esta subprueba ya no recibe respuestas.'
      }
      questionResource={
        pregunta?.recursoUrl ? (
          <img
            className="question-image"
            src={pregunta.recursoUrl}
            alt={pregunta.recursoDescripcion ?? ''}
          />
        ) : undefined
      }
      primaryActionLabel={etiquetaPrincipal}
      primaryActionDisabled={
        (activa && respuesta?.estado === 'guardando') || (fase === 'cerrada' && !onCerrada)
      }
      secondaryActionLabel={puedeRetroceder(estado) ? 'Anterior' : undefined}
      onSelect={(opcionId) => pregunta && subprueba.editar(pregunta.id, opcionId)}
      onTextResponse={(valor) => pregunta && subprueba.editar(pregunta.id, valor)}
      onBack={() => void subprueba.anterior()}
      onNext={alAvanzar}
    />
  );
}
