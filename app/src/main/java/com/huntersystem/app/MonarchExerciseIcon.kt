package com.huntersystem.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.huntersystem.app.planning.Category
import com.huntersystem.app.planning.CategoryMapper

internal enum class MonarchExerciseIcon(
    val label: String, val lightInk: Color, val darkInk: Color, val path: String
) {
    SQUAT("Sentadilla", Color(0xFF17674D), Color(0xFF77E6A9),
        "M14 3.2a1.85 1.85 0 1 0 0 3.7 1.85 1.85 0 0 0 0-3.7 " +
            "M13 8l-3.5 5.8 6.5.2-2.2 5.2h3.4 M12.4 9.5l3 1.2 3.6-1.6 M5 21h15"),
    LUNGE("Zancada y escalón", Color(0xFF526719), Color(0xFFC7E482),
        "M12 3.2a1.85 1.85 0 1 0 0 3.7 1.85 1.85 0 0 0 0-3.7 " +
            "M12 8.4v5.1l6.1.1v5.5h2 M12 13.5l-4 4.9-4-1.7 M11.9 9.5l-2.6 2.7 M3 21h18"),
    HINGE("Cadera y bisagra", Color(0xFF3D6966), Color(0xFF8BDED4),
        "M19 6a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6 " +
            "M16.1 9.3l-7.5 4.2 2.1 3.1-1.2 3.2h2.6 M14 10.6l1.4 3.7 2.2 2.1 M5 13h2m-2 0 1.3-1.3M5 13l1.3 1.3"),
    FOOT("Apoyo y trabajo de tobillo", Color(0xFF745515), Color(0xFFF6D784),
        "M9 4v7l-1.7 3.4A3 3 0 0 0 10 19h8.6a1.7 1.7 0 0 0 .4-3.4L14 13V4 " +
            "M3.5 13.8v-5m-1.6 1.6 1.6-1.6 1.6 1.6 M4 21h17"),
    PUSH("Flexión y empuje horizontal", Color(0xFF155E96), Color(0xFF83CCFF),
        "M19.7 6a2 2 0 1 0 0 4 2 2 0 0 0 0-4 " +
            "M16.4 10.3l-5.5 1.8-5.7 3.4L4 18.1 M15.6 10.7l-2.5 3.4 3 3.8H19 M2.5 20.8h19"),
    PULL("Tracción", Color(0xFF593C96), Color(0xFFC4ABFF),
        "M3 3.5h18 M12 6.6a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6 " +
            "M7 3.5V8l4.8 4.2L17 8V3.5 M11.8 12.2v4.5l-2.4 3.1 M11.8 16.7l2.5 3.1"),
    PRESS("Fuerza y levantamiento", Color(0xFF06677C), Color(0xFF7DDEFB),
        "M2.5 12h19 M5.5 7.5v9m3-10v11m7-11v11m3-10v9"),
    CORE("Estabilidad del tronco", Color(0xFF663489), Color(0xFFDBA9FF),
        "M8.2 4.5 4 7l2.3 4.1 2-1.1-.4 4.2-1 5.3h10.2l-1-5.3-.4-4.2 2 1.1L20 7l-4.2-2.5H14a2 2 0 0 1-4 0z " +
            "M10 11.7h4m-4 2.7h4"),
    PELVIC("Basculación pélvica", Color(0xFF655127), Color(0xFFFFD799),
        "M4.5 13a1.75 1.75 0 1 0 0 3.5 1.75 1.75 0 0 0 0-3.5 " +
            "M7 17h5.8l3.5-5.2 3.7 4.7h1.8 M2.5 20.5h19 M9 11.5a3.5 3.5 0 0 1 4.2-2.3m-1-1.6 1 1.6-1.8 1"),
    ROTATION("Rotación", Color(0xFF8B3562), Color(0xFFF5A3CE),
        "M12 4a1.65 1.65 0 1 0 0 3.3 1.65 1.65 0 0 0 0-3.3 M7.4 9.2l4.6 1.4 4.5-1.4 M12 10.6v6.1 " +
            "M7.2 12a4.1 4.1 0 0 0 .7 6.9c2.7 1.5 8.2.9 10-1.6m-3.3.1 3.4-.2v3.1"),
    MOBILITY("Movilidad y estiramiento", Color(0xFF116C61), Color(0xFF83E7D1),
        "M13 6a1.75 1.75 0 1 0 0 3.5 1.75 1.75 0 0 0 0-3.5 " +
            "M6 4l4.3.5 5.1 6.7 4.9 1.8 M14 9.4l-2.3 5.1-3.2 4.7 M11.7 14.5l4.5 4.7 M5 21h14"),
    BALANCE("Equilibrio", Color(0xFF80500C), Color(0xFFFFCD82),
        "M12 3a1.75 1.75 0 1 0 0 3.5 1.75 1.75 0 0 0 0-3.5 M4 9h16 M12 8v6l-.8 5.6 " +
            "M12 14l4-1.5-1-2.5 M7 21h9"),
    RUN("Cardio y desplazamiento", Color(0xFF9A4B0F), Color(0xFFFFB474),
        "M16.2 3.4a1.75 1.75 0 1 0 0 3.5 1.75 1.75 0 0 0 0-3.5 M14.8 8.6l-5.1.8-2 3 " +
            "M14.8 8.6l2.2 4h4 M14 9.7l-2.1 4.8 4.2 2.2v3.5 M11.9 14.5l-3 4.2H5 M2.8 8.5h4"),
    JUMP("Salto y aterrizaje", Color(0xFF934719), Color(0xFFFFB79C),
        "M11.7 4.5a1.75 1.75 0 1 0 0 3.5 1.75 1.75 0 0 0 0-3.5 M5.8 6.1l5.9 4.4 5-4.4 " +
            "M11.7 10.5v3.3l-3.2 3.3 M11.7 13.8l3.5 3.3 M6 21h12 M21 16v-4m-1.6 1.6L21 12l1.6 1.6"),
    CYCLE("Pedaleo", Color(0xFF135E81), Color(0xFF80DBF2),
        "M8.5 16a3.25 3.25 0 1 0-6.5 0 3.25 3.25 0 0 0 6.5 0 M22 16a3.25 3.25 0 1 0-6.5 0 3.25 3.25 0 0 0 6.5 0 " +
            "M5.2 16 9 9.8l4.5 6.2H5.2 M9 9.8h6.8L13.5 16 M15.9 6.2h1L18.8 16 M7 7.9h3.1"),
    BALL("Control de balón", Color(0xFF2E6639), Color(0xFFA0E793),
        "M21 12a9 9 0 1 0-18 0 9 9 0 0 0 18 0 M12 8l3.8 2.8-1.5 4.5H9.7l-1.5-4.5z " +
            "M12 8V3m3.8 7.8 4.7-1.5m-6.2 6 3 4m-7.6-4-3 4m1.5-8.5-4.7-1.5"),
    BOXING("Boxeo y golpeo", Color(0xFF962F48), Color(0xFFFF9DAE),
        "M8.2 16.8l-2.7-3V9.3c0-1.4 1-2.4 2.3-2.4H9V6a2.3 2.3 0 0 1 2.3-2.3h4.3a3.4 3.4 0 0 1 3.4 3.4v4.5l-2.5 5.2 " +
            "M8.2 16.8h8.3v3.5H8.2z M9 7.7v3.4l-1.1 1.3"),
    GENERAL("Ejercicio", Color(0xFF4C5C76), Color(0xFFB4C5DF),
        "M10.8 3.9a1.75 1.75 0 1 0 0 3.5 1.75 1.75 0 0 0 0-3.5 M5.6 11l5.2-1.7 4.7 2.6H20 " +
            "M10.8 9.3v5.1l-3 5.3m3-5.3 4.8 5.3 M17.6 7.6h3.2")

}

