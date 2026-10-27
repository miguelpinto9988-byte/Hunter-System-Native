# Hunter System V43 — estado técnico de integración (Rive)

## Comprobado en el repositorio

- La rama `integration/hunter-v43-compose-2000` integra Monarca 42.9 y Eclipse 41.0 en Kotlin/Jetpack Compose, sin WebView para las pantallas.
- Catálogo de **2.000 ejercicios**: 1.200 originales y 800 adicionales.
- Demostraciones de ejercicios migradas a **2.000 archivos `.riv`** en `app/src/main/assets/demos-rive/`, con manifiesto de hashes y tamaños.
- `Catalog.kt` construye rutas Rive; `HunterRiveExercise.kt` usa el runtime Rive Android. Los SVG/JSON de demostración heredados ya no se incluyen en los assets Android.
- Los archivos SVG de origen están archivados fuera de los assets, no se ejecutan como fallback. Otros recursos editoriales no forman parte de las demostraciones de ejercicios.
- Las 800 fichas nuevas continúan identificadas como pendientes y excluidas del plan automático.

## Evidencia de CI

- [Actions #40420713057](https://github.com/miguelpinto9988-byte/Hunter-System-Native/actions/runs/40420713057): generación y verificación global de los 2.000 binarios Rive.
- [Actions #40423131633](https://github.com/miguelpinto9988-byte/Hunter-System-Native/actions/runs/40423131633): pruebas Kotlin/Compose, compilación release firmada, cotejo del manifiesto y revisión de los 2.000 Rive dentro de la APK.
- Los archivos de hash comprueban integridad binaria; **no prueban** fidelidad visual, fluidez en teléfono, certificación anatómica ni ejecución física de cada animación.

## Siguiente fase: QA en Android físico

El script `tools/generate_rive_qa_queue.py` produce una matriz determinista de 2.000 entradas, **todas pendientes**, para documentar pruebas visuales, movimiento, reproducción en Android y evaluación biomecánica. Seguir [docs/QA-ANDROID-RIVE-V43.md](docs/QA-ANDROID-RIVE-V43.md). No completar casillas sin observación real.

**Restricciones vigentes:** mantener [PR #17](https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/17) como borrador, no fusionar a `main`, no activar autoplaneamiento de las 800 fichas no revisadas y no presentar la APK preview como producto validado en dispositivos.
