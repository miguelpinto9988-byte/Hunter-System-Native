# Hunter System V43 — protocolo de QA Android / Rive 2000

**Estado inicial: PENDIENTE.** Este protocolo no afirma que se hayan ejecutado pruebas físicas, visuales ni biomecánicas.

## 1. Preparar la matriz de 2000 ejercicios

```bash
python3 tools/check_hunter2000_complete.py
python3 tools/generate_rive_qa_queue.py --output build/reports/hunter-rive-2000-qa-pendiente.csv
```

La matriz contiene 1.200 IDs originales y 800 ampliados, ruta `.riv`, hash SHA-256 y estado de revisión individual. El CSV es **un punto de partida**, no un certificado; todas las columnas de revisión comienzan en `PENDIENTE`. El workflow de APK también publica una copia como artifact separado. Guardar los registros completados con la evidencia de dispositivo y el revisor, sin modificar la plantilla original ni cambiar indicadores de autoplaneamiento automáticamente.

## 2. Puertas de aceptación

1. **Instalación y firma**: instalar la APK release en un Android real compatible; verificar arranque en frío, permisos y actualización desde una versión previamente instalada que comparta clave de firma. Si no se dispone de la clave anterior, registrar la incompatibilidad; no afirmar actualización exitosa.
2. **Conservación de datos**: comprobar rutinas, historial, perfil y preferencias antes y después de actualizar; probar restauración y uso sin conexión.
3. **Reproducción visual**: abrir cada ID de la matriz en un dispositivo, comprobar carga efectiva, inicio, pausa, reanudación, espejo y reinicio, además de transiciones entre ejercicios. Registrar fallos por ID, modelo de teléfono y versión Android.
4. **Fidelidad de movimiento**: comparar cada escena Rive con su fuente de procedencia y revisar saltos, articulaciones, dirección, legibilidad, ciclos y artefactos. La validez del hash no demuestra fidelidad.
5. **Revisión biomecánica**: exigir revisión técnica individual para cada una de las 800 fichas adicionales, incluida técnica, restricciones, equipamiento y animación. Mantener `approvedForAutoplan=false` y `autoProgressionApproved=false` mientras esté pendiente.
6. **Rendimiento y accesibilidad**: medir bloqueos, cierres, memoria, fluidez y tiempos de carga en equipos reales de distinta capacidad; revisar contraste, lector de pantalla, tamaños táctiles y notificaciones.

## 3. Evidencia mínima por ejercicio

Registrar ID, dispositivo, versión de Android, build/commit probado, fecha, revisor, resultados de las cuatro revisiones, evidencia visual y observaciones; usar `APROBADO` o `RECHAZADO` solamente tras observación real. Los errores se convierten en issues rastreables de GitHub vinculados al ID.

## 4. Condición para distribuir

CI exitosa **y además** pruebas reales de instalación, actualización y reproducción, revisión visual de los 2.000 y revisión biomecánica de los 800. No fusionar [PR #17](https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/17) ni habilitar las 800 fichas nuevas antes de cerrar estas puertas de aceptación. La transformación a Rive por sí sola tampoco implementa la apariencia anatómica X-Ray.
