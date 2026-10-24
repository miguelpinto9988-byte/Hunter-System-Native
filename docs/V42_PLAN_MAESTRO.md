# Hunter System V42 — Índice maestro del rediseño
> Estado: FASE 01 MONARCA A+ IMPLEMENTADA EN RAMA DE PRUEBA. El resto de pantallas continúa migrándose por fases.
> Fecha: 2026-10-24
> Rama de trabajo: `design/v42-plan-y-opciones-20261024`.
> Punto de partida: V39.2 Kotlin Compose **sin miniaturas** (`20f365978c39ed0d0691242d41ac46eb8fd2fde8`).
> Referencias internas de auditoría: ramas V40 Premium y V41 Full Concept; NO son la base de esta rama.
> Inspiración ambiental: progresión RPG y ficción de escenarios/narrativa de acción; gráficos, iconos y personajes nuevos y originales.

## Reglas innegociables
- No perder catálogo de 1200 ejercicios, generadores, entrenamientos, misiones, registros, estadísticas, objetivos, perfil, restricciones físicas o persistencia.
- No introducir miniaturas, fotos previas ni capturas de ejercicios en el catálogo ni en las fichas: **tarjetas de ejercicios textuales**; demostraciones animadas SVG/MP4 únicamente en la vista de ejercicio y reproductor.
- Offline por defecto: no requerir conexión, registro ni servidor para usar entrenamientos. Acompañantes y escenas incluidos localmente.
- La selección visual debe afectar la apariencia, NO aleatoriamente las misiones ni los datos. Cambio de temas persistente y reversible.
- No borrar el sistema visual actual antes de migrar cada pantalla, verificar equivalencia y superar regresión.
- Una rama separada por fase y revisión de APK antes de integrar a `main`. Conservar una versión estable disponible.
- Diseños originales y sin copiar arte protegido de franquicias. Accesibilidad y ergonomía por encima del espectáculo.

## Índice de planificación y modificaciones
| Fase | Módulo | Cambios principales | Entregable verificable | Estado |
|---|---|---|---|---|
| 00 | Auditoría y protección | Inventario Kotlin, rutas, estados, navegación, activos, persistencia; backup de perfiles, comparación V39.2/V40/V41 | Mapa de dependencias, lista de regresión | Planificada |
| 01 | Elección artística | Elegida A+ Monarca; piloto de Inicio, paleta y cinco iconos vectoriales; siguiente: validación visual | Identidad aprobada, código de piloto y APK firmada | **Piloto implementado y compilado** |
| 02 | Sistema de diseño | Nueve atmósferas persistentes, tokens semánticos, componentes premium, contraste y selector visual en Perfil | `HunterDesignSystem.kt`, componentes nativos y auditoría de fase | **Implementada para validación** |
| 03 | Navegación y acceso | Splash, arranque offline, sesión local, navegación inferior, rutas, volver, carga/errores, responsive | Flujo navegable sin pérdida de estado | Planificada |
| 04 | Inicio / Dashboard | Hero 2.5D, rango/XP, misión principal, racha, métricas y CTA, accesibilidad | Home adaptable móvil/tablet | Planificada |
| 05 | Misiones | Semana, tipo de día, progreso, estados, notificaciones, registro, confirmaciones | Misiones estables y consumibles | Planificada |
| 06 | Rutinas y generadores | Filtros categoría, duración, intensidad, equipamiento, lesiones; tarjetas, secuencias | Rutinas consistentes con su categoría | Planificada |
| 07 | Ejercicios y reproducción | Fichas **sin miniaturas**, instrucciones; reproductor SVG/MP4, progreso, velocidad, pausa, espejo, tiempo | Flujo sin saltos de temporizador | Planificada |
| 08 | Perfil y personalización | Avatar, datos, calendario, objetivos, acompañante, vestimenta, temas, accesibilidad | Perfil persistente y recuperable | Planificada |
| 09 | Objetivos, progresión y estadísticas | XP, nivel, gráficos, logros, ciclos, calendario, hitos, estados vacíos | Métricas comprobables sin datos ficticios | Planificada |
| 10 | Escenas 2D / 2.5D / 3D | Animaciones vectoriales, capas parallax; personaje 3D opt-in, LOD, modo ahorro | Degradación elegante en equipos lentos | Planificada |
| 11 | Notificaciones y microinteracciones | Recordatorios Android, sonidos/vibración opcionales, transiciones, diálogos | Permisos correctos y controles accesibles | Planificada |
| 12 | Rendimiento y accesibilidad | Benchmarks, memoria, tamaños táctiles ≥48dp, contraste, texto escalado, TalkBack, tablets 8–12in | Reporte de rendimiento y UX | Planificada |
| 13 | Integración, QA y APK | Gradle release, tests de persistencia y paridad, smoke tests, firma, comprobación de APK | APK instalable firmada validada | Planificada |

