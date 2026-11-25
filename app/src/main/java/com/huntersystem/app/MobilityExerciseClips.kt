package com.huntersystem.app

import androidx.compose.ui.geometry.Offset
import com.huntersystem.app.LegacyExerciseClips.athlete
import com.huntersystem.app.LegacyExerciseClips.clip
import com.huntersystem.app.LegacyExerciseClips.cycleCos
import com.huntersystem.app.LegacyExerciseClips.cycleSin
import com.huntersystem.app.LegacyExerciseClips.pulse
import kotlin.math.abs

/** Each phase keeps the supports indicated for its own exercise. */
internal object MobilityExerciseClips {
    private val position = Regex("Desde la posición ([^,]+),")

    fun load(exercise: Exercise, gesture: String): Pair<String, ExerciseMotion>? {
        val posture = position.find(exercise.raw.s("steps"))?.groupValues?.get(1)?.let(::normalize)
        val chair = "chair" in exercise.raw.a("materialRequirements").strings()
        val motion = when (posture) {
            "bipedestacion" -> standing(gesture)
            "sentado" -> seated(gesture)?.let { if (chair) it.copy(
                apparatus = listOf(DemoApparatus(DemoGear.CHAIR))) else it }
            "cuadrupedia" -> quadruped(gesture)
            "decubito" -> lying(gesture)
            "medio arrodillado" -> kneeling(gesture)
            else -> null
        } ?: return null
        val source = LegacyExerciseClips.slug("$posture-$gesture${if (chair) "-con-silla" else ""}")
        return source to motion
    }

    private enum class Move {
        SCAPULA, ARM_FORWARD, ANKLE_ROCK, HINGE, WRIST_CIRCLE, HIP_SIDE, MARCH, HEELS_UP, ARM_BACK,
        KNEE_BEND, ELBOW, LATERAL, ANKLE_KNEE, HIP_FIXED, ROTATE_CROSSED, WEIGHT, REACH,
        ARM_OPEN, PELVIS, PELVIS_ANTERIOR, SHOULDER_CIRCLE, SLIDE_FEET, TOES, KNEE_EXTEND, BACKREST,
        WRIST_FLEX, HEAD_TURN, ROTATE, KNEE_OPEN, PALMS,
        SHOULDER_TAP, HIP_CIRCLE, HAND_SLIDE, WRIST_SHIFT, KICK, FINGERS, CAT_COW,
        PELVIS_FORWARD, DIAGONAL, PEAK, ROCK_BACK, HIP_ABDUCT, ARM_OVERHEAD, ANKLE_CIRCLE,
        HEEL_SLIDE, KNEE_SMALL, ANKLE_FLEX, SHOULDER, BREATH, HIP_ROTATE, LUMBAR_ROTATE,
        ARMS_SIDE, OPPOSITE_ARM, ARM_DIAGONAL, ARM_SIDE, FLEXOR, REAR_FOOT, PELVIS_BACK,
        SHORT_ROCK, ANKLE_ACTIVE
    }

