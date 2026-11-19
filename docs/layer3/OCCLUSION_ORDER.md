# Orden de oclusión del volumen exterior C3 (calf)

El código anterior pintaba el torso en la posición 3/24, antes de la mayoría de superficies posteriores, de modo que partes traseras podían tapar el torso. El nuevo orden reproducible dibuja las 9 superficies posteriores primero, el torso y el núcleo después, y las 10 superficies frontales a continuación. La cabeza queda al final.

**Antes → después:** perfiles posteriores detrás del torso: **0/9 → 9/9**; perfiles frontales delante del torso: **10/10 → 10/10**. Son invariantes de orden SVG, no mediciones de píxeles o de colisiones.

Prueba reproducible: `node docs/layer3/occlusion_order.test.js`. Comprueba inventario único de 24, cobertura total de grupos, orden y pares anterior/posterior. Verificación adicional: sintaxis del JS integrado y comparación textual de los datos históricos C1 y de las estaciones C3.

No se alteran 48 poses, C1, C2, estatura, escala 720×800, anclajes, entrenamiento ni APK. Quedan pendientes capturas de navegador y análisis de contactos e intersecciones entre superficies distintas.

Cobertura documentada C3: **1/1200**. Los 800 ejercicios sin poses históricas permanecen pendientes.
