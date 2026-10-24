# Hunter System V42.8 · Fase 09 — Estadísticas y evolución Monarca

## Cambios implementados
La pestaña Estadísticas utiliza el módulo nativo `MonarchProgressV42.kt` sin conexión, sin WebView y sin reconstruir miniaturas. Reemplaza el panel anterior y añade acceso directo desde Inicio. El detalle de rangos conserva las ilustraciones de cazadores, únicamente para el rango actual.

**Datos e integridad:** los períodos de 7, 30 y 90 días se calculan a partir de los logs y de las fechas existentes; cada serie requiere `actual[].done = true`. La confirmación rápida puede completar una sesión pero no crea repeticiones ni series. Se distinguen los días de entrenamiento completados y los días planificados en ciclos guardados; las rutinas extra no inflan el porcentaje de adherencia. Los logros están derivados únicamente de los registros, no conceden XP adicional.

**Diseño:** barras con transición nativa Compose, tarjetas temáticas, resumen de 7 días en Inicio, métricas de sesión, actividad por modalidad, seis hitos y clasificación E–S. Todo se adapta a las paletas Monarca del Perfil.

**Preservación:** catálogo de 1200 ejercicios, 342 vídeos MP4, generadores de misiones/rutinas y datos históricos. No modifica la lógica de perfil, firma, permisos ni las reglas de seguridad.

## Control de calidad
GitHub Actions ejecuta las auditorías históricas 01–08 y la nueva fase 09. La compilación firmada y las auditorías no equivalen a una prueba de instalación en dispositivos reales. Pendiente: Android físico (modo oscuro, 9 temas y acentos, fuentes grandes, orientación horizontal, ventanas pequeñas y tablets), datos vacíos, copia/restauración y rendimiento.

## Datos sensibles
El módulo no envía estadísticas a servidores y no incorpora análisis clínico. Todo el cálculo se hace a partir de los registros locales del usuario.
