# C3 exterior — QA de continuidad en contactos (calf)
**Nuevo:** diagnóstico reproducible de cuatro contactos de superficie: rodilla anterior/posterior y tobillo anterior/posterior. Usa exclusivamente las 48 poses C1 históricas embebidas en `comparacion_visual_calf.html`, los 24 perfiles C3 y el esculpido V2. No introduce poses, ni altera perfiles ni capas originales.

Se muestrean secciones horizontales cada 0,5 unidades SVG sobre los perfiles interpolados, después de aplicar las transformaciones del rig de pantorrilla. Se exige que exista solapamiento positivo entre las dos superficies del contacto en cada pose; se comprueba la variación temporal.

| Contacto | Solapamiento mínimo (SVG) | Máximo (SVG) | Salto temporal máximo (SVG) |
|---|---:|---:|---:|
| frontKnee | 24.4396 | 24.4702 | 0.0024 |
| rearKnee | 22.4923 | 22.5927 | 0.0066 |
| frontAnkle | 17 | 17 | 0 |
| rearAnkle | 15.9314 | 15.9667 | 0.0321 |

**Resultado:** 192 contactos/poses comprobados, 0 contactos abiertos bajo esta métrica. El contacto posterior de rodilla tiene poca extensión vertical compartida (mínimo 1 filas de 0,5 SVG): revisar en navegador antes de ajustar cualquier perfil. La superposición no equivale a ausencia de penetraciones ni garantiza el orden de oclusión. El cálculo usa polilíneas de las estaciones interpoladas, no la intersección exacta de dos curvas Bézier.

Ejecutar `node docs/layer3/contact_seams.test.js`. Resultado esperado en `contact_seams_calf.qa.json`. Cobertura C3 confirmada: 1/1200; los 800 sin poses permanecen pendientes.
