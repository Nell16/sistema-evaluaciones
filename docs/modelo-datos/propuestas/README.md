# persistencia común de revisiones

esta rama instala el esquema de doc-34 mediante `database/migrations/V202611251630__comun_revisiones_resultado.sql`, después de baremos 1610 y factores 1620. incluye las referencias físicas a claves, baremos y rangos. ninguna migración fusionada se modifica.

las pruebas de e9 usan exclusivamente las tablas creadas por flyway. se retira la copia manual del sql para conservar una sola fuente ejecutable. la publicación e12 debe ampliar esta estructura común y conservar el snapshot exacto; no debe crear otra tabla `revision_resultado` incompatible.

las pruebas locales usan cuentas y puntuaciones ficticias. quedan pendientes ci del sha publicado, revisión independiente, testing y comprobación del vps. la migración no carga normas ni acredita una calificación psicométrica.
