# Monarca Eclipse — planificador único histórico

Estado: integración unificada, sin un motor paralelo de misiones ni un segundo generador de rutinas. El planificador real es `Planner.kt`, que conserva la ejecución, las series y el almacenamiento histórico.

## Flujo único de una sesión

`NativeStore.refreshWeek() → NativeStore.makeDay() → Planner.choose() → DailyMissionPlanner.plan() (política de selección) → Planner.libraryDose()/Rules.groupDose()/Planner.cap() → NativeStore.day()/log() → MonarchMissionContent()/SessionPlayer()/SessionRecord()`.

`DailyMissionPlanner` es una **política interna utilizada desde Planner**, no una pantalla, almacenamiento o motor ejecutable paralelo. Una misma lista de IDs se usa al abrir Inicio, Misiones, el reproductor y el registro de series. `NativeStore` es responsable de fijar y conservar los días; los días con registros activos, anteriores o personalizados nunca se reescriben por una actualización de reglas.

## Diferencias históricas incorporadas

| Parte histórica Monarca | Regla integrada | Resultado |
| --- | --- | --- |
| Calendario `weekPlan` domingo=0, `weekFocus` | Cinco categorías + descanso, lunes=0, alias de categorías | Traducción por adaptador, sin reordenar perfiles existentes |
| `Rules.pool`, `compatible` y `gate` | Elegibilidad técnica, intensidad y supervisión | Rechazo conservador de actividad no apta; la intención escrita no elude filtros |
| Catálogo Hunter 1200 + expansión 800 | Familia, tipo, variante e ID estable | No se crean fichas; la expansión no aprobada no se planifica automáticamente |
| `Planner.choose`: puntuación por carga semanal, patrón, familia, recuperación, objetivo y equipo | Selección determinista y límites por categoría | La puntuación histórica decide prioridades dentro del filtro seguro |
| Duración, número diario, descanso, rondas y dosificación del catálogo | Ajuste por recuperación y revisión técnica por hitos 7/14/21 | Se conserva el reproductor nativo; jamás se aumentan cantidades por RPE bajo |
| Revisión semanal `mantener/reducir/pausar` | Fijación con validación de perfil y catálogo | Reanudar no mantiene el marcador vacío; días confirmados permanecen intactos |
| `Planner.generateRoutine`, biblioteca, gimnasio y editor de rutinas | Mismo filtro de equipo, nivel e intensidad que misiones | Un único generador histórico, sin panel alternativo |
| Series confirmadas, marca manual y tareas | `PENDING/COMPLETED/SKIPPED`, notas e interrupción separadas | Omisión no se convierte en cantidad realizada; solo se evalúan completadas/omitidas |
| RPE global anterior | RPE con `categoryRpe` solo tras guardado explícito | Nunca se atribuye un RPE antiguo a una categoría por suposición |
| Gráficos y XP históricos | Resumen por categoría y participación real | Los días pendientes y de descanso no se penalizan en la tasa por actividad |
| Volumen y unidades históricas | Registro real por variante, bloque, fecha, unidad y nivel | Solo tras completar la actividad; referencias validadas y voluntarias, sin aplicación automática |
| Misión especial antigua de 7 días | Desafío voluntario de cinco días fijado por consentimiento | Un solo desafío activo: si hay uno histórico en curso puede finalizarse sin desaparecer; no se ofrecen dos nuevos |
| Tema, personajes, iconos, assets SVG/MP4 | Ninguna intervención en recursos o demostraciones | Sin sustitución de escenas, personajes o catálogo |

## Persistencia y compatibilidad

El archivo local atómico original conserva `profile`, `cycles`, `logs` y los campos desconocidos de copias de seguridad. La planificación nueva escribe datos auxiliares en `monarcaPlanningV2`. La clave anterior `asterionV1` **solo se lee como compatibilidad de backups existentes**: no se elimina y sus campos se copian al nuevo contenedor en la primera escritura. Esta clave de legado no representa un motor, una pantalla ni un planificador adicional.

Para fecha e ID coincidentes, el resultado explícito de `logs` es la fuente de verdad por encima de las antiguas observaciones auxiliares. Los RPE sin categoría siguen globales. El plan y el registro nativos son la única ruta activa para marcar actividades, hacer sesiones y guardar sus cantidades.

Las sugerencias de volumen requieren una referencia numérica de la misma variante con identidad, unidad y procedencia verificables. Sin referencia aprobada no se crean números predeterminados ni aumentos físicos automáticos. La revisión de complejidad se limita a los hitos conocidos y tiene reversión explícita.

## Pruebas y límites

El conjunto incluye pruebas de políticas sintéticas, persistencia Robolectric, compatibilidad histórica, reglas originales de `Rules`, generación de rutinas, estadísticas y restauración. Los antiguos goldens de selección exacta dejan de ser correctos al modificar los criterios de elegibilidad: se mantiene la prueba exacta de las fórmulas y reglas históricas que siguen siendo invariantes, y para la nueva selección se comprueban seguridad, determinismo, IDs reales y consistencia de unidades.

Flujos: `.github/workflows/monarca-planner-tests.yml` y `.github/workflows/monarca-consolidation-tests.yml`. Para QA y APK debug sin depender de la descarga de arte promocional externo: `.github/workflows/monarca-debug-qa.yml`.

Persisten límites externos: verificación visual y de almacenamiento en Android físico, aprobación de fichas marcadas pendientes, referencias volumétricas no suministradas, y disponibilidad de arte promocional necesario para la APK release firmada.
