package com.huntersystem.app

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

internal enum class ExercisePosture { UPRIGHT, SUPINE, PRONE, SIDE_LYING }
internal data class ExerciseHeadPlacement(val left: Boolean, val rotation: Float)
internal enum class HandGesture {
    WRIST_CIRCLE, WRIST_FLEX, WRIST_EXTENSION, WRIST_DEVIATION, FOREARM_ROTATION,
    FINGER_CLOSE, TENDON_GLIDE, THUMB_OPPOSITION;

    fun direction(forward: Offset, phase: Float, first: Boolean): Offset {
        val signed = sin(phase * 2f * Math.PI.toFloat())
        val lift = (1f - cos(phase * 2f * Math.PI.toFloat())) * .5f
        val angle = when (this) {
            WRIST_CIRCLE -> signed * if (first) .55f else -.55f
            WRIST_FLEX -> -lift * .68f
            WRIST_EXTENSION -> lift * .65f
            WRIST_DEVIATION -> signed * .42f
            else -> 0f
        }
        return Offset(forward.x * cos(angle) - forward.y * sin(angle),
            forward.x * sin(angle) + forward.y * cos(angle))
    }

    fun curl(phase: Float): Float {
        val progress = (1f - cos(phase * 2f * Math.PI.toFloat())) * .5f
        return when (this) {
            FINGER_CLOSE -> progress
            TENDON_GLIDE -> when ((phase * 5f).toInt()) {
                1 -> .55f
                2 -> .24f
                3 -> .90f
                else -> 0f
            }
            else -> 0f
        }
    }

    fun projectedLength(phase: Float): Float = if (this == WRIST_CIRCLE)
        .85f + .15f * cos(phase * 2f * Math.PI.toFloat()) else 1f

    fun projectedWidth(phase: Float): Float = if (this == FOREARM_ROTATION)
        .35f + .65f * abs(cos(phase * 2f * Math.PI.toFloat())) else 1f

    fun thumbTarget(phase: Float): Int? = if (this == THUMB_OPPOSITION)
        (phase * 4f).toInt().coerceIn(0,3) else null

    companion object {
        fun forId(id: String): HandGesture? = when (id) {
            "wrist", "wrist_short", "wrist_easy" -> WRIST_CIRCLE
            "wristmob", "wristmob_slow", "wristmob_pause", "new46-wristcurl" -> WRIST_FLEX
            "new46-wristexte" -> WRIST_EXTENSION
            "new46-wristdeviation" -> WRIST_DEVIATION
            "forearmmob", "forearmmob_slow", "forearmmob_pause", "new46-forearmlever" -> FOREARM_ROTATION
            "fingers", "fingers_slow", "fingers_pause" -> FINGER_CLOSE
            "new46-tendonglides" -> TENDON_GLIDE
            "new46-thumbopposition" -> THUMB_OPPOSITION
            else -> null
        }
    }
}

internal enum class FootGesture {
    CIRCLE, FLEX, TOE_ALTERNATE, HEEL_RAISE, TOE_TO_HEEL, REAR_PLANTAR, FRONT_FLEX, INWARD, OUTWARD,
    PLANTAR, SHORT_ARCH, TOE_DISASSOCIATION, BIG_TOE_STRETCH;

    fun direction(axis: Offset, phase: Float, first: Boolean): Offset {
        val w = sin(phase*2f*Math.PI.toFloat())
        val p = (1f-cos(phase*2f*Math.PI.toFloat()))*.5f
        val direction = if(axis.x < 0f) -1f else 1f
        return when (this) {
            CIRCLE -> Offset(direction*(.83f+.13f*cos(phase*2f*Math.PI.toFloat())), -.44f*w)
            FLEX -> Offset(direction,-.53f*w)
            TOE_ALTERNATE -> Offset(direction,-.67f*maxOf(0f,w*if(first) 1f else -1f))
            HEEL_RAISE -> Offset(direction,.51f*p)
            TOE_TO_HEEL -> Offset(direction,.41f*w)
            REAR_PLANTAR -> if(first) axis else Offset(-direction,.68f*p)
            FRONT_FLEX -> if(first) Offset(direction,-.49f*w) else axis
            INWARD -> if(first) Offset(direction*(.67f+.27f*p),-.59f*p) else axis
            OUTWARD -> if(first) Offset(direction*(.67f+.27f*p),.59f*p) else axis
            PLANTAR -> Offset(direction,.72f*p)
            else -> axis
        }
    }

    val bareDetail get() = this in setOf(SHORT_ARCH, TOE_DISASSOCIATION, BIG_TOE_STRETCH)

