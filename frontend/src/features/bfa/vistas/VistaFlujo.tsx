import { Figura } from '../components/Figura';
import { FluidezVerbal } from '../components/FluidezVerbal';
import { TestScreen } from '../../../components/TestScreen';
import type { TestOption } from '../../../components/AnswerOption';
import type { ControladorBfa, EstadoEjecucion, EstadoFlujo } from '../flujo/controlador';
import { PAUSA_ANTES_DE, presentacionDe } from '../familias';
import { formatoTiempo } from './formato';
import { HojaSimple } from './HojaSimple';

type VistaFlujoProps = {
  estado: EstadoFlujo;
  controlador: ControladorBfa;
  alSalir: () => void;
};

const NOMBRE_TEST = 'Batería Factorial de Aptitudes';

/** El estado del resultado se informa tal como lo devuelve el backend; el navegador no calcula ni publica nada. */
function textoResultado(estadoResultado: string | null): string {
  switch (estadoResultado) {
    case 'PENDIENTE_VALIDACION':
      return 'El resultado está pendiente de validación.';
    case 'CALCULANDO':
      return 'El resultado se está calculando.';
    case 'DISPONIBLE':
      return 'El resultado está disponible.';
    default:
      return estadoResultado ? `Estado del resultado: ${estadoResultado}.` : '';
  }
}

/** Elige la pantalla según la fase oficial del flujo. No decide nada: solo presenta. */
export function VistaFlujo({ estado, controlador, alSalir }: VistaFlujoProps) {
  switch (estado.fase) {
    case 'cargando':
      return (
        <HojaSimple
          nombre={NOMBRE_TEST}
          etiqueta="Evaluación"
          titulo="Cargando evaluación"
          descripcion="Consultando el estado oficial."
        />
      );

    case 'iniciando':
      return (
        <HojaSimple
          nombre={estado.subprueba.nombre}
          etiqueta="Subprueba"
          titulo="Iniciando subprueba"
          descripcion="El tiempo oficial empieza cuando aparezca la primera pregunta."
        />
      );

    case 'error':
      return (
        <TestScreen
          testName={NOMBRE_TEST}
          questionNumber={0}
          totalQuestions={0}
          timeRemainingSeconds={null}
          prompt=""
          selectedOption={null}
          saveState="error"
          phase="closed"
          phaseLabel="Error"
          phaseTitle="No se pudo continuar"
          phaseDescription={estado.error.mensaje}
          notice={{ kind: 'error', message: `Código: ${estado.error.codigo}` }}
          primaryActionLabel={estado.error.reintentable ? 'Reintentar' : 'Salir'}
          onNext={estado.error.reintentable ? controlador.reintentar : alSalir}
        />
      );

    case 'preparacion': {
      const { subprueba, paso, posicion, total } = estado;
      const pres = presentacionDe(subprueba.codigo);
      const progreso = `Subprueba ${posicion} de ${total}. Tiempo oficial: ${formatoTiempo(subprueba.tiempoLimiteSegundos)}, que empieza solo al iniciar.`;
      const texto = paso === 'instrucciones' ? pres.instrucciones : (pres.practica ?? '');
      return (
        <TestScreen
          testName={subprueba.nombre}
          questionNumber={0}
          totalQuestions={0}
          timeRemainingSeconds={null}
          prompt=""
          selectedOption={null}
          saveState="saved"
          phase={paso === 'instrucciones' ? 'instructions' : 'practice'}
          phaseTitle={
            paso === 'instrucciones'
              ? `Instrucciones · ${subprueba.nombre}`
              : `Práctica · ${subprueba.nombre}`
          }
          phaseDescription={`${texto} ${progreso}`}
          primaryActionLabel={
            paso === 'instrucciones' && subprueba.tienePractica
              ? 'Continuar a la práctica'
              : 'Iniciar subprueba'
          }
          onNext={controlador.continuar}
        />
      );
    }

    case 'ejecucion':
      return <VistaEjecucion estado={estado} controlador={controlador} />;

    case 'transicion': {
      if (estado.cerrando) {
        return (
          <HojaSimple
            nombre={estado.cerrada.nombre}
            etiqueta="Tiempo oficial"
            titulo="El tiempo terminó"
            descripcion="No se aceptan más respuestas. Esperando la confirmación oficial del cierre."
          />
        );
      }
      const { siguiente, cerrada } = estado;
      const pausa =
        siguiente?.codigo === PAUSA_ANTES_DE
          ? ' Antes de esta subprueba se permite una pausa de 5 a 10 minutos; el tiempo no corre hasta iniciarla.'
          : '';
      return (
        <TestScreen
          testName={cerrada.nombre}
          questionNumber={0}
          totalQuestions={0}
          timeRemainingSeconds={null}
          prompt=""
          selectedOption={null}
          saveState="saved"
          phase="closed"
          phaseLabel="Subprueba cerrada"
          phaseTitle="El tiempo terminó"
          phaseDescription={
            siguiente
              ? `${cerrada.nombre} se cerró y no se aceptan más respuestas. Sigue: ${siguiente.nombre}. No se puede volver a la subprueba anterior.${pausa}`
              : `${cerrada.nombre} se cerró y no se aceptan más respuestas. No quedan más subpruebas por aplicar.`
          }
          primaryActionLabel={siguiente ? 'Continuar' : 'Ir al cierre'}
          onNext={controlador.continuar}
        />
      );
    }

    case 'listaParaFinalizar':
      if (estado.finalizando) {
        return (
          <HojaSimple
            nombre={NOMBRE_TEST}
            etiqueta="Evaluación"
            titulo="Finalizando evaluación"
            descripcion="Consolidando el cierre."
          />
        );
      }
      return (
        <TestScreen
          testName={NOMBRE_TEST}
          questionNumber={0}
          totalQuestions={0}
          timeRemainingSeconds={null}
          prompt=""
          selectedOption={null}
          saveState="saved"
          phase="closed"
          phaseLabel="Evaluación"
          phaseTitle="Todas las subpruebas están cerradas"
          phaseDescription="Finaliza la evaluación para registrar el cierre."
          primaryActionLabel="Finalizar evaluación"
          onNext={controlador.continuar}
        />
      );

    case 'finalizada':
      return (
        <TestScreen
          testName={NOMBRE_TEST}
          questionNumber={0}
          totalQuestions={0}
          timeRemainingSeconds={null}
          prompt=""
          selectedOption={null}
          saveState="saved"
          phase="closed"
          phaseLabel="Evaluación"
          phaseTitle="Evaluación finalizada"
          phaseDescription={`La evaluación quedó finalizada. ${textoResultado(estado.estadoResultado)}`}
          primaryActionLabel="Salir"
          onNext={alSalir}
        />
      );
  }
}

