# Hunter System V43.12 — Monarca con mejoras Eclipse, catálogo de 2000 SVG

Rama principal: `main`. El código integra Monarca V43.11 y las mejoras recientes de Eclipse. Las dos historias se conservan en el commit de fusión.

Monarca 42.9 + Eclipse 41.0, realizados en Kotlin y Jetpack Compose. La compilación de pruebas usa `versionCode=133` y `versionName=43.12-monarca-eclipse-2d-2000` (`applicationId=com.huntersystem.app`).

## Un solo planificador: Monarca Eclipse

El planificador histórico `Planner.kt` es la única ruta de selección de misiones y rutinas. Sus reglas de calendario, equipamiento, dosificación, reproductor y registros se han unificado con los filtros técnicos, el RPE voluntario por categoría, las revisiones de 7/14/21 días y la participación real.

`NativeStore` guarda la misma misión que se muestra y se reproduce: no hay una lista adicional de ejercicios ni un segundo motor. Se conservan los 1200 ejercicios originales, la expansión de 800 fichas pendientes y los recursos gráficos. El historial y las copias con campos antiguos siguen siendo legibles sin forzar una migración destructiva.

[Arquitectura única, cambios frente al planificador histórico y compatibilidad](docs/PLANIFICADOR-UNICO-MONARCA-ECLIPSE.md).

Las pruebas Android/JVM y la APK debug de QA están en [planificación y tests](.github/workflows/monarca-planner-tests.yml) y [compilación debug](.github/workflows/monarca-debug-qa.yml). La validación visual en Android físico y la obtención del arte promocional de terceros para la APK firmada siguen pendientes.

## Mejoras Eclipse integradas
- Perfil simplificado, cinco temas, acompañante y escenarios por tarjetas horizontales.
- Cuatro pestañas Monarca y acceso al perfil desde el avatar.
- Equipo contextual, ajustes semanales agrupados y una sola encuesta RPE.
- Adaptación de grupos a 7, 14 y 21 días.
- Tres modos de rendimiento y mensajes personalizados de los cinco acompañantes.
- Conservación del historial de sesiones y recursos propios de Monarca.

## Recursos de ejercicio
- **2000 fichas**: 1200 originales y 800 adicionales, con identificadores únicos.
- **2000 SVG animados originales** restaurados desde el repositorio anterior a la migración, alojados en `app/src/main/assets/demos30/`. Se conservan también sus ficheros JSON complementarios originales.
- Cada ficha obtiene la ruta SVG desde `demoPath30` en `Catalog.kt`. `MonarchExerciseStage` reproduce el SVG en Canvas Android con `OriginalSvg` y proporciona pausa, reproducción, espejo, velocidad y reinicio, sin usar WebView.
- El motor Rive y los 2000 `.riv` se han retirado del código y recursos empaquetados. El workflow verifica **0 archivos .riv** y **0 bibliotecas nativas de Rive** dentro de la APK.
- Las **800 fichas nuevas** continúan `pendingTechnicalReview=true`, `approvedForAutoplan=false` y `autoProgressionApproved=false`; quedan excluidas de planificación automática.

## Auditoría y compilación
```sh
python3 tools/check_hunter2000_complete.py
python3 tools/generate_svg_qa_queue.py
bash gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:assembleRelease
```
`.github/workflows/android-apk.yml` realiza las auditorías, pruebas Kotlin/Compose y compilación firmada. Verifica los 2000 SHA-256 de los SVG que realmente se empaquetan en la APK. Esta verificación **no acredita** fidelidad visual en Android, rendimiento ni validación biomecánica.

La validación visual en teléfono físico y la evaluación técnica de las 800 fichas siguen pendientes. Consultar [QA SVG](docs/QA-ANDROID-SVG-V43.md). Las 800 fichas continúan excluidas de la planificación automática.

## Restauración histórica 2D

Restauración del código del 16 de noviembre de 2026 (`c0802ce`), sin visor 3D. Se incrementó versionCode a 133 para evitar el rechazo por versión inferior al instalar sobre una APK con la misma firma. La APK debug es para pruebas; no acredita QA técnica ni compatibilidad con datos de versiones posteriores. Las pruebas unitarias siguen ejecutándose; la APK debug puede estar disponible antes de que terminen y no se considera validada si fallan.
