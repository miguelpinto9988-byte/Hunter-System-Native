package com.huntersystem.app

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.sqrt

internal enum class ExercisePosture { UPRIGHT, SUPINE, PRONE, SIDE_LYING }
internal data class ExerciseHeadPlacement(val left: Boolean, val rotation: Float)

/** Placement is expressed in pose coordinates. Mirroring is applied later to the complete scene. */
internal object ExercisePoseCommands {
    fun posture(declared: String, view: ExerciseView, frames: List<List<Offset>>): ExercisePosture {
        return when (declared) {
            "supine" -> ExercisePosture.SUPINE
            "prone" -> ExercisePosture.PRONE
            "side-lying" -> ExercisePosture.SIDE_LYING
            "upright", "seated", "kneeling" -> ExercisePosture.UPRIGHT
            else -> if (view == ExerciseView.SIDE && frames.all {
                val torso = it[2] - it[1]
                abs(torso.y) < abs(torso.x) * .45f &&
                    maxOf(it[7].y, it[9].y) < it[1].y - abs(torso.x) * .16f &&
                    maxOf(it[8].y, it[10].y) >= it[1].y - abs(torso.x) * .18f
            }) ExercisePosture.SUPINE else ExercisePosture.UPRIGHT
        }
    }

    /** Adds a readable pelvic motion to the legacy standing pose while the feet stay fixed. */
    fun animateMissingGesture(id: String, frames: List<List<Offset>>): List<List<Offset>> {
        if (id != "new46-pelvictilt" || frames.any { it != frames.first() }) return frames
        val rest = frames.first()
        val span = (rest[2] - rest[1]).getDistance()
        return frames.indices.map { index ->
            val phase = sin(2.0 * Math.PI * index / frames.size).toFloat()
            rest.toMutableList().apply {
                this[2] += Offset(.089f, -.041f) * span * phase
                this[7] += Offset(.022f, 0f) * span * phase
                this[9] += Offset(.022f, 0f) * span * phase
            }
        }
    }

    fun head(motion: ExerciseMotion, pose: List<Offset>): ExerciseHeadPlacement {
        val torso = pose[2] - pose[1]
        val neck = pose[1] - pose[0]
        val side = motion.view == ExerciseView.SIDE
        val rotation = (atan2(neck.y, neck.x) * 180f / Math.PI.toFloat() - 90f + 540f) % 360f - 180f
        val left = when {
            !side -> false
            motion.posture == ExercisePosture.SUPINE || motion.prone -> torso.x > 0f
            else -> motion.facing == "left"
        }
        val tilt = when {
            motion.view == ExerciseView.OVERHEAD -> rotation
            side && motion.posture == ExercisePosture.SUPINE -> if (left) -90f else 90f
            side && motion.prone -> rotation.coerceIn(-95f, 95f)
            side && motion.posture == ExercisePosture.SIDE_LYING -> rotation.coerceIn(-82f, 82f)
            else -> rotation.coerceIn(-38f, 38f)
        }
        // The SVG faces right by default. On the back the rear of the head stays by the floor.
        return if (side && motion.posture == ExercisePosture.SUPINE)
            ExerciseHeadPlacement(!left, tilt) else ExerciseHeadPlacement(left, tilt)
    }

    fun foot(motion: ExerciseMotion, pose: List<Offset>, knee: Offset, ankle: Offset, first: Boolean = true): Offset {
        val torso = pose[2] - pose[1]
        val forward = if (motion.facing == "left") -1f else 1f
        val awayFromShoulders = if (torso.x < 0f) -1f else 1f
        val shin = unit(ankle - knee)
        val toe = when {
            motion.view == ExerciseView.OVERHEAD -> torso + shin * (torso.getDistance() * .22f)
            motion.posture == ExercisePosture.SUPINE -> Offset(awayFromShoulders, 0f)
            motion.prone && motion.handSupport -> Offset(-awayFromShoulders * .30f, .95f)
            motion.prone || motion.posture == ExercisePosture.SIDE_LYING -> Offset(awayFromShoulders, .12f)
            motion.ankleTouchesFloor(ankle,first,torso.getDistance()) -> Offset(forward,0f)
            else -> Offset(forward, (shin.x * forward * .25f).coerceIn(-.20f,.20f))
        }
        return unit(toe)
    }

    /** Interpolate elbow and knee bends without shortening them when the pivot changes direction. */
    fun interpolate(a: List<Offset>, b: List<Offset>, t: Float): List<Offset> {
        if (t <= 0f) return a
        if (t >= 1f) return b
        val pose = a.zip(b) { start, end -> start + (end - start) * t }.toMutableList()
        straightenSegment(a,b,t,pose,0,1)
        straightenSegment(a,b,t,pose,2,1)
        for ((root,joint,end) in limbs) {
            val start = pose[root]
            val anchor = pose[end]
            val reach = (anchor - start).getDistance()
            if (reach < .1f) continue
            val upper = length(a,b,root,joint,t)
            val lower = length(a,b,joint,end,t)
            if (reach >= upper + lower || reach <= abs(upper - lower)) continue
            val axis = (anchor - start) / reach
            val normal = Offset(-axis.y,axis.x)
            val along = (upper * upper - lower * lower + reach * reach) / (2f * reach)
            val bend = sqrt((upper * upper - along * along).coerceAtLeast(0f))
            val guide = pose[joint] - start
            val projection = guide.x * normal.x + guide.y * normal.y
            // Crossing the axis is valid in the projected view. Do not flip the knee abruptly.
            if (abs(projection) < bend * .20f) continue
            pose[joint] = start + axis * along + normal * (if (projection < 0f) -bend else bend)
        }
        return pose
    }

    private val limbs = listOf(Triple(1,3,4),Triple(1,5,6),Triple(2,7,8),Triple(2,9,10))

    private fun straightenSegment(a: List<Offset>,b: List<Offset>,t: Float,pose: MutableList<Offset>,tip: Int,root: Int) {
        val vector = pose[tip] - pose[root]
        val target = length(a,b,tip,root,t)
        if (vector.getDistance() in .1f..target) pose[tip] = pose[root] + unit(vector) * target
    }

    private fun length(a: List<Offset>,b: List<Offset>,first: Int,second: Int,t: Float): Float {
        val from = (a[first] - a[second]).getDistance()
        val to = (b[first] - b[second]).getDistance()
        return from + (to - from) * t
    }

    private fun unit(v: Offset): Offset = if (v.getDistance() > .001f) v / v.getDistance() else Offset(1f,0f)
}
