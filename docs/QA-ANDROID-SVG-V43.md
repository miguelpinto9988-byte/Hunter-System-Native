# Hunter System V43.2 — Protocolo QA Android / SVG 2000

**Estado inicial: PENDIENTE.** La recuperación de fuentes SVG y sus hashes no implica validación de reproducción, anatomía ni ejecución técnica.

## Preparación
```sh
python3 tools/check_hunter2000_complete.py
python3 tools/generate_svg_qa_queue.py --output build/reports/hunter-svg-2000-qa-pendiente.csv
```
La matriz CSV debe comenzar con las 2000 entradas `PENDIENTE`. Conservar las evidencias de revisión y registrar el ID, dispositivo, Android, commit de APK, fecha y revisor de cada comprobación.

## Puertas de aceptación
1. Instalar la APK firmada en dispositivos reales; verificar arranque, permisos y actualización desde una instalación anterior si comparte clave de firma.
2. Revisar persistencia y copia de seguridad de perfil, rutinas, historial y preferencias, además de comportamiento sin conexión.
3. Inspeccionar los 2000 SVG en fichas de ejercicio, verificando reproducción, pausa, velocidad (0,5×/1×/1,5×/2×), espejo, reinicio y cambio de pantalla.
4. Comparar los movimientos con las fuentes, observando articulaciones, continuidad, escalado, legibilidad y consumo de recursos.
5. Revisar biomecánica, restricciones, equipamiento y animaciones de las 800 fichas adicionales; mantener `approvedForAutoplan=false` y `autoProgressionApproved=false` hasta tener aprobaciones documentadas.
6. Medir cierres, memoria, tiempos de carga y accesibilidad en Android de distintas capacidades.

Los fallos se registran con `exercise_id` y capturas o vídeos. Mantener [PR #17](https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/17) como borrador hasta cerrar las validaciones.
