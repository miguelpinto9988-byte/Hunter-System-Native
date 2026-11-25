package com.huntersystem.app

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class ExerciseClip(val label: String, val sourceId: String, val motion: ExerciseMotion)

/** Named phases in Hunter 2000 instructions predate the catalogue's structured sequence IDs. */
internal object LegacyExerciseClips {
    private val quoted = Regex("«([^»]+)»")

    fun labels(exercise: Exercise): List<String> =
        if (exercise.id.startsWith("hx_cardio_") || exercise.id.startsWith("hx_mob_"))
            quoted.findAll(exercise.raw.s("steps")).map { it.groupValues[1] }.toList()
        else emptyList()

    fun load(exercise: Exercise, load: (String) -> ExerciseMotion?): List<ExerciseClip>? {
        val labels = labels(exercise)
        if (labels.size != 2 || labels[0] == labels[1]) return null
        return labels.map { label ->
            val key = normalize(label)
            val motion = if (exercise.id.startsWith("hx_cardio_")) cardio(key, load)
                else MobilityExerciseClips.load(exercise, key)
            val resolved = motion ?: return null
            ExerciseClip(label, resolved.first, resolved.second)
        }
    }

    private fun cardio(key: String, load: (String) -> ExerciseMotion?): Pair<String, ExerciseMotion>? {
        val reference = when (key) {
            "caminata de oso corta" -> "bearcrawl"
            "paso de patinaje sin salto" -> "skater"
            "patinador con salto lateral" -> "skaterhop"
            "desplazamiento lateral rapido" -> "shuffle"
            "saltos de cuerda alternando pies" -> "new46-jumpropealt"
            else -> null
        }
        if (reference != null) return load(reference)?.let { motion ->
            reference to if (reference == "new46-jumpropealt") motion.withRope() else motion
        }
        val motion = when (key) {
            "burpee sin flexion" -> burpee()
            "boxeo al aire con jab-cross" -> boxing(Boxing.STRAIGHT)
            "boxeo al aire con uppercut" -> boxing(Boxing.UPPERCUT)
            "cambio de base de boxeo con rebote" -> boxing(Boxing.SWITCH)
            "esquiva lateral de boxeo" -> boxing(Boxing.DODGE)
            "golpes rectos al aire con desplazamiento" -> boxing(Boxing.TRAVELLING)
            "apertura de piernas con rebote bajo" -> hops(Hop.OPEN_LOW)
            "salto adelante y atras sobre linea" -> hops(Hop.FORWARD)
            "salto lateral corto a pies juntos" -> hops(Hop.SIDE)
            "salto con rodillas medias" -> hops(Hop.KNEES)
            "salto de cuerda a pies juntos" -> hops(Hop.ROPE).withRope()
            "salto estrella controlado" -> hops(Hop.STAR)
            "saltos tijera alternos" -> hops(Hop.SPLIT)
            "marcha atletica con rodillas elevadas" -> gait(Gait.MARCH)
            "marcha con brazos por encima de cabeza" -> gait(Gait.OVERHEAD)
            "carrera en el sitio con braceo" -> gait(Gait.RUN)
            "talones a gluteos en carrera" -> gait(Gait.HEEL)
            "rodillas altas con giro leve de tronco" -> gait(Gait.TWIST)
            "escalador de pie con brazos alternos" -> gait(Gait.CLIMB)
            "rodilla a codo alterna de pie" -> gait(Gait.CROSS)
            "skips en desplazamiento corto" -> gait(Gait.SKIP)
            "carrera adelante y atras en dos pasos" -> gait(Gait.SHUTTLE)
            "carrera zigzag de corto alcance" -> steps(Steps.ZIGZAG)
            "desplazamiento con pasos de esgrima" -> steps(Steps.FENCE)
            "dos pasos laterales y retroceso" -> steps(Steps.DOUBLE)
            "paso adelante y toque de talon" -> steps(Steps.HEEL)
            "paso atras con alcance frontal" -> steps(Steps.BACK_REACH)
            "paso cruzado detras y salida lateral" -> steps(Steps.CROSS_BEHIND)
            "paso cruzado lateral carioca" -> steps(Steps.CARIOCA)
            "paso de baile grapevine" -> steps(Steps.GRAPEVINE)
            "paso de escalera lateral imaginaria" -> steps(Steps.LADDER)
            "paso lateral en v" -> steps(Steps.V)
            "paso rapido sobre linea imaginaria" -> steps(Steps.LINE)
            "shuffle diagonal rapido" -> steps(Steps.DIAGONAL)
            "step jack con brazos en arco" -> steps(Steps.ARC)
            else -> return null
        }
        return slug(key) to motion
    }