    private fun standing(key: String): ExerciseMotion? {
        val move = when (key) {
            "apertura y cierre escapular" -> Move.SCAPULA
            "balanceo de brazo al frente" -> Move.ARM_FORWARD
            "balanceo de tobillo adelante y atras" -> Move.ANKLE_ROCK
            "bisagra de cadera sin carga" -> Move.HINGE
            "circulo pequeno de munecas" -> Move.WRIST_CIRCLE
            "desplazamiento lateral de pelvis" -> Move.HIP_SIDE
            "elevacion alterna de rodilla" -> Move.MARCH
            "elevacion controlada de talones" -> Move.HEELS_UP
            "extension suave de brazo hacia atras" -> Move.ARM_BACK
            "flexion y extension alterna de rodillas" -> Move.KNEE_BEND
            "flexion y extension suave de codo" -> Move.ELBOW
            "inclinacion lateral corta de tronco" -> Move.LATERAL
            "movilizacion de dorsiflexion de tobillo" -> Move.ANKLE_KNEE
            "rotacion de cadera con pie fijo" -> Move.HIP_FIXED
            "rotacion toracica de brazos cruzados" -> Move.ROTATE_CROSSED
            "traslado del peso de talon a punta" -> Move.WEIGHT
            else -> return null
        }
        val front = move in setOf(Move.SCAPULA,Move.WRIST_CIRCLE,Move.HIP_SIDE,Move.LATERAL,Move.ROTATE_CROSSED)
        val motion = clip(4f, if(front) ExerciseView.FRONT else ExerciseView.SIDE) { t ->
            val w = cycleSin(t)
            val p = pulse(t)
            val h = when (move) {
                Move.HIP_SIDE -> Offset(35f*w,0f)
                Move.HINGE -> Offset(-57f*p,21f*p)
                Move.ANKLE_ROCK, Move.WEIGHT -> Offset(28f*w,0f)
                Move.ANKLE_KNEE -> Offset(30f*p,11f*p)
                Move.KNEE_BEND -> Offset(15f*w,18f*abs(w))
                Move.HEELS_UP -> Offset(6f*p,-21f*p)
                Move.HIP_FIXED -> Offset(14f*w,4f*p)
                else -> Offset.Zero
            }
            val s = h + when (move) {
                Move.HINGE -> Offset(13f+104f*p,-147f+46f*p)
                Move.LATERAL -> Offset(56f*w,-147f+12f*abs(w))
                Move.ROTATE_CROSSED -> Offset(23f*w,-147f)
                else -> Offset(if(front) 0f else 9f,-147f)
            }
            val a = s + when (move) {
                Move.SCAPULA -> Offset(-92f-42f*p,45f)
                Move.ARM_FORWARD -> Offset(13f+132f*p,164f-140f*p)
                Move.ARM_BACK -> Offset(5f-74f*p,161f-21f*p)
                Move.ELBOW, Move.WRIST_CIRCLE -> Offset(28f+53f*p,137f-109f*p)
                Move.ROTATE_CROSSED -> Offset(48f+34f*w,41f)
                else -> Offset(-45f,145f)
            }
            val b = s + when (move) {
                Move.SCAPULA -> Offset(92f+42f*p,45f)
                Move.ELBOW, Move.WRIST_CIRCLE -> Offset(64f,108f)
                Move.ROTATE_CROSSED -> Offset(-48f+34f*w,42f)
                else -> Offset(45f,149f)
            }
            val nearLift = if (move == Move.MARCH) maxOf(0f,w)*95f else 0f
            val farLift = if (move == Move.MARCH) maxOf(0f,-w)*95f else 0f
            val rise = if (move == Move.HEELS_UP) -21f*p else 0f
            val stagger = if (move == Move.ANKLE_KNEE || move == Move.KNEE_BEND) 64f else 33f
            athlete(s,h,a,b,Offset(stagger+nearLift*.4f,248f-nearLift+rise),
                Offset(-33f+farLift*.4f,248f-farLift+rise),1f,if(front) 1f else -1f,frontal=front)
        }
        return motion.copy(
            handGesture = if (move==Move.WRIST_CIRCLE) HandGesture.WRIST_CIRCLE else null,
            footGesture = when (move) {
                Move.HEELS_UP -> FootGesture.HEEL_RAISE
                Move.WEIGHT -> FootGesture.TOE_TO_HEEL
                else -> null
            }
        )
    }

