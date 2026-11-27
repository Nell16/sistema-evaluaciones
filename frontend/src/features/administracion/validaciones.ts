import type {
  BaremoRequest,
  ClaveRequest,
  ConfiguracionSubpruebaRequest,
  ErroresValidacion,
  OpcionesRequest,
  OpcionRespuesta,
  PreguntaRequest,
} from './tipos';

/** Validación del lado cliente según los esquemas de DOC-05 (E26–E30). Devuelve [] si es válido. */

type Objeto = Record<string, unknown>;

const esObjeto = (v: unknown): v is Objeto =>
  typeof v === 'object' && v !== null && !Array.isArray(v);
const esEntero = (v: unknown): v is number => typeof v === 'number' && Number.isInteger(v);
const esTexto = (v: unknown): v is string => typeof v === 'string' && v.trim() !== '';

/** Convierte el texto pegado en JSON; si no es JSON válido devuelve el error. */
export function analizarJson(texto: string): { valor: unknown } | { error: string } {
  try {
    return { valor: JSON.parse(texto) as unknown };
  } catch {
    return { error: 'El texto no es JSON válido.' };
  }
}

function enteroOpcional(o: Objeto, campo: string, minimo: number, errores: ErroresValidacion) {
  const v = o[campo];
  if (v === undefined || v === null) return;
  if (!esEntero(v) || v < minimo)
    errores.push({ campo, mensaje: `debe ser un entero mayor o igual a ${minimo}` });
}

function validarOpcionesLista(
  valor: unknown,
  campo: string,
  errores: ErroresValidacion,
  versionadas = false,
) {
  if (!Array.isArray(valor)) {
    errores.push({ campo, mensaje: 'debe ser una lista' });
    return;
  }
  const ordenes = new Set<number>();
  const letras = new Set<string>();
  valor.forEach((item, i) => {
    const o = esObjeto(item) ? item : {};
    if (!esTexto(o.letra))
      errores.push({ campo: `${campo}[${i}].letra`, mensaje: 'es obligatoria' });
    else if (letras.has(o.letra.trim().toUpperCase()))
      errores.push({ campo: `${campo}[${i}].letra`, mensaje: 'está repetida' });
    else letras.add(o.letra.trim().toUpperCase());
    if (versionadas) {
      const orden = o.orden === undefined ? i + 1 : o.orden;
      if (!esEntero(orden) || orden < 1 || ordenes.has(orden))
        errores.push({ campo: `${campo}[${i}].orden`, mensaje: 'debe ser positivo y único' });
      else ordenes.add(orden);
      if (esTexto(o.letra) && [...o.letra.trim()].length > 40)
        errores.push({ campo: `${campo}[${i}].letra`, mensaje: 'máximo 40 caracteres' });
      for (const k of ['texto', 'recursoRef']) {
        if (o[k] != null && typeof o[k] !== 'string')
          errores.push({ campo: `${campo}[${i}].${k}`, mensaje: 'debe ser texto' });
      }
    }
    if (!esTexto(o.texto) && !(versionadas && esTexto(o.recursoRef)))
      errores.push({ campo: `${campo}[${i}].texto`, mensaje: 'es obligatorio' });
  });
}

export function validarConfiguracion(valor: unknown): ErroresValidacion {
  if (!esObjeto(valor)) return [{ campo: 'cuerpo', mensaje: 'debe ser un objeto JSON' }];
  const e: ErroresValidacion = [];
  const c = valor as ConfiguracionSubpruebaRequest & Objeto;
  enteroOpcional(c, 'orden', 1, e);
  enteroOpcional(c, 'tiempoLimiteSegundos', 1, e);
  enteroOpcional(c, 'opciones', 2, e);
  enteroOpcional(c, 'maximoEntradas', 1, e);
  if (
    c.tipoRespuesta !== undefined &&
    c.tipoRespuesta !== 'SELECCION' &&
    c.tipoRespuesta !== 'TEXTO_LIBRE'
  )
    e.push({ campo: 'tipoRespuesta', mensaje: 'debe ser SELECCION o TEXTO_LIBRE' });
  if (c.tienePractica !== undefined && typeof c.tienePractica !== 'boolean')
    e.push({ campo: 'tienePractica', mensaje: 'debe ser verdadero o falso' });
  if (c.instrucciones !== undefined && typeof c.instrucciones !== 'string')
    e.push({ campo: 'instrucciones', mensaje: 'debe ser texto' });
  if (c.tienePractica === true && (!Array.isArray(c.ejemplos) || c.ejemplos.length === 0))
    e.push({ campo: 'ejemplos', mensaje: 'la práctica necesita al menos un ejemplo' });
  else if (c.ejemplos !== undefined) {
    if (!Array.isArray(c.ejemplos)) e.push({ campo: 'ejemplos', mensaje: 'debe ser una lista' });
    else
      c.ejemplos.forEach((ej, i) => {
        const campo = `ejemplos[${i}]`;
        if (!esObjeto(ej)) {
          e.push({ campo, mensaje: 'debe ser un objeto' });
          return;
        }
        if (!esEntero(ej.numero) || ej.numero < 1)
          e.push({ campo: `${campo}.numero`, mensaje: 'debe ser un entero positivo' });
        if (!esTexto(ej.enunciado))
          e.push({ campo: `${campo}.enunciado`, mensaje: 'es obligatorio' });
        if (ej.opciones != null) validarOpcionesLista(ej.opciones, `${campo}.opciones`, e);
      });
  }
  return e;
}

