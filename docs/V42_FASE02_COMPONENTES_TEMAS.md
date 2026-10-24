# Hunter System V42 — Fase 02: componentes y temas Monarca

**Rama:** `design/v42-fase02-componentes-temas`  
**Versión de prueba:** `42.1-monarca-fase2`  
**Dependencia:** fase 01 Monarca A+.

## Alcance implementado

Esta fase fortalece el sistema de diseño nativo de Android Kotlin y Jetpack Compose, conservando la lógica de ejercicios, perfil, misiones, restricciones y rutinas.

- **Nueve atmósferas visuales:** Eclipse, Umbral, Abismo, Runa, Hielo, Lavanda, Aurora, Ceniza y Amanecer.
- **Selector persistente en Perfil**, con muestras visuales en cuadrícula, estado seleccionado accesible y vista previa de la paleta. Los cambios solo se aplican definitivamente al guardar el perfil.
- **Tokens semánticos:** colores de fondo, superficie, borde, texto, texto auxiliar, acento principal/secundario y selección; dimensiones, radios de esquinas y duraciones de transición.
- **Tarjetas renovadas:** sombras sugeridas por superposición de superficies, contornos luminosos, barra decorativa de energía y separación uniforme.
- **Botones y chips:** esquinas adaptadas, transiciones de color y alto mínimo de objetivo táctil de 48 dp.
- **Entradas:** bordes redondeados y alto mínimo de 48 dp, sin cambiar el modo de persistir los datos.
- **Indicadores de progreso:** trazo más alto, gradiente contextual y control de fracciones entre 0 y 1.
- **Ventanas:** contenedor modal con límites redondeados 26 dp y degradado. Conserva funciones, cierre, navegación y controles.
- **Tarjetas de métricas:** componentes independientes reutilizados en Inicio.
- **Cabeceras de sección:** Eyebrow, Heading y línea divisoria reutilizable.

## Arquitectura técnica

`HunterDesignSystem.kt` introduce `HunterThemeSpec`, `HunterDesignSystem.tokens()`, `HunterDesignSystem.Dimensions`, `MonarchThemePicker()`, `MonarchStatTile()` y `MonarchSectionHeading()`.

`Design.kt` aplica las propiedades visuales globales a `HPanel`, `HButton`, `HChip`, `HInput` y `HProgress`, reutilizados por varias pantallas. El nombre histórico `resolveV39Theme` se conserva temporalmente para no romper sus invocaciones, pero admite los nueve IDs nuevos.

Las variaciones de color solo modifican `theme` del perfil y `HunterColors`. El selector no altera el catálogo ni el generador de misiones. Los temas nuevos deben verificarse visualmente en teléfono y tablet antes de fusionar con la rama estable.

## Invariantes y auditoría

1. Se preservan **1200 ejercicios**, **342 demostraciones MP4** y sus recursos SVG.
2. **Sin miniaturas de ejercicios**; se mantienen fichas de texto y demostraciones en su reproductor.
3. El entrenamiento funciona sin conexión y no añade ningún servicio externo.
4. No se modifican `Rules.kt`, `Planner.kt`, ni formatos de guardado y registros.
5. GitHub Actions ejecuta `tools/check_v42_phase1.py` y `tools/check_v42_phase2.py` antes de compilar el APK release firmado.
6. La fase 02 es reversible y no altera `main`.

## Comprobaciones visuales pendientes en dispositivo

Verificar tema Eclipse y cada variación en Perfil, diálogos, chips, tarjetas, scroll, textos largos y contraste en pantallas de 360 dp y tablet en horizontal. Realizar prueba de regresión guardando el perfil y recuperando registros. El resultado de la compilación solo demuestra que el código se construyó, no que estas pruebas manuales se hayan efectuado.