    private fun seated(key: String): ExerciseMotion? {
        val move = when (key) {
            "alcance al frente de manos" -> Move.REACH
            "apertura de brazos a los lados" -> Move.ARM_OPEN
            "balanceo de pelvis atras-adelante" -> Move.PELVIS
            "inclinacion pelvica anterior sentado" -> Move.PELVIS_ANTERIOR
            "circulo controlado de hombros" -> Move.SHOULDER_CIRCLE
            "deslizamiento alterno de pies" -> Move.SLIDE_FEET
            "elevacion alterna de puntas" -> Move.TOES
            "extension de rodilla sin carga" -> Move.KNEE_EXTEND
            "extension toracica sobre respaldo" -> Move.BACKREST
            "flexion y extension de munecas" -> Move.WRIST_FLEX
            "inclinacion lateral del tronco" -> Move.LATERAL
            "movilizacion de tobillos sentado" -> Move.ANKLE_CIRCLE
            "presion suave de palmas frente al pecho" -> Move.PALMS
            "rotacion suave de cabeza hacia el lado libre" -> Move.HEAD_TURN
            "rotacion toracica sentado" -> Move.ROTATE
            "separacion ligera de rodillas" -> Move.KNEE_OPEN
            else -> return null
        }
        val front = move in setOf(Move.ARM_OPEN,Move.LATERAL,Move.PALMS,Move.HEAD_TURN,Move.ROTATE,Move.KNEE_OPEN)
        val motion = clip(4.4f, if(front) ExerciseView.FRONT else ExerciseView.SIDE) { t ->
            val p = pulse(t)
            val w = cycleSin(t)
            val h = Offset(when(move) {
                Move.PELVIS -> 22f*w
                Move.PELVIS_ANTERIOR -> 18f*p
                else -> 0f
            },0f)
            val s = h + when (move) {
                Move.PELVIS -> Offset(4f-25f*w,-147f)
                Move.PELVIS_ANTERIOR -> Offset(5f+11f*p,-147f)
                Move.BACKREST -> Offset(-12f-43f*p,-145f+10f*p)
                Move.LATERAL -> Offset(45f*w,-146f+10f*abs(w))
                Move.ROTATE -> Offset(26f*w,-147f)
                Move.SHOULDER_CIRCLE -> Offset(13f*w,-143f-14f*cycleCos(t))
                else -> Offset(0f,-147f)
            }
            val a = s + when(move) {
                Move.REACH -> Offset(55f+110f*p,112f-80f*p)
                Move.ARM_OPEN -> Offset(-49f-120f*p,129f-115f*p)
                Move.BACKREST -> Offset(35f,-25f)
                Move.PALMS -> Offset(-6f,75f)
                Move.WRIST_FLEX -> Offset(89f,92f)
                else -> Offset(if(front) -38f else 66f,137f)
            }
            val b = s + when(move) {
                Move.REACH -> Offset(42f+113f*p,109f-86f*p)
                Move.ARM_OPEN -> Offset(49f+120f*p,129f-115f*p)
                Move.BACKREST -> Offset(22f,-35f)
                Move.PALMS -> Offset(7f,75f)
                Move.WRIST_FLEX -> Offset(81f,85f)
                else -> Offset(if(front) 46f else 40f,142f)
            }
            val slide = if(move==Move.SLIDE_FEET) 63f*w else 0f
            val extendA = if(move==Move.KNEE_EXTEND) maxOf(0f,w) else 0f
            val extendB = if(move==Move.KNEE_EXTEND) maxOf(0f,-w) else 0f
            val feet = if(front) Pair(Offset(-55f,134f),Offset(55f,134f))
                else Pair(Offset(136f+slide+96f*extendA,135f-111f*extendA),
                    Offset(107f-slide+110f*extendB,128f-106f*extendB))
            athlete(s,h,a,b,feet.first,feet.second,1f,if(front) 1f else -1f,frontal=front).toMutableList().apply {
                if(move==Move.HEAD_TURN) this[0] += Offset(18f*w,2f*abs(w))
                if(move==Move.KNEE_OPEN) {
                    this[7] += Offset(-26f*p,0f)
                    this[9] += Offset(26f*p,0f)
                }
            }
        }
        return motion.copy(
            staticHold = move==Move.PALMS,
            handGesture = if (move==Move.WRIST_FLEX) HandGesture.WRIST_FLEX else null,
            footGesture = when(move) {
                Move.TOES -> FootGesture.TOE_ALTERNATE
                Move.ANKLE_CIRCLE -> FootGesture.CIRCLE
                else -> null
            })
    }

