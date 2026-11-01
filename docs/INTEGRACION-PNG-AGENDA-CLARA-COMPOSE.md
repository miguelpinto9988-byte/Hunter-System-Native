# Iconografía PNG nativa en Hunter System V43.12

## Criterio técnico

Agenda Clara carga recursos Android mediante `R.drawable` y `ImageView.setImageResource`.
Hunter System, que está escrito en Jetpack Compose, conserva **exactamente el mismo tipo de recurso Android** (PNG en `drawable-nodpi`) pero lo muestra directamente con `Image(bitmap = ImageBitmap.imageResource(id = ...))` dentro de Compose.

Según la documentación de Android, `AndroidView` debe reservarse para componentes que no tengan equivalente Compose. La biblioteca iconográfica no requiere ninguna vista legacy.

## Garantías

- 49 PNG funcionales, 6 variantes de navegación y 2 recursos para inicio y notificaciones.
- 76 identificadores funcionales resueltos mediante `hunterIconDrawableId`.
- Sin transcodificar los PNG a XML, SVG ni VectorDrawable.
- Sin `Canvas` personalizado ni contenedor `AndroidView` para los íconos.
- `FilterQuality.None` para un filtrado de vecino más próximo, útil para Pixel art.
- `ContentScale.Fit`, sin recorte; `contentDescription` se mantiene para accesibilidad.
- Recursos empaquetados en la APK; sin conexiones de red.
- Archivos originales `drawable-nodpi` sin alteraciones, para evitar el preescalado por densidad.
- El launcher y el icono de notificación conservan sus referencias originales `R.drawable`.

## Fuentes oficiales

- https://developer.android.com/develop/ui/compose/graphics/images/loading
- https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose
- https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/FilterQuality
- https://developer.android.com/training/multiscreen/screendensities

## Verificación

`python3 tools/check_pixel_icons.py`, `python3 tools/check_v43_fusion.py` y pruebas Kotlin/Robolectric en GitHub Actions.
El aspecto final de íconos complejos a 24 dp debe validarse en un dispositivo físico.
