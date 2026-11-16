# Consolidación Monarca Eclipse + Asterion (16/11/2026)

## Fuente de verdad

Hunter System V43.12 en Kotlin/Jetpack Compose conserva el catálogo local original, el reproductor SVG/MP4 y los registros tradicionales. Las reglas nuevas viven en `app/.../asterion/`, con adaptadores `AsterionAndroidBridge` y `AsterionLegacyMigration`. **No se añade ni se inventa ninguna variante del catálogo.**

La implementación alternativa de [PR #25](https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/25) se revisó como fuente de requisitos; **no se fusionan dos motores de progreso**. Se adoptan sus funciones relevantes: normalización del calendario, seguridad de la clasificación, datos explícitos, no aumentar dosis por RPE bajo, historial real y separación del RPE global. La integración histórica Monarca/Hunter2000 de [PR #18](https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/18) ya está en `main` respecto de código del catálogo: `Rules.kt` es idéntico y `Catalog.kt` solo difiere en un comentario; fusionar indiscriminadamente V42 sobre V43.12 regresaría código.

## Sesiones, categorías y participación

El perfil puede guardar la semana lunes–domingo como índices 0 a 5, compatible con `weekCategories` textuales y con el calendario histórico con domingo en el índice cero. La categoría del día se respeta, y descanso no crea ejercicios.

En la pantalla de Misiones y en Inicio se usa **el mismo plan Asterion fijado localmente** cuando no existe una sesión previa activa o personalización. Los registros anteriores, asignaciones propias y sesiones pausadas continúan con su reproductor y edición heredados. No aparecen dos listas de misión a la vez.

El progreso combina datos antiguos y Asterion por lectura, con prioridad para cambios explícitos más recientes; no modifica `logs`. Solo reconoce estados `PENDING`, `COMPLETED` y `SKIPPED`, más el indicador separado de interrupción; las tareas pendientes no cuentan en el denominador de cumplimiento. Los registros numéricos contienen únicamente series realmente confirmadas. Ninguna pantalla crea entrenamientos sintéticos para llenar gráficos.

## Esfuerzo y seguridad

El RPE es **voluntario y solo se guarda con un botón**. Ya no se guarda automáticamente al cerrar la pantalla y no se exige RPE para declarar una actividad completa. Un RPE histórico sin `categoryRpe` permanece global, nunca se transforma automáticamente en RPE de tren superior/central/inferior, movilidad o cardio.

`GroupEvolution` y `Planner` conservan la posibilidad de reducir la propuesta por signos de sobreesfuerzo o interrupción, pero se han eliminado los aumentos automáticos de repeticiones, segundos y series basados solo en RPE bajo. La progresión de volumen independiente requiere parámetros de la variante validados, además de consentimiento y observaciones compatibles. Los objetivos de texto libre solo afectan la prioridad dentro de los filtros seguros.

Las fases técnicas se valoran en los hitos naturales 7/14/21 con evidencia; las fases se pueden revertir expresamente. Una modificación de experiencia reinicia solo los bloques activos, no el historial.

## Volumen y referencia externa

`VolumeProfileInterface` lee parámetros numéricos validados de `catalog[].volumeProfile` o de `asterionV1.validatedVolumeProfiles` con ID, unidad, mínimos, máximos, incremento, origen y revisión. Si no existen, la interfaz explica que **no hay referencia validada** y no calcula una dosis ficticia.

El usuario puede registrar manualmente valores reales (unidad consistente, tipo, categoría, fecha, nivel técnico, variante y bloque), y aceptar una referencia orientativa **sin aplicarla automáticamente** a las misiones ni a las series del pasado.

## Monarca

Los desafíos nuevos de cinco días requieren consentimiento expreso y se fijan a IDs en `asterionV1.monarchPlans`; al reabrir se validan equipos y restricciones, sin inventar reemplazos si dejan de ser compatibles. La participación de Monarca se mantiene separada y es idempotente. La interfaz Monarca antigua se muestra únicamente cuando existe un ciclo histórico **ya activo**, para permitir terminarlo o detenerlo sin perder sus datos; no se presentan simultáneamente dos desafíos nuevos.

## Integración con GitHub y verificación

La rama actual de trabajo es `integration/monarca-eclipse-asterion-consolidation`. Añade pruebas de regresión para esfuerzos con categoría explícita y para rechazar el RPE exclusivamente global. La compilación del código nuevo y el flujo de APK pasaron en una revisión intermedia, pero cada commit posterior requiere revisión final.

La suite integral de Android usa Robolectric. GitHub Actions llegó a ejecutar 109 pruebas; varias ejecuciones fallaron por HTTP 429 al descargar dependencias de Robolectric. Tras una ejecución exitosa en el motor de Asterion, la suite integral detectó pruebas históricas que asumían erróneamente que un esfuerzo global servía como evidencia por categoría; se actualizaron para introducir `categoryRpe` de forma explícita.

## Límites comprobables

- No se puede afirmar validación física en teléfono Android sin ejecutar la APK allí.
- No se puede fabricar ni suponer una fuente de referencia numérica para una variante sin metadatos suficientes.
- Las escenas e iconos de ramas V43 todavía exclusivas deben revisarse por sus diferencias, no sobrescribirse con ramas V39/V42 antiguas.
- Los registros antiguos sin categoría explícita permanecen sin categoría, incluso cuando el usuario en el pasado había trabajado un grupo determinado.