    private fun quadruped(key: String): ExerciseMotion? {
        val move = when (key) {
            "alcance de mano hacia hombro opuesto" -> Move.SHOULDER_TAP
            "balanceo lateral de pelvis" -> Move.HIP_SIDE
            "basculacion pelvica en cuadrupedia" -> Move.PELVIS
            "circunduccion de cadera en cuadrupedia" -> Move.HIP_CIRCLE
            "deslizamiento frontal de mano" -> Move.HAND_SLIDE
            "desplazamiento de hombros sobre munecas" -> Move.WRIST_SHIFT
            "extension corta de pierna hacia atras" -> Move.KICK
            "flexion y extension de dedos apoyados" -> Move.FINGERS
            "gato-vaca toracico segmentado" -> Move.CAT_COW
            "inclinacion pelvica hacia adelante" -> Move.PELVIS_FORWARD
            "oscilacion de tronco en diagonal" -> Move.DIAGONAL
            "rotacion de pecho en cuadrupedia" -> Move.ROTATE
            "rotacion toracica abriendo un codo" -> Move.PEAK
            "separacion y aproximacion de escapulas" -> Move.SCAPULA
            "traslado lento de cadera a talones" -> Move.ROCK_BACK
            else -> return null
        }
        val overhead = move in setOf(Move.HIP_SIDE,Move.HIP_CIRCLE,Move.DIAGONAL)
        val motion = clip(4.8f, if(overhead) ExerciseView.OVERHEAD else ExerciseView.SIDE,
            ExercisePosture.PRONE, handSupport=true) { t ->
            val p = pulse(t)
            val w = cycleSin(t)
            if (overhead) {
                val s = Offset(-70f+if(move==Move.DIAGONAL) 24f*w else 0f,
                    if(move==Move.DIAGONAL) 24f*w else 0f)
                val h = Offset(75f+if(move==Move.HIP_CIRCLE) 21f*cycleCos(t) else 0f,32f*w)
                listOf(s+Offset(-49f,0f),s,h,Offset(-62f,-65f),Offset(-105f,-84f),
                    Offset(-62f,65f),Offset(-105f,84f),Offset(112f,-54f),Offset(198f,-59f),
                    Offset(112f,54f),Offset(198f,59f))
            } else {
                val s = when(move) {
                    Move.CAT_COW -> Offset(-74f,-28f*w)
                    Move.WRIST_SHIFT -> Offset(-72f-31f*p,0f)
                    Move.ROCK_BACK -> Offset(-72f+36f*p,56f*p)
                    Move.SCAPULA -> Offset(-73f,25f*p)
                    Move.ROTATE,Move.PEAK -> Offset(-73f,-19f*p)
                    else -> Offset(-73f,0f)
                }
                val h = when(move) {
                    Move.CAT_COW -> Offset(74f,18f*w)
                    Move.PELVIS -> Offset(74f+12f*w,-21f*w)
                    Move.PELVIS_FORWARD -> Offset(74f-17f*p,19f*p)
                    Move.ROCK_BACK -> Offset(74f+51f*p,47f*p)
                    else -> Offset(74f,0f)
                }
                val wrist = when(move) {
                    Move.SHOULDER_TAP -> Offset(-72f+20f*p,184f-151f*p)
                    Move.HAND_SLIDE -> Offset(-72f-86f*p,184f)
                    Move.ROTATE -> Offset(-72f-91f*p,184f-214f*p)
                    Move.PEAK -> Offset(-72f-23f*p,184f-151f*p)
                    else -> Offset(-72f,184f)
                }
                val knee = if(move==Move.KICK) Offset(77f+123f*p,145f-143f*p)
                    else Offset(77f,145f)
                val ankle = if(move==Move.KICK) Offset(176f+92f*p,183f-177f*p)
                    else Offset(176f,183f)
                athlete(s,h,wrist,Offset(-59f,176f),ankle,Offset(165f,177f),1f,-1f,forward=-1f)
                    .toMutableList().apply {
                        this[7] = knee
                        this[8] = ankle
                        this[9] = Offset(88f,140f)
                        this[10] = Offset(165f,177f)
                        if(move==Move.PEAK) this[3] += Offset(0f,-35f*p)
                    }
            }
        }
        return motion.copy(facing="left",handGesture=if(move==Move.FINGERS) HandGesture.FINGER_CLOSE else null)
    }

