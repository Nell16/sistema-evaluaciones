// Recorrido de regresión de interfaz de GEN-02-TEST (TC-GEN-02-001 a 024).
// Interfaz de main d33f6a8 compilada con los mocks por defecto (VITE_USAR_MOCKS sin definir).
// Datos ficticios del mock. Uso: node gen02-recorrido.js <url-base> <carpeta-capturas>
const { chromium } = require('playwright');
const fs = require('fs');

const BASE = process.argv[2] || 'http://127.0.0.1:5173';
const CAPS = process.argv[3] || 'capturas';
fs.mkdirSync(CAPS, { recursive: true });
const resultados = [];

async function caso(id, titulo, fn) {
  const navegador = await chromium.launch();
  const p = await navegador.newPage({ viewport: { width: 1280, height: 900 } });
  let estado = 'PASÓ';
  let obtenido = '';
  try {
    obtenido = await fn(p);
  } catch (e) {
    estado = 'FALLÓ';
    obtenido = String(e.message || e).split('\n')[0];
  }
  await p.screenshot({ path: `${CAPS}/${id}.jpg`, type: 'jpeg', quality: 70, fullPage: true });
  await navegador.close();
  resultados.push({ id, titulo, estado, obtenido });
  console.log(`${id} | ${estado} | ${titulo} | ${obtenido}`);
}

const esperar = (p, ms = 1200) => p.waitForTimeout(ms);
const abrir = async (p, testId = '1') => {
  await p.goto(`${BASE}/evaluaciones/inicio?testId=${testId}`);
  await esperar(p, 1600);
};
const panel = async (p) => (await p.locator('.ev-panel, .ev-pagina').first().innerText()).replace(/\s+/g, ' ');
const pagina = async (p) => (await p.locator('.ev-pagina').innerText()).replace(/\s+/g, ' ');
function exigir(condicion, mensaje) {
  if (!condicion) throw new Error(mensaje);
}
const sujeto = (p) => p.getByLabel('ID del evaluado', { exact: true });
const periodo = (p) => p.getByLabel('Período', { exact: true });
const letra = (p) => p.getByLabel('Letra inicial de Fluidez Verbal');
const iniciar = (p) => p.getByRole('button', { name: 'Iniciar evaluación' });
async function buscar(p, id) {
  await sujeto(p).fill(id);
  await p.getByRole('button', { name: 'Buscar' }).click();
  await esperar(p);
}
async function retomar(p, id) {
  await p.getByRole('tab', { name: 'Retomar' }).click();
  await p.getByLabel('ID de la evaluación').fill(id);
  await p.getByRole('button', { name: 'Recuperar' }).click();
  await esperar(p);
}

