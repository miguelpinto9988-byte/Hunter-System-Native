# Hunter System V43.8 — Reinos de fantasía en todas las ventanas

## Referencia visual
Se respetan las capturas reales aportadas por el usuario: tipografía Monarca, botones azul/cian, cartas negras, panel de nivel, experiencia, misión diaria, métricas, accesos rápidos, perfil y sus posiciones actuales. Se incorporan escenas evocadoras de fantasía de cazadores y mazmorras de *Solo Leveling*; son **ilustraciones vectoriales originales**, no imágenes oficiales de la franquicia. Las imágenes promocionales de los jefes de V43.7 se conservan sin cambios.

## Arte por ruta
- `home`: umbral y portal de cazador azul.
- `plan`: fortaleza y trono carmesí de misiones.
- `routines`: forja y espadas encantadas.
- `book`: archivo holográfico de técnicas.
- `stats`: observatorio de experiencia y órbitas.
- `objectives`: cumbres y constelaciones doradas.
- `profile`: santuario y corona de sombras.
- `recovery`: escudo de resguardo rojo.
- Los diálogos reutilizan el arte relacionado con su contenido (misión, ejercicio, rutina, progreso, objetivo, perfil o recuperación).

## Integración
Se usa `HunterRealmWallpaper.kt` como capa Compose debajo de todo el contenido nativo, tanto en `HunterRoot` como en `OverlayRoot`. Los archivos SVG `realm-art/*.svg` se dibujan con `OriginalSvg` (AndroidSVG offline). El nuevo parámetro `crop=true` ajusta estas escenas a pantallas de distintas relaciones de aspecto sin alterar el modo Fit por defecto de las animaciones de ejercicios. Degradado oscuro para legibilidad de tarjetas y texto. Sin permisos, sin red, sin servicios, sin nuevo estado persistente.

## Conservación
- No se alteran funciones, datos de usuario, historial, plan, 2000 SVG de ejercicios ni jefes promocionales originales de V43.7.
- Los 800 ejercicios adicionales permanecen excluidos de la planificación automática y pendientes de QA.
- PR #17 en borrador, `main` sin cambios.
- La compilación y comprobación estática no sustituyen pruebas visuales en Android físico; validar contraste, legibilidad y rendimiento en teléfono/tableta antes de fusionar.
