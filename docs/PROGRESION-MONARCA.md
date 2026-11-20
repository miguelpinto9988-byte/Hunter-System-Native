# Progresión local de Monarca Eclipse

El calendario usa cinco categorías de actividad, en orden estable, y un índice de descanso. Las denominaciones anteriores se normalizan al leerlas. El perfil inicial expresa preferencias y experiencia técnica. El objetivo escrito aporta afinidad por etiquetas reconocidas localmente. La selección aplica primero las restricciones del perfil, el equipo declarado y los límites del catálogo.

## Componentes

| Componente | Responsabilidad |
| --- | --- |
| `MonarchPlanningRules.kt` | Contrato de catálogo, clasificación, filtros, perfil, firma, selección reproducible, esfuerzo y revisiones técnicas. |
| `TrainingSessionCoordinator.kt` | Coordina la propuesta del día, la adaptación de la categoría y el hito técnico. Consulta por separado las referencias de volumen. |
| `MonarchVolumeRules.kt` | Valida observaciones comparables, separa unidades, revisa límites y calcula referencias que el usuario puede aceptar. |
| `MonarchPlanningBridge.kt` | Traduce el perfil y los metadatos existentes, lee los registros históricos y guarda resultados y decisiones explícitas. |
| `Planner.kt` y `NativeStore.kt` | Conectan las políticas con el calendario nativo, las sesiones y el almacenamiento local. |
| Pantallas de Misiones, Rutinas, Progreso y Monarca | Permiten consultar propuestas y explicaciones, declarar resultados y guardar referencias opcionales. |

El contrato `ActivityRepositoryInterface` consulta por categoría, familia, tipo, ID y etiquetas. La fuente existente se lee mediante un adaptador. Una fuente futura debe suministrar IDs estables, relaciones válidas, técnica, intensidad, equipo, modalidad, supervisión, restricciones y elegibilidad. Los metadatos ausentes pueden dejar una variante fuera de la selección automática. Un conjunto compatible vacío produce una explicación.

## Volumen y fases

Una actividad declarada completada puede tener una observación de volumen y esfuerzo opcional. Se comparan hasta 21 días anteriores del mismo bloque, categoría, variante, tipo, nivel y unidad. Dos fechas comparables con esfuerzo bajo permiten calcular un incremento limitado. Tres fechas bajas con series consistentes y al menos siete días de separación permiten revisar una serie adicional, hasta tres, manteniendo la cantidad por serie. El esfuerzo alto recentra la revisión en recuperación. Las interrupciones suspenden los aumentos.

El contrato `VolumeProfileInterface` entrega una referencia individual validada. El adaptador acepta `volumeProfile` de la variante o un registro local de `validatedVolumeProfiles` con identificador y revisión del proveedor. La referencia requiere `validated`, `variantId`, `unit`, `minimum`, `maximum`, `permittedIncrement` y `automaticReviewEligible`. Los campos `initialSets` e `initialValue` son opcionales. Sin una fuente validada se conserva la observación real y se muestra el estado de revisión. Los valores orientativos guardados son independientes de la sesión y se pueden editar dentro de sus límites.

Los hitos técnicos de 7, 14 y 21 días utilizan solo evidencia de la categoría. Si un hito incorpora una variante diferente, se abre un bloque numérico independiente. Cambiar de experiencia también inicia nuevas referencias. El historial antiguo sigue disponible por fecha y unidad.

## Rutinas y Monarca

Rutinas ofrece hasta seis combinaciones distintas para una categoría y un nivel, con ventanas de 10, 15, 20, 30, 45 o 60 minutos. Se estima la duración con descansos y se respeta el máximo de pasos. El usuario puede completar la guía y declarar su participación en una fecha de actividad. La primera declaración válida del día concede 10 puntos en un historial propio. Las rutinas generadas conservan la misión fijada del día.

Monarca requiere aceptación explícita y prepara cinco fechas consecutivas con el calendario vigente. Usa como máximo dos actividades y nivel técnico 2, dentro del equipo y la intensidad compatibles. Las señales de recuperación pueden reducir la propuesta. El enfoque opcional del encuentro ordena alternativas elegibles y el historial favorece la variedad entre ciclos. Las declaraciones de Monarca se almacenan aparte.

## Persistencia y verificación

El archivo local de Android conserva perfil, propuestas, resultados, valoraciones, fases, volumen, rutinas y Monarca como conjuntos distintos. La lectura de claves anteriores conserva los registros y el alcance global de las valoraciones heredadas. Las claves de fecha, variante y bloque evitan duplicar observaciones. Los cálculos pueden ejecutarse offline y no utilizan medidas corporales, servicios remotos ni registros simulados para inferir capacidad física.

Ejecuta `bash gradlew :app:testDebugUnitTest` para las políticas y la persistencia, y `bash gradlew :app:assembleDebug` para compilar la APK.
