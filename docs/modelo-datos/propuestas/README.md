# persistencia de revisiones

esta rama activa la propuesta común de doc-34 mediante `database/migrations/V202611251630__comun_revisiones_resultado.sql`. la copia manual del ddl se retira para mantener una sola fuente ejecutable. las pruebas usan flyway; no crean tablas leyendo documentos.

orden de dependencias: 1600 claves, 1610 baremos, 1620 factores del contenedor, 1630 revisiones comunes, 261000 revisión de palabras y 261010 protección de reglas utilizadas. las versiones completas están en los nombres de las migraciones. ninguna migración fusionada se modifica.

`revision_resultado`, `resultado_escala` y `conversion_resultado_escala` conservan el snapshot publicado. e10–e12 amplían esa estructura; no crean otra tabla de revisiones. e9 lee exclusivamente la revisión vigente validada y verifica formulario, reglas, audiencia y alcance. el sujeto no recibe puntajes.

el recálculo admite solo la implementación identificada por `bfa-verbal-v1`, con reglas aprobadas y evidencia explícita. si no se indica versión y no hay revisión previa, debe existir una sola regla compatible. el identificador no acredita reglas psicométricas ni baremos oficiales.

los datos de prueba son ficticios. siguen pendientes ci sobre los sha publicados, revisión independiente, testing y comprobación del despliegue. véase `docs/evidencias/sprint03/INT-33-cierre-local-bfa.md` para la evidencia conjunta y sus límites.
