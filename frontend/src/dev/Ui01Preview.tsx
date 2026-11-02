import { useState } from 'react';
import { TestScreen } from '../components/TestScreen';

const demoOptions = [
  { id: 'a', label: 'Opción de ejemplo A' },
  { id: 'b', label: 'Opción de ejemplo B' },
  { id: 'c', label: 'Opción de ejemplo C' },
  { id: 'd', label: 'Opción de ejemplo D' },
  { id: 'e', label: 'Opción de ejemplo E' },
];

export function Ui01Preview() {
  const [selectedOption, setSelectedOption] = useState<string | null>('b');
  const [questionNumber, setQuestionNumber] = useState(1);

  const handleNext = () => {
    setQuestionNumber((current) => (current === 3 ? 1 : current + 1));
    setSelectedOption(null);
  };

  return (
    <main className="app-shell">
      <div className="test-stage">
        <TestScreen
          testName="Subprueba de muestra"
          questionNumber={questionNumber}
          totalQuestions={3}
          timeRemainingSeconds={272}
          prompt="Palabra de ejemplo"
          options={demoOptions}
          selectedOption={selectedOption}
          saveState="saved"
          onSelect={setSelectedOption}
          onNext={handleNext}
        />
      </div>
    </main>
  );
}
