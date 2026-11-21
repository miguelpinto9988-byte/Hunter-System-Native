package com.huntersystem.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.cos
import kotlin.math.sin

/** Floor movements with explicit joint placement and support contacts. */
internal enum class AuthoredFloorMovement(val posture: ExercisePosture) {
    V_UP(ExercisePosture.SUPINE),
    REVERSE_CLAM(ExercisePosture.SIDE_LYING),
    KICKBACK(ExercisePosture.PRONE),
    COPENHAGEN_HOLD(ExercisePosture.SIDE_LYING),
    COPENHAGEN_LIFT(ExercisePosture.SIDE_LYING),
    SIDE_ADDUCTION(ExercisePosture.SIDE_LYING),
    PRONE_HIP_ROTATION(ExercisePosture.PRONE);

    fun render(id: String, frameCount: Int): ExerciseMotion {
        val tempo = id.endsWith("-tempo23") || id.endsWith("_slow")
        val pause = id.endsWith("-pause23") || id.endsWith("_pause")
        val up = if (tempo) 1.2f else 1.6f
        val hold = if (pause) 2f else .25f
        val down = if (tempo) 3f else 1.6f
        val duration = up + hold + down + .35f
        val times = List(frameCount) { it.toFloat() / frameCount }
        val frames = times.map { phase ->
            val elapsed = phase * duration
            val progress = when {
                elapsed <= up -> elapsed / up
                elapsed <= up + hold -> 1f
                else -> (1f - (elapsed - up - hold) / down).coerceAtLeast(0f)
            }
            val lift = progress * progress * (3f - 2f * progress)
            when (this) {
                V_UP -> vUp(lift)
                REVERSE_CLAM -> reverseClam(lift)
                KICKBACK -> kickback(lift)
                COPENHAGEN_HOLD -> copenhagen(1f)
                COPENHAGEN_LIFT -> copenhagen(lift)
                SIDE_ADDUCTION -> sideAdduction(lift)
                PRONE_HIP_ROTATION -> proneHipRotation(lift)
            }
        }
        val bench = if (this == COPENHAGEN_HOLD || this == COPENHAGEN_LIFT)
            listOf(ExerciseProp("bench",Rect(554f,574f,657f,659f))) else emptyList()
        return ExerciseMotion(frames,duration,times,"original",if(this==KICKBACK) "left" else "right",
            staticHold=this==COPENHAGEN_HOLD,props=bench,
            view=if(this==PRONE_HIP_ROTATION) ExerciseView.OVERHEAD else ExerciseView.SIDE,
            prone=posture==ExercisePosture.PRONE,handSupport=this==KICKBACK,posture=posture,
            authoredFloor=this)
    }

    private fun vUp(lift: Float): List<Offset> {
        val hip = Offset(382f,645f)
        val chestAxis = vector(180f + lift * 53f)
        val legAxis = vector(-lift * 58f)
        val shoulder = hip + chestAxis * 154f
        val head = shoulder + chestAxis * 53f
        val knee = hip + legAxis * 137f
        val ankle = knee + legAxis * 126f
        // Arms reach towards the shins as the trunk and legs rise together.
        val arm = vector(180f + lift * 156f)
        val elbow = shoulder + arm * 92f
        val wrist = elbow + arm * 90f
        return listOf(head,shoulder,hip,elbow,wrist,elbow+Offset(-9f,-11f),wrist+Offset(-9f,-11f),
            knee,ankle,knee+Offset(-9f,-11f),ankle+Offset(-9f,-11f))
    }

    private fun reverseClam(lift: Float): List<Offset> {
        val shoulder = Offset(238f,624f)
        val hip = Offset(401f,626f)
        val bottomKnee = Offset(478f,675f)
        val upperKnee = bottomKnee + Offset(-5f,-13f)
        val bottomAnkle = bottomKnee + Offset(-104f,11f)
        val upperAnkle = upperKnee + vector(174f+lift*43f) * 104f
        return listOf(Offset(179f,624f),shoulder,hip,Offset(221f,662f),Offset(171f,648f),
            Offset(311f,595f),Offset(383f,612f),bottomKnee,bottomAnkle,upperKnee,upperAnkle)
    }

    private fun kickback(lift: Float): List<Offset> {
        val hip = Offset(453f,475f)
        val knee = hip + vector(90f - lift * 87f) * 165f
        val ankle = knee + vector(-lift * 80f) * 108f
        return listOf(Offset(221f,459f),Offset(278f,475f),hip,Offset(276f,564f),Offset(277f,660f),
            Offset(258f,556f),Offset(259f,649f),knee,ankle,Offset(439f,642f),Offset(547f,652f))
    }

    private fun copenhagen(lift: Float): List<Offset> {
        val hip = Offset(359f,564f)
        val lowerAxis = vector(22f - 17f * lift)
        return listOf(Offset(150f,550f),Offset(202f,556f),hip,
            Offset(204f,660f),Offset(292f,660f),Offset(277f,526f),Offset(334f,555f),
            hip + lowerAxis * 121f,hip + lowerAxis * 242f,
            hip + Offset(122f,-4f),hip + Offset(242f,0f))
    }

    private fun sideAdduction(lift: Float): List<Offset> {
        val hip = Offset(361f,619f)
        val lowerAxis = vector(9f - 18f * lift)
        return listOf(Offset(152f,604f),Offset(205f,619f),hip,
            Offset(176f,661f),Offset(117f,659f),Offset(259f,647f),Offset(329f,655f),
            hip + lowerAxis * 125f,hip + lowerAxis * 246f,
            Offset(467f,559f),Offset(420f,659f))
    }

    private fun proneHipRotation(lift: Float): List<Offset> {
        val first = Offset(321f,537f)
        val second = Offset(399f,537f)
        val lateral = 5f + 67f * lift
        val depth = 56f - 12f * lift
        return listOf(Offset(360f,195f),Offset(360f,251f),Offset(360f,407f),
            Offset(294f,335f),Offset(296f,424f),Offset(426f,335f),Offset(424f,424f),
            first,first + Offset(-lateral,-depth),second,second + Offset(lateral,-depth))
    }

    private fun vector(degrees: Float): Offset {
        val radians = Math.toRadians(degrees.toDouble())
        return Offset(cos(radians).toFloat(),sin(radians).toFloat())
    }

    companion object {
        fun forId(id: String): AuthoredFloorMovement? = when {
            id=="home-vup23" || id.startsWith("home-vup23-") -> V_UP
            id=="new46-reverseclam" || id.startsWith("new46-reverseclam-") -> REVERSE_CLAM
            id=="kickback" || id in setOf("kickback_slow","kickback_pause","kickback_easy","kickback_short") -> KICKBACK
            id=="new46-copenhagen" -> COPENHAGEN_HOLD
            id=="new46-copenhagendyn" -> COPENHAGEN_LIFT
            id=="new46-sideadduct" -> SIDE_ADDUCTION
            id=="new46-pronehipir" -> PRONE_HIP_ROTATION
            else -> null
        }
    }
}
