# Hunter System V43.12.2 — Monarca Eclipse con demostraciones 2D

Rama principal: `main`. El código integra Monarca V43.11 y las mejoras recientes de Eclipse. Las dos historias se conservan en el commit de fusión.

Monarca 42.9 + Eclipse 41.0, realizados en Kotlin y Jetpack Compose. La compilación de pruebas usa `versionCode=132` y `versionName=43.12.2-monarca-eclipse` (`applicationId=com.huntersystem.app`).

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
- La biblioteca activa contiene **1.063 fichas** entre ejercicios base y variantes. Las fichas históricas y los alias siguen disponibles para resolver sesiones guardadas e historial.
- Las fichas, las sesiones guiadas y Misiones Monarca utilizan demostraciones vectoriales 2D. Se conservan los recursos históricos en el repositorio.
- La sesión guiada y Misiones Monarca usan el personaje vectorial de Compose. Las secuencias reutilizan los movimientos de sus gestos.
- La auditoría comprueba la cobertura de poses y la ausencia de las ilustraciones y clips históricos en los recursos de la APK. Los SVG faciales forman parte del personaje actual.

## Auditoría y compilación
```sh
python3 tools/check_hunter2000_complete.py
python3 tools/generate_svg_qa_queue.py
bash gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:assembleRelease
```
`.github/workflows/android-apk.yml` realiza las auditorías, pruebas Kotlin/Compose y compilación firmada. Verifica que no queden SVG animados en la APK y que las 1000 ilustraciones estáticas estén empaquetadas. Esta verificación **no acredita** fidelidad visual en Android, rendimiento ni validación biomecánica.

La validación visual en teléfono físico y la evaluación técnica de las 800 fichas siguen pendientes. Los 800 ejercicios nuevos no tienen demostración SVG. Consultar [QA SVG](docs/QA-ANDROID-SVG-V43.md). Las 800 fichas continúan excluidas de la planificación automática.

## Pantalla de inicio V43.12.2

El perfil nativo Compose se abre por defecto. La compilación 132 aparece junto al formulario. La prueba de dispositivo comprueba los campos y los píxeles de la pantalla, no solamente la URL. Para errores exclusivos de teléfonos físicos se necesita su registro adb logcat; no borrar datos.