    private fun ExerciseMotion.withRope() = copy(
        apparatus = listOf(DemoApparatus(DemoGear.JUMP_ROPE, GearMount.BOTH, DemoGear.JUMP_ROPE.label))
    )

    internal fun slug(text: String) = "gesture-" + text.replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private enum class Boxing { STRAIGHT, UPPERCUT, SWITCH, DODGE, TRAVELLING }
    private enum class Hop { OPEN_LOW, FORWARD, SIDE, KNEES, ROPE, STAR, SPLIT }
    private enum class Gait { MARCH, OVERHEAD, RUN, HEEL, TWIST, CLIMB, CROSS, SKIP, SHUTTLE }
    private enum class Steps { ZIGZAG, FENCE, DOUBLE, HEEL, BACK_REACH, CROSS_BEHIND, CARIOCA, GRAPEVINE, LADDER, V, LINE, DIAGONAL, ARC }

    private fun boxing(kind: Boxing): ExerciseMotion {
        val side = kind in setOf(Boxing.UPPERCUT, Boxing.SWITCH, Boxing.STRAIGHT, Boxing.TRAVELLING)
        return clip(2.2f, if (side) ExerciseView.SIDE else ExerciseView.FRONT) { phase ->
            val wave = cycleSin(phase)
            val dodge = if (kind == Boxing.DODGE) wave else 0f
            val travel = if (kind == Boxing.TRAVELLING) wave * 52f else 0f
            val bounce = if (kind == Boxing.SWITCH) pulse(phase * 2f) * 19f else 0f
            val h = Offset(travel+dodge*30f, -bounce+abs(dodge)*24f)
            val s = h + Offset(dodge*30f, -145f)
            val motion = when (kind) {
                Boxing.STRAIGHT, Boxing.UPPERCUT, Boxing.TRAVELLING -> wave
                else -> 0f
            }
            fun grip(near: Boolean): Offset {
                val ext = pulse((phase + if (near) 0f else .5f) * 2f) *
                    if (motion * (if (near) 1f else -1f) > 0f) abs(wave) else 0f
                if (kind == Boxing.UPPERCUT) return s + Offset(53f+62f*ext, -27f-86f*ext)
                if (side) return s + Offset(if (near) 60f+103f*ext else 43f+124f*ext,
                    if (near) -30f-2f*ext else -18f-5f*ext)
                val sign = if (near) -1f else 1f
                return s + Offset(sign * (32f + 101f * ext), -38f + 28f*ext)
            }
            val footShift = if (kind == Boxing.SWITCH) wave * 49f else travel*.74f
            athlete(s,h,grip(true),grip(false),Offset(-50f+footShift,249f-bounce),
                Offset(50f + if (kind == Boxing.SWITCH) -footShift else footShift,249f-bounce),
                if (side) -1f else 1f, if(side) -1f else 1f, frontal=!side)
        }
    }

    private fun hops(kind: Hop): ExerciseMotion {
        val side = kind in setOf(Hop.FORWARD, Hop.KNEES, Hop.SPLIT)
        return clip(2f, if (side) ExerciseView.SIDE else ExerciseView.FRONT) { phase ->
            val flight = pulse(phase * 2f)
            val alternating = cycleCos(phase)
            val open = pulse(phase)
            val travel = if (kind in setOf(Hop.FORWARD, Hop.SIDE)) alternating * 52f else 0f
            val height = when (kind) {
                Hop.KNEES, Hop.STAR -> 65f
                Hop.ROPE, Hop.OPEN_LOW -> 23f
                else -> 39f
            }
            val h = Offset(travel,-height * flight + 13f * (1f-flight))
            val s = h + Offset(if (side) 15f else 0f,-147f)
            val width = when (kind) {
                Hop.STAR -> 33f + 80f*flight
                Hop.OPEN_LOW -> 32f + 53f*open
                else -> 24f
            }
            val split = if (kind == Hop.SPLIT) alternating * 76f else 0f
            val tuck = if (kind == Hop.KNEES) flight*79f else 0f
            val feet = Offset(travel + tuck*.36f, 248f-height*flight-tuck)
            val wristSpread = when (kind) {
                Hop.STAR -> 47f + 111f*flight
                Hop.OPEN_LOW -> 44f + 25f*open
                else -> 68f
            }
            val ra = s + Offset(wristSpread, if (kind == Hop.STAR) 135f-180f*flight else 139f)
            val la = s + Offset(-wristSpread, if (kind == Hop.STAR) 135f-180f*flight else 143f)
            athlete(s,h,la,ra,feet+Offset(-width+split,0f),feet+Offset(width-split,0f),
                if (side) -1f else 1f, if(side) -1f else 1f, frontal=!side)
        }
    }

