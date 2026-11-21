package com.huntersystem.app

import androidx.compose.ui.geometry.Offset
import com.huntersystem.app.LegacyExerciseClips.clip
import com.huntersystem.app.LegacyExerciseClips.pulse
import kotlin.math.sqrt

/** Individual Hunter 2000 entries whose historical silhouette shows a different setup. */
internal object LegacySingleExerciseMotions {
    fun load(id: String, load: (String) -> ExerciseMotion?): ExerciseMotion? = when(id) {
        "hx_lower_0007" -> bridgeOnBall()
        "hx_lower_0009" -> load("gym-presssingle30")
        "hx_lower_0022" -> load("home-thrustsingle30")
        "hx_lower_0023" -> load("gym-leg-extension23")?.let { original ->
            val rest = original.frames.first()
            original.copy(frames=original.frames.map { pose ->
                pose.toMutableList().apply {
                    this[9]=rest[9]
                    this[10]=rest[10]
                }
            })
        }
        "hx_lower_0030" -> bridgeOnBall(curl=true,unilateral=true)
        "new46-ballham" -> bridgeOnBall(curl=true)
        "hx_lower_0031", "hx_lower_0032" -> load(if(id=="hx_lower_0031") "gym-abductc30" else "gym-adductc30")
            ?.let { original ->
                // Keep the active ankle as the near cable attachment.
                original.copy(frames=original.frames.map { pose ->
                    pose.take(7)+listOf(pose[9],pose[10],pose[7],pose[8])
                })
            }
        else -> null
    }?.copy(source="authored-variant")

    private fun bridgeOnBall(curl: Boolean = false, unilateral: Boolean = false): ExerciseMotion =
        clip(5f,ExerciseView.SIDE,ExercisePosture.SUPINE) { phase ->
            val progress = pulse(phase)
            val shoulder = Offset(-83f,88f)
            val hip = Offset(65f,if(curl) 46f else 88f-55f*progress)
            val ankle = Offset(302f-if(curl) 132f*progress else 0f,37f)
            val axis = ankle-hip
            val distance = axis.getDistance()
            val unit = axis/distance
            val thigh = 138f
            val shin = 116f
            val along = (thigh*thigh-shin*shin+distance*distance)/(2f*distance)
            val height = sqrt((thigh*thigh-along*along).coerceAtLeast(0f))
            val knee = hip+unit*along+Offset(unit.y,-unit.x)*height
            val farKnee = if(unilateral) hip+Offset(89f,-105f) else knee+Offset(-11f,-6f)
            val farAnkle = if(unilateral) farKnee+Offset(117f,-17f) else ankle+Offset(-10f,-6f)
            listOf(Offset(-129f,89f),shoulder,hip,Offset(4f,103f),Offset(82f,106f),
                Offset(-4f,99f),Offset(74f,101f),knee,ankle,farKnee,farAnkle)
        }
}
