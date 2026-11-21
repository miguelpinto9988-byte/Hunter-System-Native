package com.huntersystem.app

import com.huntersystem.app.planning.Category
import com.huntersystem.app.planning.CategoryMapper

internal data class ExerciseClassification(val category: Category, val family: String, val type: String)

internal val trainingCategoryLabels = mapOf(
    Category.UPPER to "Tren superior", Category.CENTRAL to "Tren central",
    Category.LOWER to "Tren inferior", Category.MOBILITY to "Movilidad", Category.CARDIO to "Cardio"
)

/** Classifies inventory for new sessions while leaving the original metadata available to old records. */
internal object ExerciseTaxonomy {
    val essentialMobility = mapOf(
        "neck" to "Cuello",
        "shoulder" to "Hombros", "home-wallangel30" to "Hombros", "chestopen" to "Hombros",
        "elbowmob" to "Codos y antebrazos", "forearmmob" to "Codos y antebrazos",
        "wrist" to "Muñecas y manos", "fingers" to "Muñecas y manos",
        "cat" to "Columna y pelvis", "thoracic" to "Columna y pelvis",
        "new46-chairthoracic" to "Columna y pelvis", "sidebendstand" to "Columna y pelvis",
        "new46-pelvictilt" to "Columna y pelvis",
        "hipcircle" to "Caderas", "home-hip909030" to "Caderas", "hipflex" to "Caderas",
        "new46-adductorrock" to "Caderas", "hamstretch" to "Caderas",
        "ankle" to "Tobillos y pantorrillas", "calfstretch" to "Tobillos y pantorrillas"
    )
    private val unloadedUpper = setOf(
        "wallslide", "scapular", "reverse", "pau-armworld", "pau-armcircle-f", "pau-armcircle-b",
        "pau-armupdown", "pau-armpulses", "pau-armfly", "pau-armsback", "pau-armraise", "pau-armcurl"
    )
    private val lowerStrength = setOf(
        "calf", "sideleg", "seatedcalf", "clam", "seatedhip", "toelift", "seatedheel",
        "wallcalf", "standingabduct", "calfhold", "abductcontrolled", "calf-control18",
        "standingabduct-control18", "new46-shortfoot", "new46-toeyoga", "new46-toetowel",
        "new46-reverseclam", "new46-pronehipir", "new46-walltibia", "new46-firehydrant", "new46-sideadduct"
    )
    private val skillOnly = setOf(
        "sport-futbol-sole", "sport-basket-figure8", "sport-volley-wall-set", "sport-tennis-shadow-forehand",
        "sport-box-slip-rope", "sport-track-a-wall", "sport-ampl-track-march-toe", "sport-ampl-football-inside-out",
        "sport-ampl-football-hook-behind", "sport-ampl-football-shield-exit", "sport-ampl-football-v-pull",
        "sport-ampl-football-toe-taps", "sport-pilates-saw", "new46-droplanding",
        "new46-mbrotation", "new46-mbslam", "new46-mbchest", "new46-mbscoop", "new46-mboverback",
        "new46-mbcatch", "pau-pushchild"
    )
    private val centralComponents = setOf("anti_rotate", "plank", "deadbug", "crunch", "torso_twist")
    private val expansionReassigned = mapOf(
        "hamstring_curl_hamstring_curl" to Category.LOWER,
        "lateral_raise_lateral_raise" to Category.UPPER,
        "side_step_side_step" to Category.CARDIO
    )

    fun classify(exercise: Exercise, resolve: (String) -> Exercise?): ExerciseClassification? {
        val parts = exercise.raw.a("sequenceParts").strings()
        if (parts.isNotEmpty()) {
            val components = parts.map { id ->
                val part = resolve(id) ?: return null
                if (part.raw.a("sequenceParts").length() != 0) return null
                individual(part) ?: return null
            }
            val first = components.first()
            if (first.category == Category.MOBILITY || components.any { it.category != first.category }) return null
            return ExerciseClassification(first.category,
                components.map { it.family }.distinct().sorted().joinToString(" + "),
                components.joinToString(" → ") { it.type })
        }
        return individual(exercise)
    }