    private fun lying(key: String): ExerciseMotion? {
        val move = when (key) {
            "abduccion alterna de cadera" -> Move.HIP_ABDUCT
            "alcance de brazo sobre la cabeza" -> Move.ARM_OVERHEAD
            "balanceo pelvico supino" -> Move.PELVIS
            "circulo pequeno de tobillos" -> Move.ANKLE_CIRCLE
            "deslizamiento de talon en decubito", "deslizamiento de un talon" -> Move.HEEL_SLIDE
            "flexion corta de rodilla" -> Move.KNEE_SMALL
            "flexion y extension de tobillos" -> Move.ANKLE_FLEX
            "movilidad de hombro acostado" -> Move.SHOULDER
            "respiracion costal con costillas moviles" -> Move.BREATH
            "rotacion de cadera supina" -> Move.HIP_ROTATE
            "rotacion lumbar supina con rodillas flexionadas" -> Move.LUMBAR_ROTATE
            "rotacion suave de pelvis" -> Move.ROTATE
            "separacion de rodillas sin forzar" -> Move.KNEE_OPEN
            "traslado lento de ambos brazos al costado" -> Move.ARMS_SIDE
            else -> return null
        }
        val overhead = move in setOf(Move.HIP_ABDUCT,Move.HIP_ROTATE,Move.LUMBAR_ROTATE,Move.ROTATE,Move.KNEE_OPEN,Move.ARMS_SIDE)
        val motion = clip(4.8f, if(overhead) ExerciseView.OVERHEAD else ExerciseView.SIDE,ExercisePosture.SUPINE) { t ->
            val p = pulse(t)
            val w = cycleSin(t)
            val h = Offset(52f, when {
                move==Move.ROTATE -> 31f*w
                overhead -> 9f*w
                else -> 8f*p
            })
            val s = Offset(-94f, if(move==Move.BREATH) -13f*p else 0f)
            if(overhead) {
                val sway = when(move) {
                    Move.LUMBAR_ROTATE -> 70f*w
                    Move.HIP_ROTATE -> 53f*w
                    Move.ROTATE -> 21f*w
                    else -> 0f
                }
                val open = if(move==Move.KNEE_OPEN) 60f*p else 0f
                val bothArms = if(move==Move.ARMS_SIDE) p else 0f
                val bent = move in setOf(Move.KNEE_OPEN,Move.LUMBAR_ROTATE,Move.ROTATE)
                val kneesX = if(bent) 115f else 181f
                val feetX = if(bent) 206f else 299f
                val abductionA = if(move==Move.HIP_ABDUCT) maxOf(0f,w)*82f else 0f
                val abductionB = if(move==Move.HIP_ABDUCT) maxOf(0f,-w)*82f else 0f
                listOf(Offset(-143f,0f),s,h,Offset(-7f-67f*bothArms,-40f-48f*bothArms),
                    Offset(76f-164f*bothArms,-49f-108f*bothArms),
                    Offset(-7f-67f*bothArms,40f+48f*bothArms),Offset(76f-164f*bothArms,49f+108f*bothArms),
                    Offset(kneesX,-35f+sway-open-abductionA*.52f),Offset(feetX,-35f-abductionA),
                    Offset(kneesX,35f+sway+open+abductionB*.52f),Offset(feetX,35f+abductionB))
            } else {
                val a = when(move) {
                    Move.ARM_OVERHEAD -> s+Offset(125f-279f*p,17f-56f*cycleSin(t*.5f))
                    Move.SHOULDER -> s+Offset(132f-135f*p,17f-155f*p)
                    Move.BREATH -> s+Offset(61f,-21f)
                    else -> s+Offset(146f,12f)
                }
                val slide = when(move) {
                    Move.HEEL_SLIDE -> 129f*p
                    Move.KNEE_SMALL -> 55f*p
                    Move.PELVIS,Move.BREATH -> 87f
                    else -> 0f
                }
                val legs = Offset(297f-slide,13f)
                athlete(s,h,a,s+Offset(133f,22f),legs,Offset(288f-if(move==Move.PELVIS || move==Move.BREATH)87f else 0f,20f),
                    1f,-1f,forward=-1f)
            }
        }
        return motion.copy(footGesture = when(move) {
            Move.ANKLE_CIRCLE -> FootGesture.CIRCLE
            Move.ANKLE_FLEX -> FootGesture.FLEX
            else -> null
        })
    }

