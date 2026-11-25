package com.huntersystem.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.huntersystem.app.LegacyExerciseClips.athlete
import com.huntersystem.app.LegacyExerciseClips.clip
import com.huntersystem.app.LegacyExerciseClips.cycleSin
import com.huntersystem.app.LegacyExerciseClips.pulse
import kotlin.math.abs

/** A setup and its movement modifier describe one exercise, with continuous supports. */
internal object CoreExerciseClips {
    private val description = Regex("Adopta la posición «([^»]+)».*?ejecuta (.*?) sin perder")
    private val end = Offset(0f,185f)

    private val inconsistentActions = setOf("pase de balon entre manos", "inclinacion lateral controlada sin carga")

    fun hasInconsistentInstructions(exercise: Exercise): Boolean {
        if (!exercise.id.startsWith("hx_core_")) return false
        val (setup, action) = description.find(exercise.raw.s("steps"))?.destructured ?: return false
        return setup.startsWith("Pallof press", ignoreCase=true) && normalize(action) in inconsistentActions
    }

    fun load(exercise: Exercise): ExerciseMotion? {
        if (!exercise.id.startsWith("hx_core_")) return null
        val (setup, action) = description.find(exercise.raw.s("steps"))?.destructured ?: return null
        val position = normalize(setup)
        val move = normalize(action)
        return when {
            position.startsWith("plancha alta") -> highPlank(position, move)
            position.startsWith("bird dog") || position.startsWith("cuadrupedia") -> quadruped(position,move)
            position.startsWith("dead bug") || position.startsWith("hollow hold") -> lying(position,move)
            position.startsWith("sentado") -> seated(position,move)
            position.startsWith("pallof press") || position.startsWith("marcha de maleta") -> standing(position,move)
            else -> null
        }
    }

    private fun highPlank(setup: String, move: String): ExerciseMotion {
        val high = "manos elevadas en step" in setup
        val handles = "sobre agarres" in setup
        val footStep = move == "alternancia de apoyo de pies en escalon"
        val feetWide = if ("separados" in setup) 88f else 48f
        val transverse = move in setOf("apertura alterna de pierna", "arrastre de disco hacia el costado",
            "desplazamiento lateral de ambas manos", "flexion de rodilla hacia codo contrario",
            "pase de pelota pequena bajo el torso","rotacion controlada de pelvis","toque de tobillo contrario")
        val supportHeight = when {
            high -> 44f
            handles -> 34f
            else -> 0f
        }
        val motion = clip(4.6f,if(transverse) ExerciseView.OVERHEAD else ExerciseView.SIDE,
            ExercisePosture.PRONE,handSupport=true) { t ->
            val w = cycleSin(t)
            val a = maxOf(w,0f)
            val b = maxOf(-w,0f)
            if(transverse) {
                var s = Offset(-75f,0f)
                var h = Offset(75f,0f)
                var handA = Offset(-82f,-96f)
                var handB = Offset(-82f,96f)
                var footA = Offset(310f,-feetWide)
                var footB = Offset(310f,feetWide)
                when(move) {
                    "apertura alterna de pierna" -> {
                        footA += Offset(-30f*a,-91f*a)
                        footB += Offset(-30f*b,91f*b)
                    }
                    "arrastre de disco hacia el costado", "pase de pelota pequena bajo el torso" -> {
                        handA += Offset(66f*a,157f*a)
                        handB += Offset(66f*b,-157f*b)
                    }
                    "desplazamiento lateral de ambas manos" -> {
                        s += Offset(0f,31f*w)
                        handA += Offset(0f,49f*w)
                        handB += Offset(0f,49f*w)
                    }
                    "flexion de rodilla hacia codo contrario" -> {
                        footA += Offset(-205f*a,95f*a)
                        footB += Offset(-205f*b,-95f*b)
                    }
                    "rotacion controlada de pelvis" -> h += Offset(0f,40f*w)
                    "toque de tobillo contrario" -> {
                        handA += Offset(219f*a,164f*a)
                        handB += Offset(219f*b,-164f*b)
                        h += Offset(68f*abs(w),0f)
                        s += Offset(86f*abs(w),0f)
                    }
                }
                athlete(s,h,handA,handB,footA,footB,1f,1f,forward=-1f)
            } else {
                val floor = end.y
                val planted = Offset(-77f,floor-supportHeight)
                var handA = planted
                var handB = planted+Offset(16f,-9f)
                var footA = Offset(309f,floor)
                var footB = Offset(293f,floor-7f)
                when(move) {
                    "alcance frontal alterno de mano" -> {
                        handA += Offset(-163f*a,-170f*a)
                        handB += Offset(-163f*b,-170f*b)
                    }
                    "alternancia de apoyo de pies en escalon" -> {
                        footA += Offset(-32f*a,-58f*a)
                        footB += Offset(-32f*b,-58f*b)
                    }
                    "deslizamiento de un antebrazo al frente" -> {
                        handA += Offset(-100f*a,0f)
                        handB += Offset(-100f*b,0f)
                    }
                    "flexion de rodilla hacia codo del mismo lado" -> {
                        footA += Offset(-208f*a,-53f*a)
                        footB += Offset(-208f*b,-53f*b)
                    }
                    "traccion de banda con una mano" -> handA += Offset(108f*a,-110f*a)
                }
                val s = Offset(-75f,-supportHeight)
                athlete(s,Offset(74f,41f-supportHeight*.55f),handA,handB,footA,footB,1f,1f,forward=-1f)
            }
        }.copy(facing="left")
        val props = if (transverse) emptyList() else buildList {
            if(high) add(ExerciseProp("step",Rect(-139f,142f,-17f,194f)))
            if(footStep) add(ExerciseProp("step",Rect(251f,128f,324f,194f)))
        }
        return motion.copy(props=props)
    }

