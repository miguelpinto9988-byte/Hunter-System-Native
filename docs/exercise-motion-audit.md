# Auditoría de poses de ejercicios

Ejecutar `python3 tools/audit_exercise_poses.py --output /tmp/hunter-poses.json` para generar el inventario de revisión. El informe lee los archivos de origen y vincula los indicadores de cada animación con las fichas y secuencias que la utilizan. Incluye fichas activas e históricas.

El programa señala fotogramas idénticos sin marca isométrica, poses idénticas compartidas por bases de movimiento distintas, segmentos que parecen comprimidos y proyecciones frontales con un torso horizontal. El escorzo y las variantes de tempo pueden producir coincidencias legítimas. Los indicadores requieren comparar el título, la ejecución y los apoyos con una referencia.

## Reconstrucciones verificadas con referencias

- Elevación abdominal en V. El torso y las piernas suben juntos. Las manos se acercan a las espinillas. La variante lenta conserva tres segundos de descenso. [Catalyst Athletics](https://www.catalystathletics.com/exercise/313/V-Up/)
- Rotación interna de cadera de lado. Las rodillas permanecen juntas y se eleva el pie superior con la pelvis estable. [AAOS OrthoInfo](https://www.orthoinfo.org/bodypart/hip-thigh/hip-conditioning-program/)
- Extensión de cadera en cuadrupedia. Las manos y la rodilla de apoyo permanecen fijas mientras se eleva el muslo contrario con la rodilla flexionada. [American Council on Exercise](https://www.acefitness.org/resources/everyone/exercise-library/270/quadruped-bent-knee-hip-extensions/)

El reproductor sustituye estas poses al cargar los movimientos, también cuando son parte de una secuencia. El informe sigue leyendo el material original para conservar la evidencia de origen.
