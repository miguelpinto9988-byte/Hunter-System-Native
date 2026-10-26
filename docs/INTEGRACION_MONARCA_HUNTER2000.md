# Integración Hunter System — Monarca 42.9 + Hunter 2000

## Estado de esta rama
- Base: `design/v42-fase10-microinteracciones`, Android **nativo Kotlin y Jetpack Compose** (sin WebView).
- Integrado: carga opcional de catálogo extendido en `Catalog.kt`, preservando los 1.200 IDs originales.
- Integrado: guardia `Rules.reasons` que excluye de los generadores las entradas con `sourceExpansion2k=true` y `approvedForAutoplan=false`.
- Pendiente: importar y verificar `app/src/main/assets/catalog-hunter2000-extra.json` (exactamente 800 entradas), y las 800 demostraciones SVG nuevas. **Esta rama no contiene aún 2.000 ejercicios reales.**
- Versión Android: conserva temporalmente `42.9-monarca-fase10` / `versionCode=117`. No publicar como versión final ni incrementar versión hasta pruebas y recursos completos.

## Restricciones de integración
1. Mantener 1.200 IDs previos y datos de usuario; los 800 ID nuevos deben ser distintos, con metadatos, familias y requisitos completos.
2. Validar que el JSON de 800 entradas sea correcto y que `byId.size == 2000`. El código acepta catálogo de 1.200 si falta el archivo y **no debe anunciar 2.000** en tal caso.
3. Los 800 nuevos deben iniciar como `sourceExpansion2k=true` y `approvedForAutoplan=false`; la liberación para planificación exige revisión individual de técnica, equipo, restricciones y multimedia.
4. Verificar los SVG mediante la ruta nativa de `NativeMedia.kt`: rutas, animación y fallback, sin incorporar HTML/CSS/JavaScript/WebView.
5. Ejecutar Gradle debug, pruebas unitarias, auditorías Monarca 01–10, release firmada y comprobación de firma; después probar instalación, navegación, persistencia, animaciones y rendimiento en Android físico.
6. Revisar diferencias de diseño Eclipse 41 frente a Monarca 42 antes de declarar una unificación visual definitiva. No sobrescribir indiscriminadamente archivos divergentes como `MainActivity.kt`, `Design.kt` o `Screens.kt`.
7. Mantener copia de seguridad JSON y validar compatibilidad de firma/versionCode antes de actualizar una instalación.

## Criterios para salida final
`2000` registros únicos y validados en archivos; pruebas de seguridad; ilustraciones y reproducciones comprobadas; APK release firmada y probada físicamente. Hasta cumplir todo, este desarrollo es **borrador de integración**.
