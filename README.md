# Hunter System V43 — Android nativo, catálogo Hunter 2000 en Rive

Rama de integración: `integration/hunter-v43-compose-2000`. PR [#17](https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/17), **en borrador**. La rama `main` no se modifica durante la evaluación.

Monarca 42.9 + Eclipse 41.0, en Kotlin, Jetpack Compose y Canvas Android, sin WebView para las pantallas. `applicationId=com.huntersystem.app`, `minSdk=26`, `compileSdk=36`, `targetSdk=35`; vista previa de V43 (`versionCode=118`).

## Estado real de los recursos de ejercicios

- Catálogo: **1.200 ejercicios originales + 800 adicionales = 2.000 IDs únicos**.
- Demostraciones empaquetadas: **2.000 archivos `.riv`** en `app/src/main/assets/demos-rive/`, acompañados de `manifest.json` con tamaño y SHA-256. Las rutas se generan en `Catalog.kt` y se reproducen mediante `HunterRiveExercise.kt`.
- El paquete Android **no incluye los 2.000 SVG de demostración ni sus JSON de animación antiguos** en `assets/demos30/`. Los SVG decorativos y vídeos editoriales no son el reproductor de ejercicios.
- El SVG fuente permanece archivado fuera de los assets Android en `.hunter2000/source/hunter2000-assets.tar.xz`, como procedencia del material convertido.
- **La conversión a Rive no equivale a anatomía X-Ray 2D ni a validación visual o biomecánica**. Las 800 fichas nuevas continúan con `pendingTechnicalReview=true`, `approvedForAutoplan=false` y `autoProgressionApproved=false`, excluidas de la planificación automática.

## Verificación técnica

```bash
python3 tools/check_hunter2000_complete.py
python3 tools/generate_rive_qa_queue.py
bash gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:assembleRelease
```

El workflow `.github/workflows/android-apk.yml` ejecuta las auditorías, pruebas Kotlin/Compose, compilación release firmada y verificación dentro de la APK de los 2.000 Rive y ausencia de demos SVG/JSON antiguas. Ejecución confirmada para el código Rive: [Actions #40423131633](https://github.com/miguelpinto9988-byte/Hunter-System-Native/actions/runs/40423131633).

El generador de matriz QA produce un CSV de **2.000 filas marcadas PENDIENTE**, no resultados de pruebas. Véase [plan de QA](docs/QA-ANDROID-RIVE-V43.md) y [migración Rive](docs/HUNTER-RIVE-2000-MIGRACION.md).

## Antes de distribuir

Revisar instalación y actualización firmada en Android físico, restauración de datos, carga y reproducción real de animaciones, rendimiento, accesibilidad, notificaciones y técnica de las 800 fichas adicionales. Mantener el PR en borrador y sin habilitar las 800 para autoplaneamiento hasta completar sus revisiones.