    companion object {
        fun forId(id: String): FootGesture? = when (id) {
            "ankle", "ankle_short", "ankle_easy" -> CIRCLE
            "new46-ankleinvert" -> INWARD
            "new46-ankleevert" -> OUTWARD
            "new46-bandplantar" -> PLANTAR
            "new46-peronealnerve" -> FLEX
            "new46-shortfoot" -> SHORT_ARCH
            "new46-toeyoga" -> TOE_DISASSOCIATION
            "new46-bigtoestretch" -> BIG_TOE_STRETCH
            else -> null
        }
    }
}

internal data class ToeLiftMotion(val rise: Float, val pause: Float, val lower: Float) {
    val duration = rise + pause + lower + .35f

    fun elevation(phase: Float): Float {
        val seconds = (phase - floor(phase)) * duration
        val fraction = minOf(seconds / rise, 1f, (duration - .35f - seconds) / lower).coerceAtLeast(0f)
        return fraction * fraction * (3f - 2f * fraction)
    }

    companion object {
        fun forId(id: String): ToeLiftMotion? = when (id) {
            "toelift", "gym-tibialis30" -> ToeLiftMotion(1.25f, .25f, 1.6f)
            "toelift_slow", "gym-tibialis30-ecc30" -> ToeLiftMotion(1.25f, .25f, 3f)
            "toelift_pause", "gym-tibialis30-hold30" -> ToeLiftMotion(1.25f, 2f, 1.6f)
            else -> null
        }
    }
}

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

    /** Exercise-specific placement commands supplement the recorded landmarks. */
    fun apply(id: String, frames: List<List<Offset>>): List<List<Offset>> {
        if (id in setOf("neck", "neck_short", "neck_easy")) return frames.indices.map { index ->
            val wave = sin(2f * Math.PI.toFloat() * index / frames.size)
            frames[index].toMutableList().apply {
                this[0] += Offset(23f * wave, 1.5f * abs(wave))
            }
        }
        if (id in floorTrackCorrections) return correctedTrack(id, frames)
        if (id.startsWith("gym-pulloverm30")) return animateMachinePullover(frames)
        if (id.startsWith("gym-highrow30")) return animateMachineHighRow(frames)
        if (id in setOf("reverse","reverse_slow","reverse_pause") &&
            frames.all { it==frames.first() }) return sweepProneArms(id,frames)
        if (id.startsWith("home-diamond23")) {
            val initial = frames.first()
            val torso = initial[2] - initial[1]
            val chest = initial[1] + torso * .20f
            val across = unit(Offset(-torso.y, torso.x)) * (torso.getDistance() * .065f)
            return frames.map { frame ->
                frame.toMutableList().apply {
                    this[4] = chest + across
                    this[6] = chest - across
                }
            }
        }
        if (frames.all { it == frames.first() } &&
            id in setOf("prone", "prone_slow", "prone_pause", "contralateral")) {
            return animateProneLift(id, frames.first(), frames.size)
        }
        // Adds a readable pelvic motion to the legacy standing pose with fixed feet.
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

    private val floorTrackCorrections = setOf("new46-bandir","new46-supineshoulderir",
        "new46-sidecrow","new46-sidepallof","new46-ropecircles","new46-crosshop","new46-tucklever","hx_upper_0001")

    private fun correctedTrack(id: String, frames: List<List<Offset>>): List<List<Offset>> =
        frames.indices.map { index ->
            val phase = index.toFloat()/frames.size
            val rest = frames.first()
            val span = (rest[1]-rest[2]).getDistance()
            val w = LegacyExerciseClips.cycleSin(phase)
            val p = LegacyExerciseClips.pulse(phase)
            rest.toMutableList().apply {
                when (id) {
                    "new46-bandir" -> {
                        // The elbow stays by the ribs. Show the transverse arc in an oblique projection.
                        val arc = 1.14f + 1.17f*p
                        this[6] = rest[5]+Offset(cos(arc),-sin(arc)*.65f)*(span*.56f)
                    }
                    "new46-supineshoulderir" -> {
                        val arc = -.25f - 1.45f*p
                        this[6] = rest[5]+Offset(cos(arc)*.82f,sin(arc))*(span*.57f)
                    }
                    "new46-sidecrow", "new46-sidepallof" -> {
                        val shoulder = Offset(222f,340f)
                        val hip = Offset(369f,381f)
                        this[0] = Offset(166f,320f)
                        this[1]=shoulder
                        this[2]=hip
                        this[3]=Offset(232f,464f)
                        this[4]=Offset(151f,466f)
                        this[7]=Offset(484f,425f)
                        this[8]=Offset(601f,462f)
                        this[9]=Offset(481f,434f)
                        this[10]=Offset(592f,468f)
                        if(id=="new46-sidecrow") {
                            this[5]=Offset(296f-119f*p,282f+13f*p)
                            this[6]=Offset(368f-115f*p,267f+9f*p)
                        } else {
                            this[5]=Offset(265f+48f*p,293f-11f*p)
                            this[6]=Offset(242f+143f*p,270f-29f*p)
                        }
                    }
                    "new46-ropecircles" -> {
                        for (arm in listOf(3,5)) {
                            val sign = if(arm==3) -1f else 1f
                            this[arm] = rest[1]+Offset(sign*(.43f+.10f*p),.57f)*span
                            this[arm+1] = this[arm]+Offset(sign*(.29f+.22f*w),.24f-.24f*LegacyExerciseClips.cycleCos(phase))*span
                        }
                    }
                    "new46-crosshop" -> {
                        val hop = LegacyExerciseClips.pulse(phase*4f)
                        val travel = if (phase<.5f) .16f*LegacyExerciseClips.pulse(phase*2f)
                            else .54f*LegacyExerciseClips.pulse(phase*2f)
                        val shift = Offset(travel,-.31f*hop)*span
                        indices.forEach { this[it] += shift }
                        this[3] += Offset(-.13f*hop,-.14f*hop)*span
                        this[5] += Offset(.13f*hop,-.14f*hop)*span
                        this[4] += Offset(-.18f*hop,-.20f*hop)*span
                        this[6] += Offset(.18f*hop,-.20f*hop)*span
                        this[9] += Offset(-.06f*hop,-.05f*hop)*span
                    }
                    "new46-tucklever" -> {
                        // Tuck the feet toward the pelvis while both elbows remain extended.
                        this[7]=rest[2]+Offset(-30f,-134f)
                        this[8]=rest[2]+Offset(54f,-51f)
                        this[9]=rest[2]+Offset(-15f,-130f)
                        this[10]=rest[2]+Offset(69f,-49f)
                    }
                    "hx_upper_0001" -> {
                        val pose = LegacyExerciseClips.athlete(Offset(8f,-147f),Offset.Zero,
                            Offset(71f+64f*p,-115f-159f*p),Offset(-20f,0f),
                            Offset(135f,176f),Offset(-152f,176f),-1f,1f)
                        pose.forEachIndexed { i, point -> this[i] = point }
                        this[7] = Offset(125f,56f)
                        this[9] = Offset(-44f,163f)
                    }
                }
            }
        }

    private fun sweepProneArms(id: String, frames: List<List<Offset>>): List<List<Offset>> {
        val outward = if(id=="reverse_slow") .29f else .43f
        val returnStart = if(id=="reverse_pause") .67f else outward
        val rest=frames.first()
        return frames.indices.map { index ->
            val phase=index.toFloat()/frames.size
            val progress=minOf(phase/outward,1f,(1f-phase)/(1f-returnStart))
            val radians = progress*progress*(3f-2f*progress)*.39f
            rest.toMutableList().apply {
                for (joint in 3..6) {
                    val arm=rest[joint]-rest[1]
                    val turn=if(joint<=4) -radians else radians
                    this[joint]=rest[1]+Offset(arm.x*cos(turn)-arm.y*sin(turn),
                        arm.x*sin(turn)+arm.y*cos(turn))
                }
            }
        }
    }

    private fun animateMachinePullover(frames: List<List<Offset>>): List<List<Offset>> {
        val reach=frames.maxOf { it[4].x }
        val close=frames.minOf { it[4].x }
        return frames.map { frame ->
            val progress=((reach-frame[4].x)/(reach-close).coerceAtLeast(1f)).coerceIn(0f,1f)
            val upper=-1.54f+2.78f*progress
            val forearm=upper-1.47f
            val elbow=frame[1]+Offset(cos(upper),sin(upper))*98f
            val grip=elbow+Offset(cos(forearm),sin(forearm))*77f
            frame.toMutableList().apply {
                this[3]=elbow
                this[4]=grip
                this[5]=elbow+Offset(-12f,-10f)
                this[6]=grip+Offset(-12f,-10f)
            }
        }
    }

    private fun animateMachineHighRow(frames: List<List<Offset>>): List<List<Offset>> {
        val open = frames.maxOf { it[4].x }
        val closed = frames.minOf { it[4].x }
        return frames.map { frame ->
            val progress=((open-frame[4].x)/(open-closed).coerceAtLeast(1f)).coerceIn(0f,1f)
            val reach=Offset(171f-106f*progress,-109f+158f*progress)
            val distance=reach.getDistance()
            val axis=reach/distance
            val bend=sqrt((112f*112f-distance*distance/4f).coerceAtLeast(0f))
            val elbow=frame[1]+reach*.5f+Offset(-axis.y,axis.x)*bend
            val hand=frame[1]+reach
            frame.toMutableList().apply {
                this[3]=elbow
                this[4]=hand
                this[5]=elbow+Offset(-12f,-10f)
                this[6]=hand+Offset(-12f,-10f)
            }
        }
    }

    private fun animateProneLift(id: String, rest: List<Offset>, count: Int): List<List<Offset>> {
        val span = (rest[2] - rest[1]).getDistance()
        return List(count) { index ->
            val phase = index.toFloat() / count
            val pose = rest.toMutableList()
            fun raise(root: Int, joint: Int, end: Int, progress: Float) {
                val lift = progress * progress * (3f - 2f * progress)
                for (landmark in listOf(joint, end)) {
                    // A small oblique projection reveals elevation while the torso stays on the mat.
                    pose[landmark] = rest[root] + (rest[landmark] - rest[root]) * (1f - .09f * lift) -
                        Offset(0f,span * .15f * lift)
                }
            }
            if (id == "contralateral") {
                val progress = abs(sin(2f * Math.PI.toFloat() * phase))
                if (phase < .5f) {
                    raise(1,3,4,progress)
                    raise(2,9,10,progress)
                } else {
                    raise(1,5,6,progress)
                    raise(2,7,8,progress)
                }
            } else {
                val rise = if (id == "prone_slow") .30f else .40f
                val startLowering = if (id == "prone_pause") .65f else rise
                val progress = minOf(phase / rise, 1f, (1f - phase) / (1f - startLowering))
                raise(1,3,4,progress)
                raise(1,5,6,progress)
            }
            pose
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
            motion.authoredFloor == AuthoredFloorMovement.V_UP -> rotation
            side && motion.posture == ExercisePosture.SUPINE -> if (left) -90f else 90f
            side && (motion.prone || motion.floorTransition) -> rotation.coerceIn(-95f, 95f)
            side && motion.posture == ExercisePosture.SIDE_LYING -> rotation.coerceIn(-82f, 82f)
            else -> rotation.coerceIn(-38f, 38f)
        }
        // The SVG faces right by default. On the back the rear of the head stays by the floor.
        return if (side && motion.posture == ExercisePosture.SUPINE)
            ExerciseHeadPlacement(!left, tilt) else ExerciseHeadPlacement(left, tilt)
    }

    fun foot(motion: ExerciseMotion, pose: List<Offset>, knee: Offset, ankle: Offset,
             first: Boolean = true, phase: Float = 0f): Offset {
        val torso = pose[2] - pose[1]
        val forward = if (motion.facing == "left") -1f else 1f
        val awayFromShoulders = if (torso.x < 0f) -1f else 1f
        val shin = unit(ankle - knee)
        val toe = when {
            motion.toeLift != null -> Offset(forward, -.60f * motion.toeLift.elevation(phase))
            motion.view == ExerciseView.OVERHEAD -> torso + shin * (torso.getDistance() * .22f)
            motion.authoredFloor?.isHamstringSlide == true -> Offset(awayFromShoulders,-.36f)
            motion.authoredFloor == AuthoredFloorMovement.V_UP -> shin
            motion.authoredFloor == AuthoredFloorMovement.REVERSE_CLAM -> Offset(-.85f,.35f)
            motion.authoredFloor == AuthoredFloorMovement.KICKBACK -> Offset(shin.x,.60f)
            motion.posture == ExercisePosture.SUPINE -> Offset(awayFromShoulders, 0f)
            motion.prone && motion.handSupport -> Offset(-awayFromShoulders * .30f, .95f)
            motion.prone || motion.posture == ExercisePosture.SIDE_LYING -> Offset(awayFromShoulders, .12f)
            motion.ankleTouchesFloor(ankle,first,torso.getDistance()) -> Offset(forward,0f)
            else -> Offset(forward, (shin.x * forward * .25f).coerceIn(-.20f,.20f))
        }
        return unit(motion.footGesture?.direction(toe,phase,first) ?: toe)
    }

    fun toeGather(phase: Float): Float {
        val bend = sin(Math.PI.toFloat() * phase)
        return bend * bend
    }

    /** Interpolate elbow and knee bends without shortening them when the pivot changes direction. */
    fun interpolate(a: List<Offset>, b: List<Offset>, t: Float): List<Offset> {
        if (t <= 0f || a == b) return a
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
        if (a[tip] == b[tip] && a[root] == b[root]) return
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
