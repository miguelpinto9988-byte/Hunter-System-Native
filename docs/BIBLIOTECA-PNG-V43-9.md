# Hunter System V43.9 — Regla de oro: fondos e iconos PNG

## Nueva regla vinculante
No utilizar diseños SVG, polígonos Canvas ni trazados geométricos como **fondos decorativos** ni para los iconos de navegación, rutinas o metas. Los ocho fondos se crean como fotografías/ilustraciones raster en PNG a partir de las **imágenes WebP cinematográficas que ya contiene el propio proyecto** (no se dibujan figuras vectoriales). No se modifica la imagen promocional original de los jefes Igris/Beru/Baran.

## Biblioteca
`tools/build_hunter_png_library.py` produce antes del ensamblado:
- **8 fondos PNG** en `assets/hunter-png/wallpapers/`: Panel, Misiones, Rutinas, Ejercicios, Progreso, Objetivos, Perfil, Recuperación.
- **40 iconos PNG ilustrados**, en `assets/hunter-png/icons/`, adaptados a la sección y a palabras reales de la rutina/meta. Basados en el arte rasterizado de OpenEmoji, con atribución a **OpenEmoji by Profullstack, Inc.** bajo [CC BY 4.0](https://github.com/profullstack/openemoji), con una tarjeta neón raster propia. Una imagen se selecciona por clave, no se dibuja un símbolo SVG en Compose.
- `HunterPngLibrary.kt` contiene selección `hunterRoutinePngKey` y `hunterGoalPngKey` con rutas locales.
- `HunterSideMenu.kt` implementa un menú lateral auténtico con fotos PNG, enlaces a pantallas existentes y accesos a Academia/Recordatorios/Perfil.
- `HunterMissionCompletedCard.kt` aparece únicamente si existe `complete=true` en el registro real de la misión, no presenta misiones ficticiamente completadas.
- Estadísticas: iconos ilustrados y visuales sobre datos verdaderos de `MonarchAnalytics`, sin números de demostración.

Los fondos se superponen al contenido **sin tocar los botones ni los datos**. Las animaciones individuales de los 2000 ejercicios siguen usando SVG porque son formato funcional de ejercicio, no el arte de fondo de interfaz.

## Puesta en producción
- La generación de PNG es un paso del pipeline de GitHub Actions y exige Pillow 11.3.0 y conexión para descargar los PNG ilustrados de OpenEmoji.
- El APK final funciona sin conexión. `AssetImage` decodifica y recorta `PNG` como bitmaps.
- Se conservan datos personales, catálogo y los estados QA pendientes.
- QA visual y de rendimiento en Android físico pendiente. PR #17 en borrador, `main` intacta.
