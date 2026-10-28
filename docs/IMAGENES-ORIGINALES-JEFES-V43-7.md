# Hunter System V43.7 — Imágenes originales sin rediseño

El usuario ha solicitado sustituir las ilustraciones SVG inventadas de los jefes por imágenes existentes publicadas en Internet, **sin rediseñarlas**.

## Procedencia y atribución de los tres JPEG

| Misión | Personaje | Archivo promocional original | Fuente |
|---|---|---|---|
| Guardia del Umbral | Igris / Igrit | `assets/monarch-boss-original/guardia.jpg` | [Sitio oficial del anime](https://sololeveling-anime.net/special/shadows-visual/), `igrit.jpg` |
| Asalto a la Fortaleza | Beru | `assets/monarch-boss-original/fortaleza.jpg` | [Sitio oficial del anime](https://sololeveling-anime.net/special/shadows-visual/), `beru.jpg` |
| Juicio del Trono | Baran | `assets/monarch-boss-original/trono.jpg` | [Nota de prensa de Netmarble](https://ch.netmarble.com/Eng/Newsroom/Detail?bbs_code=1020&post_seq=5134) |

Los archivos se descargan en el entorno de compilación mediante `python3 tools/download_monarch_boss_originals.py`; los JPEG se empaquetan sin transcodificación ni filtros. En pantalla se usa `AssetImage` y `ContentScale.Crop`, únicamente para ajustarlos al rectángulo; el archivo original permanece intacto en la APK.

Se retiran los SVG rediseñados `monarch-boss/guardia.svg`, `fortaleza.svg` y `trono.svg`. Las misiones reales y sus requisitos, contadores, sellos, vencimiento semanal y controles permanecen iguales.

**Derechos de autor:** estas imágenes son propiedad de los titulares de *Solo Leveling*. La publicación en una página web no constituye autorización para incorporarlas a una app y redistribuirla. Uso únicamente como prototipo de prueba privada; para publicar en tiendas o redistribuir legalmente deben obtenerse las licencias correspondientes. No afirmar que el arte es libre o autorizado.

**Compilación reproducible:** se requiere conexión HTTPS de la máquina que ejecuta el script para descargar los originales. La app instalada no precisa Internet. Validar manualmente el encuadre visual y el uso de memoria en Android antes de distribuir.
