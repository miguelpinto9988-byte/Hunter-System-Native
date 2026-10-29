# Hunter System V43.12 · Objetivo y equipo simplificado

## Distribución del perfil

- **Tu compañero:** identidad y personaje, sin controles de planificación.
- **Objetivo y equipo:** nivel de entrenamiento, objetivo final, meta personal y modalidad de equipo.
- **Desafíos o rutinas:** modo y planificación semanal.
- **Lesiones y salud:** mantiene restricciones, avisos y confirmaciones por separado.

La explicación interpretativa de la meta se muestra cuando hay una meta escrita, para evitar ocupar espacio innecesario.

## Presentación reducida

El usuario ve únicamente el selector de **Peso corporal / Mancuernas / Gimnasio** y, cuando proceda, el peso de una mancuerna. Un único botón despliega los complementos y habilidades opcionales. Se muestran grupos de equipo habituales en vez de ofrecer las 42 claves de materiales, 13 habilidades y todos los submenús técnicos.

- Básicos: espacio libre y pared, colchoneta, silla, banda y anclaje, barra de dominadas, escalón.
- Mancuernas: banco de entrenamiento.
- Gimnasio: barras y banco; poleas y máquinas; equipo de cardio; rack con seguros.
- Habilidades: equilibrio, suspensión, aterrizaje y control de carga superior (cuando corresponda).

Cuando una opción contiene **varios elementos separados por coma**, marcarla afirma que **todos** están disponibles: nunca se añaden equipos ni capacidades implícitas. Las opciones antiguas y específicas de un perfil existente permanecen guardadas y pueden quitarse mediante un botón explícito dentro del único apartado opcional.

## Compatibilidad

Se mantienen las claves de datos `difficulty`, `goalGroup`, `finalGoal`, `equipment`, `gymEquipment23`, `materials46` y `capabilities46`. Los planificadores y filtros de seguridad siguen recibiendo los mismos identificadores. No se modifican registros históricos, restricciones médicas ni rutinas completadas.

Pruebas ampliadas en `ProfileEquipmentTest.kt` para la ubicación de campos, selección de equipo permitida, agrupación explícita y conservación de configuraciones previas.
