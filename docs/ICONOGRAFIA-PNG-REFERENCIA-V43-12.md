# Iconografía PNG de referencia — Hunter System V43.12

Esta actualización sustituye la biblioteca genérica previa por PNG extraídos de la lámina visual proporcionada para el proyecto.

## Recursos incorporados

- 49 iconos funcionales, `hunter_pixel_*.png`, 256 × 256 px, RGBA.
- 6 variantes de navegación seleccionada, `hunter_nav_*.png`, 256 × 256 px, RGBA.
- Iconos de aplicación y notificaciones, `app_icon_pixel.png` y `notification_icon_pixel.png`.
- 76 identificadores semánticos existentes conservados sin romper enlaces de pantalla.
- Todo se carga sin internet desde `app/src/main/res/drawable-nodpi/`, con `ImageBitmap.imageResource` y `FilterQuality.None`.

## Procedencia y fidelidad

La lámina de referencia es una composición PNG RGB con fondo y textos, no un juego de iconos PNG sueltos. Por ello se recortaron y se separaron las siluetas visibles, recuperando alfa transparente y ampliando con interpolación nearest-neighbor. La resolución de archivo es 256 × 256, pero no se inventa detalle que no estaba en la imagen de origen.

De las 49 funciones, 39 reutilizan o transforman motivos de la lámina. Las 10 acciones que no estaban dibujadas como símbolos individuales (añadir, chat, reloj, cerrar, editar, bloquear, pausar, quitar, buscar, eliminar) se crearon como extensiones Pixel art RGBA siguiendo la misma paleta. Algunas claves reutilizan el mismo motivo original por significado.

Los PNG son recursos nativos de Android. No se dibujan mediante `Canvas`, `VectorDrawable` ni SVG en la capa de iconografía. Gráficos, fondos y otras partes funcionales pueden tener renderizadores independientes.

## Integración

- `HunterPngLibrary.kt`: iconos PNG funcionales y variantes seleccionadas.
- `MonarchNavigationV42.kt`: usa las variantes PNG cuando una pestaña está seleccionada.
- `MonarchV42.kt`: los accesos rápidos y métricas ya usan PNG mediante `HunterPngIcon`.
- `tools/check_pixel_icons.py`: verifica firma, 256 px, RGBA y transparencia real mediante decodificación PNG.
- `HunterMinimalIconResolutionTest.kt`: verifica recursos raster Android sin Canvas.

**Importante**: la lámina no incluía versiones individuales de todos los controles; los 10 diseños extendidos no son recortes exactos de la imagen.
