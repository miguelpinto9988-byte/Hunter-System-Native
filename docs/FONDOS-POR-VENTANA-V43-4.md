# Hunter System V43.4 — Ambientaciones por ventana

## Cambio realizado
Se retira el recuadro `ECLIPSE × MONARCA / Tu próxima evolución` y su recurso `eclipse/portal.webp` de la APK. Se conserva la mecánica Monarca V42, Eclipse, todas las misiones, rutinas, progreso, perfiles y ejercicios.

La ambientación se integra como una capa de fondo nativa en `MainActivity.HunterRoot` y las ventanas `OverlayRoot`: `Modifier.hunterSceneBackdrop(route)` definido en `HunterSceneBackdropsV43.kt`. El efecto se dibuja detrás de controles e interacciones, sin capturar gestos ni acceder a internet.

## Un diseño para cada ruta
| Pantalla | Escena |
|---|---|
| Panel | Portal azul de eclipse entre ruinas |
| Misiones | Ciudadela violeta y runas |
| Rutinas | Armería magenta con espadas simbólicas |
| Ejercicios | Archivo holográfico cian |
| Progreso | Observatorio verde y órbitas de datos |
| Objetivos | Constelación dorada |
| Perfil | Santuario índigo |
| Ventanas emergentes | Fondo de la familia correspondiente; oscuro si no hay correspondencia |
| Recuperación | Sombra roja de bajo contraste |

## Técnica conservada para integrar ilustraciones futuras
1. Guardar ilustraciones verticales optimizadas `.webp` en `app/src/main/assets/`; no copiar píxeles dentro de la lógica.
2. Leer las imágenes una sola vez con `remember(context)`, `BitmapFactory.decodeStream` y `Image(...ContentScale.Crop)`, como hace `HunterAnimatedBootArtwork.kt`.
3. Superponer Canvas o `graphicsLayer` para halos, estrellas y movimiento sutil. Usar `LocalMonarchMotion.current.reduced` para respetar la reducción de movimiento.
4. Conservar un degradado oscuro que garantice contraste y mantenga todos los botones accesibles.
5. Comprobar imágenes, rendimiento, tamaño de APK y diseño real en dispositivos antes de publicar.

En esta versión, los fondos de las ventanas están dibujados directamente con Canvas; pueden reemplazarse por imágenes WebP sin cambiar las pantallas.

## QA
- Se verifican pruebas Kotlin/Compose, 2.000 SVG originales, ausencia Rive y splash actual.
- Los 800 ejercicios adicionales permanecen pendientes de revisión técnica y excluidos del autoplaneamiento.
- `main` permanece intacta; [PR #17](https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/17) seguirá en borrador hasta QA Android físico.
