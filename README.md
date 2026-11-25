# Hunter System V43.12.2 — Monarca Eclipse, personaje vectorial 2D y archivo de 2.000 fichas

Rama principal: `main`. El código integra Monarca V43.11 y las mejoras recientes de Eclipse. Las dos historias se conservan en el commit de fusión.

Monarca 42.9 + Eclipse 41.0, realizados en Kotlin y Jetpack Compose. La compilación de pruebas usa `versionCode=134` y `versionName=43.12.2-monarca-eclipse` (`applicationId=com.huntersystem.app`).

## Un solo planificador: Monarca Eclipse

El planificador histórico `Planner.kt` es la única ruta de selección de misiones y rutinas. Sus reglas de calendario, equipamiento, dosificación, reproductor y registros se han unificado con los filtros técnicos, el RPE voluntario por categoría, las revisiones de 7/14/21 días y la participación real.

`NativeStore` guarda la misma misión que se muestra y se reproduce: no hay una lista adicional de ejercicios ni un segundo motor. Se conservan los 1200 ejercicios originales, la expansión de 800 fichas pendientes y los recursos gráficos. El historial y las copias con campos antiguos siguen siendo legibles sin forzar una migración destructiva.

[Arquitectura única, cambios frente al planificador histórico y compatibilidad](docs/PLANIFICADOR-UNICO-MONARCA-ECLIPSE.md).

Las pruebas Android/JVM y la APK debug de QA están en [planificación y tests](.github/workflows/monarca-planner-tests.yml) y [compilación debug](.github/workflows/monarca-debug-qa.yml). La validación visual en Android físico sigue pendiente; la compilación no requiere descargas de arte promocional.

## Mejoras Eclipse integradas
- Perfil simplificado, cinco temas, acompañante y escenarios por tarjetas horizontales.
- Cuatro pestañas Monarca y acceso al perfil desde el avatar.
- Equipo contextual, ajustes semanales agrupados y una sola encuesta RPE.
- Adaptación de grupos a 7, 14 y 21 días.
- Tres modos de rendimiento y mensajes personalizados de los cinco acompañantes.
- Conservación del historial de sesiones y recursos propios de Monarca.

## Recursos de ejercicio
- La biblioteca actual conserva las exclusiones auditadas y progresiones de la revisión del 22 de noviembre. La pestaña Catálogo 2.000 permite consultar las 1.200 fichas originales y 800 de expansión por ID, sin habilitar fichas retiradas para nuevas selecciones automáticas.
- La ficha, la sesión guiada y Misiones Monarca usan el personaje vectorial de Compose con las poses del catálogo. Las secuencias reutilizan los movimientos de sus dos gestos.
- La auditoría comprueba la cobertura de poses y la ausencia de las ilustraciones y clips históricos en los recursos de la APK. Los SVG faciales forman parte del personaje actual.

## Auditoría y compilación
```sh
python3 tools/check_hunter2000_complete.py
python3 tools/generate_svg_qa_queue.py
bash gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:assembleRelease
```
`.github/workflows/android-apk.yml` realiza las auditorías, pruebas Kotlin/Compose y compilación firmada. Verifica las poses JSON, los catálogos y la ausencia de clips históricos y del visor 3D. Esta verificación **no acredita** fidelidad visual en Android, rendimiento ni validación biomecánica.

La validación visual en teléfono físico y la evaluación técnica de las 800 fichas siguen pendientes. Las 800 fichas de expansión conservan datos de movimiento vectorial; la cobertura de poses no equivale a validación visual. Consultar [QA SVG](docs/QA-ANDROID-SVG-V43.md). Las 800 fichas continúan excluidas de la planificación automática.

## Pantalla de inicio V43.12.2

El perfil nativo Compose se abre por defecto. La compilación 134 aparece junto al formulario. La prueba de dispositivo comprueba los campos y los píxeles de la pantalla, no solamente la URL. Para errores exclusivos de teléfonos físicos se necesita su registro adb logcat; no borrar datos.

Restauración basada en 5f76c8ce (22-11-2026), padre inmediato de la introducción del visor 3D. Se conserva el personaje vectorial de Compose y no se recuperan los MP4 antiguos.
