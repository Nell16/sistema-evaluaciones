# cierre local de integración bfa

base verificada: main `6b77b2a`. la entrega está preparada localmente; todavía no acredita fusión, despliegue ni testing independiente.

## resultado

- integración de #135 sobre las claves de #155, conservando las cuatro correcciones de claves que estaban en main.
- esquema común de revisiones de #163 activado con flyway y referencias físicas a claves, baremos y rangos. se retiran las cargas manuales de ddl de las pruebas.
- e8 de #153 calcula desde respuestas persistidas y conserva el id de clave, el contenedor y la idempotencia.
- e10–e12 de #166 adaptados al mismo esquema: decisiones de palabras, recálculo real, snapshots de factores y conversiones, publicación validada y lectura exacta mediante e9.
- alcance explícito cuenta–sujeto, bloqueo por evaluación, idempotencia de solicitudes, comprobación de la revisión esperada y auditoría con la evaluación asociada.
- una revisión validada permanece inmutable aunque deje de ser la vigente. los metadatos de sus reglas tampoco se reescriben; retirar la regla conserva su identidad.
- la primera revisión puede omitir la versión únicamente si existe una sola regla aprobada para la implementación `bfa-verbal-v1`. una clave o baremo solicitado incompatible se rechaza.

## validación final

jdk 17 y postgres 16 aislado. comando final:

```sh
mvn -f backend/pom.xml -Dspring.datasource.hikari.maximum-pool-size=2 -Dspring.test.context.cache.maxSize=4 clean verify
```

resultado: **586 pruebas, cero fallos, errores y omisiones**. las propiedades limitan únicamente los recursos del proceso local de pruebas. se usaron variables de conexión a una base ficticia; no se escribió en el vps.

frontend idéntico al de la integración e8 verificada: lint, prettier, tipos, **148 pruebas** y build correctos. las dependencias reutilizadas tenían el mismo package.json y lockfile; ci debe repetir npm ci en el sha publicado.

la prueba por http real usa un puerto aleatorio y un esquema exclusivo, con cuentas ficticias y bearer emitido por e14. comprueba dos recálculos simultáneos con una sola revisión, dos publicaciones que compiten con un ganador y un 409, y rollback completo de publicación ante un fallo deliberado de auditoría. e9 conserva la revisión anterior después del fallo.

las pruebas mvc adicionales recorren e8 → e11 → e10 → e12 → e9; comprueban cero, rango y baremo exactos, audiencia, sujeto sin puntajes, falta de alcance, reglas no aprobadas, implementación desconocida, palabras pendientes y revisiones históricas protegidas.

## entregas por dependencia

| parte | alcance | líneas antes de la documentación final |
|---|---|---:|
| 1 | contratos y metadatos comunes | 243 |
| 2 | recálculo con insumos y normas trazables | 172 |
| 3 | persistencia y snapshots | 588 |
| 4 | servicio autorizado de revisión y publicación | 686 |
| 5 | regresiones de permisos y conflictos | 157 |
| 6 | api y recorrido integrado | 685 |
| 7 | concurrencia y rollback por http | 170 |
| 8 | resolución de reglas aprobadas | 71 |
| 9 | conservación histórica de reglas | 64 |

cada parte compiló y pasó sus pruebas relevantes antes del commit. la suite final valida la cadena completa. las partes dependen de la integración previa de baremos, esquema común y e8/e9; no deben fusionarse hacia una base sin esas dependencias. los títulos y cuerpos de los prs quedaron preparados en archivos locales.

hilo confirmó los tamaños frente a las bases reales de la cadena. su validador de cuerpo sigue exigiendo la capitalización literal de la etiqueta «Tarea ClickUp»; los borradores conservan las minúsculas solicitadas por el usuario. no se falsearon etiquetas de testing ni hechos de aprobación.

## límites y seguimiento

esto no acredita una batería psicométrica completa: fluidez y las familias sin calificador aprobado mantienen `NO_CALCULABLE`; no se inventan puntajes, normas ni percentiles. todos los valores de aceptación son ficticios. las reglas y los insumos reales requieren su aprobación propia.

faltan push autorizado, ci en github sobre los sha publicados, revisión de otro code owner, testing independiente y comprobación en la url desplegada. #166 solo debe cerrarse como sustituido cuando las entregas de reemplazo estén identificadas e integradas. #144 y #145 conservan sus requisitos de qa independiente.

se preservan el historial y los aportes originales de fernando, nelson, daysi, davide y rodrigo. el aporte de kisnner es la resolución de dependencias, las correcciones de integridad y autorización, la conexión del flujo común y las nuevas regresiones. también están preparadas las correcciones documentales de #146 y #170 en sus ramas locales.
