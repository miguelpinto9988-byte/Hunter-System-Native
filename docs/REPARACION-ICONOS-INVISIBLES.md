# Reparación de íconos invisibles de Hunter System V43.12

## Síntoma y causa de riesgo
Los íconos minimalistas se encontraban como SVG, pero `HunterPngIcon` los convertía en un bitmap en segundo plano. Si AndroidSVG, el acceso al archivo o la rasterización fallaban, `runCatching(...).getOrNull()` silenciaba el problema y el componente no mostraba nada. Tener los archivos SVG dentro de la APK no prueba su renderizado.

## Solución permanente
- Los **49 SVG originales** se mantienen como material vectorial editable con sus colores y transparencia.
- Se incorporan **49 VectorDrawable XML nativos**, equivalentes a esos SVG, en `res/drawable/hunter_icon_*.xml`.
- `HunterPngIcon` mantiene su interfaz pero ahora emplea `painterResource` con identificadores `R.drawable` compilados en Android, sin carga asíncrona ni rasterización manual.
- Se conservan **76 claves** de iconografía; se incluyen los cuatro destinos inferiores y los seis grupos del selector semanal.
- Android escala el vector a cualquier densidad sin fijarlo a 128 o 256 píxeles, sin fondos añadidos.
- **Prueba Robolectric nueva:** dibuja los 49 recursos y comprueba que contienen píxeles visibles, además de verificar los alias de navegación.
- La validación de GitHub comprueba los 49 SVG y sus 49 vectores XML asociados, no únicamente la presencia de fuentes SVG.

No se modifica la pantalla de perfil independiente, las misiones Monarca ni la distribución de la interfaz.
