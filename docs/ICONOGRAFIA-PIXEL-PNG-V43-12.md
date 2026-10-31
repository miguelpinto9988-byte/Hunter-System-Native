# Hunter System V43.12 — Biblioteca PNG Pixel Art Monarca Eclipse

- Estética aprobada: Pixel Art moderno, sombras violeta/azules, multicolor según función, sin marcos dorados.
- 49 iconos PNG transparentes únicos (256 × 256, RGBA), 76 claves funcionales existentes.
- Las imágenes se empaquetan como recursos Android en `app/src/main/res/drawable-nodpi/` y Compose utiliza `ImageBitmap.imageResource` con `FilterQuality.None`.
- Icono de lanzador y notificaciones actualizados con variantes PNG transparentes.
- El script `tools/build_hunter_pixel_icons.py` permite regenerar los 49 recursos sin dependencias externas. Ejecutar `python3 tools/build_hunter_pixel_icons.py --output app/src/main/res/drawable-nodpi --scale 4 --verify`.
- El control `python3 tools/check_pixel_icons.py` verifica 49 archivos, RGBA, transparencia, 76 claves, consistencia generador/PNG y uso de Compose.
- Se conservan intactos el catálogo y las animaciones SVG de 2000 ejercicios, los datos y la navegación.
- Inspiración conceptual de fantasía oscura y progresión RPG, sin reproducir iconografía o personajes propietarios.
- Falta ejecutar compilación completa en runner Android y comprobar el resultado visual en un dispositivo real. No se afirma certificación de la APK.
