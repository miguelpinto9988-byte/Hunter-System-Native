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
import com.huntersystem.app.MonarchExerciseIcon.*
import com.huntersystem.app.planning.Category
import com.huntersystem.app.planning.CategoryMapper

internal enum class MonarchExerciseIcon(
    val label: String, val lightInk: Color, val darkInk: Color, val path: String
) {
    SQUAT("Sentadilla", Color(0xFF17674D), Color(0xFF77E6A9),
        "M14 3.2a1.85 1.85 0 1 0 0 3.7 1.85 1.85 0 0 0 0-3.7 " +
            "M13 8l-3.5 5.8 6.5.2-2.2 5.2h3.4 M12.4 9.5l3 1.2 3.6-1.6 M5 21h15"),
    LUNGE("Zancada", Color(0xFF526719), Color(0xFFC7E482),
        "M12 3.2a1.85 1.85 0 1 0 0 3.7 1.85 1.85 0 0 0 0-3.7 " +
            "M12 8.4v5.1l6.1.1v5.5h2 M12 13.5l-4 4.9-4-1.7 M11.9 9.5l-2.6 2.7 M3 21h18"),
    HINGE("Bisagra de cadera", Color(0xFF3D6966), Color(0xFF8BDED4),
        "M19 6a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6 " +
            "M16.1 9.3l-7.5 4.2 2.1 3.1-1.2 3.2h2.6 M14 10.6l1.4 3.7 2.2 2.1 M5 13h2m-2 0 1.3-1.3M5 13l1.3 1.3"),
    FOOT("Apoyo y trabajo de tobillo", Color(0xFF745515), Color(0xFFF6D784),
        "M9 4v7l-1.7 3.4A3 3 0 0 0 10 19h8.6a1.7 1.7 0 0 0 .4-3.4L14 13V4 " +
            "M3.5 13.8v-5m-1.6 1.6 1.6-1.6 1.6 1.6 M4 21h17"),
    PUSH("Flexión y empuje horizontal", Color(0xFF155E96), Color(0xFF83CCFF),
        "M19.7 6a2 2 0 1 0 0 4 2 2 0 0 0 0-4 " +
            "M16.4 10.3l-5.5 1.8-5.7 3.4L4 18.1 M15.6 10.7l-2.5 3.4 3 3.8H19 M2.5 20.8h19"),
    PULL("Tracción vertical", Color(0xFF593C96), Color(0xFFC4ABFF),
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
    RUN("Carrera y desplazamiento", Color(0xFF9A4B0F), Color(0xFFFFB474),
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
    ROW("Remo y tracción horizontal", Color(0xFF28567E), Color(0xFF9CC8FA),
        "M7 4a1.7 1.7 0 1 0 0 3.4A1.7 1.7 0 0 0 7 4 M7 9l2 6 5-2 3 4h3 " +
            "M7 10l5 1.5 5-2 M17 9.5h4 M3 20h18"),
    PLANK("Plancha", Color(0xFF694895), Color(0xFFCDB7FF),
        "M19.2 5.5a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6 " +
            "M16.5 10.5 4.5 14.7 3 18 M16.5 10.5l-1.1 7.5h5 M2.5 21h19"),
    CRUNCH("Flexión abdominal", Color(0xFF8A4B68), Color(0xFFFFB5CF),
        "M6.5 7a1.8 1.8 0 1 0 0 3.6A1.8 1.8 0 0 0 6.5 7 " +
            "M8.7 12l4 5.4 4-5.4 3.1 5.4h1M9.7 13.5l3.6-1.5 M3 21h18"),
    BRIDGE("Puente de glúteos", Color(0xFF79492E), Color(0xFFF6B78E),
        "M4.4 14a1.7 1.7 0 1 0 0 3.4 1.7 1.7 0 0 0 0-3.4 M7.4 17l6-6 3.3-1.1 3 8.2h1.8 " +
            "M8 17l4 1.1 M3 21h18"),
    HIP("Trabajo de cadera", Color(0xFF547020), Color(0xFFC6E88D),
        "M10 3a1.7 1.7 0 1 0 0 3.4 1.7 1.7 0 0 0 0-3.4 M10 8v5l-1.2 6.5 M10 13l5.5 2 3.1-2.5 " +
            "M9.8 9 6 12m4-3 4.5 1.5 M7 21h5"),
    KNEE("Flexión y extensión de rodilla", Color(0xFF7C5028), Color(0xFFFFC58E),
        "M9 3a1.7 1.7 0 1 0 0 3.4 1.7 1.7 0 0 0 0-3.4 M9 8v6h6l3.5 2v2h2 M9 9l3 2h3 " +
            "M6 14h5m-4 0v6m5-6v6 M16 12a4 4 0 0 1 4-1m-1.4-1.4L20 11l-1.8.7"),
    ARM("Hombros y brazos", Color(0xFF266383), Color(0xFF8BDFFC),
        "M12 3a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6 M12 8.5v8l-2.3 4m2.3-4 2.3 4 " +
            "M12 9 7.4 11 5 7m7 2 4.6 2L19 7 M3.5 6h3M17.5 6h3"),
    CARRY("Transporte de carga", Color(0xFF725530), Color(0xFFFFD7A4),
        "M11.7 3a1.7 1.7 0 1 0 0 3.4 1.7 1.7 0 0 0 0-3.4 M11.7 8v6l-3 6m3-6 4.2 5.4 " +
            "M11.7 9 8 12v2.5m3.7-5.5 4.3 3 M6.3 14.5h3.4v5H6.3z"),
    WALK("Marcha y caminata", Color(0xFF367442), Color(0xFF93EAA7),
        "M13.4 3a1.75 1.75 0 1 0 0 3.5 1.75 1.75 0 0 0 0-3.5 M12.7 8.1l-1.4 6 4.3 3.2 1.4 3 " +
            "M11.3 14.1 9.5 18 6.6 20 M12.7 8.1l3 4.2h2.7M12 8.3l-3.5 2.2-1.6 4"),
    STEP("Pasos laterales y escalón", Color(0xFF265F78), Color(0xFF8AD8EB),
        "M12.5 3a1.65 1.65 0 1 0 0 3.3 1.65 1.65 0 0 0 0-3.3 M12.5 8v5l-4 5v2m4-7 4.3.5v3 " +
            "M12.5 9l-4 1m4-1 4 1 M3 21h11v-4h7"),
    ROPE("Salto de cuerda", Color(0xFF8C416D), Color(0xFFF6A4DC),
        "M12 4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2 M12 9v5.8l-1.9 4m1.9-4 1.9 4 " +
            "M12 9 8 12H5m7-3 4 3h3 M5 12C1 16 5 22 12 22s11-6 7-10"),
    DANCE("Danza y pasos rítmicos", Color(0xFF8C4637), Color(0xFFFFA99B),
        "M9 4a1.65 1.65 0 1 0 0 3.3 1.65 1.65 0 0 0 0-3.3 M9.4 9l2.2 5.2-5 5.6 " +
            "M11.6 14.2l4.5-.3 1.8 4.3 M9.4 9 5 11l-2-2m6.4 0 3.3-2.5 1.1-3 M18 9v-5l3 1 M18 9h-1.8"),
    RACKET("Golpe con raqueta", Color(0xFF486722), Color(0xFFD4E886),
        "M9.3 14.7c-2.1-2.1-1.4-5.9 1.5-8.8s6.7-3.6 8.8-1.5 1.4 5.9-1.5 8.8-6.7 3.6-8.8 1.5z " +
            "M9.3 14.7 4 20 M11 10l3 3m-.5-5.5 3 3m-5 1 4.5-4.5"),
    GENERAL("Ejercicio", Color(0xFF4C5C76), Color(0xFFB4C5DF),
        "M10.8 3.9a1.75 1.75 0 1 0 0 3.5 1.75 1.75 0 0 0 0-3.5 M5.6 11l5.2-1.7 4.7 2.6H20 " +
            "M10.8 9.3v5.1l-3 5.3m3-5.3 4.8 5.3 M17.6 7.6h3.2")

}

private val exerciseIconRules = listOf(
    """basculacion(?: de la pelvis| pelvica)|(?:inclinacion|anteversion|retroversion|balanceo) (?:pelvic[ao]|de (?:la )?pelvis)|pelvic tilt""" to PELVIC,
    """(?:saltos?|saltar) (?:de|a la|con) (?:la )?(?:cuerda|comba)|jump rope""" to ROPE,
    """puente(?: de (?:gluteos?|cadera)| gluteo| supino| con)|hip ?thrust|glute bridge""" to BRIDGE,
    """(?:marcha|paseo|caminata) (?:del? )?(?:maleta|granjero)|farmer(?:'s)? (?:walk|carry)|suitcase(?: carry)?|transporte de carga""" to CARRY,
    """boxeo|\b(?:jab|cross|uppercut)\b|golpeo de puno|golpes ascendentes al aire|boxupper|esquiva lateral""" to BOXING,
    """pedaleo|bicicleta|ciclismo|cadencia sentado""" to CYCLE,
    """(?:derecha|reves|volea|saque) (?:de tenis|con raqueta)|raqueta|forehand|backhand|badminton|swing de tenis""" to RACKET,
    """(?:pases?|control|conduccion|recepcion|arrastre|recorte|toques?|golpeo) (?:[a-z]+ ){0,4}(?:balon|pelota)|baloncesto|drib[lb]|toques? de (?:dedos|antebrazos)|bote (?:bajo|alterno|lateral|de)|doble contacto|recorte por detras del pie|arrastre en v|arrastre hacia atras del balon|proteccion con salida orientada""" to BALL,
    """traccion horizontal|\bremo\b|(?:bent|support)?row\b|face pull""" to ROW,
    """traccion(?! diagonal de tronco)|dominadas?|jalon|pull[ -]?up|chin[ -]?up|retraccion escapular en suspension""" to PULL,
    """empuje horizontal|\bflexiones\b|flexion(?: escapular)? (?:de suelo|de brazos|en pared|inclinada|declinada|con manos|con pies|de recorrido)|closepush|push[ -]?up|kneepush|wallpush|empuje con apoyos""" to PUSH,
    """\bplancha\b|\bplank\b""" to PLANK,
    """flexion(?: de)? tronco|encogimientos? abdominal|abdominal(?:es)?|crunch|sit[ -]?up""" to CRUNCH,
    """pallof|dead bug|bird dog|hollow|estabilizacion (?:del? )?tronco|estabilidad (?:del? )?tronco|control lumbopelvico|traccion diagonal de tronco|extension alterna en suspension|extension contralateral|cuadrupedia con (?:rodillas|manos|banda)|sentado en (?:silla con pelota|fitball|banco sin respaldo|suelo con rodillas|banco con banda)""" to CORE,
    """flexion plantar|control(?: de)? tobillo|tobillo|pantorrilla|elevaciones? de talones|gemelos|dorsiflexion|elevaciones? de puntas|arco plantar|talon(?:es)? al frente|heeltoe|heeldig|movilidad de tobillos?""" to FOOT,
    """bisagra|peso muerto|deadlift|\brdl\b|good morning""" to HINGE,
    """sentadilla|squat|^rodilla$""" to SQUAT,
    """zancada|estocada|lunge""" to LUNGE,
    """subida (?:al? )?escalon|subida a plataforma|step[ -]?up|step ?jack|frontbackstep|aperturas laterales sin salto|paso de patin(?:ador|aje) sin salto|pasos? (?:adelante|atras)|pasos? (?:lateral(?:es)?|cruzad[oa]s?|en v)|side ?step|stepcross|stepreach|vstep|doubletouch""" to STEP,
    """abduccion (?:lateral|(?:de )?cadera)|aduccion (?:de )?cadera|(?:extension|flexion) (?:de )?cadera(?! sin arco)|patada (?:de |hacia |lateral)|kickback|standingextend""" to HIP,
    """(?:extension (?:terminal )?|flexion )(?:(?:de |las |de las ))?rodillas?|curl femoral|standingcurl|legextend""" to KNEE,
    """rotacion|giro de tronco|giro toracico|circulos (?:suaves )?de cadera|circunduccion de cadera|car de cadera""" to ROTATION,
    """equilibrio|\bbalance\b|tandem|linewalk|\bguerrero iii\b""" to BALANCE,
    """estiramiento|stretch|movilizacion|movilidad (?:toracica|de )|chestopen|extension toracica|extension de flexor(?:es)? de cadera|apertura (?:toracica|de pecho|de hombros)|gato[ -]vaca|inclinacion (?:lateral|toracica)|movilidad suave|respiracion|deslizamiento (?:de talon|de rodilla)|balanceo (?:de aductores|anterior de cadera)""" to MOBILITY,
    """empuje vertical|press|levantamiento|\bthruster\b|cargada(?:s)?\b|recepcion de cargada|prensa de piernas""" to PRESS,
    """flexion (?:de )?codo|extension (?:de )?codo|(?:abduccion|aduccion|extension) (?:de )?hombros?|control escapular|elevacion (?:lateral|frontal|[ytw] de hombros|en [ytw]\b)|flexion de biceps|\bcurl\b(?! femoral)|\bhammer\b|triceps|circulos de (?:hombros|brazos)|car de hombro|reverse ?fly|seatedfly|apertura de brazos boca abajo|pulsos (?:laterales|frontales) de brazos|sujecion de biceps|wallslide|hs379-scapular""" to ARM,
    """danza|baile|grapevine|paso de salsa|paso ritmico""" to DANCE,
    """salto|aterrizaje|jump|rebote|pliometr|skater|patinador""" to JUMP,
    """marcha|camina(?:ta|r)|(?:fast)?march|heelback|talones alternos hacia atras|kneeliftcardio|rodillas alternas sin salto|walk""" to WALK,
    """carrera|trote|sprint|locomocion|shuffle|jog|desplazamiento lateral|\bescalador\b|\bburpee\b|fastfeet|eliptica""" to RUN
).map { (pattern, icon) -> Regex(pattern) to icon }

internal fun monarchExerciseIcon(exercise: Exercise?): MonarchExerciseIcon {
    if (exercise == null) return MonarchExerciseIcon.GENERAL
    val name = normalize(exercise.name).substringBefore(" · ")
    val primaryName = if (name.startsWith("intervalo combinado:")) name.substringBefore(" y ") else name
    val primaryPattern = normalize(exercise.pattern).substringBefore(" y ").replace('_', ' ')

    // En una secuencia prevalece el primer movimiento, no el equipo o un gesto posterior.
    for (sequence in listOf(primaryName, primaryPattern)) {
        val movement = sequence.substringBefore(" → ").substringBefore(" + ")
        val firstMatch = exerciseIconRules.mapNotNull { (rule, icon) ->
            rule.find(movement)?.let { it.range.first to icon }
        }.minByOrNull { it.first }
        if (firstMatch != null) return firstMatch.second
    }

    return when (CategoryMapper.normalize(exercise.type)) {
        Category.UPPER -> MonarchExerciseIcon.PRESS
        Category.CENTRAL -> MonarchExerciseIcon.CORE
        Category.LOWER -> MonarchExerciseIcon.HIP
        Category.MOBILITY -> MonarchExerciseIcon.MOBILITY
        Category.CARDIO -> MonarchExerciseIcon.RUN
        null -> MonarchExerciseIcon.GENERAL
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
