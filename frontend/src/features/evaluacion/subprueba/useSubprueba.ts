import { useCallback, useEffect, useReducer, useRef, useState } from 'react';
import { ApiError } from '../../../services/http';
import type { SubpruebaApi } from './contrato';
import {
  esUltimaPregunta,
  estadoInicial,
  preguntaActual,
  reducir,
  segundosRestantes,
} from './estado';

/** Espera tras la última pulsación antes de guardar una respuesta de texto libre. */
const ESPERA_GUARDADO_TEXTO_MS = 800;

function mensajeDe(error: unknown, porDefecto: string) {
  return error instanceof ApiError && error.error?.mensaje ? error.error.mensaje : porDefecto;
}

/**
 * Orquesta una subprueba: carga, guardado de respuestas, navegación y temporizador.
 * El servidor es la autoridad del tiempo: el reloj local solo cuenta hacia atrás desde el último
 * valor informado y se vuelve a sincronizar con cada respuesta confirmada.
 */
export function useSubprueba(subpruebaId: string, api: SubpruebaApi) {
  const [estado, dispatch] = useReducer(reducir, estadoInicial);
  const [ahora, setAhora] = useState(() => Date.now());
  const [aviso, setAviso] = useState<string | null>(null);

  const estadoRef = useRef(estado);
  const temporizadoresTexto = useRef(new Map<string, ReturnType<typeof setTimeout>>());

  useEffect(() => {
    estadoRef.current = estado;
  }, [estado]);

  const cargar = useCallback(async () => {
    dispatch({ tipo: 'carga-iniciada' });
    setAviso(null);
    try {
      const subprueba = await api.obtener(subpruebaId);
      dispatch({ tipo: 'cargada', subprueba, ahora: Date.now() });
    } catch (error) {
      dispatch({
        tipo: 'carga-fallida',
        mensaje: mensajeDe(error, 'No se pudo cargar la subprueba. Intenta de nuevo.'),
      });
    }
  }, [api, subpruebaId]);

  useEffect(() => {
    // La carga inicial vive en un efecto porque depende de la red, no del render.
    void cargar();
    const temporizadores = temporizadoresTexto.current;
    return () => temporizadores.forEach((id) => clearTimeout(id));
  }, [cargar]);

  const { fase, limite } = estado;

  // Temporizador: cuenta desde el límite informado por el servidor y cierra al llegar a cero.
  useEffect(() => {
    if (fase !== 'activa' || limite === null) return;

    const latido = () => {
      const marca = Date.now();
      setAhora(marca);
      if (segundosRestantes(limite, marca) === 0) {
        dispatch({ tipo: 'tiempo-agotado' });
        // El servidor decide: si el cierre falla, la próxima respuesta rechazada lo confirmará.
        void api.cerrar(subpruebaId).catch(() => undefined);
      }
    };

    latido();
    const intervalo = setInterval(latido, 1000);
    return () => clearInterval(intervalo);
  }, [api, subpruebaId, fase, limite]);

  const enviar = useCallback(
    async (preguntaId: string, valor: string, version: number) => {
      dispatch({ tipo: 'guardando', preguntaId, version });
      try {
        const resultado = await api.guardarRespuesta(subpruebaId, { preguntaId, valor });
        dispatch({
          tipo: 'confirmada',
          preguntaId,
          version,
          fase: resultado.fase,
          tiempoRestanteSegundos: resultado.tiempoRestanteSegundos,
          ahora: Date.now(),
        });
        return true;
      } catch (error) {
        dispatch({ tipo: 'guardado-fallido', preguntaId, version });
        setAviso(
          mensajeDe(error, 'No se pudo guardar la respuesta. Reintenta antes de continuar.'),
        );
        return false;
      }
    },
    [api, subpruebaId],
  );

  /** Guarda ya la respuesta de una pregunta si está pendiente o falló (también sirve para reintentar). */
  const guardar = useCallback(
    async (preguntaId: string) => {
      const temporizador = temporizadoresTexto.current.get(preguntaId);
      if (temporizador !== undefined) {
        clearTimeout(temporizador);
        temporizadoresTexto.current.delete(preguntaId);
      }
      const respuesta = estadoRef.current.respuestas[preguntaId];
      if (!respuesta || respuesta.estado === 'confirmada') return true;
      setAviso(null);
      return enviar(preguntaId, respuesta.valor, respuesta.version);
    },
    [enviar],
  );

  const editar = useCallback(
    (preguntaId: string, valor: string) => {
      const pregunta = estadoRef.current.subprueba?.preguntas.find((p) => p.id === preguntaId);
      if (!pregunta || estadoRef.current.fase !== 'activa') return;

      const version = (estadoRef.current.respuestas[preguntaId]?.version ?? 0) + 1;
      dispatch({ tipo: 'editada', preguntaId, valor });
      setAviso(null);

      const previo = temporizadoresTexto.current.get(preguntaId);
      if (previo !== undefined) clearTimeout(previo);

      if (pregunta.tipo === 'seleccion') {
        void enviar(preguntaId, valor, version);
        return;
      }
      temporizadoresTexto.current.set(
        preguntaId,
        setTimeout(() => {
          temporizadoresTexto.current.delete(preguntaId);
          void enviar(preguntaId, valor, version);
        }, ESPERA_GUARDADO_TEXTO_MS),
      );
    },
    [enviar],
  );

  const pasarAPracticaOIniciar = useCallback(async () => {
    const actual = estadoRef.current;
    if (actual.fase === 'instrucciones' && actual.subprueba?.practica) {
      dispatch({ tipo: 'ir-a-practica' });
      return;
    }
    setAviso(null);
    try {
      const subprueba = await api.iniciar(subpruebaId);
      dispatch({ tipo: 'cargada', subprueba, ahora: Date.now() });
    } catch (error) {
      setAviso(mensajeDe(error, 'No se pudo iniciar la subprueba. Intenta de nuevo.'));
    }
  }, [api, subpruebaId]);

  const terminar = useCallback(async () => {
    const pendiente = Object.keys(estadoRef.current.respuestas);
    for (const preguntaId of pendiente) {
      if (!(await guardar(preguntaId))) return;
    }
    setAviso(null);
    try {
      const subprueba = await api.cerrar(subpruebaId);
      dispatch({ tipo: 'cargada', subprueba, ahora: Date.now() });
    } catch (error) {
      setAviso(mensajeDe(error, 'No se pudo cerrar la subprueba. Intenta de nuevo.'));
    }
  }, [api, guardar, subpruebaId]);

  /** Avanza solo cuando la respuesta actual quedó confirmada; si el guardado falla, se queda. */
  const siguiente = useCallback(async () => {
    const actual = estadoRef.current;
    const pregunta = preguntaActual(actual);
    if (!pregunta) return;
    if (!(await guardar(pregunta.id))) return;
    if (esUltimaPregunta(actual)) {
      await terminar();
      return;
    }
    dispatch({ tipo: 'indice', indice: actual.indice + 1 });
  }, [guardar, terminar]);

  const anterior = useCallback(async () => {
    const actual = estadoRef.current;
    const pregunta = preguntaActual(actual);
    if (!pregunta || actual.subprueba?.navegacion !== 'libre' || actual.indice === 0) return;
    if (!(await guardar(pregunta.id))) return;
    dispatch({ tipo: 'indice', indice: actual.indice - 1 });
  }, [guardar]);

  return {
    estado,
    aviso,
    segundosRestantes: segundosRestantes(limite, ahora),
    cargar,
    editar,
    guardar,
    siguiente,
    anterior,
    terminar,
    pasarAPracticaOIniciar,
  };
}