    private fun quadruped(setup: String, move: String): ExerciseMotion {
        val high = "elevadas en step" in setup
        val hover = "rodillas elevadas" in setup
        val overhead = move in setOf("desplazamiento de rodillas hacia un costado","elevacion lateral de pierna",
            "rotacion toracica con brazo libre","alcance diagonal de brazo")
        val motion = clip(4.8f,if(overhead) ExerciseView.OVERHEAD else ExerciseView.SIDE,
            ExercisePosture.PRONE,handSupport=true) { t ->
            val w = cycleSin(t)
            val p = pulse(t)
            val a = maxOf(w,0f)
            val b = maxOf(-w,0f)
            val shoulderHeight = if (high && !overhead) -34f else 0f
            val scapularTravel = if (move == "separacion escapular controlada") 24f*p else 0f
            val s = Offset(-74f+if(move == "desplazamiento corto adelante y atras") 32f*w else 0f,
                shoulderHeight+scapularTravel)
            val h = Offset(74f+if(move=="desplazamiento corto adelante y atras") 32f*w else 0f,
                if(move=="desplazamiento de rodillas hacia un costado") 28f*w else 0f)
            if(overhead) {
                val arm = move in setOf("rotacion toracica con brazo libre","alcance diagonal de brazo")
                val leg = move == "elevacion lateral de pierna"
                val crawl = if(move=="desplazamiento de rodillas hacia un costado") 53f*w else 0f
                val reach = if(arm) if(move=="alcance diagonal de brazo") 134f else 102f else 0f
                val handA = Offset(-83f-reach*a,-87f-67f*a*(if(arm) 1f else 0f))
                val handB = Offset(-83f-reach*b,87f+67f*b*(if(arm) 1f else 0f))
                val selection = athlete(s,h,handA,handB,Offset(178f+if(leg) 76f*a else 0f,-59f+crawl-if(leg) 116f*a else 0f),
                    Offset(178f+if(leg) 76f*b else 0f,59f+crawl+if(leg) 116f*b else 0f),1f,1f,forward=-1f)
                selection.toMutableList().apply {
                    if(!leg) {
                        this[7]=Offset(91f,-63f+crawl)
                        this[9]=Offset(91f,63f+crawl)
                    }
                }
            } else {
                val plant = Offset(-76f,end.y-if(high) 44f else 0f)
                var armA = plant
                var armB = plant + Offset(17f,-8f)
                var footA = Offset(169f,185f)
                var footB = Offset(153f,179f)
                var kneeA = Offset(77f,if(hover) 152f else 176f)
                var kneeB = Offset(90f,kneeA.y-8f)
                when(move) {
                    "deslizamiento frontal de mano" -> {
                        armA += Offset(-111f*a,0f)
                        armB += Offset(-111f*b,0f)
                    }
                    "extension de pierna hacia atras" -> {
                        footA += Offset(138f*a,-180f*a)
                        footB += Offset(138f*b,-172f*b)
                        kneeA += Offset(114f*a,-171f*a)
                        kneeB += Offset(114f*b,-165f*b)
                    }
                    "presion isometrica de mano contra rodilla", "traccion de rodilla hacia codo" -> {
                        kneeA += Offset(-80f*a,-97f*a)
                        kneeB += Offset(-80f*b,-97f*b)
                        footA += Offset(-81f*a,-78f*a)
                        footB += Offset(-81f*b,-78f*b)
                        if(move=="presion isometrica de mano contra rodilla") {
                            armB += Offset(74f*a,-79f*a)
                            armA += Offset(74f*b,-79f*b)
                        }
                    }
                    "toque alterno del hombro opuesto" -> {
                        armA += Offset(49f*a,-146f*a)
                        armB += Offset(49f*b,-146f*b)
                    }
                }
                athlete(s,h,armA,armB,footA,footB,1f,-1f,forward=-1f).toMutableList().apply {
                    this[7]=kneeA
                    this[9]=kneeB
                }
            }
        }.copy(facing="left")
        return if (high && !overhead) motion.copy(props=listOf(ExerciseProp("step",Rect(-137f,141f,-15f,195f))))
            else motion
    }

