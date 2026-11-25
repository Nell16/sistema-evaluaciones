import { describe, expect, it } from 'vitest';
import { TOTAL_REACTIVOS, laminaYPosicion } from './estructura';

describe('laminaYPosicion', () => {
  it('ubica el primer reactivo en la lámina 1, posición 1', () => {
    expect(laminaYPosicion(1)).toEqual({ lamina: 1, posicion: 1 });
  });

  it('cierra cada lámina en la posición 12 y abre la siguiente en la 1', () => {
    expect(laminaYPosicion(12)).toEqual({ lamina: 1, posicion: 12 });
    expect(laminaYPosicion(13)).toEqual({ lamina: 2, posicion: 1 });
  });

  it('ubica el último reactivo en la lámina 5, posición 12', () => {
    expect(laminaYPosicion(TOTAL_REACTIVOS)).toEqual({ lamina: 5, posicion: 12 });
  });

  it('rechaza órdenes fuera de rango o no enteros', () => {
    expect(() => laminaYPosicion(0)).toThrow(RangeError);
    expect(() => laminaYPosicion(TOTAL_REACTIVOS + 1)).toThrow(RangeError);
    expect(() => laminaYPosicion(1.5)).toThrow(RangeError);
  });
});
