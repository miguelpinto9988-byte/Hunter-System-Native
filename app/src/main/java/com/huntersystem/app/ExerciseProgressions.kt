package com.huntersystem.app

internal class ExerciseProgression(val guidance: String, vararg val nextIds: String)

internal val performanceProgressions = mapOf(
    "gym-pullassist30" to ExerciseProgression(
        "Reduce la asistencia cuando completes las repeticiones con el mismo recorrido y sin impulso.",
        "new46-pullup"),
    "new46-pullup" to ExerciseProgression(
        "Domina varias repeticiones limpias antes de introducir lastre o practicar la transición en anillas.",
        "performance-weighted-pullup", "performance-ring-false-grip"),
    "performance-ring-false-grip" to ExerciseProgression(
        "Mantén el agarre con apoyo de pies antes de practicar la transición.", "performance-ring-transition"),
    "performance-ring-transition" to ExerciseProgression(
        "Reduce la ayuda de los pies gradualmente. Domina también los fondos en anillas antes del muscle-up completo.",
        "performance-strict-ring-muscle-up"),
    "new46-tucklever" to ExerciseProgression(
        "Compara los segundos con el mismo ángulo. Aleja las rodillas cuando puedas mantener el tronco horizontal.",
        "performance-advanced-tuck-lever"),
    "performance-advanced-tuck-lever" to ExerciseProgression(
        "Extiende una pierna solo si mantienes la pelvis nivelada en el agrupado avanzado.", "performance-one-leg-lever"),
    "new46-trxsquat" to ExerciseProgression(
        "Reduce la ayuda de las correas cuando controles el descenso. Después usa un banco como referencia.",
        "home-pistol-box23"),
    "home-pistol-box23" to ExerciseProgression(
        "Usa un banco más bajo cuando controles la bajada y la subida sin impulso.", "single-squat19"),
    "performance-outdoor-run" to ExerciseProgression(
        "Si ya mantienes un ritmo conversacional con control, prueba un bloque de carrera continua.",
        "performance-continuous-run"),
    "performance-continuous-run" to ExerciseProgression(
        "Registra los minutos continuos. Aumenta el tiempo antes de subir el ritmo y vuelve a los intervalos si necesitas pausas.",
        "performance-outdoor-run"),
    "benchpress" to ExerciseProgression(
        "Compara el peso por mancuerna y las repeticiones con el mismo recorrido antes de subir la carga."),
    "row" to ExerciseProgression(
        "Registra el peso y las repeticiones de cada lado. Aumenta la carga cuando ambos lados mantengan el control."),
    "bentrow" to ExerciseProgression(
        "Usa el apoyo de pecho si sostener la inclinación del torso limita la serie.",
        "performance-db-chest-supported-row"),
    "stepup" to ExerciseProgression(
        "Cuando controles la subida y la bajada de cada lado, conserva la altura y añade mancuernas.",
        "performance-db-step-up"),
    "gym-dead-bb23" to ExerciseProgression(
        "Si tienes barra hexagonal, las asas altas ofrecen otra posición de partida para cargar la bisagra.",
        "performance-high-handle-trapbar-deadlift"),
    "gym-skierg30" to ExerciseProgression(
        "En Rutinas de gimnasio selecciona Cardio → Por intervalos. Compara el ritmo o los vatios en el mismo tramo de trabajo."),
    "bulgarian" to ExerciseProgression(
        "Mantén el apoyo y el recorrido constantes. Aumenta la carga cuando completes las repeticiones con ambos lados."),
    "single-rdl19" to ExerciseProgression(
        "Amplía el recorrido solo mientras conserves el equilibrio y la pelvis estable. Después aumenta la carga."),
    "new46-nordic" to ExerciseProgression(
        "Empieza con asistencia y recorrido parcial. Amplía el descenso controlado antes de reducir la asistencia."),
    "new46-abwheel" to ExerciseProgression(
        "Aumenta el recorrido desde las rodillas manteniendo la zona lumbar estable antes de probar de pie.",
        "new46-abwheelstand"),
    "new46-suitcasecarry" to ExerciseProgression(
        "Compara la carga y el tiempo por lado. Mantén el mismo recorrido para comparar sesiones."),
    "gym-pallof23" to ExerciseProgression(
        "Aumenta la resistencia cuando puedas extender los brazos sin girar el tronco."),
    "new46-bentcalfstretch" to ExerciseProgression(
        "Acerca la rodilla a la pared con el talón apoyado. Compara la distancia entre los dedos y la pared en ambos lados."),
    "home-hip909030" to ExerciseProgression(
        "Controla ambas transiciones antes de añadir el despegue activo de pierna.", "new46-hip9090lift"),
    "thoracic" to ExerciseProgression(
        "Compara el giro controlado de cada lado sin arrastrar la pelvis ni forzar el hombro.")
)
