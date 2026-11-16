# Asterion System — núcleo local de progreso y adaptación

## Alcance de esta rama

Esta implementación añade un motor Kotlin autocontenido bajo
`app/src/main/java/com/huntersystem/app/asterion/`, un adaptador Android y
paneles Compose accesibles desde Misiones, Rutinas y Progreso.

**No añade, copia ni modifica el catálogo de ejercicios.** Los registros
del catálogo actual se leen mediante `AsterionActivitySource`; una
implementación futura de `ActivityRepositoryInterface` puede sustituir
esa fuente sin cambiar las reglas de progresión.

Este PR está abierto como borrador porque el planificador histórico sigue
existiendo y requiere una migración gradual para adoptar todas las reglas.

## Categorías, clasificación y fuentes

Los índices 0–4 representan, en orden, `tren_superior`,
`tren_central`, `tren_inferior`, `movilidad` y `cardio`; el 5
representa descanso. Las expresiones heredadas `tronco`,
`integrado`, `piernas` y `coordinacion` se normalizan en memoria,
sin cambiar los IDs guardados.

Cada `ActivityMetadata` enlaza una variante externa con su categoría,
familia y tipo; además expone complejidad técnica, intensidad descriptiva,
equipo, modalidad, necesidad de supervisión, etiquetas y restricciones.
`ActivityCatalogValidator` rechaza IDs repetidos, familias con
categorías conflictivas y tipos con relaciones incompatibles.

El adaptador interpreta la escala existente `planning49.level` de
0–2 como complejidad técnica 1–3 **solo si existe ese metadato**.
Si no hay datos de intensidad o de material verificables, el resultado
queda excluido de la selección automática. Los niveles 0, 4 y 5,
las variantes supervisadas o no autorizadas, y la intensidad alta o
desconocida quedan excluidos.

Esto puede producir propuestas vacías para registros actuales cuya
información descriptiva no permita verificar sus requisitos. No se
generan nombres ni movimientos ficticios para cubrir el vacío.

## Perfil y objetivo personal

`AsterionProfileAdapter` lee el perfil existente y convierte su
calendario histórico (domingo en índice cero) al contrato lunes–domingo.
El campo `custom_goal` tiene prioridad, con compatibilidad mediante
`finalGoal`. No se utiliza edad, altura, peso, sexo ni apariencia para
calcular una puntuación física.

`PersonalGoalInterpreter` utiliza una lista finita de reglas locales.
Devuelve el texto original, las etiquetas de afinidad encontradas y
un indicador de reconocimiento. Una negación simple bloquea el segmento
negado. Las etiquetas solo ordenan alternativas que ya superaron
`EligibilityPolicy`. No autorizan equipamiento ni elevan intensidad.

La firma SHA-256 del perfil incluye experiencia, fecha de inicio,
calendario, equipamiento, restricciones, objetivo, modalidad y
preferencias funcionales. Evita almacenar la meta íntegra dentro de
cada misión fijada.

## Misiones, rutinas y Monarca

`DailyMissionPlanner` consulta la categoría del día. Si es descanso,
devuelve una propuesta vacía. Filtra antes de ordenar y fija los IDs
de la fecha local usando la firma del perfil y de los metadatos de
origen. Una propuesta restaurada se valida nuevamente. El esfuerzo
registrado hoy no reordena la misión de hoy.

`MissionDifficultyMixPolicy` conserva las fases 0–2: 25/40/60%
de nivel 3 para intermedio y 33/55/75% para avanzado, como preferencias
de selección, no cuotas obligatorias. Un principiante solo puede
recibir niveles 1 y 2.

`RoutineGenerator` admite 10, 15, 20, 30, 45 o 60 minutos. Ofrece
hasta seis combinaciones reproducibles, de 3–6 pasos como límite,
sin relajar restricciones cuando hay pocas alternativas.

`MonarchChallengePlanner` produce un ciclo opcional de cinco días.
Requiere consentimiento en su llamada, respeta descansos y utiliza
la misma elegibilidad. El estado de consentimiento por ciclo se
almacena de forma independiente en `monarchConsent`.
**La interfaz Monarca heredada aún no está sustituida por este nuevo
planificador; requiere integración específica para evitar dos ciclos
competidores.**

## Participación, valoraciones y adaptación

