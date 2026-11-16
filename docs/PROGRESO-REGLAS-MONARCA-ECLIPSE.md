# Monarca Eclipse — motor de progreso basado en reglas

## Alcance

El motor se encuentra en `HunterProgressRules.kt` y el adaptador del catálogo nativo en
`HunterProgressBridge.kt`. No añade ni altera registros del catálogo, inventarios,
fichas, imágenes, animaciones ni identificadores históricos. El adaptador conserva el
catálogo de la APK como dependencia y separa su lectura de la escritura del historial.

## Contrato externo

`HunterCatalogPort` expone las cinco categorías, familias, tipos, variantes, búsqueda,
consulta por ID y revisión del contenido. `ValidatedHunterCatalog` verifica duplicados,
referencias y pertenencia a una única categoría. Se rechazan variantes de complejidad
desconocida (nivel 0), niveles 4 y 5 y aquellas que exigen supervisión o están pendientes.
El filtro combina elegibilidad, intensidad, equipamiento y restricciones del perfil.

La clasificación nueva es:
- `tren_superior`, `tren_central`, `tren_inferior`, `movilidad`, `cardio`.
- `descanso` es exclusivamente una opción del calendario.
- Alias de lectura: `tronco` e `integrado` a tren central; `piernas` a tren inferior;
  `coordinacion` a movilidad.
- La representación nueva del calendario es lunes a domingo. Los arreglos anteriores
  `weekPlan/weekFocus` siguen almacenándose domingo a sábado y se normalizan
  solamente al consultar. No se reescriben los días históricos.

Los metadatos existentes `planning49.level` (0–2) se traducen a complejidad inicial
(1–3) cuando no existe un nivel técnico explícito. Esto no es una medición de capacidad.
Para intensidad descriptiva, los metadatos ya existentes de impacto y acondicionamiento
pueden identificar las opciones bajas; si no hay señal suficiente se clasifica de manera
conservadora como moderada. No se infieren equipos.

## Persistencia y selección

`NativeStore.makeDay` utiliza el perfil guardado, la categoría de la fecha local y
la revisión del catálogo. Cada día conserva el hash SHA-256 de las preferencias
relevantes y otro hash del catálogo. El texto del objetivo no se duplica en las misiones.
Los IDs fijados se validan nuevamente al restaurar la propuesta. En la misma fecha
no se reordena la propuesta tras registrar esfuerzo o completar una actividad.

El texto personal se almacena en `custom_goal` y, por compatibilidad visual, el editor
existente sigue usando `finalGoal`. La interpretación es local y admite negaciones
básicas. Las afinidades ordenan únicamente alternativas que ya superaron los filtros.

Los estados explícitos son `PENDING`, `COMPLETED` y `SKIPPED`. Las marcas de
participación no inventan series. Las omisiones son reversibles. El porcentaje se
calcula solo sobre completadas y omitidas; si no hay evaluadas se presenta
«Sin datos suficientes».

## Esfuerzo y adaptación

Las nuevas valoraciones de esfuerzo completadas y asignables a una categoría llevan
`categoryRpe`; el formato global `rpe` sigue disponible. Historiales antiguos
sin `categoryRpe` **no** se convierten en evidencias de categorías individuales.
Una valoración es opcional. Las sesiones sin actividad completada no acreditan avance.

Se consulta un máximo de 21 días por categoría y únicamente días anteriores al de
la propuesta: 1–3 invita a revisar variedad equivalente, 4–7 mantiene, 8 limita
a dos actividades y 9–10 limita a una, dando prioridad a recuperación. La revisión
semanal se habilita tras siete días naturales desde la primera valoración; exige
dos días distintos con sesiones completadas y esfuerzo bajo antes de permitir
una alternativa del mismo tipo, nivel, unidad e intensidad.

Las fases 0–2 se revisan en los hitos de 7, 14 y 21 días desde el inicio del nivel.
El estado es independiente por categoría y se guarda en `hunterPhases`. Una reducción
manual de fase anula una subida en el mismo hito y se aplica a futuras propuestas.
No se realizan subidas automáticas de carga, repeticiones o series a partir del RPE.
La revisión global del ciclo de 21 días es meramente descriptiva.

## Integración con rutinas y verificación

El generador de rutinas ahora utiliza el mismo contrato de elegibilidad técnica y
de equipamiento. Los filtros antiguos de lesiones y material se mantienen en
`Rules`, siempre con prioridad. Los registros de volumen distinguen segundos de
repeticiones, series registradas y confirmaciones rápidas.

Pruebas sintéticas en `HunterProgressRulesTest` verifican categorías, alias, contrato,
negaciones, descanso, equipo, niveles técnicos, fijación diaria, RPE independiente,
semanas, fases y unidades. `monarca-progress-tests.yml` ejecuta pruebas Kotlin sin
requerir descarga de arte remoto ni firma de APK.

## Límites

El motor no diagnostica lesiones, no valida equivalencia biomecánica ni certifica
la calidad de la demostración visual. Los elementos sin evidencia técnica o sin
equipo compatible no se inventan ni fuerzan. La evaluación de los recursos pendientes
del catálogo continúa siendo un trabajo aparte.
