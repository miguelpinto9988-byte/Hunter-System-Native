package com.huntersystem.app

internal data class ExerciseProgression(val guidance: String, val nextId: String? = null)

internal val performanceProgressions = mapOf(
    "gym-pullassist30" to ExerciseProgression(
        "Reduce la asistencia cuando completes las repeticiones con el mismo recorrido y sin impulso.",
        "new46-pullup"),
    "new46-pullup" to ExerciseProgression(
        "Completa varias repeticiones limpias antes de introducir lastre.", "performance-weighted-pullup"),
    "benchpress" to ExerciseProgression(
        "Compara el peso por mancuerna y las repeticiones con el mismo recorrido antes de subir la carga."),
    "row" to ExerciseProgression(
        "Registra el peso y las repeticiones de cada lado. Aumenta la carga cuando ambos lados mantengan el control."),
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