`AsterionProgressRepository` guarda acciones explícitas bajo
`asterionV1` en el mismo archivo local atómico preexistente.
Se distinguen `PENDING`, `COMPLETED` y `SKIPPED`; la clave de
fecha + variante es idempotente. El porcentaje de participación
usa solamente completadas y omitidas; sin evaluadas el resultado es
«Sin datos suficientes».

Las valoraciones RPE se guardan por categoría y fecha solo cuando
se confirma la acción. La categoría nula se reserva para valoraciones
globales, que jamás migran automáticamente a una categoría.

La adaptación consulta como máximo 21 días anteriores de la misma
categoría: sin dato mantiene; RPE 1–3 admite considerar variedad
equivalente; 4–7 mantiene; 8 limita a dos alternativas de niveles
bajos; 9–10 limita a una sencilla. Una señal elevada de esfuerzo
tiene prioridad sobre búsqueda de variedad.

Las revisiones semanales exigen dos días distintos realmente
completados y valorados con RPE bajo; el tiempo por sí solo
no implica avance. Los hitos 7/14/21 permiten, con evidencia,
avanzar como máximo a fase 2. Una modificación del nivel de
experiencia reinicia fases y referencias activas, nunca los eventos
históricos.

## Volumen independiente

`VolumeProfileInterface` define los parámetros que **debe**
proporcionar una fuente externa validada. No se generan valores
iniciales cuando falten. `VolumeProgressionEngine` solo compara
categoría, variante, tipo, nivel, unidad y bloque compatibles;
excluye intervenciones incompletas e interrupciones.

Los cambios matemáticos de RPE 1–10 se limitan por los parámetros
validados. Los aumentos se devuelven como **opcionales**; el motor
de consulta no actualiza por sí mismo ninguna referencia.
Una serie adicional exige tres sesiones en días diferentes y
al menos siete días de separación, y nunca aumenta a la vez
la cantidad por serie.

`VolumeResetPolicy` produce identificadores de bloque por variante,
categoría, experiencia y fecha. Las observaciones anteriores se
conservan íntegramente. Segundos y repeticiones nunca se comparan.

**La fuente de perfiles numéricos sigue desconectada de la interfaz
Android**: por defecto se devuelve «revisión con orientación» sin
referencia. Esto es deliberado para evitar inventar cantidades.

## Persistencia y compatibilidad

`asterionV1` almacena por separado:
- `missions`: fecha, hashes de perfil/fuente, IDs, explicación.
- `participation`: estado individual y observación opcional.
- `efforts`: RPE por categoría o global, con condición de completada.
- `phases`: fase y fecha del cambio por categoría.
- `volume`: observaciones, unidad, nivel y bloque de referencia.
- `blocks`: identificadores de los bloques activos.
- `routineDays`: fechas con 10 puntos otorgados una vez.
- `monarchConsent`: permisos explícitos por ciclo.
- `experienceSince`: fecha del inicio de la experiencia seleccionada.

La migración se realiza por adición de campos. No se reescriben
`logs`, `cycles`, `profile` ni los datos del catálogo.
La compatibilidad de las pantallas originales se mantiene y las
copias de seguridad existentes conservan datos desconocidos.

## Pruebas y validación

`AsterionRulesTest` cubre reglas de categorías, descanso,
metadatos, negaciones, estabilidad, restricciones, ventanas de RPE,
hitos, rutinas, resumen, volumen, bloques, experiencia y Monarca.

`AsterionPersistenceTest` verifica con Robolectric los guardados
idempotentes, restauración, distinción de esfuerzos globales, volumen
y consentimiento. Los movimientos de prueba son **sintéticos**, no
se introducen en producción.

Comando de verificación:

```bash
bash gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest
```

El flujo Android general del repositorio incluye una descarga de arte
promocional externa que puede fallar antes de ejecutar Gradle. Por eso
se añade `.github/workflows/asterion-engine.yml` para comprobar el
motor de forma independiente de ese servicio.

## Trabajo pendiente antes de fusionar

- Verificar compilación completa y todas las pruebas en CI.
- Conectar una fuente validada de `VolumeProfileInterface`.
- Unificar el planificador histórico de misiones con el nuevo
  coordinador, sin perder personalizaciones ni resultados existentes.
- Integrar el ciclo Monarca de cinco días con la interfaz del desafío
  original y sus permisos de participación.
- Completar las acciones de aceptación de cambios de fase/volumen
  en la interfaz, manteniendo siempre la posibilidad de descansar.
- Comprobar la experiencia nativa en dispositivos Android físicos,
  accesibilidad y migración de instalaciones existentes.
