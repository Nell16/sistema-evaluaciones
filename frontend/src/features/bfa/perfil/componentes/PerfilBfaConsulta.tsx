import { useEffect } from 'react';
import { usaMocksResultados } from '../../../resultados/cliente';
import { useResultado } from '../../../resultados/hooks/useResultado';
import type { ClienteResultados } from '../../../resultados/tipos';
import { EstadoPerfilBfa } from './EstadoPerfilBfa';

interface PerfilBfaConsultaProps {
  evaluacionId: number;
  /** Por defecto, el cliente de E9 de GEN-05 (mock o backend real según VITE_USAR_MOCKS). */
  cliente?: ClienteResultados;
}

/** Consulta E9 con el hook general de GEN-05 y presenta el perfil BFA; no repite su mecanismo. */
export function PerfilBfaConsulta({ evaluacionId, cliente }: PerfilBfaConsultaProps) {
  const { estado, ejecutar } = useResultado(cliente);

  useEffect(() => {
    void ejecutar(evaluacionId);
  }, [ejecutar, evaluacionId]);

  return (
    <EstadoPerfilBfa
      estado={estado}
      onReintentar={() => void ejecutar(evaluacionId)}
      datosSimulados={usaMocksResultados && !cliente}
    />
  );
}
