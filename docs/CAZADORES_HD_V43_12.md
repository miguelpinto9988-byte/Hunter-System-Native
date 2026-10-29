# Cazadores HD — Hunter System V43.12

Los cuatro retratos principales de Kim Dokja, Yoo Joonghyuk, cazador masculino y
cazadora femenina se entregan en **720 × 1080 píxeles** (HD vertical, proporción 2:3),
en formato **WebP RGBA con transparencia**. No se recortan ni se amplían.

## Cómo se empaquetan

- Los originales de alta resolución se conservan en `art-masters/hunter-4k/` como
  fuentes de edición, **fuera de la APK**.
- `tools/prepare_hunter_hd.py` genera los cuatro
  `app/src/main/assets/hunter-hd/{reader,regressor,hunter-male,hunter-female}.webp`.
- Las evoluciones anteriores E–S disponibles en `ui33`/`ui34` se reducen
  automáticamente si exceden **720 × 1080**. No se sustituye el diseño de cada rango.
- `prepareHunterHd` se ejecuta antes del empaquetado de assets con Gradle.
  También existe un paso explícito en GitHub Actions.
- Para compilaciones locales, instalar Python 3 y Pillow:
  `python3 -m pip install Pillow==11.3.0`.
- Las tres opciones de calidad controlan partículas y decodificación; ninguna
  vuelve a exigir archivos 4K en el instalador.

Esta optimización no modifica el XP, los rangos, el perfil ni las misiones.
Las ilustraciones de Misiones Monarca siguen siendo independientes del fondo global.
