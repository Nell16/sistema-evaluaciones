import { useParams } from 'react-router';
import { TestNotice } from '../../../components/TestNotice';
import '../../evaluacion/evaluacion.css';
import { validarId } from '../../evaluacion/validaciones';
import '../../resultados/resultados.css';
import { PerfilBfaConsulta } from './componentes/PerfilBfaConsulta';
import './perfil.css';

/** BFA-FE-08: perfil de resultados BFA de una evaluación finalizada (consulta E9). */
export function PerfilBfaPagina() {
  const { evaluacionId } = useParams();
  const errorId = validarId(evaluacionId ?? '', 'de la evaluación');

  return (
    <div className="ev-pagina">
      <header className="ev-pagina-cabecera">
        <h1>Resultados BFA</h1>
        <p>
          Muestra la revisión vigente publicada de una evaluación finalizada, tal como la entrega el
          servidor.
        </p>
      </header>
      <div className="ev-panel">
        {errorId ? (
          <TestNotice kind="error" message={errorId} />
        ) : (
          // key: si cambia el ID de la ruta se reinicia la consulta.
          <PerfilBfaConsulta key={evaluacionId} evaluacionId={Number(evaluacionId)} />
        )}
      </div>
    </div>
  );
}
