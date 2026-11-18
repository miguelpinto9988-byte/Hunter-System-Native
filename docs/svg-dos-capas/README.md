# Demostraciones antiguas en dos capas

Esta carpeta contiene una **extracción de la animación SVG antigua** en dos recursos independientes por ejercicio. Se utiliza el commit histórico `c0802ce30837605752faed9a97a161beb9a11838`, anterior a la retirada de los 2.000 SVG animados.

## Capa 1: movimiento

`<id>.motion.json` almacena:

- Un esqueleto vectorial de 11 articulaciones, con 10 conexiones y 48 poses sucesivas del JSON histórico.
- Canales de animación originales (`d` de trazados, `transform` y otros atributos), con duración, valores, tiempos y curva de interpolación.
- Identificadores de destino `data-rig-key`, independientes de los colores y rellenos.

Las poses sirven para visualizar la **estructura del movimiento**. Están en el sistema de coordenadas original del JSON; no se deben superponer directamente al SVG de 480×480 sin una transformación o calibración.

## Capa 2: aspecto

`<id>.appearance.svg` conserva trazados, siluetas, rellenos, sombras, contornos y detalles gráficos **sin etiquetas `<animate>`**. Por sí solo es una ilustración estática.

## Recombinar

En un visor SVG capaz de SMIL, se carga la apariencia y se recorren `motion.tracks`: localizar `[data-rig-key="<target>"]` y añadir un elemento SVG `<animate>` o `<animateTransform>` con los atributos del canal. Así se recuperan las transformaciones de la animación original sin incrustarlas en la capa estética.

Se ofrecen dos ejemplos: **sentadilla (`squat`)** y **curl de bíceps (`curl`)**. Sus datos provienen de los archivos originales del repositorio, no de fotogramas inventados.

## Convertir otras animaciones antiguas

```bash
python3 tools/split_legacy_svg_layers.py squat curl
python3 tools/split_legacy_svg_layers.py benchpress
# Opcional: extraer todo el conjunto a una carpeta fuera de assets Android
python3 tools/split_legacy_svg_layers.py --all --out build/svg-dos-capas
```

El script utiliza `git show` contra el commit histórico para que los SVG animados **no reaparezcan en la APK**. No altera `main`, los 1.200 SVG estáticos de `media2d/` ni los 342 MP4.

**Alcance:** separación de datos y prueba de visualización web. Todavía no se ha creado un motor nativo de animación esquelética reutilizable para los personajes de la app Android. Las muestras no constituyen una validación biomecánica.