    private fun gait(kind: Gait): ExerciseMotion {
        val front = kind in setOf(Gait.TWIST,Gait.CROSS)
        return clip(if (kind in setOf(Gait.MARCH,Gait.OVERHEAD,Gait.CROSS)) 2.4f else 1.65f,
            if (front) ExerciseView.FRONT else ExerciseView.SIDE) { phase ->
            val stride = cycleSin(phase)
            val strideX = cycleCos(phase)
            val travel = if (kind==Gait.SHUTTLE || kind==Gait.SKIP) 78f * cycleCos(phase) else 0f
            val flight = when (kind) {
                Gait.RUN,Gait.HEEL,Gait.TWIST,Gait.SHUTTLE,Gait.SKIP -> 19f * pulse(phase*2f)
                else -> 0f
            }
            val h = Offset(travel,-flight+5f)
            val s = h + Offset(if (front) stride*11f else 12f,-146f)
            val lift = when (kind) {
                Gait.MARCH,Gait.CLIMB,Gait.CROSS,Gait.TWIST,Gait.HEEL -> 100f
                Gait.OVERHEAD,Gait.SKIP -> 73f
                else -> 53f
            }
            fun foot(near: Boolean): Offset {
                val direction = if (near) 1f else -1f
                val height = maxOf(0f, direction*stride)*lift
                val x = when {
                    front -> direction*37f
                    kind == Gait.HEEL -> -height*.62f + direction*strideX*24f
                    else -> direction*strideX*48f + height*.26f
                }
                return Offset(h.x+x,246f-flight-height)
            }
            fun wrist(near: Boolean): Offset {
                val direction = if (near) 1f else -1f
                return s + when (kind) {
                    Gait.OVERHEAD -> Offset(direction*37f,-147f+direction*stride*7f)
                    Gait.CLIMB -> Offset(47f+direction*stride*19f,34f+direction*stride*143f)
                    Gait.CROSS -> Offset(direction*48f+stride*65f,41f+direction*stride*43f)
                    Gait.TWIST -> Offset(direction*47f + stride*35f, 65f+direction*stride*25f)
                    else -> Offset(direction*stride*72f, 105f+direction*stride*39f)
                }
            }
            athlete(s,h,wrist(true),wrist(false),foot(true),foot(false),1f,
                if (front) 1f else -1f, frontal=front)
        }
    }

    private fun steps(kind: Steps): ExerciseMotion {
        val side = kind in setOf(Steps.FENCE,Steps.HEEL,Steps.BACK_REACH)
        return clip(if (kind in setOf(Steps.DIAGONAL, Steps.LADDER, Steps.ZIGZAG)) 2.1f else 3.2f,
            if (side) ExerciseView.SIDE else ExerciseView.FRONT) { phase ->
            val sway = cycleCos(phase)
            val step = cycleSin(phase*if(kind == Steps.ZIGZAG) 3f else 2f)
            val open = pulse(phase)
            val travel = when (kind) {
                Steps.ARC, Steps.LINE -> 8f
                Steps.HEEL, Steps.BACK_REACH, Steps.FENCE -> 46f
                else -> 91f
            } * sway
            val h = Offset(travel,8f+if(kind == Steps.FENCE) 28f*open else 5f*abs(step))
            val s = h + Offset(if(side) 13f else step*9f,-147f)
            val cross = when(kind) {
                Steps.CARIOCA -> 148f
                Steps.GRAPEVINE -> 133f
                Steps.CROSS_BEHIND -> 125f
                else -> 0f
            }
            val crossA = maxOf(0f,step)*cross
            val crossB = maxOf(0f,-step)*cross
            val behindA = if (kind!=Steps.CARIOCA || phase < .5f) 8f else -8f
            val crossDepthA = if (cross == 0f) 0f else behindA*maxOf(0f,step)
            val crossDepthB = if (cross == 0f) 0f else -behindA*maxOf(0f,-step)
            val width = when (kind) {
                Steps.V, Steps.ARC -> 29f + 74f * open
                Steps.FENCE -> 65f + 35f * open
                else -> 41f + 27f * pulse(phase*2f)
            }
            val depth = when (kind) {
                Steps.DOUBLE -> 10f * pulse(phase)
                Steps.DIAGONAL, Steps.ZIGZAG, Steps.V -> 19f * sway
                else -> 0f
            }
            val heel = if(kind==Steps.HEEL) 33f*open else 0f
            val quickStep = when (kind) {
                Steps.DOUBLE -> 16f*cycleSin(phase*4f)
                Steps.LINE -> 23f*step
                Steps.LADDER -> 27f*cycleCos(phase*4f)
                else -> 0f
            }
            val left = Offset(travel-width+crossA+quickStep,246f+depth+crossDepthA-maxOf(0f,step)*30f)
            val right = Offset(travel+width-crossB+heel+quickStep,246f-depth+crossDepthB-maxOf(0f,-step)*30f)
            val a = s + when (kind) {
                Steps.ARC -> Offset(-46f-112f*open,137f-235f*open)
                Steps.BACK_REACH -> Offset(42f+108f*open,129f-116f*open)
                Steps.FENCE -> Offset(111f,24f)
                else -> Offset(-47f+step*34f,123f)
            }
            val b = s + when (kind) {
                Steps.ARC -> Offset(46f+112f*open,137f-235f*open)
                Steps.BACK_REACH -> Offset(37f+109f*open,134f-116f*open)
                Steps.FENCE -> Offset(-61f,-14f)
                else -> Offset(47f-step*34f,123f)
            }
            athlete(s,h,a,b,left,right,if(side) -1f else 1f,if(side) -1f else 1f,frontal=!side)
        }.copy(footGesture=if(kind==Steps.HEEL) FootGesture.TOE_ALTERNATE else null)
    }

