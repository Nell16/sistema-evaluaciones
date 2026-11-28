import { createContext, useContext } from 'react';
import { bfaApiHttp, type BfaApi } from './bfaApi';
import { crearApiSimulada } from './bfaApiSimulada';

/** Usa el mismo selector de datos que el resto de las pantallas. */
function crearApiPorDefecto(): BfaApi {
  return import.meta.env.VITE_USAR_MOCKS === 'true' ? crearApiSimulada() : bfaApiHttp;
}

export const BfaApiContext = createContext<BfaApi>(crearApiPorDefecto());

export function useBfaApi(): BfaApi {
  return useContext(BfaApiContext);
}
