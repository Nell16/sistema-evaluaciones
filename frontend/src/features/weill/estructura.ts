/**
 * Estructura de la Forma A según el borrador DOC-02 v03: 5 láminas de 12 posiciones y
 * 8 opciones por reactivo. Son metadatos de esa edición: cuando exista la API, deben
 * leerse de la edición publicada y no de estas constantes. Su autorización consta en DOC-01.
 */
export const LAMINAS = 5;
export const POSICIONES_POR_LAMINA = 12;
export const OPCIONES_POR_REACTIVO = 8;
export const TOTAL_REACTIVOS = LAMINAS * POSICIONES_POR_LAMINA;

/**
 * Lámina y posición de un reactivo a partir de su orden de presentación (desde 1).
 * Fórmula de DOC-07: lámina = 1 + división entera de (orden − 1) entre 12; posición = 1 + resto.
 */
export function laminaYPosicion(orden: number): { lamina: number; posicion: number } {
  if (!Number.isInteger(orden) || orden < 1 || orden > TOTAL_REACTIVOS) {
    throw new RangeError(`El orden debe ser un entero entre 1 y ${TOTAL_REACTIVOS}.`);
  }
  return {
    lamina: 1 + Math.floor((orden - 1) / POSICIONES_POR_LAMINA),
    posicion: 1 + ((orden - 1) % POSICIONES_POR_LAMINA),
  };
}
