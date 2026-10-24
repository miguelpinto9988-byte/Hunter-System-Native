# Hunter System · Android nativo

Versión 38.50-native, código 104, paquete `com.huntersystem.app`. Kotlin y Jetpack Compose; Android 8.0 (API 26) o posterior. La aplicación usa almacenamiento local y conserva los medios del proyecto V38.49.

## Compilar

Requisitos: JDK 17, Android SDK Platform 35 y Build Tools 35.0.0. El wrapper descarga Gradle 8.11.1. AGP 8.9.2 y Kotlin/Compose Compiler 2.1.20 están fijados en el proyecto.

Configura `ANDROID_HOME` o crea `local.properties` con `sdk.dir=/ruta/android-sdk`. Para una compilación firmada, indica la clave adjunta al chat mediante estas variables, sin incorporarla al repositorio:

```bash
export HUNTER_KEYSTORE=/ruta/hunter-system-release.p12
read -r -s -p 'Contraseña de firma: ' HUNTER_STORE_PASSWORD
export HUNTER_STORE_PASSWORD
./gradlew :app:assembleRelease
```

El alias es `hunter-system-release` y el almacén es PKCS12. La APK se genera en `app/build/outputs/apk/release/`. Las credenciales y la clave privada no forman parte de este proyecto.

```bash
./gradlew :app:testDebugUnitTest :app:lintRelease
```

## Implementación

- `Rules.kt` y `Planner.kt`: clasificación 38.49, filtros por equipo y capacidades, restricciones, potencia, selección, dosis, fases y generador independiente de rutinas.
- `NativeStore.kt`: archivo JSON con escritura atómica, calendario, registros explícitos, estadísticas, protección de sesiones iniciadas e importación/exportación.
- `Screens.kt`, `Profile.kt`, `Overlays.kt`, `Supplement.kt`, `Design.kt`: seis secciones, perfil, misión, biblioteca, registro, guía, contenidos educativos, apariencia y acompañante en Compose.
- `NativeMedia.kt`: SVG originales dibujados en Canvas de Android, con interpolación de los valores de animación. Los vídeos usan VideoView.
- `SystemServices.kt`: avisos locales, sonido original y actualización RSS opcional de los canales de referencia.

La APK no incorpora HTML, JavaScript, CSS ni un WebView. Los recursos de `demos30/`, `ui33/`, `ui34/`, `offline2d/`, `fonts/` y el resto del inventario original se conservan sin modificar. `asset-manifest.json` registra sus SHA-256.

## Datos e instalación

La firma suministrada es diferente a la del APK V38.49. Exporta una copia JSON desde la aplicación anterior antes de sustituirla; Android no permite actualizar el mismo paquete con una firma diferente sin una rotación autorizada. Después de instalar la edición nativa puedes restaurar la copia desde Perfil.

Los campos desconocidos del archivo original se conservan. Las cantidades de estadísticas proceden de series confirmadas: una casilla de misión no inventa repeticiones, tiempo ni kg. Una guía de sesión procedente de la versión web puede comenzar de nuevo si su firma de pasos no coincide; se conservan las series del registro.

## Verificación y límites

`ParityTest` compara el motor Kotlin con resultados del código original: 21 perfiles, 1.200 ejercicios por perfil, tres clases de sesión, 45 misiones y 90 combinaciones de biblioteca. `NativeStoreTest` comprueba persistencia, restauración, vistas futuras sin escritura, asignación exacta, protección del historial, tiempos de avisos, orden de rondas y dibujo animado en Canvas. `ComposeSmokeTest` ejercita la composición de las seis pantallas y el perfil inicial.

Las pruebas de entorno no sustituyen la instalación en un teléfono. Consulta el informe de entrega para los resultados finales. La interfaz se reconstruyó en Compose usando las fuentes, colores, iconos e imágenes originales; no se ha certificado una coincidencia visual píxel a píxel con la aplicación WebView.