    private fun lying(setup: String, move: String): ExerciseMotion {
        val hollow = setup.startsWith("hollow")
        val oneOut = "una pierna extendida" in setup
        val holdBall = "balon sostenido" in setup
        val transverse = move in setOf("abduccion alterna de cadera con minibanda","alcance lateral de mano hacia el tobillo",
            "desplazamiento lateral de pelvis","rotacion corta controlada de tronco","traccion diagonal corta de banda")
        return clip(5.2f,if(transverse) ExerciseView.OVERHEAD else ExerciseView.SIDE,ExercisePosture.SUPINE) { t ->
            val w = cycleSin(t)
            val p = pulse(t)
            val a = maxOf(w,0f)
            val b = maxOf(-w,0f)
            val s = Offset(-72f,if (hollow && !transverse) -42f else 0f)
            var h = Offset(73f,0f)
            if(transverse) {
                var handA = Offset(-16f,-74f)
                var handB = Offset(-16f,74f)
                var footA = Offset(if(oneOut) 289f else 179f,-43f)
                var footB = Offset(179f,43f)
                var shoulder = s
                when(move) {
                    "abduccion alterna de cadera con minibanda" -> {
                        footA += Offset(-15f*a,-63f*a)
                        footB += Offset(-15f*b,63f*b)
                    }
                    "alcance lateral de mano hacia el tobillo" -> {
                        handA += Offset(117f*a,49f*a)
                        handB += Offset(117f*b,-49f*b)
                    }
                    "desplazamiento lateral de pelvis" -> h += Offset(0f,33f*w)
                    "rotacion corta controlada de tronco" -> shoulder += Offset(0f,34f*w)
                    "traccion diagonal corta de banda" -> handA += Offset(133f*p,-63f*p)
                }
                athlete(shoulder,h,handA,handB,footA,footB,1f,-1f,forward=-1f)
            } else {
                val supportY = if(hollow) -62f else -54f
                var footA = Offset(if(oneOut) 298f else 196f, supportY)
                var footB = Offset(183f,supportY+10f)
                var handA = s + Offset(if(hollow) 142f else if(holdBall) 35f else 8f,if(hollow) 21f else -167f)
                var handB = handA + Offset(-13f,9f)
                when(move) {
                    "aproximacion alterna de rodilla al pecho" -> {
                        footA += Offset(-82f*a,-18f*a)
                        footB += Offset(-82f*b,-18f*b)
                    }
                    "descenso unilateral lento de pierna" -> footA += Offset(117f*p,46f*p)
                    "deslizamiento simultaneo de ambos talones" -> {
                        footA=Offset(174f+136f*p,13f)
                        footB=footA+Offset(-13f,7f)
                    }
                    "elevacion alterna de brazo hacia el techo" -> {
                        handA=s+Offset(132f-120f*a,14f-178f*a)
                        handB=s+Offset(115f-120f*b,20f-178f*b)
                    }
                    "extension diagonal contralateral" -> {
                        handA+=Offset(-130f*a,151f*a)
                        handB+=Offset(-130f*b,151f*b)
                        footB+=Offset(124f*a,50f*a)
                        footA+=Offset(124f*b,50f*b)
                    }
                    "presion alterna de mano contra rodilla" -> {
                        handA+=Offset(128f*a,53f*a)
                        handB+=Offset(128f*b,53f*b)
                        footA+=Offset(-39f*a,0f)
                        footB+=Offset(-39f*b,0f)
                    }
                    "presion de balon entre rodillas" -> {
                        footA += Offset(-7f*p,0f)
                        footB += Offset(7f*p,0f)
                    }
                }
                athlete(s,h,handA,handB,footA,footB,-1f,-1f,forward=-1f)
            }
        }.copy(facing="left")
    }