export function validarPregunta(valor: unknown): ErroresValidacion {
  if (!esObjeto(valor)) return [{ campo: 'cuerpo', mensaje: 'debe ser un objeto JSON' }];
  const e: ErroresValidacion = [];
  const p = valor as Partial<PreguntaRequest> & Objeto;
  if (!esEntero(p.numeroVisible) || p.numeroVisible < 1)
    e.push({ campo: 'numeroVisible', mensaje: 'es obligatorio (entero mayor o igual a 1)' });
  if (!esEntero(p.ordenPresentacion) || p.ordenPresentacion < 1)
    e.push({ campo: 'ordenPresentacion', mensaje: 'es obligatorio (entero mayor o igual a 1)' });
  if (!esTexto(p.enunciado)) e.push({ campo: 'enunciado', mensaje: 'es obligatorio' });
  if (p.recursoRef != null && typeof p.recursoRef !== 'string')
    e.push({ campo: 'recursoRef', mensaje: 'debe ser texto' });
  return e;
}

export function validarOpciones(valor: unknown): ErroresValidacion {
  if (!esObjeto(valor) || !('opciones' in valor))
    return [{ campo: 'opciones', mensaje: 'es obligatorio' }];
  const e: ErroresValidacion = [];
  const lista = (valor as Partial<OpcionesRequest>).opciones;
  validarOpcionesLista(lista, 'opciones', e, true);
  if (Array.isArray(lista) && lista.length === 0)
    e.push({ campo: 'opciones', mensaje: 'se necesita al menos una opción' });
  return e;
}

export function validarClave(valor: unknown): ErroresValidacion {
  if (!esObjeto(valor) || !Array.isArray(valor.entradas))
    return [{ campo: 'entradas', mensaje: 'es obligatorio y debe ser una lista' }];
  const e: ErroresValidacion = [];
  const vistos = new Set<number>();
  (valor as Partial<ClaveRequest>).entradas!.forEach((entrada, i) => {
    const o: Objeto = esObjeto(entrada) ? entrada : {};
    if (!esEntero(o.preguntaId))
      e.push({ campo: `entradas[${i}].preguntaId`, mensaje: 'es obligatorio (entero)' });
    else if (vistos.has(o.preguntaId))
      e.push({ campo: `entradas[${i}].preguntaId`, mensaje: 'está repetido' });
    else vistos.add(o.preguntaId);
    if (!esTexto(o.opcion)) e.push({ campo: `entradas[${i}].opcion`, mensaje: 'es obligatoria' });
  });
  return e;
}

export function validarBaremo(valor: unknown): ErroresValidacion {
  if (!esObjeto(valor)) return [{ campo: 'cuerpo', mensaje: 'debe ser un objeto JSON' }];
  const e: ErroresValidacion = [];
  const b = valor as Partial<BaremoRequest> & Objeto;
  if (!esTexto(b.nombre)) e.push({ campo: 'nombre', mensaje: 'es obligatorio' });
  if (!esTexto(b.version)) e.push({ campo: 'version', mensaje: 'es obligatoria' });
  if (!esEntero(b.anioNorma)) e.push({ campo: 'anioNorma', mensaje: 'es obligatorio (entero)' });
  if (!Array.isArray(b.rangos)) {
    e.push({ campo: 'rangos', mensaje: 'es obligatorio y debe ser una lista' });
    return e;
  }
  b.rangos.forEach((r, i) => {
    const o: Objeto = esObjeto(r) ? r : {};
    const c = (campo: string) => `rangos[${i}].${campo}`;
    if (!esTexto(o.idFactor)) e.push({ campo: c('idFactor'), mensaje: 'es obligatorio' });
    if (!esEntero(o.percentil) || o.percentil < 1 || o.percentil > 99)
      e.push({ campo: c('percentil'), mensaje: 'debe estar entre 1 y 99' });
    if (o.estadoCelda !== 'RANGO' && o.estadoCelda !== 'SIN_DATO')
      e.push({ campo: c('estadoCelda'), mensaje: 'debe ser RANGO o SIN_DATO' });
    else if (o.estadoCelda === 'RANGO') {
      if (!esEntero(o.limiteInferior))
        e.push({ campo: c('limiteInferior'), mensaje: 'es obligatorio si estadoCelda es RANGO' });
      if (!esEntero(o.limiteSuperior))
        e.push({ campo: c('limiteSuperior'), mensaje: 'es obligatorio si estadoCelda es RANGO' });
      else if (esEntero(o.limiteInferior) && o.limiteSuperior < o.limiteInferior)
        e.push({
          campo: c('limiteSuperior'),
          mensaje: 'no puede ser menor que el límite inferior',
        });
    }
  });
  return e;
}

/** Texto «A = opción» por línea → lista de opciones. */
export function analizarOpciones(texto: string): OpcionRespuesta[] {
  return texto
    .split('\n')
    .filter((linea) => linea.trim() !== '')
    .map((linea) => {
      const [letra = '', ...resto] = linea.split('=');
      return { letra: letra.trim(), texto: resto.join('=').trim() };
    });
}
