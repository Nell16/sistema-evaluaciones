import { TestNotice } from '../../../../components/TestNotice';
import { EstadoCarga } from '../../../evaluacion/componentes/EstadoCarga';
import { MensajeError } from '../../../evaluacion/componentes/MensajeError';
import type { EstadoPeticion } from '../../../evaluacion/tipos';
import { esNoFinalizada } from '../../../resultados/errores';
import type { ResultadoResponse } from '../../../resultados/tipos';
import { sinFactoresNoPublicados } from '../resultadoMostrable';
import { PerfilBfa } from './PerfilBfa';

interface EstadoPerfilBfaProps {
  estado: EstadoPeticion<ResultadoResponse>;
  onReintentar?: () => void;
  /** Muestra el aviso de datos simulados cuando E9 viene del mock. */
  datosSimulados?: boolean;
}

/**
 * Qué se ve en cada fase de la consulta de E9. Una evaluación no finalizada (409) es un aviso;
 * un resultado pendiente de validación llega como estado y lo explica el propio perfil.
 */
export function EstadoPerfilBfa({ estado, onReintentar, datosSimulados }: EstadoPerfilBfaProps) {
  switch (estado.fase) {
    case 'inicial':
    case 'cargando':
      return <EstadoCarga texto="Consultando el perfil de resultados…" />;
    case 'error':
      return esNoFinalizada(estado.error) ? (
        <TestNotice kind="warning" message={estado.error.mensaje} />
      ) : (
        <MensajeError error={estado.error} onReintentar={onReintentar} />
      );
    case 'exito':
      return (
        <PerfilBfa
          resultado={sinFactoresNoPublicados(estado.datos)}
          datosSimulados={datosSimulados}
        />
      );
  }
}
