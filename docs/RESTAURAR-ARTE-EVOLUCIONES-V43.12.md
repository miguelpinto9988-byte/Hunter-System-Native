# Recuperación del arte original — Monarca Eclipse V43.12

Se recibieron cuatro láminas con seis evoluciones cada una: Reader, Regressor, Cazador y Cazadora; rangos E, D, C, B, A y S. El commit anterior restauró rutas `ui34` pero no las 24 variantes exactas de estas láminas.

## Integración

Los 24 WebP de las cuatro láminas están publicados en `main` en `app/src/main/assets/hunter-evolutions-v43/` con nombres `reader-e.webp`, ..., `hunter-female-s.webp`. La rama `main` ya contiene `HunterEvolutionImage`, que recupera la variante correspondiente al rango y personaje guardado en el selector, vista previa y tarjeta Sistema Monarca. Si falta algún recurso, muestra el arte heredado como respaldo (evita tarjetas invisibles). Se utiliza `ContentScale.Fit`.

Para reprocesar las imágenes PNG originales y reemplazar recursos, instalar Pillow y ejecutar:

```bash
python tools/restore_evolution_art_v43.py --reader reference/reader.png --regressor reference/regressor.png --hunter-male reference/hunter-male.png --hunter-female reference/hunter-female.png --out app/src/main/assets/hunter-evolutions-v43
```

Las 24 evoluciones ya están en GitHub como WebP RGBA de calidad 90; un ZIP separado conserva las versiones sin pérdida, los cuatro PNG originales y el script de recorte.

**Verificaciones**: comprobar cuatro selecciones, seis rangos, proporción completa del personaje, cambios entre pestañas, reinicio del perfil y selección guardada. Compilar con `./gradlew :app:assembleDebug` en un entorno con SDK Android configurado. No se modifican catálogo, progreso ni datos de perfil.