## Arquitectura visual propuesta — sin tocar dominio
- `presentation/design/`: `ColorTokens`, `TypographyTokens`, `ShapeTokens`, `MotionTokens`, `ThemeRepository`, `HunterTheme`.
- `presentation/components/`: `HunterScaffold`, `HunterCard`, `HudRing`, `HunterButton`, `CategoryChips`, `HunterDialog`, `HunterIcon`, `CompanionStage`, `StatusBanner`.
- `presentation/screens/`: `Splash`, `Home`, `Missions`, `Routines`, `ExerciseList`, `ExerciseDetail`, `SessionPlayer`, `Goals`, `Progress`, `Profile`, `Settings`, `Companions`.
- `presentation/scene/`: `LayeredParallax` (2.5D), `OriginalSvg` (demostración), `VideoDemo` (offline), `CharacterScene3D` (opcional).
- `domain/` y persistencia actuales: conservar `NativeStore`, `Planner`, `Rules`, `Catalog` y sus JSON; refactor de UI con adaptadores de estado y migración comprobada, sin cambiar contratos funcionales.
- Primera implementación sin nuevas dependencias pesadas; sólo agregar motor 3D cuando se pruebe su coste real en Android y su fallback.

## Mapa de navegación
```
Splash → Inicio / configuración inicial de perfil
  ├── Inicio (rango, XP, acompañante, próxima misión)
  ├── Misiones → día → checklist → detalle → sesión → registro
  ├── Rutinas → filtros → generador → vista de rutina → sesión
  ├── Progreso → calendario / estadísticas / logros / objetivos
  └── Perfil → datos / temas / personajes / restricciones / notificaciones / accesibilidad
            └── Ajustes / privacidad / exportar datos
Biblioteca de ejercicios → búsqueda / filtro → ficha textual → demostración animada
```
La biblioteca puede abrirse desde Rutinas o navegación secundaria; 5 destinos principales máximos.

## Diseño y criterios técnicos
- Tipografía principal UI sans variable legible + tipografía acento para rango/capítulos. No sacrificar lectura por apariencia.
- Espaciado base 4/8/12/16/24/32dp; cards entre 14–24dp; control 48dp o más; respeto a barras Android e insets.
- Iconografía vectorial original, grosor y perspectiva uniformes; estados activo/desactivado, contraste WCAG objetivo AA.
- Transiciones 140–300ms, reducir animaciones opcional; no flashes, parpadeos, recomposiciones globales innecesarias.
- Companions: cambiar SOLO desde Perfil y reflejar en Home, con animación sutil y configuraciones persistentes; variantes coherentes con paleta.
- 2D para controles/iconos; 2.5D para escenas Home; 3D solo para acompañante/escena voluntaria y adaptativa, NUNCA requisito para entrenar.
- Temas: al menos 6 tokens cromáticos interoperables, claro/oscuro; adaptación de ilustración y sombreado; contraste validado.
- Estado vacío/cargando/error/desconectado diseñado en cada pantalla; evitar bloquear uso offline.
- No prometer 90 FPS constante: medir jank, memoria, arranque y calentamiento en dispositivos reales de distintos rangos.

## Puertas de validación entre fases
1. Captura o prototipo revisable de la fase.
2. Equivalencia funcional (mismo ejercicio, misión, perfil y datos antes/después).
3. Revisión pantallas pequeñas, tablet en retrato/paisaje y fuentes grandes.
4. Build Android release y tests correspondientes.
5. Revisión visual del usuario; sólo entonces migrar la siguiente fase.
6. Rollback sencillo; un cambio de tema no debe invalidar datos.
**Dirección elegida:** A+ Monarca. Primera integración en `design/v42-fase01-monarca-a-plus` y detalles en `docs/V42_FASE01_MONARCA.md`. Las restantes fases todavía no han reemplazado sus pantallas originales.

## Criterios de aceptación final
- Todas las pantallas y overlays migrados; misma capacidad de entrenamiento que antes.
- Ninguna miniatura de ejercicio; solamente demostraciones al entrar a ejercicio/sesión.
- Perfil/objetivos/estadísticas conservados tras actualizar la APK.
- Splash/transiciones sin parpadeo, sin recortes, soporte back y rotation.
- Firma release válida, instalación y apertura verificadas en hardware Android; reporte de defectos.
