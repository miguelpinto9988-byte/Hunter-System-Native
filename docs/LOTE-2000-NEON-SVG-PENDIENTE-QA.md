# Hunter System — Lote de 2.000 SVG Neon (prototipos, QA pendiente)

Fecha de generación: 2026-10-26. Rama de trabajo: `integration/hunter-v43-compose-2000`; no se altera `main`.

## Alcance del prototipo
Se ha generado un paquete externo reproducible, **no incorporado todavía en la APK**:
- 2.000 SVG móviles para los 2.000 IDs del catálogo actual, con rutas previstas `app/src/main/assets/demos30/{id}.svg`.
- 2.000 fichas SVG ampliadas, con estética oscura, bordes cian y panel de instrucciones del catálogo.
- CSV con SHA256 por ejercicio y matriz de QA de 2.000 filas `PENDIENTE`.
- Generador Python que toma como entrada la APK V43.2 o los assets del checkout.

**No se ha sobrescrito ningún SVG activo del repositorio.** El paquete completo se entregó en la conversación de trabajo del usuario; requiere revisión y aprobación antes de integrarse.

## Fidelidad técnica
- Los movimientos SMIL y fotogramas fueron **conservados exactamente** desde los SVG presentes en V43.2; no se han generado nuevas cinemáticas biomecánicas.
- Los originales contienen 126 firmas distintas de movimientos de geometría `d`; contando todos los canales de animación, son 146 firmas. Los patrones se repiten entre ejercicios.
- Se reutiliza el rig animable existente, con colores y paneles de la estética Neon. **NO reproduce todavía con fidelidad geométrica absoluta el monigote estático aprobado**. Para ello debe hacerse rigging/dibujo y comprobación individual por patrón y por ejercicio.
- El nuevo lote **no equivale a 2.000 animaciones diferenciadas y aprobadas** ni prueba reproducción en teléfono real.

## Seguridad y QA
Los 800 ejercicios nuevos siguen bajo las restricciones `pendingTechnicalReview=true`, `approvedForAutoplan=false`, `autoProgressionApproved=false`. Antes de integrar:
1. Aprobar equivalencia del nuevo diseño con el personaje solicitado; rectificar el rig cuando sea necesario.
2. Revisar técnica y movimiento por ejercicio; registrar casos que comparten un patrón no aplicable.
3. Validar reproducción (incluidas las animaciones SMIL) en el reproductor Android real.
4. Actualizar los SHA256 y auditorías de assets solo después de comprobar el resultado.
5. Compilar y firmar una preview nueva, verificar contenido y volver a hacer QA físico.

Mantener [PR #17](https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/17) en borrador y continuar QA en [issue #19](https://github.com/miguelpinto9988-byte/Hunter-System-Native/issues/19).
