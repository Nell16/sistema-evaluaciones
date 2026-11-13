import { describe, expect, it } from 'vitest';
import type { EstadoSubprueba } from './contrato';
import {
  estadoInicial,
  hayRespuestasSinConfirmar,
  puedeRetroceder,
  reducir,
  segundosRestantes,
  type Accion,
  type EstadoPantalla,
} from './estado';

const AHORA = 1_000_000;

const subprueba = (cambios: Partial<EstadoSubprueba> = {}): EstadoSubprueba => ({
  subpruebaId: 's1',
  nombre: 'Subprueba de prueba',
  fase: 'activa',
  instrucciones: 'Instrucciones',
  preguntas: [
    { id: 'p1', enunciado: 'Uno', tipo: 'seleccion', opciones: [{ id: 'a', texto: 'A' }] },
    { id: 'p2', enunciado: 'Dos', tipo: 'texto' },
    { id: 'p3', enunciado: 'Tres', tipo: 'texto' },
  ],
  respuestas: [],
  tiempoRestanteSegundos: 60,
  navegacion: 'secuencial',
  ...cambios,
});

const aplicar = (estado: EstadoPantalla, ...acciones: Accion[]) => acciones.reduce(reducir, estado);

const cargar = (cambios?: Partial<EstadoSubprueba>) =>
  aplicar(estadoInicial, { tipo: 'cargada', subprueba: subprueba(cambios), ahora: AHORA });

describe('carga', () => {
  it('calcula el límite a partir del tiempo que informa el servidor', () => {
    const estado = cargar({ tiempoRestanteSegundos: 90 });
    expect(estado.limite).toBe(AHORA + 90_000);
    expect(segundosRestantes(estado.limite, AHORA)).toBe(90);
  });

  it('no corre el tiempo antes de la fase activa', () => {
    const estado = cargar({ fase: 'instrucciones', tiempoRestanteSegundos: null });
    expect(estado.limite).toBeNull();
    expect(estado.fase).toBe('instrucciones');
  });

  it('recupera las respuestas confirmadas y retoma en la primera sin responder', () => {
    const estado = cargar({ respuestas: [{ preguntaId: 'p1', valor: 'a' }] });
    expect(estado.respuestas.p1).toMatchObject({ valor: 'a', estado: 'confirmada' });
    expect(estado.indice).toBe(1);
  });

  it('si todo está respondido queda en la última pregunta', () => {
    const estado = cargar({
      respuestas: [
        { preguntaId: 'p1', valor: 'a' },
        { preguntaId: 'p2', valor: 'x' },
        { preguntaId: 'p3', valor: 'y' },
      ],
    });
    expect(estado.indice).toBe(2);
  });

  it('un fallo de carga queda registrado con su mensaje', () => {
    const estado = aplicar(estadoInicial, { tipo: 'carga-fallida', mensaje: 'Sin conexión' });
    expect(estado).toMatchObject({ carga: 'error', mensajeError: 'Sin conexión' });
  });
});

describe('respuestas pendientes y confirmadas', () => {
  it('una edición deja la respuesta pendiente', () => {
    const estado = aplicar(cargar(), { tipo: 'editada', preguntaId: 'p1', valor: 'a' });
    expect(estado.respuestas.p1).toMatchObject({ valor: 'a', estado: 'pendiente', version: 1 });
    expect(hayRespuestasSinConfirmar(estado)).toBe(true);
  });

  it('la confirmación la marca como confirmada', () => {
    const estado = aplicar(
      cargar(),
      { tipo: 'editada', preguntaId: 'p1', valor: 'a' },
      { tipo: 'guardando', preguntaId: 'p1', version: 1 },
      {
        tipo: 'confirmada',
        preguntaId: 'p1',
        version: 1,
        fase: 'activa',
        tiempoRestanteSegundos: 50,
        ahora: AHORA + 1000,
      },
    );
    expect(estado.respuestas.p1.estado).toBe('confirmada');
    expect(hayRespuestasSinConfirmar(estado)).toBe(false);
  });

  it('una confirmación vieja no pisa una edición más nueva', () => {
    const estado = aplicar(
      cargar(),
      { tipo: 'editada', preguntaId: 'p2', valor: 'ho' },
      { tipo: 'guardando', preguntaId: 'p2', version: 1 },
      { tipo: 'editada', preguntaId: 'p2', valor: 'hola' },
      {
        tipo: 'confirmada',
        preguntaId: 'p2',
        version: 1,
        fase: 'activa',
        tiempoRestanteSegundos: 50,
        ahora: AHORA,
      },
    );
    expect(estado.respuestas.p2).toMatchObject({ valor: 'hola', estado: 'pendiente', version: 2 });
  });

  it('un fallo de guardado deja la respuesta en error sin perder el valor', () => {
    const estado = aplicar(
      cargar(),
      { tipo: 'editada', preguntaId: 'p1', valor: 'a' },
      { tipo: 'guardado-fallido', preguntaId: 'p1', version: 1 },
    );
    expect(estado.respuestas.p1).toMatchObject({ valor: 'a', estado: 'error' });
  });

  it('un fallo viejo no marca error sobre una edición más nueva', () => {
    const estado = aplicar(
      cargar(),
      { tipo: 'editada', preguntaId: 'p1', valor: 'a' },
      { tipo: 'editada', preguntaId: 'p1', valor: 'b' },
      { tipo: 'guardado-fallido', preguntaId: 'p1', version: 1 },
    );
    expect(estado.respuestas.p1.estado).toBe('pendiente');
  });

  it('no acepta ediciones fuera de la fase activa', () => {
    const estado = aplicar(cargar({ fase: 'cerrada' }), {
      tipo: 'editada',
      preguntaId: 'p1',
      valor: 'a',
    });
    expect(estado.respuestas.p1).toBeUndefined();
  });
});

