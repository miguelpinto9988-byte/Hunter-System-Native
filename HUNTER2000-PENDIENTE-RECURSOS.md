# Hunter System 2.000 SVG — etapa de integración
**Estado: rama preparatoria, SIN APK compilada.**
Se introducen dos ajustes en Kotlin:
1. Catálogo: carga opcional de 800 fichas desde `app/src/main/assets/catalog-hunter2000-extra.json`, conservando 1.200 ID existentes e identificadores de historial.
2. Motor de reglas: las fichas `sourceExpansion2k=true` con `approvedForAutoplan=false` **no entran en las misiones ni rutinas generadas**, manteniendo las restricciones existentes.

Los recursos binarios **no forman parte de esta rama**. Se preparó fuera del repositorio un ZIP descargable en ChatGPT con los 2.000 SVG y 800 fichas: `Hunter_System_V41_Paquete_Integracion_2000_SVG.zip`.
Para aplicar: descargar ZIP, copiar `app/src/main/assets/` sobre el repositorio y asegurarse de que estén los 2.000 `demos30/*.svg` más `catalog-hunter2000-extra.json`. **No activar** las 800 propuestas sin revisar técnica, material y animaciones.
Requiere Gradle Android SDK 35 + firma original y pruebas reales de dispositivo. No afirmar que hay APK nueva hasta comprobar build e instalación.