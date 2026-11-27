# Claves de corrección BFA

## Alcance

La migración `V202611251600__bfa_claves_versionadas.sql`, recuperada del PR #50 para
BFA-BE-32, agrega `version_clave_bfa` y `entrada_clave_bfa`. Este tema documenta
exclusivamente las claves de preguntas de selección. Fluidez Verbal usa respuestas
de texto libre y no genera entradas de clave cerrada.

## Relaciones y restricciones

- `version_clave_bfa.id_version_formulario` apunta a `version_formulario`. El par
  `(id_version_formulario, version)` es único. Una clave nueva inicia en
  `BORRADOR`; `APROBADA` y `RETIRADA` requieren evidencia de aprobación.
- `entrada_clave_bfa.id_clave` apunta a la versión de clave. Cada entrada asocia
  una `pregunta` con una `opcion_respuesta` correcta. El par
  `(id_clave, id_pregunta)` es único.
- E29 comprueba que la pregunta pertenezca a una configuración de selección de
  la versión solicitada, que la opción pertenezca a esa pregunta y que exista
  exactamente una entrada por cada pregunta cerrada. Solo acepta versiones de
  formulario y claves en `BORRADOR`.
- La recarga de E29 reemplaza las entradas de la clave en una transacción y
  conserva `id_clave`. El endpoint responde únicamente con ese identificador;
  nunca devuelve las respuestas correctas.

## Procedencia y datos

El modelo de dominio proviene del PR #60; las dos tablas y sus adaptadores de
persistencia provienen del PR #50. Las pruebas usan únicamente contenido
ficticio. E29 exige el permiso `OPCIONES_EDITAR` mediante la autorización por
sesión Bearer del módulo común.