    private fun individual(e: Exercise): ExerciseClassification? {
        essentialMobility[e.id]?.let { return ExerciseClassification(Category.MOBILITY, it, e.family) }
        val roots = setOf(e.id, e.family, e.oldFamily)
        if (roots.any { it in skillOnly || it in unloadedUpper }) return null
        val declared = CategoryMapper.normalize(e.type) ?: return null
        val name = normalize(e.name)
        if (e.hunter2000 && declared == Category.CENTRAL && name.startsWith("sentado ") &&
            "banda" !in name && "mancuerna" !in name && "contra" !in name) return null
        val category = when {
            "contralateral" in roots -> Category.CENTRAL
            roots.any { it in lowerStrength } -> Category.LOWER
            roots.any { it == "march" || it == "slowmarch" } -> Category.CARDIO
            e.hunter2000 && declared == Category.CENTRAL ->
                expansionReassigned[e.family] ?: if (centralComponents.any { first ->
                    centralComponents.any { second -> e.family == "${first}_${second}" }
                }) Category.CENTRAL else return null
            e.hunter2000 && declared == Category.LOWER && e.family in setOf("march_", "side_step_") ->
                Category.CARDIO
            declared == Category.MOBILITY || e.category == "Movilidad" -> return null
            else -> declared
        }
        val movement = "${normalize(e.pattern)} ${normalize(e.oldFamily)} $name"
        fun has(vararg terms: String) = terms.any { it in movement }
        val family = when (category) {
            Category.UPPER -> when {
                has("traccion_vertical", "dominada", "chin-up", "jalon") -> "Tracción vertical"
                has("remo", "traccion", "row", "retraccion", "escapul", "face pull") -> "Tracción horizontal y escapular"
                has("tricep", "extension_codo", "fondo de triceps") -> "Extensión de codos"
                has("bicep", "curl", "martillo", "flexion_codo") -> "Flexión de codos"
                has("overhead", "empuje_vertical", "press de hombro", "press militar", "pica", "arnold") -> "Empuje vertical"
                has("press", "empuje", "flexion de brazo", "flexion en pared", "push", "flexion de suelo", "flexion inclinada", "flexion con", "flexion diamante", "fondo") -> "Empuje horizontal"
                else -> "Fuerza de hombros y pecho"
            }
            Category.CENTRAL -> when {
                has("pallof", "anti_rotate", "antirrotacion") -> "Antirrotación"
                has("plancha lateral", "sideplank", "maleta", "suitcase") -> "Estabilidad lateral"
                has("crunch", "curlup", "flexion abdominal", "elevacion abdominal", "elevacion de rodillas colgado") -> "Flexión abdominal"
                has("rotacion", "twist", "chop", "windmill") -> "Rotación del tronco"
                else -> "Estabilidad y resistencia del tronco"
            }
            Category.LOWER -> when {
                has("pantorrilla", "plantar", "tobillo", "calf", "tibial", "arco plantar", "puntas") -> "Pantorrillas y pies"
                has("bisagra", "peso muerto", "hinge") -> "Bisagra de cadera"
                has("puente", "thrust", "extension_cadera", "bridge") -> "Extensión de cadera"
                has("abduccion", "aduccion", "clam", "abduct") -> "Abducción y aducción"
                has("zancada", "lunge", "subida", "stepup", "step_up") -> "Zancadas y escalones"
                has("femoral", "hamstring", "flexion_rodilla", "extension_rodilla", "legextend") -> "Flexión y extensión de rodilla"
                has("aterrizaje", "salto") -> "Potencia de piernas"
                else -> "Sentadillas y prensa"
            }
            Category.CARDIO -> when {
                has("pedaleo", "bicicleta", "cadencia sentado") -> "Bicicleta"
                has("ergometro", "eliptica", "escaladora") -> "Ergómetros y escaladora"
                has("box", "jab", "uppercut") -> "Boxeo aeróbico"
                has("cuerda", "jump_rope") -> "Salto de cuerda"
                has("burpee", "escalador", "mountain") -> "Cardio de cuerpo completo"
                has("carrera", "trote", "sprint", "march", "caminata") -> "Marcha y carrera"
                has("salto", "jump", "rebote") -> "Saltos rítmicos"
                else -> "Pasos y desplazamientos"
            }
            Category.MOBILITY -> return null
        }
        return ExerciseClassification(category, family, e.family)
    }
}
