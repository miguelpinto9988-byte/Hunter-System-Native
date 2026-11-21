package com.huntersystem.app

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/** Floor movements with explicit joint placement and support contacts. */
internal enum class AuthoredFloorMovement(val posture: ExercisePosture) {
    V_UP(ExercisePosture.SUPINE),
    REVERSE_CLAM(ExercisePosture.SIDE_LYING),
    KICKBACK(ExercisePosture.PRONE);

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
            }
        }
        return ExerciseMotion(frames,duration,times,"original",if(this==KICKBACK) "left" else "right",
            view=ExerciseView.SIDE,prone=this==KICKBACK,handSupport=this==KICKBACK,posture=posture,
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
        val bottomKnee = Offset(497f,664f)
        val upperKnee = bottomKnee + Offset(-5f,-13f)
        val bottomAnkle = bottomKnee + Offset(106f,0f)
        val upperAnkle = upperKnee + vector(-35f*lift) * 106f
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

    private fun vector(degrees: Float): Offset {
        val radians = Math.toRadians(degrees.toDouble())
        return Offset(cos(radians).toFloat(),sin(radians).toFloat())
    }

    companion object {
        fun forId(id: String): AuthoredFloorMovement? = when {
            id=="home-vup23" || id.startsWith("home-vup23-") -> V_UP
            id=="new46-reverseclam" || id.startsWith("new46-reverseclam-") -> REVERSE_CLAM
            id=="kickback" || id in setOf("kickback_slow","kickback_pause","kickback_easy","kickback_short") -> KICKBACK
            else -> null
        }
    }
}
