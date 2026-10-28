# Hunter System V43.11 — Misiones Especiales Monarca: desafío extremo voluntario y equipo real

## Nueva regla
La oferta y la posibilidad de aceptar una Misión Monarca ya NO dependen del nivel, experiencia, objetivo, calendario ni del estado de configuración del perfil. Un cazador puede aceptar o no el desafío, tras marcar consentimiento voluntario. La condición de contenido es el **equipo disponible** que el cazador ha marcado en Perfil.

## Adaptación y rotación
- Rotación global semanal: Guardia del Umbral → Asalto a la Fortaleza → Juicio del Trono → Fortaleza; independientemente del nivel.
- 4 metas acumuladas durante la semana para Guardia; 5 metas para Fortaleza y Trono.
- Objetivos deliberadamente más exigentes que las misiones normales, repartidos en siete días. Nunca se presentan como actividad obligatoria ni como indicación de entrenamiento intensivo para una única sesión.
- Sin equipamiento: exclusivamente ejercicios originales con `equipment=body` que no requieren material adicional.
- Con mancuernas: se prioriza al menos un ejercicio original que requiera mancuernas.
- Con gimnasio: se incluyen hasta dos ejercicios con el equipamiento realmente marcado (barra, polea, máquina, banco, etc.) y ejercicios corporales.
- Se consideran TODOS los requisitos declarados en `materialRequirements` y `materials46`, así como `gymEquipment23`, `bench` y `step`. No se atribuyen aparatos no seleccionados.
- Los 800 ejercicios de expansión siguen excluidos: aún carecen de QA biomecánica para recomendarse automáticamente.
- La oferta se calcula desde el catálogo original; al aceptar queda almacenada en el registro de esa semana. Los datos previos no se borran.

## Seguridad y honestidad
- La misión es una meta voluntaria, no una prescripción automática ni una prueba de aptitud física.
- Si un movimiento supera las capacidades del usuario o produce dolor, debe detenerse; el desafío puede suspenderse sin penalización.
- Los avances continúan siendo declaración manual del usuario; no hay sensores ni certificación de ejecución.
- Se mantienen la ventana semanal, el bloqueo de segundo intento durante la misma semana y los sellos únicamente tras completar metas.
- Si después de aceptar cambia el equipo marcado y falta un aparato, se informa de la incompatibilidad; no se permite fingir que dicho aparato existe. Puede detenerse la misión sin penalización.

## QA
Pruebas Kotlin en Robolectric comprueban independencia del perfil, correspondencia con materiales, adaptación a body/db/gym y cambio de equipo a mitad de semana. La compilación y los tests estáticos no reemplazan QA de ejecución y accesibilidad en dispositivos Android.

El modo de planificación normal sigue usando sus propios filtros y restricciones; no se han eliminado los controles del entrenamiento convencional.
