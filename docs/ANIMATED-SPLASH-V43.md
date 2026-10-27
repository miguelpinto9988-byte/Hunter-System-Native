# Hunter System — Pantalla de carga animada

Se reemplazó completamente el contenido visual de `MonarchBootScreen` de Monarca V42 por `HunterAnimatedBootArtwork`, sin cambiar el inicio de datos nativo.

- Fondo aprobado en `assets/monarch_splash_background.webp`, WebP optimizado 843×1864.
- El arte ya contiene logo, títulos y una barra decorativa; no se renderizan duplicados.
- Compose aplica zoom muy suave, pulsos azules y partículas. Respetar reducción de movimiento.
- Cuando `HunterRepository` termine de abrir los datos se abandona automáticamente la pantalla de carga; ningún retraso artificial.
- Si ocurre una excepción, presentar `Reintentar` en un panel visible.
- La barra del fondo no representa un porcentaje real de inicialización; no se comunica progreso numérico ficticio.
- La animación es de la pantalla Compose, no modifica el splash del sistema Android 12+.
- Pruebas manuales pendientes: distintos tamaños Android, reducir animaciones, inicio sin conexión y recuperación con datos corruptos.