    private fun burpee(): ExerciseMotion {
        fun position(shoulder: Offset, hip: Offset, hand: Offset, ankle: Offset) =
            athlete(shoulder,hip,hand,hand+Offset(-16f,-10f),ankle,ankle+Offset(-16f,-10f),-1f,1f,
                forward = -1f)
        val upright = position(Offset(20f,-145f),Offset.Zero,Offset(16f,3f),Offset(-20f,246f))
        val crouch = position(Offset(-93f,42f),Offset(34f,124f),Offset(-112f,244f),Offset(-18f,246f))
        val plank = position(Offset(-103f,57f),Offset(37f,107f),Offset(-112f,244f),Offset(239f,246f))
        val reach = position(Offset(19f,-164f),Offset(0f,-20f),Offset(11f,-349f),Offset(-20f,223f))
        return ExerciseMotion(
            listOf(upright,crouch,plank,plank,crouch,upright,reach,upright),
            4.2f,listOf(0f,.16f,.32f,.45f,.61f,.76f,.86f,.96f),"authored-phase","left",view=ExerciseView.SIDE,floorTransition=true)
    }

    internal fun clip(duration: Float, view: ExerciseView, posture: ExercisePosture = ExercisePosture.UPRIGHT,
                      handSupport: Boolean = false, pose: (Float) -> List<Offset>): ExerciseMotion {
        val times = List(24) { it/24f }
        return ExerciseMotion(times.map(pose),duration,times,"authored-phase","right",
            view=view,posture=posture,prone=posture==ExercisePosture.PRONE,handSupport=handSupport)
    }

    internal fun athlete(s: Offset, h: Offset, a: Offset, b: Offset, l: Offset, r: Offset,
                         armBend: Float, legBend: Float, forward: Float = 1f,
                         frontal: Boolean = false): List<Offset> {
        val neck = (s-h).unit()*49f + Offset(8f*forward,0f)
        val armSide = if (frontal && a.x > b.x) -armBend else armBend
        val legSide = if (frontal && l.x > r.x) -legBend else legBend
        val frontWidth = if (frontal) .42f else 1f
        val (elbowA, wristA) = joint(s,a,102f,91f,armSide)
        val (elbowB, wristB) = joint(s,b,102f,91f,if(frontal) -armSide else armSide)
        val (kneeA, ankleA) = joint(h,l,137f,122f,legSide*frontWidth)
        val (kneeB, ankleB) = joint(h,r,137f,122f,(if(frontal) -legSide else legSide)*frontWidth)
        return listOf(s+neck,s,h,elbowA,wristA,elbowB,wristB,kneeA,ankleA,kneeB,ankleB)
    }

    private fun joint(root: Offset, end: Offset, upper: Float, lower: Float, bend: Float): Pair<Offset,Offset> {
        val vector = end-root
        val axis = vector.unit()
        val distance = vector.getDistance().coerceIn(abs(upper-lower)+1f,upper+lower-1f)
        val along = (upper*upper-lower*lower+distance*distance)/(2f*distance)
        val lift = sqrt((upper*upper-along*along).coerceAtLeast(0f))
        return root+axis*along+Offset(-axis.y,axis.x)*(bend*lift) to root+axis*distance
    }

    private fun Offset.unit(): Offset = if (getDistance() > .001f) this / getDistance() else Offset(1f,0f)
    internal fun cycleSin(phase: Float) = sin(phase*2f*PI.toFloat())
    internal fun cycleCos(phase: Float) = cos(phase*2f*PI.toFloat())
    internal fun pulse(phase: Float) = (1f-cycleCos(phase))*.5f
}
