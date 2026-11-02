import { useState } from 'react';
import { TestScreen, type TestPhase, type TestResponseType } from '../components/TestScreen';

const demoOptions = [
  { id: 'a', label: 'Opción de ejemplo A' },
  { id: 'b', label: 'Opción de ejemplo B' },
  { id: 'c', label: 'Opción de ejemplo C' },
  { id: 'd', label: 'Opción de ejemplo D' },
  { id: 'e', label: 'Opción de ejemplo E' },
];

const previewSaveStates = ['saved', 'saving', 'error'] as const;
type PreviewSaveState = (typeof previewSaveStates)[number];
const previewPhases = ['instructions', 'practice', 'active', 'closed'] as const;

function getSearchParams() {
  return new URLSearchParams(window.location.search);
}

function getPreviewSaveState(): PreviewSaveState {
  const saveState = getSearchParams().get('save');

  return previewSaveStates.includes(saveState as PreviewSaveState)
    ? (saveState as PreviewSaveState)
    : 'saved';
}

function getPreviewNotice() {
  const notice = getSearchParams().get('notice');

  if (notice === 'warning') {
    return { kind: 'warning' as const, message: 'Aviso de tiempo de ejemplo' };
  }

  if (notice === 'error') {
    return { kind: 'error' as const, message: 'Error de ejemplo' };
  }

  return undefined;
}

function getPreviewPhase(): TestPhase {
  const phase = getSearchParams().get('phase');

  return previewPhases.includes(phase as TestPhase) ? (phase as TestPhase) : 'active';
}

function getPreviewResponseType(): TestResponseType {
  return getSearchParams().get('response') === 'text' ? 'text' : 'selection';
}

function getPreviewResource() {
  if (getSearchParams().get('resource') !== 'visual') return undefined;

  return (
    <div className="question-resource-placeholder" role="img" aria-label="Recurso visual de ejemplo">
      Recurso visual de ejemplo
    </div>
  );
}

function getPreviewActionLabel(phase: TestPhase) {
  if (phase === 'instructions') return 'Comenzar';
  if (phase === 'practice') return 'Continuar';
  if (phase === 'closed') return 'Volver';
  return 'Siguiente';
}

export function Ui01Preview() {
  const [selectedOption, setSelectedOption] = useState<string | null>('b');
  const [textResponse, setTextResponse] = useState('');
  const [questionNumber, setQuestionNumber] = useState(1);
  const phase = getPreviewPhase();
  const responseType = getPreviewResponseType();

  const handleNext = () => {
    setQuestionNumber((current) => (current === 3 ? 1 : current + 1));
    setSelectedOption(null);
    setTextResponse('');
  };

  return (
    <main className="app-shell">
      <div className="test-stage">
        <TestScreen
          testName="Subprueba de muestra"
          questionNumber={questionNumber}
          totalQuestions={3}
          timeRemainingSeconds={phase === 'active' ? 272 : null}
          prompt="Palabra de ejemplo"
          options={demoOptions}
          responseType={responseType}
          selectedOption={selectedOption}
          textResponse={textResponse}
          saveState={getPreviewSaveState()}
          notice={getPreviewNotice()}
          phase={phase}
          questionResource={getPreviewResource()}
          primaryActionLabel={getPreviewActionLabel(phase)}
          onSelect={setSelectedOption}
          onTextResponse={setTextResponse}
          onNext={handleNext}
        />
      </div>
    </main>
  );
}