describe('tiempo', () => {
  it('cada confirmación vuelve a sincronizar el límite con el servidor', () => {
    const estado = aplicar(
      cargar({ tiempoRestanteSegundos: 60 }),
      { tipo: 'editada', preguntaId: 'p1', valor: 'a' },
      {
        tipo: 'confirmada',
        preguntaId: 'p1',
        version: 1,
        fase: 'activa',
        tiempoRestanteSegundos: 30,
        ahora: AHORA + 5000,
      },
    );
    expect(estado.limite).toBe(AHORA + 5000 + 30_000);
  });

  it('si el servidor informa fase cerrada, la subprueba se cierra', () => {
    const estado = aplicar(
      cargar(),
      { tipo: 'editada', preguntaId: 'p1', valor: 'a' },
      {
        tipo: 'confirmada',
        preguntaId: 'p1',
        version: 1,
        fase: 'cerrada',
        tiempoRestanteSegundos: 0,
        ahora: AHORA,
      },
    );
    expect(estado.fase).toBe('cerrada');
    expect(estado.limite).toBeNull();
  });

  it('el tiempo agotado cierra y lo deja anotado', () => {
    const estado = aplicar(cargar(), { tipo: 'tiempo-agotado' });
    expect(estado).toMatchObject({ fase: 'cerrada', limite: null, cerradaPorTiempo: true });
  });

  it('segundosRestantes redondea hacia arriba y nunca baja de cero', () => {
    expect(segundosRestantes(AHORA + 1500, AHORA)).toBe(2);
    expect(segundosRestantes(AHORA - 5000, AHORA)).toBe(0);
    expect(segundosRestantes(null, AHORA)).toBeNull();
  });
});

describe('navegación', () => {
  it('con regla secuencial no se puede retroceder', () => {
    const estado = aplicar(cargar(), { tipo: 'indice', indice: 1 });
    expect(puedeRetroceder(estado)).toBe(false);
  });

  it('con regla libre se puede retroceder salvo en la primera pregunta', () => {
    const libre = cargar({ navegacion: 'libre' });
    expect(puedeRetroceder(libre)).toBe(false);
    expect(puedeRetroceder(aplicar(libre, { tipo: 'indice', indice: 1 }))).toBe(true);
  });

  it('no se retrocede en una subprueba cerrada', () => {
    const estado = aplicar(
      cargar({ navegacion: 'libre' }),
      { tipo: 'indice', indice: 1 },
      {
        tipo: 'tiempo-agotado',
      },
    );
    expect(puedeRetroceder(estado)).toBe(false);
  });

  it('el índice queda acotado al rango de preguntas', () => {
    const estado = cargar();
    expect(aplicar(estado, { tipo: 'indice', indice: 99 }).indice).toBe(2);
    expect(aplicar(estado, { tipo: 'indice', indice: -3 }).indice).toBe(0);
  });

  it('pasa de instrucciones a práctica solo si hay práctica', () => {
    const sinPractica = cargar({ fase: 'instrucciones', tiempoRestanteSegundos: null });
    expect(aplicar(sinPractica, { tipo: 'ir-a-practica' }).fase).toBe('instrucciones');

    const conPractica = cargar({
      fase: 'instrucciones',
      practica: 'Práctica',
      tiempoRestanteSegundos: null,
    });
    expect(aplicar(conPractica, { tipo: 'ir-a-practica' }).fase).toBe('practica');
  });
});
