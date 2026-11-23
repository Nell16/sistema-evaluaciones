import type { RouteObject } from 'react-router';
import { PerfilBfaPagina } from './PerfilBfaPagina';

export const rutasPerfilBfa: RouteObject[] = [
  // BFA-FE-08: E9 no indica el tipo de test, por eso el perfil BFA tiene su propia ruta.
  { path: 'bfa/evaluaciones/:evaluacionId/perfil', element: <PerfilBfaPagina /> },
];