(async () => {
  // ---------- A. Carga y configuraciones ----------
  await caso('TC-GEN-02-001', 'Configuraciones permitidas del test 1', async (p) => {
    await abrir(p);
    const filas = await p.locator('.ev-tabla tbody tr').allInnerTexts();
    const ordenes = filas.map((f) => Number(f.trim().split(/\s+/)[0]));
    exigir(filas.length === 13, `filas=${filas.length}`);
    exigir(ordenes.every((o, i) => o === i + 1), `orden=${ordenes.join(',')}`);
    const t = await pagina(p);
    exigir(t.includes('Completa (13 subpruebas)'), 'no muestra aplicación completa');
    return `13 subpruebas en orden 1–13 (VOC1…FV) con tiempo, respuesta y preguntas; períodos disponibles: ${(await periodo(p).locator('option').allInnerTexts()).slice(1).join(' / ')}`;
  });
  await caso('TC-GEN-02-002', 'testId inválido (abc y 0)', async (p) => {
    await abrir(p, 'abc');
    const a = await pagina(p);
    await abrir(p, '0');
    const b = await pagina(p);
    exigir(/entero positivo/.test(a) && /entero positivo/.test(b), 'sin mensaje de validación');
    exigir((await iniciar(p).count()) === 0, 'muestra el formulario');
    return 'Mensaje «El ID del test debe ser un número entero positivo. Vuelva al catálogo…»; no muestra el formulario';
  });
  await caso('TC-GEN-02-003', 'Test inexistente (testId=2)', async (p) => {
    await abrir(p, '2');
    const t = await pagina(p);
    exigir(/no existe o no está activo/.test(t), t.slice(0, 200));
    return 'Aviso «No encontrado: El test seleccionado no existe o no está activo.» con opción de reintentar';
  });

  // ---------- B. Validaciones ----------
  await caso('TC-GEN-02-004', 'Enviar el formulario vacío', async (p) => {
    await abrir(p);
    await iniciar(p).click();
    await esperar(p, 400);
    const t = await panel(p);
    exigir(/Ingrese el ID del evaluado/.test(t) && /Seleccione un período/.test(t), t.slice(0, 300));
    exigir(!/Evaluación creada/.test(t), 'avanzó');
    return 'Errores en Evaluado («Ingrese el ID del evaluado.»), Período («Seleccione un período.») y letra FV; no se crea nada';
  });
  await caso('TC-GEN-02-005', 'ID del evaluado no numérico', async (p) => {
    await abrir(p);
    await buscar(p, 'abc');
    const t = await panel(p);
    exigir(/debe ser un número entero positivo/.test(t), t.slice(0, 200));
    return '«El ID del evaluado debe ser un número entero positivo.»; no se llama a la búsqueda';
  });
  await caso('TC-GEN-02-006', 'Iniciar sin confirmar al evaluado', async (p) => {
    await abrir(p);
    await sujeto(p).fill('26');
    await periodo(p).selectOption({ index: 1 });
    await letra(p).fill('M');
    await iniciar(p).click();
    await esperar(p, 400);
    const t = await panel(p);
    exigir(/Busque y confirme al evaluado/.test(t), t.slice(0, 200));
    exigir(!/Evaluación creada/.test(t), 'avanzó');
    return '«Busque y confirme al evaluado antes de iniciar.»; no avanza';
  });
  await caso('TC-GEN-02-007', 'Evaluado inexistente (99)', async (p) => {
    await abrir(p);
    await buscar(p, '99');
    const t = await panel(p);
    exigir(/No existe un evaluado con ese ID/.test(t), t.slice(0, 200));
    return 'Aviso «No encontrado: No existe un evaluado con ese ID. Debe registrarse antes de evaluarlo.» (SUJETO_NO_ENCONTRADO)';
  });
  await caso('TC-GEN-02-008', 'Parcial sin subpruebas', async (p) => {
    await abrir(p);
    await buscar(p, '26');
    await periodo(p).selectOption({ index: 1 });
    await p.getByLabel(/Parcial/).check();
    await iniciar(p).click();
    await esperar(p, 400);
    const t = await panel(p);
    exigir(/Seleccione al menos una subprueba/.test(t), t.slice(0, 200));
    return '«Seleccione al menos una subprueba.»; no avanza';
  });
  await caso('TC-GEN-02-009', 'Letra de FV vacía, con un carácter que no es letra o con dos letras', async (p) => {
    await abrir(p);
    await buscar(p, '26');
    await periodo(p).selectOption({ index: 1 });
    await iniciar(p).click();
    await esperar(p, 400);
    const vacia = /una sola letra inicial/.test(await panel(p));
    await letra(p).fill('5');
    await iniciar(p).click();
    await esperar(p, 400);
    const numero = /una sola letra inicial/.test(await panel(p));
    await letra(p).fill('ab');
    const valorDos = await letra(p).inputValue();
    exigir(vacia && numero, `vacía=${vacia} número=${numero}`);
    exigir(valorDos.length === 1, `acepta ${valorDos.length} caracteres`);
    exigir(!/Evaluación creada/.test(await panel(p)), 'avanzó');
    return `«Fluidez Verbal requiere una sola letra inicial.» con letra vacía y con «5»; el campo solo admite un carácter (al escribir «ab» queda «${valorDos}»); no avanza`;
  });

  // ---------- C. Iniciar ----------
  await caso('TC-GEN-02-010', 'Aplicación completa con datos válidos', async (p) => {
    await abrir(p);
    await buscar(p, '26');
    await periodo(p).selectOption({ index: 1 });
    await letra(p).fill('m');
    await iniciar(p).click();
    await esperar(p, 1600);
    const t = await panel(p);
    exigir(/Evaluación creada/.test(t) && /En curso/.test(t), t.slice(0, 200));
    const pend = (t.match(/Pendiente/g) || []).length;
    exigir(pend >= 13, `pendientes=${pend}`);
    return `«Evaluación creada», En curso, #${(t.match(/#(\d+)/) || [])[1]}, 13 subpruebas Pendiente, progreso 0 de 13; botón «Continuar con la evaluación»`;
  });
  await caso('TC-GEN-02-011', 'Parcial VOC1 y CV (marcadas en otro orden)', async (p) => {
    await abrir(p);
    await buscar(p, '26');
    await periodo(p).selectOption({ index: 1 });
    await p.getByLabel(/Parcial/).check();
    await p.getByRole('checkbox', { name: /Comprensión Verbal/ }).check();
    await p.getByRole('checkbox', { name: /Vocabulario - Forma A/ }).check();
    exigir((await letra(p).count()) === 0, 'pide letra sin FV');
    await iniciar(p).click();
    await esperar(p, 1600);
    const t = await panel(p);
    exigir(/Evaluación creada/.test(t), t.slice(0, 200));
    const iV = t.indexOf('Vocabulario - Forma A');
    const iC = t.indexOf('Comprensión Verbal');
    exigir(iV > -1 && iC > iV && !/Fluidez Verbal/.test(t), 'orden o subpruebas incorrectas');
    exigir(/0 de 2/.test(t), 'progreso');
    return 'Creada con 2 subpruebas en orden oficial (VOC1, CV), sin pedir letra; progreso 0 de 2';
  });
  await caso('TC-GEN-02-012', 'Parcial con FV', async (p) => {
    await abrir(p);
    await buscar(p, '26');
    await periodo(p).selectOption({ index: 1 });
    await p.getByLabel(/Parcial/).check();
    await p.getByRole('checkbox', { name: /Fluidez Verbal/ }).check();
    exigir((await letra(p).count()) === 1, 'no pide letra');
    await iniciar(p).click();
    await esperar(p, 400);
    const sin = /una sola letra inicial/.test(await panel(p));
    await letra(p).fill('S');
    await iniciar(p).click();
    await esperar(p, 1600);
    const t = await panel(p);
    exigir(sin && /Evaluación creada/.test(t) && /Fluidez Verbal/.test(t), t.slice(0, 200));
    return 'Al marcar FV aparece la letra; sin letra no avanza; con «S» se crea con FV';
  });
  await caso('TC-GEN-02-013', 'Durante el envío no se avanza ni se crea otra', async (p) => {
    await abrir(p);
    await buscar(p, '26');
    await periodo(p).selectOption({ index: 1 });
    await letra(p).fill('M');
    await iniciar(p).click();
    await esperar(p, 150);
    const campos = await p.locator('form.ev-form fieldset').first().evaluate((n) => n.disabled);
    const btn = p.locator('form.ev-form button[type=submit]');
    const boton = { texto: await btn.innerText(), deshabilitado: await btn.isDisabled() };
    const durante = await panel(p);
    exigir(campos, 'los campos siguen habilitados');
    exigir(/Creando la evaluación en el servidor/.test(durante), 'sin indicador de carga');
    exigir(!/Evaluación creada/.test(durante), 'avanzó antes de la respuesta');
    exigir(boton.deshabilitado, 'el botón se puede volver a pulsar');
    await btn.click({ force: true });
    await esperar(p, 1800);
    const t = await panel(p);
    const num = (t.match(/#(\d+)/) || [])[1];
    exigir(/Evaluación creada/.test(t) && num === '100', `evaluación #${num}`);
    return `Mientras espera: campos deshabilitados, «Creando la evaluación en el servidor…» y sin «creada». El botón «${boton.texto}» queda deshabilitado (está dentro del fieldset deshabilitado); un clic forzado durante el envío no crea otra (queda #100)`;
  });

  // ---------- D. No duplicar ----------
  await caso('TC-GEN-02-014', 'Doble clic en «Iniciar evaluación»', async (p) => {
    await abrir(p);
    await buscar(p, '26');
    await periodo(p).selectOption({ index: 1 });
    await letra(p).fill('M');
    await iniciar(p).dblclick();
    await esperar(p, 1800);
    const t = await panel(p);
    const num = (t.match(/#(\d+)/) || [])[1];
    exigir(/Evaluación creada/.test(t) && num === '100', `evaluación #${num}`);
    await p.getByRole('button', { name: 'Iniciar otra evaluación' }).click();
    await buscar(p, '26');
    await periodo(p).selectOption({ index: 1 });
    await letra(p).fill('M');
    await iniciar(p).click();
    await esperar(p, 1600);
    exigir(/ya tiene una evaluación abierta/.test(await panel(p)), 'el segundo intento no fue rechazado');
    return 'Se crea una sola evaluación (#100, la primera del mock); un nuevo intento para el mismo evaluado y período se rechaza con 409';
  });
  await caso('TC-GEN-02-015', 'Repetir el inicio para el mismo evaluado y período', async (p) => {
    await abrir(p);
    for (let i = 0; i < 2; i++) {
      if (i === 1) await p.getByRole('button', { name: 'Iniciar otra evaluación' }).click();
      await buscar(p, '26');
      await periodo(p).selectOption({ index: 1 });
      await letra(p).fill('M');
      await iniciar(p).click();
      await esperar(p, 1600);
    }
    const t = await panel(p);
    exigir(/ya tiene una evaluación abierta/.test(t) && /Retómela/.test(t), t.slice(0, 300));
    exigir(!/Evaluación creada/.test(t), 'mostró otra creada');
    return 'Segundo inicio: aviso «No se puede continuar: El evaluado ya tiene una evaluación abierta… Retómela en lugar de iniciar otra.» (409 EVALUACION_EN_CURSO)';
  });
  await caso('TC-GEN-02-016', 'Evaluado con una evaluación abierta (25)', async (p) => {
    await abrir(p);
    await buscar(p, '25');
    await periodo(p).selectOption({ index: 1 });
    await letra(p).fill('M');
    await iniciar(p).click();
    await esperar(p, 1600);
    const t = await panel(p);
    exigir(/ya tiene una evaluación abierta/.test(t), t.slice(0, 200));
    return '409 EVALUACION_EN_CURSO: el sujeto 25 ya tiene la evaluación 15 abierta en 2026-2';
  });
  await caso('TC-GEN-02-017', 'Sujeto 409', async (p) => {
    await abrir(p);
    await buscar(p, '409');
    await periodo(p).selectOption({ index: 2 });
    await letra(p).fill('M');
    await iniciar(p).click();
    await esperar(p, 1600);
    const t = await panel(p);
    exigir(/ya tiene una evaluación abierta/.test(t), t.slice(0, 200));
    return '409 EVALUACION_EN_CURSO mostrado; no avanza';
  });

  // ---------- E. Retomar ----------
  await caso('TC-GEN-02-018', 'Retomar la evaluación 15 (en curso)', async (p) => {
    await abrir(p);
    await retomar(p, '15');
    const a = await panel(p);
    await retomar(p, '15');
    const b = await panel(p);
    await p.reload();
    await esperar(p, 1600);
    await retomar(p, '15');
    const c = await panel(p);
    for (const t of [a, b, c]) {
      exigir(/Evaluación recuperada/.test(t) && /#15/.test(t) && /En curso/.test(t), 'no recuperó');
      exigir(/tiempo restante 3:34 \(según el servidor\)/.test(t), 'tiempo distinto');
    }
    exigir(/Vocabulario - Forma A Cerrada/.test(a) && /Vocabulario - Forma B Activa/.test(a) && /Comprensión Verbal Pendiente/.test(a), 'estados');
    return '#15 En curso: VOC1 Cerrada, VOC2 Activa, CV Pendiente, «tiempo restante 3:34 (según el servidor)»; igual al recuperar dos veces y tras recargar';
  });
  await caso('TC-GEN-02-019', 'Retomar la evaluación 16 (finalizada)', async (p) => {
    await abrir(p);
    await retomar(p, '16');
    const t = await panel(p);
    exigir(/Finalizada/.test(t) && /ya finalizó; no admite más respuestas/.test(t), t.slice(0, 300));
    exigir((await p.getByRole('button', { name: 'Continuar con la evaluación' }).count()) === 0, 'permite continuar');
    return '#16 Finalizada con fin 29 oct 9:10, 2 de 2 cerradas; «La evaluación ya finalizó; no admite más respuestas.» y sin botón «Continuar»';
  });
  await caso('TC-GEN-02-020', 'Retomar una evaluación inexistente (99)', async (p) => {
    await abrir(p);
    await retomar(p, '99');
    const t = await panel(p);
    exigir(/No existe una evaluación con ese ID/.test(t), t.slice(0, 200));
    return 'Aviso «No encontrado: No existe una evaluación con ese ID.» (EVALUACION_NO_ENCONTRADA)';
  });
  await caso('TC-GEN-02-021', 'Retomar con ID inválido', async (p) => {
    await abrir(p);
    await retomar(p, 'x');
    const t = await panel(p);
    exigir(/debe ser un número entero positivo/.test(t), t.slice(0, 200));
    exigir(!/Evaluación recuperada/.test(t), 'recuperó');
    return '«El ID de la evaluación debe ser un número entero positivo.»; no consulta';
  });

  // ---------- F. Acceso y conexión ----------
  await caso('TC-GEN-02-022', 'Acceso denegado (403)', async (p) => {
    await abrir(p);
    await buscar(p, '403');
    const a = await panel(p);
    await retomar(p, '403');
    const b = await panel(p);
    exigir(/No tiene permiso/.test(a) && /No tiene permiso/.test(b), 'sin mensaje de acceso');
    exigir(!/Evaluación recuperada/.test(b), 'recuperó');
    return 'Evaluado 403 y evaluación 403: «Acceso denegado: No tiene permiso para realizar esta acción o ver esta evaluación.»';
  });
  await caso('TC-GEN-02-023', 'Error del servidor (500)', async (p) => {
    await abrir(p);
    await retomar(p, '500');
    const t = await panel(p);
    exigir(/error en el servidor/i.test(t), t.slice(0, 200));
    exigir((await p.getByRole('button', { name: /Reintentar/ }).count()) > 0, 'sin reintentar');
    return '«Error del servidor: Ocurrió un error en el servidor. Intente de nuevo más tarde.» con botón Reintentar';
  });
  await caso('TC-GEN-02-024', 'Sin conexión (503)', async (p) => {
    await abrir(p);
    await retomar(p, '503');
    const a = await panel(p);
    await p.getByRole('tab', { name: 'Iniciar nueva' }).click();
    await buscar(p, '503');
    const b = await panel(p);
    exigir(/No se pudo conectar con el servidor/.test(a) && /No se pudo conectar con el servidor/.test(b), 'sin mensaje de conexión');
    exigir(!/Evaluación recuperada/.test(a), 'recuperó');
    exigir((await p.getByRole('button', { name: /Reintentar/ }).count()) > 0, 'sin reintentar');
    return '«Sin conexión: No se pudo conectar con el servidor…» al retomar y al buscar, con Reintentar; no avanza';
  });

  fs.writeFileSync(`${CAPS}/resultados.json`, JSON.stringify(resultados, null, 2));
  const n = (e) => resultados.filter((r) => r.estado === e).length;
  console.log(`TOTAL: ${resultados.length} · PASÓ ${n('PASÓ')} · FALLÓ ${n('FALLÓ')}`);
})();
