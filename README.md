# Hunter System V43 — Android nativo con 2000 ejercicios

Esta rama integra **Monarca 42.9 + Eclipse 41.0 + Hunter 2000** sobre **Kotlin, Jetpack Compose y Canvas Android**. No usa WebView para sus pantallas ni depende de HTML, CSS o JavaScript. Compatible desde Android 8.0 (API 26); target API 35.

## Recursos integrados en GitHub

- **1200 ejercicios originales + 800 fichas adicionales = 2000 IDs.**
- **2000 archivos SVG** en `app/src/main/assets/demos30/`, incluidos los 800 nuevos.
- `app/src/main/assets/catalog-hunter2000-extra.json` contiene las fichas adicionales.
- `hunter2000-manifest.json` y `hunter2000-qa-status.json` documentan los SVG y sus hashes.
- La fuente compacta verificable se conserva en `.hunter2000/source/hunter2000-assets.tar.xz`.
- Hash SHA-256 del archivo compactado: `c661292ab8df9aefa8ce234325b0b7ca3a2dc1da9f32f211399cfc339c5b6e71`.

**Estado de seguridad:** las 800 fichas adicionales permanecen con `pendingTechnicalReview=true`, `approvedForAutoplan=false` y `autoProgressionApproved=false`; pueden figurar en el catálogo, pero `Rules.kt` las excluye de la planificación automática. No se ha hecho una validación técnica/biomecánica individual ni una prueba de todos los movimientos en dispositivos físicos.

## Verificación obligatoria

```bash
python3 tools/check_hunter2000_complete.py
bash gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest
bash gradlew --no-daemon --max-workers=2 :app:assembleDebug
```

La auditoría estricta falla si falta cualquiera de las 800 fichas, si hay IDs duplicados, si una ruta SVG falta, si alguno de los 2000 hashes es incorrecto o si se habilitan fichas nuevas sin QA.

GitHub Actions:
- `hunter2000-import.yml`: expandió los recursos, los confirmó en la rama e inicia tests y compilación de APK release.
- `hunter2000-full.yml`: verificación independiente que comprueba además que **la APK contiene físicamente los 2000 SVG y el catálogo de 800 registros**.
- `android-apk.yml`: compilación Android normal de la rama.

## Integración visual

Se conserva la navegación, misiones, rutinas, progreso, perfiles y animaciones nativas de Monarca. Eclipse aporta iconos, temas, portal de inicio y reliquia geométrica 3D mediante Compose y Canvas. Los SVG son archivos vectoriales renderizados mediante código Android, no contenido WebView.

## Antes de distribuir como versión final

1. Confirmar ejecución exitosa de test, build y verificación del contenido APK con los 2000 recursos.
2. Comprobar instalación y actualización de APK en un dispositivo real y la firma usada anteriormente.
3. Evaluar rendimiento, accesibilidad, copias de seguridad y comportamiento de notificaciones.
4. Revisar la técnica y la representación visual de cada ejercicio añadido antes de habilitarlo para planificación automática.

Ver [PR #17](https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/17) y [documentación de integración](INTEGRACION-V43-ESTADO.md).