function VistaEjecucion({
  estado,
  controlador,
}: {
  estado: EstadoEjecucion;
  controlador: ControladorBfa;
}) {
  const { subprueba } = estado;
  const pres = presentacionDe(subprueba.codigo);
  const segundos = controlador.segundosRestantes(estado);
  const aviso = estado.recuperada
    ? { kind: 'warning' as const, message: 'Se recuperó la sesión. El tiempo no se detuvo.' }
    : undefined;

  if (estado.alFinal) {
    return (
      <TestScreen
        testName={subprueba.nombre}
        questionNumber={0}
        totalQuestions={0}
        timeRemainingSeconds={segundos}
        prompt=""
        selectedOption={null}
        saveState={estado.guardado}
        phase="closed"
        phaseLabel="En curso"
        phaseTitle="Respondiste todo lo disponible"
        phaseDescription="La subprueba se cierra cuando termina el tiempo. Mientras tanto puedes revisar tus respuestas."
        primaryActionLabel="Revisar respuestas"
        onNext={controlador.revisar}
      />
    );
  }

  if (subprueba.tipoRespuesta === 'TEXTO_LIBRE') {
    return (
      <TestScreen
        testName={subprueba.nombre}
        questionNumber={1}
        totalQuestions={1}
        questionLabel={`Hasta ${subprueba.maximoEntradas ?? 0} palabras`}
        timeRemainingSeconds={segundos}
        prompt="Escribe palabras con la letra asignada"
        selectedOption={null}
        saveState={estado.guardado}
        notice={aviso}
        responseArea={
          <FluidezVerbal
            letra={estado.letraInicial ?? ''}
            maxEntradas={subprueba.maximoEntradas ?? 75}
            palabras={estado.palabras}
            onChange={controlador.escribirPalabras}
          />
        }
        primaryActionLabel="Listo"
        onNext={controlador.siguiente}
      />
    );
  }

  const pregunta = estado.preguntas[estado.indice];
  const opciones: TestOption[] = pregunta.opciones.map((o) => ({
    id: o.letra,
    label: o.texto,
    figure: o.recursoUrl ? <Figura rol="opcion" src={o.recursoUrl} alt="" /> : undefined,
  }));
  const total = subprueba.numeroPreguntas;
  // La hoja puede numerar distinto (VOC2: 41–77): solo se muestra «de N» si la numeración es corrida.
  const consecutiva = pregunta.numero === estado.indice + 1 && total > 0;
  const ultima = estado.indice >= estado.preguntas.length - 1;

  return (
    <TestScreen
      testName={subprueba.nombre}
      questionNumber={pregunta.numero}
      totalQuestions={total}
      questionLabel={consecutiva ? undefined : `Pregunta ${pregunta.numero}`}
      timeRemainingSeconds={segundos}
      prompt={pregunta.enunciado}
      promptVariant={pres.promptVariant}
      optionsLayout={pres.optionsLayout}
      numeric={pres.numeric}
      options={opciones}
      questionResource={
        pregunta.recursoUrl ? (
          <Figura rol="estimulo" src={pregunta.recursoUrl} alt="Figura del ítem" />
        ) : undefined
      }
      selectedOption={estado.respuestas[pregunta.id] ?? null}
      saveState={estado.guardado}
      notice={aviso}
      primaryActionLabel={ultima ? 'Listo' : 'Siguiente'}
      onPrevious={pres.volverAtras && estado.indice > 0 ? controlador.anterior : undefined}
      onSelect={(letra) => controlador.seleccionar(pregunta.id, letra)}
      onNext={controlador.siguiente}
    />
  );
}