private val exerciseIconRules = listOf(
    """basculacion(?: de la pelvis| pelvica)|(?:inclinacion|anteversion|retroversion|balanceo) (?:pelvic[ao]|de (?:la )?pelvis)|pelvic[_ -]tilt""" to MonarchExerciseIcon.PELVIC,
    """boxeo|(?:jab|cross|uppercut)|golpeo de puno""" to MonarchExerciseIcon.BOXING,
    """pedaleo|bicicleta|ciclismo|cadencia sentado""" to MonarchExerciseIcon.CYCLE,
    """drib[lb]|conduccion de balon|recepcion.*balon|pase.*balon|control.*balon|toques.*balon""" to MonarchExerciseIcon.BALL,
    """rotacion|giro de tronco""" to MonarchExerciseIcon.ROTATION,
    """equilibrio|balance(?!o)|tandem|linewalk""" to MonarchExerciseIcon.BALANCE,
    """estiramiento|stretch|movilizacion|movilidad (?:toracica|de )|chestopen""" to MonarchExerciseIcon.MOBILITY,
    """sentadilla|squat|^rodilla$|flexion_rodilla""" to MonarchExerciseIcon.SQUAT,
    """zancada|estocada|lunge|subida[_ ](?:al?[_ ])?escalon|step[- ]?up""" to MonarchExerciseIcon.LUNGE,
    """bisagra|extension[_ ]cadera|puente de gluteos|peso muerto|hip thrust""" to MonarchExerciseIcon.HINGE,
    """flexion_plantar|tobillo|pantorrilla|elevaciones? de talones""" to MonarchExerciseIcon.FOOT,
    """traccion|\bremo\b|dominadas?|upper_row|upper_bentrow""" to MonarchExerciseIcon.PULL,
    """pallof""" to MonarchExerciseIcon.CORE,
    """empuje_vertical|upper_press|press|levantamiento|flexion_codo|extension_codo|curl""" to MonarchExerciseIcon.PRESS,
    """empuje_horizontal|upper_pushup|\bflexiones\b|flexion (?:de suelo|de brazos|en pared)|push[- ]?up""" to MonarchExerciseIcon.PUSH,
    """plancha|estabilizacion_tronco|abdominal|dead bug|bird dog|hollow""" to MonarchExerciseIcon.CORE,
    """^salto|aterrizaje|jumpjack|rebote""" to MonarchExerciseIcon.JUMP,
    """carrera|marcha|trote|paso lateral|locomocion|cardio_|shuffle""" to MonarchExerciseIcon.RUN
).map { (pattern,icon) -> Regex(pattern) to icon }

