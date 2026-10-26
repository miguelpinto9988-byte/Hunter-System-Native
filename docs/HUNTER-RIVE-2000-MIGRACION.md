# Hunter System V43 — migración de animaciones de ejercicios a Rive

## Cambios en la rama de integración

- **2000 binarios nativos .riv** en `app/src/main/assets/demos-rive/` (1 por ID, sin sustituciones con MP4 ni SVG).
- `demos-rive/manifest.json` incluye tamaños y SHA256 de todos los binarios; la comprobación de integridad de 2000 archivos forma parte de la CI.
- **0 SVG y 0 JSON de animación** en la antigua carpeta `app/src/main/assets/demos30/`, retirada del empaquetado.
- `Catalog.kt` determina `demos-rive/<id>.riv`; `HunterRiveExercise.kt` carga los archivos con el runtime Rive Android+Compose; las pantallas de detalle y de sesión usan el nuevo reproductor.
- Compatibilidad disponible: pausa, reproducción, espejo y reinicio. Se retiró temporalmente la manipulación de velocidad/posición del control que no estaba conectada al runtime nuevo.
- `compileSdk 36` para la dependencia del runtime Rive, `targetSdk 35` conservado.
- El conjunto de vídeo editorial local (342 MP4 de información) no se usa como demostración principal de ejercicio y no fue convertido.
- El paquete fuente SVG original permanece en `.hunter2000/source/hunter2000-assets.tar.xz`, **fuera de assets Android**, como archivo de procedencia reproducible. No vuelve a importarse en la aplicación.

## Verificación reproducible

```bash
python3 tools/check_hunter2000_complete.py
bash gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:assembleRelease
```

CI debe inspeccionar la APK y exigir **2000 archivos .riv con RIVE magic + SHA256** y **ningún archivo `assets/demos30/*.svg` ni `*.json`**.

- Generación Rive original y auditoría global: [Actions #40420713057](https://github.com/miguelpinto9988-byte/Hunter-System-Native/actions/runs/40420713057).
- Rama: `integration/hunter-v43-compose-2000`.
- PR #17 permanece en borrador.

## Advertencias de producto

La conversión masiva transforma los movimientos de las ilustraciones SVG de origen en una animación Rive con vértices interpolados. **No produce automáticamente cuerpos anatómicos X-Ray con músculos rojos, capas transparentes, cambios de cámara ni biomecánica certificada**. Los resultados deben revisarse visualmente: gradientes, transformaciones de grupos u otros atributos pueden diferir del SVG original aunque el binario compile.

Los **800 ejercicios adicionales** siguen con `pendingTechnicalReview=true` y se excluyen del entrenamiento automático. Sus animaciones .riv no implican certificación técnica.

La reproducción real de 2000 escenas y el rendimiento de los modelos más complejos deben ensayarse en dispositivos Android físicos antes de distribución definitiva.