    private fun seated(setup: String, move: String): ExerciseMotion {
        val floor = "en suelo" in setup
        val front = move in setOf("desplazamiento del balon alrededor de tronco","inclinacion lateral y retorno",
            "paso lateral de un pie","presion de balon entre rodillas","rotacion corta de pecho contra banda")
        return clip(4.8f,if(front) ExerciseView.FRONT else ExerciseView.SIDE) { t ->
            val w = cycleSin(t)
            val a = maxOf(w,0f)
            val b = maxOf(-w,0f)
            val p = pulse(t)
            val h = Offset.Zero
            val s = h+Offset(when(move) {
                "inclinacion lateral y retorno" -> 47f*w
                "rotacion corta de pecho contra banda" -> 22f*w
                else -> 8f
            },-147f)
            val legBase = if(floor) 25f else 138f
            var footA = if(front) Offset(-67f,legBase) else Offset(if(floor) 180f else 138f,legBase)
            var footB = if(front) Offset(67f,legBase) else footA+Offset(-22f,-7f)
            var handA = s+Offset(if(front) -39f else 62f,135f)
            var handB = s+Offset(43f,131f)
            when(move) {
                "alcance diagonal de una mano" -> handA += Offset(91f*p,-162f*p)
                "deslizamiento alterno de talones" -> {
                    footA += Offset(89f*a,0f)
                    footB += Offset(89f*b,0f)
                }
                "desplazamiento del balon alrededor de tronco" -> {
                    handA += Offset(114f*w,-56f*p)
                    handB += Offset(114f*w,-56f*p)
                }
                "elevacion alterna de rodilla", "elevacion contralateral de brazo y pierna",
                "flexion de rodilla y alcance contrario" -> {
                    footA += Offset(38f*a,-53f*a)
                    footB += Offset(38f*b,-53f*b)
                    if(move!="elevacion alterna de rodilla") {
                        handA += Offset(94f*b,-142f*b)
                        handB += Offset(94f*a,-142f*a)
                    }
                }
                "paso lateral de un pie" -> footA += Offset(-107f*p,-22f*pulse(t*2f))
                "presion de balon entre rodillas" -> {
                    footA += Offset(12f*p,0f)
                    footB += Offset(-12f*p,0f)
                }
                "rotacion corta de pecho contra banda", "traccion diagonal de banda" -> {
                    handA += Offset(70f*w,-109f*p)
                    handB += Offset(70f*w,-109f*p)
                }
            }
            athlete(s,h,handA,handB,footA,footB,1f,if(front) 1f else -1f,frontal=front)
        }
    }

    private fun standing(setup: String, move: String): ExerciseMotion {
        val suitcase = setup.startsWith("marcha de maleta")
        val stagger = "escalonada" in setup
        val high = "brazo alto" in setup
        val side = move == "cambio de apoyo adelante-atras"
        return clip(4.6f,if(side) ExerciseView.SIDE else ExerciseView.FRONT) { t ->
            val w = cycleSin(t)
            val p = pulse(t)
            val nearLift = maxOf(w,0f)
            val farLift = maxOf(-w,0f)
            val shift = if(move in setOf("paso lateral corto y retorno","cambio de apoyo adelante-atras")) 47f*w else 0f
            val h = Offset(shift,0f)
            val s = h + Offset(if(move=="rotacion toracica hacia el lado libre") 20f*w else 0f,-147f)
            val base = if (high) -57f else 57f
            var handA = s+Offset(-12f,base)
            var handB = s+Offset(12f,base)
            if(suitcase) handA=s+Offset(-57f,158f)
            var footA = Offset(-47f+shift,248f)
            var footB = Offset(47f+shift,if(stagger) 234f else 248f)
            val reach = when(move) {
                "alcance lateral a la altura del hombro" -> Offset(117f*p,-55f*p)
                "alcance por encima de la cabeza" -> Offset(20f*p,-184f*p)
                "cruce de mano a rodilla opuesta" -> Offset(-65f*w,105f*abs(w))
                "desplazamiento diagonal de brazos" -> Offset(118f*w,-122f*p)
                "extension corta de brazo contra banda" -> Offset(45f*p,-25f*p)
                "inclinacion lateral controlada sin carga" -> Offset(48f*w,0f)
                "rotacion toracica hacia el lado libre" -> Offset(54f*w,-17f*p)
                "pase de balon entre manos" -> Offset(72f*w,0f)
                else -> Offset(22f*p,-29f*p)
            }
            if(!suitcase) handA+=reach
            handB+=reach
            if(suitcase || move in setOf("elevacion alterna de rodilla","flexion de cadera alterna","cruce de mano a rodilla opuesta")) {
                footA += Offset(0f,-90f*nearLift)
                footB += Offset(0f,-90f*farLift)
            }
            if(shift != 0f) {
                footA += Offset(-25f*nearLift,-27f*nearLift)
                footB += Offset(25f*farLift,-27f*farLift)
            }
            athlete(s,h,handA,handB,footA,footB,1f,if(side) -1f else 1f,frontal=!side)
        }
    }
}
