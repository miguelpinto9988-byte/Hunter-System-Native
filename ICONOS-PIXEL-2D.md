# Hunter System V43.12 — iconografía Pixel 2D

Integración de iconos retro pixel art, planos y multicolor en la biblioteca nativa del proyecto.

- Directorio de fuentes: `app/src/main/assets/hunter-icons/` (49 SVG originales con transparencia).
- Los 76 identificadores antiguos de `HunterPngIcon` apuntan a estas ilustraciones.
- Las 29 variantes del enum `Sigil` usan la misma biblioteca, por lo que deja de haber dos estilos distintos.
- Las imágenes se vectorizan en el recurso original y se rasterizan una vez a 128x128 con AndroidSVG, guardándolas en una caché LRU y mostrándolas con interpolación desactivada.
- Se actualiza el icono de inicio para que incluya la corona Pixel Monarca.
- No se modifica ninguna rutina, misión, contenido técnico, estado de usuario o formato de guardado.

## Comprobaciones sugeridas al compilar
Verificar iconos en navegación inferior, menú lateral, misiones, rutinas, progreso, controles de reproducción, diálogos y lanzador Android. Probar también un dispositivo con resolución pequeña y uno con DPI alto.

Los íconos son SVG editables; los PNG mostrados previamente eran prototipos visuales. Esta biblioteca es una adaptación vectorial consistente del estilo aprobado, no una conversión píxel por píxel de aquellas imágenes.

## Retirada definitiva de iconos PNG heredados

- La APK conserva los **49 iconos SVG Pixel 2D** y ninguna copia raster PNG antigua en `assets/hunter-png/icons/`.
- Se retiraron los generadores de iconos PNG que se ejecutaban durante la compilacion.
- Se eliminó el instalador de fondos PNG generales: no se necesitan fondos de personajes detrás de la interfaz.
- `tools/check_pixel_icons.py` comprueba la validez de los 49 SVG, las 76 claves de iconos y la ausencia de rutas PNG activas. La verificacion del APK vuelve a comprobar que no se empaqueten iconos PNG.
- Se eliminaron los fondos globales PNG de personajes; se mantienen las ilustraciones interiores de ejercicios, tarjetas y jefes Monarca, que son recursos independientes.

## Iconos por grupo de entrenamiento semanal

Se añadieron seis SVG originales y transparentes: `week_upper.svg`, `week_core.svg`, `week_lower.svg`, `week_mobility.svg`, `week_rest.svg`, `week_cardio.svg`.
La sección **Personaliza tu semana → Elige tu semana** usa directamente `V39Training.of(week[day], focus[day])` para mostrar el ícono correspondiente a cada día; también los muestra en la leyenda inferior. Al pulsar una casilla para cambiar de actividad, se actualizan la categoría, el foco y el ícono de forma reactiva, y al guardar el perfil se conservan las claves `weekPlan` y `weekFocus` preexistentes. No se alteraron los registros de entrenamiento.

## Fondos generales sin sombras

- `HunterFlatBackground.kt` dibuja únicamente el color de fondo del tema; no carga fondos PNG ni imágenes de personajes.
- La aplicación principal, las ventanas emergentes y la cabecera del menú lateral utilizan fondos planos sin imágenes.
- Se retira `HunterRealmWallpaper.kt` y el instalador obsoleto `tools/install_hunter_png_ui.py`.
- `tools/check_plain_backgrounds.py` borra posibles PNG de fondo antiguos en compilaciones locales y exige que la interfaz no vuelva a utilizarlos.
- El empaquetado de la APK comprueba que ya no se incluyen los ocho fondos PNG anteriores.
- **Sin cambios:** portadas y tarjetas de jefes Monarca, compañero, biblioteca de ejercicios, íconos SVG y arte de arranque.
