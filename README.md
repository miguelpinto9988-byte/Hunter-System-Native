# Hunter System — Android Kotlin + Jetpack Compose

## Integración V43 (vista previa)

Hunter System es una aplicación Android nativa desarrollada en **Kotlin, Jetpack Compose y Canvas**, sin WebView, HTML/CSS/JS ni dependencias de una web para sus pantallas principales. Identificador `com.huntersystem.app`; Android 8.0+ (API 26), target API 35.

La rama `integration/hunter-v43-compose-2000` fusiona el diseño **Monarca 42.9** y elementos **Eclipse 41.0**: inicio y portal, reliquia geométrica 3D giratoria en Canvas, temas Eclipse, panel de misiones, biblioteca, rutinas, estadísticas locales, acompañantes, guardado y accesibilidad. Conserva el sistema de reglas y planificación y los recursos multimedia originales.

## Estado del catálogo 2000

- Catálogo original integrado en la rama: **1200 ejercicios**.
- Fuente separada: paquete `Hunter_System_V41_Paquete_Integracion_2000_SVG.zip` con **800 fichas nuevas** y **2000 recursos SVG**. El paquete **no forma parte del árbol de esta rama** todavía.
- El código Kotlin `Catalog.kt` puede cargar las 800 fichas adicionales desde `catalog-hunter2000-extra.json`.
- `Rules.kt` excluye las nuevas fichas de la planificación automática hasta revisión.
- Los nuevos ejercicios **requieren QA técnico/biomecánico** y validación visual; su presencia no demuestra que sean seguros para todos los perfiles.

Para importar el paquete a un clon de esta rama:
```bash
python3 tools/import_hunter2000_assets.py /ruta/Hunter_System_V41_Paquete_Integracion_2000_SVG.zip
python3 tools/import_hunter2000_assets.py --check
bash gradlew :app:testDebugUnitTest :app:assembleDebug
```
Tras integrar, revisar el diff e incorporar los cambios de `app/src/main/assets` al control de versiones y lanzar el workflow de release.

## Compilación y CI

Java 17, Gradle 8.11.1, Android SDK 35, AGP 8.9.2, Kotlin Compose 2.1.20.

```bash
bash gradlew :app:testDebugUnitTest :app:assembleDebug
```

GitHub Actions ejecuta verificaciones de V42, test Kotlin/Compose, import-check y, si terminan bien, la compilación firmada y publicación de la APK.

## Importante antes de distribuir

Validar en Android real las imágenes y gestos, estadísticas, copia/restauración, rendimiento, notificaciones, firma y compatibilidad de actualización. No mezclar certificados incompatibles sin migración autorizada. No marcar esta vista previa como versión definitiva de 2000 ejercicios antes de importar recursos y superar QA.

Ver [INTEGRACION-V43-ESTADO.md](INTEGRACION-V43-ESTADO.md) y [PR #17](https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/17).
