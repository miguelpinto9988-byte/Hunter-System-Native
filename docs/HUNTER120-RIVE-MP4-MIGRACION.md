# Hunter120: 2.000 ejercicios, formatos .riv y MP4 (120 fotogramas)

## Estado verificable

La rama V43 conserva **2.000 SVG de origen** (1.200 del catálogo original + 800 propuestas adicionales). No se ha afirmado que sean equivalentes a una anatomía X-Ray validada.

- **Preparado:** exportador `tools/hunter120_export.py` de los SVG/SMIL existentes a vídeos MP4/H.264 sin audio, exactamente **120 fotogramas** a **25 fps** (4,8 s). Se comprueba el conteo real con `ffprobe` para cada archivo; registra fallos por ID sin falsificar salidas.
- **Probado en GitHub Actions:** el workflow `hunter120-export.yml` produce un lote inicial de tres ejercicios (squat, pushup y hx_upper_0001) y publica un archivo descargable con vídeos más manifiesto. Fuente y estado del lote: [Actions](https://github.com/miguelpinto9988-byte/Hunter-System-Native/actions/workflows/hunter120-export.yml).
- **En la APK:** `NativeExerciseVideo.kt` permite reproducir MP4 instalados en `assets/demos120/` con control de pausa, velocidad, espejo y avance; `MonarchExerciseV42.kt` recurre automáticamente al SVG cuando no existe un MP4 expresamente autorizado.
- **Sin sustituir todavía:** `assets/demos120/approved.txt` está vacío, por lo que todas las demostraciones siguen usando los 2.000 SVG que ya funcionaban.

## Qué no se ha hecho todavía

1. **No hay 2.000 `.riv` nativos.** Un `.riv` es una animación interactiva propia de Rive, mientras que `.mp4` almacena cuadros de vídeo. Se requieren dos entregables por ejercicio, no una extensión doble.
2. **No hay 2.000 vídeos anatómicos X-Ray nuevos.** El convertidor reproduce fielmente el estilo del SVG de origen, que generalmente es una ilustración deportiva y no una capa de músculos anatómicos.
3. **No se ha realizado revisión biomecánica individual.** Particularmente, los 800 ejercicios adicionales siguen como propuestas no aprobadas; un vídeo generado de esos SVG no cambia su estado.
4. El lote de muestra **no se distribuye dentro de la APK** hasta pasar una comprobación visual real en Android.

## Uso del convertidor

Requisitos: Python 3.11+, `lxml`, `cairosvg`, `Pillow`, `ffmpeg` y `ffprobe`.

```bash
python3 -m pip install cairosvg lxml Pillow

# Tres demostraciones existentes — 120 fotogramas por ejercicio
python3 tools/hunter120_export.py \
  --source app/src/main/assets/demos30 \
  --output build/hunter120/pilot \
  --ids squat,pushup,hx_upper_0001

# Lote 0/20: los primeros cien, en orden de IDs
python3 tools/hunter120_export.py \
  --source app/src/main/assets/demos30 \
  --output build/hunter120/000 \
  --start 0 --count 100

# Revisar en 20 lotes (0,100,...,1900) antes de empaquetar los 2.000.
```

En GitHub puede ejecutarse `Hunter120 - exportar demos a MP4 de 120 fotogramas` por ID o por rango; el workflow **no hace despliegues automáticos ni sobrescribe los SVG**.

## Puerta de migración a los 2.000 recursos

Para cada ID se requiere:
- Fichero **`.riv` real** compilado con Rive CLI/RML o exportado desde el editor, inspeccionado en un runtime compatible con Android.
- MP4 de **120 fotogramas comprobados**, que represente la misma animación; grabación offline, sin pistas de audio obligatorias.
- Vistas apropiadas según el movimiento y anatomía X-Ray con los músculos implicados; **verificación de ambas piernas, pies, proporciones y técnica**.
- Archivo físico empaquetado en `app/src/main/assets/demos120/<id>.mp4`.
- Solo tras la revisión técnica, añadir el ID a `app/src/main/assets/demos120/approved.txt`.
- Un inventario final de **2.000 `.riv` + 2.000 MP4**, con el número de frames y hashes, antes de proponer borrar algún SVG.

La herramienta oficial [Rive CLI](https://rive.app/agents) permite crear `.riv` usando **Rive Markup Language (RML)**; no equivale a renombrar un SVG. Los rigging/animaciones y su validación requieren desarrollo adicional. El [editor de Rive](https://rive.app/editor) también permite exportar el binario y el vídeo por separado.

La conversión por lotes de SVG a MP4 sirve para preservar animaciones originales mientras se diseña y verifica el nuevo estilo. **No debe interpretarse como una migración integral completada**.

## Política de seguridad

No introducir miniaturas en el catálogo; conservar compatibilidad con el sistema de fases V42, sin WebView. Proteger la rama main; la migración solo podrá fusionarse tras pruebas Android, auditorías y validación de técnica.