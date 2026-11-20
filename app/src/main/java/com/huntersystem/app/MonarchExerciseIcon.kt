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
        "M11 4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2 M10.5 9L7 14h6l-2 5h5 M9.5 10.5l4 1.5 4-2"),
    LUNGE("Zancada y escalón", Color(0xFF526719), Color(0xFFC7E482),
        "M12 4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2 M11.5 9v5l4-2.5 2 6h3 M11.5 14l-4 3-3 1 M10 10l-3 3"),
    HINGE("Cadera y bisagra", Color(0xFF3D6966), Color(0xFF8BDED4),
        "M16.7 5a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2 M15.5 9l-6 4 3 2.5 1 4 M9.5 13l-3 6h-3 M13 11l3 4h3"),
    FOOT("Apoyo y trabajo de tobillo", Color(0xFF745515), Color(0xFFF6D784),
        "M8 4v9l-1.5 3a2.5 2.5 0 0 0 2.3 3.5H18a2 2 0 0 0 .8-3.8L13 13V4 M4 12l2-2 M17 11l2-2 2 2"),
    PUSH("Empuje", Color(0xFF155E96), Color(0xFF83CCFF),
        "M18.5 7a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2 M4 16l11-4h2l-1 4 2 2 M14 18h7 M3 18h5"),
    PULL("Tracción", Color(0xFF593C96), Color(0xFFC4ABFF),
        "M4 4h16 M12 9a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2 M7 5v6l5 4 5-4V5 M12 15v3l-3 3 M12 18l3 3"),
    PRESS("Fuerza y levantamiento", Color(0xFF06677C), Color(0xFF7DDEFB),
        "M3 8v8m3-10v12m12-12v12m3-10v8M6 12h12"),
    CORE("Estabilidad del tronco", Color(0xFF663489), Color(0xFFDBA9FF),
        "M12 3l7 3v6c0 4-2.8 6.5-7 9-4.2-2.5-7-5-7-9V6l7-3z M12 8v7m-3.5-3.5h7"),
    ROTATION("Rotación", Color(0xFF8B3562), Color(0xFFF5A3CE),
        "M7 8a7 7 0 0 1 12 3m.1-4v4h-4 M17 16A7 7 0 0 1 5 13m-.1 4v-4h4 M12 10v4"),
    MOBILITY("Movilidad y estiramiento", Color(0xFF116C61), Color(0xFF83E7D1),
        "M12 4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2 M4 9l8 2 7-3 M12 11l-2 4-4 4 M10 15l8 4 M19 5l1-1"),
    BALANCE("Equilibrio", Color(0xFF80500C), Color(0xFFFFCD82),
        "M12 4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2 M4 10h16 M12 9v6l-2 4 M12 14l4 2 1-3 M6 21h12"),
    RUN("Cardio y desplazamiento", Color(0xFF9A4B0F), Color(0xFFFFB474),
        "M15 4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2 M13 9l-4 1-3 3 M13 9l3 3h4 M13 9l-2 5 5 4 M11 14l-3 4H4"),
    JUMP("Salto y aterrizaje", Color(0xFF934719), Color(0xFFFFB79C),
        "M10 17v-7m-3 3l3-3 3 3m3 1V5m-3 3l3-3 3 3 M5 20h14"),
    CYCLE("Pedaleo", Color(0xFF135E81), Color(0xFF80DBF2),
        "M9 16a4 4 0 1 0-8 0 4 4 0 0 0 8 0 M23 16a4 4 0 1 0-8 0 4 4 0 0 0 8 0 M5 16l4-7 5 7H5 M9 9h7 M15 6h2l2 10"),
    BALL("Control de balón", Color(0xFF2E6639), Color(0xFFA0E793),
        "M21 12a9 9 0 1 0-18 0 9 9 0 0 0 18 0 M12 8l4 3-1.5 4h-5L8 11z M12 8V3m4 8 4.5-1.5m-6 5.5 2.8 4.2m-7.8-4.2-2.8 4.2M8 11l-4.5-1.5"),
    BOXING("Boxeo y golpeo", Color(0xFF962F48), Color(0xFFFF9DAE),
        "M8 17l-3-4V9a2 2 0 0 1 2-2h2V5a2 2 0 0 1 2-2h4a4 4 0 0 1 4 4v5l-2 5 M8 17h9v4H8z M9 7v4l-2 2"),
    GENERAL("Ejercicio", Color(0xFF4C5C76), Color(0xFFB4C5DF),
        "M4 17h4v3H4z M10 12h4v8h-4z M16 5h4v15h-4z")
}

private val exerciseIconRules = listOf(
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
    """empuje_horizontal|\bflexiones\b|flexion (?:de suelo|de brazos|en pared)|push[- ]?up""" to MonarchExerciseIcon.PUSH,
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
