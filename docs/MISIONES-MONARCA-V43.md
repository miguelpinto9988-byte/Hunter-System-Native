# Misiones Monarca semanales — Hunter System V43

Módulo nativo Kotlin + Jetpack Compose, visible en Inicio bajo la misión diaria. Ya no existen ventanas de 30, 45 o 60 minutos: **cada reto termina el domingo siguiente a las 00:00 de la zona horaria del móvil**.

## Cada domingo y cada cuatro semanas

- **Cada domingo** la oferta semanal se renueva automáticamente. Si la aplicación estaba cerrada, la renovación ocurre al abrirla; no se requiere conexión ni ejecutar servicios externos en segundo plano.
- La rotación para niveles avanzados se repite por dificultad cada **cuatro semanas**: Guardia → Fortaleza → Trono → Fortaleza. El quinto domingo comienza el siguiente ciclo (Guardia otra vez).
- Los ejercicios complementarios varían por semana, e incluso al reiniciar el ciclo de cuatro semanas.
- Los perfiles intermedios reciben Guardia con diferentes ejercicios cada semana; los iniciales no pueden activar las misiones exigentes.
- Las personas adultas con perfil y capacidad compatibles deben aceptar voluntariamente la misión. No hay autoinscripción ni obligación de completar un reto.
- Un intento completado o detenido permanece registrado hasta el siguiente domingo; no se puede repetir varias veces para obtener sellos extras.
- La misión iniciada guarda una instantánea de sus metas, de manera que cambiar el perfil no reescribe el reto en curso.

## Desafíos intensificados (totales repartidos durante toda la semana)

| Misión | Sentadillas | Otros ejercicios | Renovación |
|---|---:|---|---|
| Guardia del Umbral | **60** | Saltos de tijera o escaladores, plancha de 90–150 segundos | Domingo |
| Asalto a la Fortaleza | **80** | 35 burpees, zancadas o step-ups, 120 segundos de plancha | Domingo |
| Juicio del Trono Monarca | **120** | 60 burpees, 70 flexiones, más core o cardio variable | Domingo |

Inspiración narrativa: entrenamientos extraordinarios al estilo de protagonistas de anime (incluyendo ideas de resistencia y disciplina como en One Punch Man), sin replicar sus rutinas como recomendación de salud. La programación utiliza fichas existentes con SVG nativo original y excluye las 800 incorporaciones que siguen pendientes de QA.

**Estos números son metas semanales acumuladas, no dosis para una sola sesión.** Se aconseja distribuirlas con descanso y no tratar de terminarlas necesariamente en una jornada.

## Implementación y seguridad

Los archivos MonarchSpecialMissionState.kt y MonarchSpecialMissionUI.kt definen la rotación de cuatro semanas, calendario dominical, guardado, recuentos, progreso y el panel de Inicio. MonarchSpecialMissionTest.kt incluye pruebas del comienzo de semana, la fecha límite exacta, la rotación, el archivo histórico y la restricción de no iniciar sin perfil.

El cierre semanal usa la zona horaria del dispositivo. Se conserva el progreso al reiniciar y se archiva al llegar la nueva semana. El registro es **manual/autodeclarado**, no equivale a una medición de actividad física ni atribuye series o XP. Hay bloqueo por edad, nivel, lesiones, dolor y compatibilidad de ejercicios; si cambia el perfil, se reevaluará al registrar resultados. No existen penalizaciones por expiración o abandono.

La intensidad alta requiere revisión profesional individual si hay dudas de capacidad, técnica o recuperación. Que las reglas de software permitan una misión no significa que sea apropiada para cualquier usuario. Se requiere QA físico en dispositivos y evaluación técnica antes de una distribución definitiva.

## Compilación

Java 17, Android SDK 35:

    bash gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:assembleRelease

El PR #17 contiene estos cambios en la rama integration/hunter-v43-compose-2000 sin modificar main.
