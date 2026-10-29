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
