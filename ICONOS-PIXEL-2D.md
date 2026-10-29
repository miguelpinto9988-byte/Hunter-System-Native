# Hunter System V43.12 — iconografía Pixel 2D

Integración de iconos retro pixel art, planos y multicolor en la biblioteca nativa del proyecto.

- Directorio de fuentes: `app/src/main/assets/hunter-icons/` (43 SVG originales con transparencia).
- Los 70 identificadores antiguos de `HunterPngIcon` apuntan a estas ilustraciones.
- Las 29 variantes del enum `Sigil` usan la misma biblioteca, por lo que deja de haber dos estilos distintos.
- Las imágenes se vectorizan en el recurso original y se rasterizan una vez a 128x128 con AndroidSVG, guardándolas en una caché LRU y mostrándolas con interpolación desactivada.
- Se actualiza el icono de inicio para que incluya la corona Pixel Monarca.
- No se modifica ninguna rutina, misión, contenido técnico, estado de usuario o formato de guardado.

## Comprobaciones sugeridas al compilar
Verificar iconos en navegación inferior, menú lateral, misiones, rutinas, progreso, controles de reproducción, diálogos y lanzador Android. Probar también un dispositivo con resolución pequeña y uno con DPI alto.

Los íconos son SVG editables; los PNG mostrados previamente eran prototipos visuales. Esta biblioteca es una adaptación vectorial consistente del estilo aprobado, no una conversión píxel por píxel de aquellas imágenes.

## Retirada definitiva de iconos PNG heredados

- La APK conserva los **43 iconos SVG Pixel 2D** y ninguna copia raster PNG antigua en `assets/hunter-png/icons/`.
- Se retiraron los generadores de iconos PNG que se ejecutaban durante la compilacion.
- `tools/install_hunter_png_ui.py` ahora instala solamente los ocho **fondos PNG** de las pantallas y limpia posibles iconos PNG de compilaciones locales previas.
- `tools/check_pixel_icons.py` comprueba la validez de los 43 SVG, las 70 claves de iconos y la ausencia de rutas PNG activas. La verificacion del APK vuelve a comprobar que no se empaqueten iconos PNG.
- Los fondos de pantalla PNG, las ilustraciones de ejercicios y otros contenidos graficos no se eliminan, porque no forman parte de la biblioteca antigua de iconos.