    private fun kneeling(key: String): ExerciseMotion? {
        val move = when (key) {
            "alcance alto de brazo contrario" -> Move.OPPOSITE_ARM
            "alcance diagonal de ambas manos" -> Move.ARM_DIAGONAL
            "apertura de hombros con brazos en cruz" -> Move.ARM_OPEN
            "balanceo anterior de cadera en medio arrodillamiento" -> Move.ANKLE_ROCK
            "balanceo corto de cadera al frente" -> Move.SHORT_ROCK
            "deslizamiento de rodilla delantera", "deslizamiento de rodilla hacia adelante" -> Move.ANKLE_KNEE
            "elevacion de brazo hacia el costado" -> Move.ARM_SIDE
            "extension de flexor de cadera sin arco lumbar" -> Move.FLEXOR
            "flexion plantar de pie trasero" -> Move.REAR_FOOT
            "flexion y extension de tobillo delantero" -> Move.ANKLE_ACTIVE
            "movilidad de tobillo delantero con pie apoyado" -> Move.ANKLE_FLEX
            "giro suave de cabeza hacia lado libre" -> Move.HEAD_TURN
            "inclinacion pelvica posterior suave" -> Move.PELVIS_BACK
            "rotacion de pecho hacia pierna adelantada" -> Move.ROTATE
            "rotacion toracica con una rodilla apoyada" -> Move.ROTATE_CROSSED
            else -> return null
        }
        val front = move in setOf(Move.ARM_OPEN,Move.ARM_SIDE,Move.ARM_DIAGONAL,Move.ROTATE,Move.ROTATE_CROSSED,Move.HEAD_TURN)
        val motion = clip(4.8f,if(front) ExerciseView.FRONT else ExerciseView.SIDE) { t ->
            val p = pulse(t)
            val w = cycleSin(t)
            val shift = when (move) {
                Move.ANKLE_ROCK -> 39f*p
                Move.FLEXOR -> 32f*p
                Move.SHORT_ROCK -> 23f*p
                Move.ANKLE_KNEE -> 32f*p
                Move.ANKLE_FLEX -> 24f*p
                Move.PELVIS_BACK -> -15f*p
                else -> 0f
            }
            val h=Offset(shift,0f)
            val s=h+Offset(if(move in setOf(Move.ROTATE,Move.ROTATE_CROSSED)) 32f*w else 8f,-147f)
            val a=s+when(move) {
                Move.OPPOSITE_ARM -> Offset(101f*p,142f-281f*p)
                Move.ARM_DIAGONAL -> Offset(-112f*p,130f-240f*p)
                Move.ARM_OPEN -> Offset(-41f-127f*p,133f-124f*p)
                Move.ARM_SIDE -> Offset(-41f-127f*p,133f-124f*p)
                Move.ROTATE -> Offset(-57f+60f*w,87f)
                Move.ROTATE_CROSSED -> Offset(49f+31f*w,47f)
                else -> Offset(if(front) -39f else 61f,131f)
            }
            val b=s+when(move) {
                Move.ARM_DIAGONAL -> Offset(-83f*p,134f-207f*p)
                Move.ARM_OPEN -> Offset(41f+127f*p,133f-124f*p)
                Move.ROTATE -> Offset(59f+60f*w,87f)
                Move.ROTATE_CROSSED -> Offset(-49f+31f*w,47f)
                else -> Offset(if(front) 41f else 41f,128f)
            }
            val knee=if(front) Offset(-66f,75f) else Offset(120f+if(move in setOf(Move.ANKLE_KNEE,Move.ANKLE_FLEX))32f*p else 0f,47f)
            val backKnee = if(front) Offset(63f,148f) else Offset(-37f,149f)
            val forwardAnkle = if(front) Offset(-76f,197f) else Offset(132f,174f)
            val rearAnkle = if(front) Offset(105f,161f) else Offset(-150f,173f)
            athlete(s,h,a,b,forwardAnkle,rearAnkle,1f,1f,frontal=front).toMutableList().apply {
                this[7]=knee
                this[8]=forwardAnkle
                this[9]=backKnee
                this[10]=rearAnkle
                if(move==Move.HEAD_TURN) this[0]+=Offset(19f*w,2f*abs(w))
            }
        }
        return motion.copy(footGesture=when(move) {
            Move.REAR_FOOT -> FootGesture.REAR_PLANTAR
            Move.ANKLE_ACTIVE -> FootGesture.FRONT_FLEX
            else -> null
        })
    }
}
