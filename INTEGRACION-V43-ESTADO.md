# Hunter System V43 — Estado verificado de la integración Kotlin + Compose

## Integración final en la rama
- Monarca 42.9 + Eclipse 41.0, en Kotlin, Jetpack Compose y Canvas, sin WebView.
- 1200 fichas originales y 800 adicionales: **2000 ejercicios** con IDs únicos.
- **2000 SVG físicos** dentro de `app/src/main/assets/demos30/`, de los cuales 800 son recursos nuevos.
- Fichas adicionales: `app/src/main/assets/catalog-hunter2000-extra.json`.
- Manifiestos: `hunter2000-manifest.json` y `hunter2000-qa-status.json`.
- Archivo compactado de procedencia: `.hunter2000/source/hunter2000-assets.tar.xz` (SHA-256: `c661292ab8df9aefa8ce234325b0b7ca3a2dc1da9f32f211399cfc339c5b6e71`).

## Auditoría y APK
El workflow [#40391971139](https://github.com/miguelpinto9988-byte/Hunter-System-Native/actions/runs/40391971139) terminó correctamente: validó archivos y hashes, creó el commit de 2000 SVG y 800 fichas, superó los tests Kotlin/Compose, compiló una APK release firmada y verificó dentro de la propia APK los 2000 SVG y 800 registros nuevos.

La APK `Hunter-System-V43-2000-Ejercicios-Preview-APK` está disponible como artifact de ese workflow (retención de 14 días).

## Seguridad y limitaciones
Las **800 fichas nuevas están pendientes de revisión técnica y biomecánica** (`pendingTechnicalReview=true`, `approvedForAutoplan=false`, `autoProgressionApproved=false`). Se muestran claramente identificadas en el catálogo pero no se asignan automáticamente. Los SVG fueron validados como XML y por hash; **eso no verifica corrección biomecánica ni renderizado real en todos los dispositivos**.

No se han ejecutado pruebas físicas Android en esta revisión. Antes de marcar la distribución como definitiva, validar instalación/actualización, restauración del historial, tiempo de respuesta, animaciones, accesibilidad, notificaciones y firma en dispositivos reales.

## Repetir la validación
```bash
python3 tools/check_hunter2000_complete.py
bash gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:assembleDebug
```

Ver [PR #17](https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/17). La rama `main` permanece sin cambiar.
