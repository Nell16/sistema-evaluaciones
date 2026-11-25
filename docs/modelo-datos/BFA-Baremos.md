# BFA: baremos y conversión a percentil

Esta integración reúne la persistencia de [PR #69](https://github.com/Nell16/sistema-evaluaciones/pull/69), la carga E30 de [PR #70](https://github.com/Nell16/sistema-evaluaciones/pull/70) y las reglas de conversión de [PR #75](https://github.com/Nell16/sistema-evaluaciones/pull/75). Los ejemplos y pruebas contienen únicamente datos ficticios.

## Persistencia y estados

E30 (`POST /api/v1/baremos`) guarda la cabecera `baremo_bfa` y sus celdas `rango_baremo_bfa`. Un baremo recién cargado no habilita percentiles. La tabla `asignacion_baremo_bfa` vincula un baremo con una versión exacta de formulario y una versión exacta de reglas. La aprobación requiere evidencia; una asignación retirada deja de ser candidata. No se agregó una ruta REST de aprobación porque no figura en OpenAPI.

La migración es `V202611251021__bfa_baremos.sql`, posterior a las migraciones integradas al iniciar BFA-BE-37. El custodio de migraciones debe validar su orden antes de fusionar.

## Conversión

`AplicarBaremoBFAService` busca asignaciones aprobadas para la pareja formulario/reglas. Con una sola asignación, `ConversionPercentilBFAService` utiliza únicamente los rangos del factor que contienen el puntaje bruto. Conserva los identificadores del baremo y del rango para trazabilidad.

Si falta la aprobación, hay varias candidatas, no existe un rango exacto o el factor carece de puntuación directa, el resultado conserva el estado correspondiente y deja el percentil vacío. Las celdas `SIN_DATO` no generan un rango aplicable. El servicio interno de aprobación registra la evidencia de compatibilidad revisada; la comprobación de equivalencia psicométrica de la fuente queda a cargo de quien aprueba el baremo.
