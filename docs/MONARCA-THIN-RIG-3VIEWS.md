# Monarca Eclipse — rig delgado original, tres vistas (piloto)

Fuentes: `app/src/main/assets/media2d/squat.svg` y `pushup.svg` (personaje 2D histórico). Los 2000 `demos30` existentes permanecen sin modificación. El carácter es la ilustración estilizada clara con bordes azul oscuro, detalles coral y acentos cian; **no** se adopta la versión más musculosa de `demos30`.

## Rutas y uso

| ID original | Vista | Archivo |
|---|---|---|
| `squat` | frontal | `media2d/rig-v2/squat_front.svg` |
| `squat` | lateral | `media2d/rig-v2/squat_side.svg` |
| `pushup` | cenital | `media2d/rig-v2/pushup_top.svg` |

`MonarchExerciseStage` ofrece selector FRONTAL/LATERAL para sentadilla y resuelve flexiones en CENITAL. El resto de ejercicios utiliza la ruta de siempre, preservando planificación, IDs, informes, perfiles y rutinas.

## Modelo

- Rig compartido por dos capas SVG: `layer-aesthetic` (visible) y `layer-skeleton` (visible parcialmente, inspeccionable).
- 25 posiciones cerradas en un ciclo de 4,8 segundos, una fuente de posiciones por ambos grupos, interpolación nativa SMIL (compatible con `NativeSvgView`).
- 3D ortográfico simple (`x` lateral, `y` altura, `z` anterior/posterior). Sentadilla: IK analítica de dos segmentos para piernas y brazos, tobillos y puntas anclados. Flexión: manos y pies anclados, tronco en línea, codos con IK de dos segmentos.
- Invariantes verificables: longitudes de huesos constantes en 3D, trayectorias continuas y bucle cerrado, puntos de apoyo inmóviles. Cada círculo `j-*` incluye `data-xyz` por muestra para que un test independiente compare el movimiento real y los enlaces del rig.
- Como el SVG se proyecta a 2D, una vista por sí sola no basta para medir ángulos o diagnosticar una técnica de ejercicio. La escala del personaje y los parámetros de movimiento son ilustrativos, NO clínicos.

## Regeneración programática

El archivo `tools/generate_thin_rig_v2.mjs` es el código fuente editable para construir nuevos SVG a partir de parámetros 3D, cinemática inversa (IK) y paleta visual delgada. No requiere paquetes npm ni WebView. Para regenerar en un directorio separado:

```bash
node tools/generate_thin_rig_v2.mjs --output /tmp/monarca-rig
python3 tools/check_thin_rig_v2.py /tmp/monarca-rig
```

Revisar visualmente antes de sustituir cualquier recurso de la APK. La versión de prueba regenerada puede variar en trazos respecto a los SVG ya incorporados; ambos pasan el mismo contrato geométrico.

## Validación

```bash
python3 tools/check_thin_rig_v2.py
bash gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest
```

Revisar captura en Android físico, pausa/reproducción, mirror, velocidad, reinicio, FPS y compatibilidad del parser de SMIL antes de promoverlo a producción. No sustituir los 1200 modelos originales ni los 800 de revisión pendiente en masa. Cualquier ampliación por ejercicio requiere comprobar patrón, equipo y biomecánica individualmente.
