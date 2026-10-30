# Galería HD de escenarios — V43.12 Monarca + Eclipse

Ocho ambientes distintos: Biblioteca lunar, Jardín sagrado, Palacio submarino,
Templo del ocaso, Ciudadela boreal, Reino de las nubes, Trono volcánico y
Santuario de bambú. Originales a 960×720 WebP.

En creación y edición de Perfil, **debajo de Escoge tu cazador**:
- Galería horizontal de escenarios, con selección persistente.
- Ajustes de iluminación, atmósfera y marco.
- Vista previa con la misma capa visual de la tarjeta Sistema Monarca.

Las opciones se guardan en `appearance33` como `scene`, `lighting`,
`atmosphere` y `frame`; los objetivos, rutinas y XP no se modifican.

Las ocho imágenes se almacenan como ZIP en `art-masters/hunter-scenes-hd.zip`,
fuera del directorio de recursos Android. El script
`tools/prepare_hunter_scenes.py` las extrae antes de compilar, evitando
duplicaciones en el repositorio. El APK solo contiene las imágenes WebP HD.

**Importante:** ningún escenario vuelve a utilizarse como wallpaper global;
solo aparece en la tarjeta Monarca y su selector.
