# Hunter System V42.6 — Fase 07: fichas y reproductor Monarca

Implementación en rama `design/v42-fase07-reproductor-monarca`, basada en fase 06.

**Alcance:** escenario nativo 2D de ejercicios, bordes y superficies Monarca, estado visible de reproducción, controles de pausa/reinicio/espejo y velocidades 0,5×/1×/1,5×/2× con objetivos de toque grandes. Integración en ficha individual y sesión guiada. Barras de progreso en vista de ejercicio. Conservados `OriginalSvg`, clips locales MP4, instrucciones, marcación de series, contadores y firma `nativeSignature`.

**Garantías:** no usa WebView ni red, no añade miniaturas, no reinterpreta imágenes ni declara biomecánica validada. Velocidad visual independiente del temporizador y de repeticiones. Acceso a todas las 1.200 fichas y 342 MP4 existentes. `main` no cambia.

**Validación automática:** CI comprueba invariantes de archivos, rutas y build release. Revisión aún necesaria en teléfonos/tablets físicos para temporización al suspender, controles accesibles, contraste en todos los temas, exactitud mecánica de dibujos y estabilidad al girar pantalla.

**Pendiente:** test instrumental Android del ciclo de vida y sincronización exacta para temporizador con reloj monotónico. La fase 07 mejora las pantallas, pero no certifica esos comportamientos sin pruebas.