internal fun monarchExerciseIcon(exercise: Exercise?): MonarchExerciseIcon {
    if(exercise==null)return MonarchExerciseIcon.GENERAL
    val firstMovement=normalize(exercise.name).substringBefore(" · ")
    val firstPattern=normalize(exercise.pattern).substringBefore(" y ")
    // El nombre principal conserva la intención en las secuencias deportivas del catálogo.
    for(term in listOf(firstMovement,firstPattern)) {
        val visual=exerciseIconRules.firstOrNull { (rule,_) -> rule.containsMatchIn(term) }
        if(visual!=null)return visual.second
    }
    return when(CategoryMapper.normalize(exercise.type)){
        Category.UPPER->MonarchExerciseIcon.PRESS
        Category.CENTRAL->MonarchExerciseIcon.CORE
        Category.LOWER->MonarchExerciseIcon.LUNGE
        Category.MOBILITY->MonarchExerciseIcon.MOBILITY
        Category.CARDIO->MonarchExerciseIcon.RUN
        null->MonarchExerciseIcon.GENERAL
    }
}

@Composable
internal fun MonarchExerciseBadge(icon: MonarchExerciseIcon, modifier: Modifier=Modifier) {
    val isLight=LocalHunter.current.isLight
    val ink=if(isLight)icon.lightInk else icon.darkInk
    val shape=RoundedCornerShape(8.dp)
    val path=remember(icon) { PathParser().parsePathString(icon.path).toPath() }
    Box(modifier.size(34.dp)
        .background(ink.copy(alpha=if(isLight).10f else .15f),shape)
        .border(1.dp,ink.copy(alpha=.45f),shape)
        .semantics { contentDescription=icon.label },
        contentAlignment=Alignment.Center) {
        Canvas(Modifier.size(22.dp)) {
            withTransform({ scale(size.width/24f,size.height/24f,pivot=Offset.Zero) }) {
                drawPath(path,ink,style=Stroke(1.65f,cap=StrokeCap.Round,join=StrokeJoin.Round))
            }
        }
    }
}
