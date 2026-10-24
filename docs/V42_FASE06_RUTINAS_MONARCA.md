# Hunter System V42.5 — Fase 06: Rutinas y generadores Monarca

**Rama:** `design/v42-fase06-rutinas-monarca`  
**Base:** `design/v42-fase05-misiones-monarca`  
**Versión Android:** `42.5-monarca-fase6` (`versionCode = 113`)  
**Estado:** Implementación nativa enviada a CI; pendiente de pruebas manuales en dispositivos.

## Componentes implementados

1. **Centro de entrenamiento Monarca:** acceso diferenciado a Biblioteca Hunter y Gimnasio, cantidades calculadas desde el catálogo, acceso al editor, canales de referencia y fuentes preexistentes.
2. **Biblioteca nativa filtrable:** nombre, tres niveles, categoría, intervalos de duración, equipo real de la secuencia, impactos y compatibilidad con el perfil; limpieza de filtros y contador de resultados.
3. **Generador dinámico:** selección de múltiples grupos, tres rangos de duración, nivel único o los tres niveles, exclusión voluntaria de salto/potencia y opción de solo peso corporal para biblioteca sin gimnasio.
4. **Integración con motor real:** usa `Planner.generateRoutine()`, `Rules.pool()`, `Rules.compatible()`, `NativeStore.adjusted()`; mantiene experiencia, equipamiento, lesiones e historial.
5. **Resultados reversibles:** las combinaciones generadas se guardan solo en `NativeUi.generated[mode]` y pueden restaurarse a la biblioteca; las misiones diarias no cambian.
6. **Tarjetas de rutinas originales:** duración estimada, número de ejercicios, series, ejercicios por nombre, aviso de compatibilidad y acceso a la secuencia existente. **No se introducen miniaturas.**

## Garantías y restricciones

- El perfil no es modificado al generar. La selección sin saltos **añade** `restrictions.noImpact = true` a una copia temporal, sin anular restricciones previas.
- El modo solo peso corporal crea un perfil temporal con menos equipo; no concede equipamiento no declarado.
- El gimnasio exige `equipment = gym` en Perfil y respeta `gymEquipment23`.
- Antes de publicar los resultados generados se vuelve a verificar cada ejercicio contra `Rules.compatible()`; si cambia el perfil durante el cálculo se descarta la tanda.
- La asignación final continúa utilizando `NativeStore.assign()` con bloqueo de día pasado o con actividad, sin sobrescribir registros reales.
- Se mantiene el catálogo de **1200 ejercicios** y **342 demostraciones MP4**, el reproductor original y el acceso offline.
- Las secuencias de entrenadores externos siguen como referencias; no se declara reproducción o transcripción original de sus vídeos sin verificación.

## QA a realizar en dispositivos

- Comprobar los nueve temas, fuentes grandes, contraste, TalkBack, modo horizontal y tablets de 8–12 pulgadas.
- Consultar biblioteca y gimnasio, aplicar filtros cruzados, limpiarlos y buscar por título.
- Generar 1, 2 y 5 grupos en modo básico/intermedio/avanzado y en los tres niveles simultáneamente; los resultados deben corresponder a la selección.
- Configurar sin saltos, sin equipo y diferentes lesiones/restricciones; no debe colarse un ejercicio incompatible.
- Comprobar que un perfil configurado con equipo corporal no pueda generar gimnasio.
- Cambiar el perfil durante una generación: descartar los resultados anteriores.
- Abrir una ficha, revisar animaciones SVG/MP4 y asignar una secuencia; corroborar que no se alteran sesiones ya registradas.
- Repetir la navegación sin internet y comprobar que no hay parpadeos ni saltos de scroll.

## Próxima fase
Fase 07: fichas de ejercicios y reproductor de demostraciones. Se mantendrá la regla: **sin miniaturas en catálogo ni tarjetas**.
